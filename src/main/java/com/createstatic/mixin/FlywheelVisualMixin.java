package com.createstatic.mixin;

import com.createstatic.CreateStaticConfig;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorVisual;
import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockEntityVisual;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.BlockEntityVisual;
import dev.engine_room.flywheel.api.visual.TickableVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;

/**
 * Flywheel 1.0.5 instances bypass the normal BlockEntityRenderer path entirely, so cancelling
 * KineticBlockEntityRenderer#renderSafe (see KineticBlockEntityRendererMixin) has no effect on
 * GPU-instanced visuals - they have to be hidden here instead.
 *
 * This used to call Visual#delete() on the whole SingleAxisRotatingVisual to hide it, re-running
 * delete() guarded by a "did we already delete it" flag. That is unsafe to do repeatedly/on a
 * hot path: delete() is a one-way teardown (AbstractBlockEntityVisual's _delete() releases the
 * instance's slot back to its Instancer for good), so toggling static mode back off never actually
 * restored the shaft - update() kept running against an instance whose handle had already been
 * released. Pinning visibility via Instance#setVisible(false) instead is non-destructive, requires
 * no bookkeeping flag, and toggles cleanly in both directions.
 *
 * ChainConveyorVisual is a SingleAxisRotatingVisual subclass too (its update() calls
 * super.update() via invokespecial, which runs straight into this injected HEAD code), but unlike
 * a bare shaft/cogwheel it must NOT be hidden wholesale here: the chain conveyor's sprocket
 * geometry has no chunk-baked static model to fall back on, so ChainConveyorVisualMixin handles its
 * own shaft/package hiding separately and this mixin bails out for it immediately.
 *
 * update()'s HEAD cancel above was never the whole story for CPU cost: SingleAxisRotatingVisual
 * also implements SimpleTickableVisual, with its own tick(TickableVisual.Context) that runs every
 * game tick independently of update() - it recomputes the overstress red/green tint and calls
 * RotatingInstance#setChanged() (which dirties the instance for re-upload to the GPU) regardless of
 * whether update() ever runs. That is the real source of the "still doing matrix math every frame"
 * cost the static toggle wasn't actually fixing - cancelling update() alone left this running
 * unconditionally for every shaft/cogwheel/chain-drive instance in the world. ChainConveyorVisual
 * overrides tick() itself with its own unrelated body (advancing package visuals, see
 * ChainConveyorVisualMixin) and never calls super.tick(), so this injection naturally never reaches
 * it and doesn't need a bail-out the way update() does.
 *
 * update() used to just pin visibility and cancel every single tick without recording whether it
 * had already done so, and never touched rotationalSpeed at all - relying on setVisible(false)
 * alone to mask the still-live, still-nonzero rotation uniform. That is also what let a chain drive
 * (or any shaft/cogwheel) that was already spinning at full speed when static mode was toggled on
 * - or that loads in from a freshly-read chunk while static mode is already on, before its first
 * update() call ever runs - visibly carry on turning for a frame if anything downstream ever reads
 * the instance's rotation while visibility is momentarily inconsistent. It's now a one-shot that
 * pins both: hides AND zeroes rotationalSpeed exactly once on the transition into static mode
 * (covering a live chain drive caught mid-spin), with a matching <init>-time hook covering the
 * chunk-load-while-already-static case the original code never addressed at all.
 */
@Mixin(SingleAxisRotatingVisual.class)
public abstract class FlywheelVisualMixin {

    @Unique
    private boolean createstatic$frozen = false;

    @Inject(method = "<init>(Ldev/engine_room/flywheel/api/visualization/VisualizationContext;Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;FLnet/minecraft/core/Direction;Ldev/engine_room/flywheel/api/model/Model;)V",
            at = @At("RETURN"), remap = false)
    private void createstatic$onInit(VisualizationContext context, KineticBlockEntity blockEntity, float partialTick,
                                       Direction direction, Model model, CallbackInfo ci) {
        if (CreateStaticConfig.STATIC_MODE_ENABLED && !((Object) this instanceof ChainConveyorVisual)) {
            createstatic$freeze();
        }
    }

    @Inject(method = "update", at = @At("HEAD"), cancellable = true, remap = false)
    private void createstatic$update(float partialTicks, CallbackInfo ci) {
        if ((Object) this instanceof ChainConveyorVisual) {
            return;
        }

        if (CreateStaticConfig.STATIC_MODE_ENABLED) {
            if (!createstatic$frozen) {
                createstatic$freeze();
            }
            ci.cancel();
        } else if (createstatic$frozen) {
            RotatingInstance rotatingModel = ((SingleAxisRotatingVisualAccessor) (Object) this).createstatic$getRotatingModel();
            if (rotatingModel != null) {
                rotatingModel.setVisible(true);
            }
            createstatic$frozen = false;
        }
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, remap = false)
    private void createstatic$tick(TickableVisual.Context context, CallbackInfo ci) {
        if (CreateStaticConfig.STATIC_MODE_ENABLED) {
            ci.cancel();
        }
    }

    @Unique
    private void createstatic$freeze() {
        RotatingInstance rotatingModel = ((SingleAxisRotatingVisualAccessor) (Object) this).createstatic$getRotatingModel();
        if (rotatingModel != null) {
            rotatingModel.setVisible(false);
            rotatingModel.rotationalSpeed = 0f;
            rotatingModel.setChanged();
        }
        createstatic$frozen = true;
    }
}

/**
 * create() is a static factory that assembles the bracketed-shaft/cog visual composite out of
 * multiple internal Flywheel instances whose types aren't exposed here, so there's no single
 * instance field to pin invisible after the fact the way plain shafts/cogs are handled above.
 * Building the composite and then immediately deleting it was wasted allocation on every chunk
 * load/rebake while static mode is on, and still risked whatever delete() does internally in
 * flywheel-lib running against freshly-constructed (never-rendered) instances. Intercepting at
 * HEAD instead means the composite, and all the GPU instances it would have allocated, are never
 * created in the first place - zero allocation, zero delete() calls, nothing to leak.
 */
@Mixin(BracketedKineticBlockEntityVisual.class)
abstract class BracketedKineticBlockEntityVisualMixin {

    @Unique
    private static final BlockEntityVisual<BracketedKineticBlockEntity> createstatic$NOOP = new BlockEntityVisual<>() {
        @Override
        public void update(float partialTicks) {
        }

        @Override
        public void delete() {
        }

        @Override
        public void collectCrumblingInstances(Consumer<Instance> consumer) {
        }
    };

    @Inject(method = "create", at = @At("HEAD"), cancellable = true, remap = false)
    private static void createstatic$create(VisualizationContext context, BracketedKineticBlockEntity blockEntity,
                                              float partialTicks,
                                              CallbackInfoReturnable<BlockEntityVisual<BracketedKineticBlockEntity>> cir) {
        if (CreateStaticConfig.STATIC_MODE_ENABLED) {
            cir.setReturnValue(createstatic$NOOP);
        }
    }
}
