package com.createstatic.mixin;

import com.createstatic.CreateStaticConfig;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorVisual;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.TickableVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PIVOT: forcing getRenderShape() to MODEL and hiding these Flywheel instances (the previous
 * approach) took every shaft/cogwheel/chain-drive off Flywheel's GPU instancing and dumped them onto
 * the vanilla chunk builder instead - that's what made Indigo/the culling mod spend ~29ms/frame doing
 * CPU frustum culling against 10,000 baked-in-chunk kinetic models. Flywheel's own GPU-side instance
 * culling is what we actually want handling that, so the blocks stay ENTITYBLOCK_ANIMATED and these
 * visuals stay visible and Flywheel-rendered - only their rotation is pinned to zero so there's
 * nothing for the GPU to animate and nothing for the CPU to recompute.
 *
 * update()'s HEAD cancel stops SingleAxisRotatingVisual#update() from re-running RotatingInstance
 * setup()/matrix math every tick for every shaft/cogwheel/chain-drive in the world - that CPU cost
 * exists independently of whether the instance is visible. ChainConveyorVisual is a
 * SingleAxisRotatingVisual subclass too (its update() calls super.update() via invokespecial, which
 * runs straight into this injected HEAD code), but it must NOT be frozen generically here: the chain
 * conveyor's sprocket needs its own handling alongside the package visuals it also drives, so
 * ChainConveyorVisualMixin owns its freeze separately and this mixin bails out for it immediately.
 *
 * tick() is SimpleTickableVisual's separate per-game-tick entry point, independent of update(): it
 * recomputes the overstress red/green tint and calls RotatingInstance#setChanged() (dirtying the
 * instance for re-upload to the GPU) regardless of whether update() ever ran. Cancelling it removes
 * that per-tick CPU cost too. ChainConveyorVisual overrides tick() itself with an unrelated body
 * (advancing package visuals, see ChainConveyorVisualMixin) and never calls super.tick(), so this
 * injection naturally never reaches it.
 *
 * Freezing is a one-shot: HEAD zeroes rotationalSpeed exactly once on the transition into static
 * mode (covering a live chain drive caught mid-spin) and cancels the rest of update() outright, with
 * a matching <init>-time hook covering the chunk-load-while-already-static case. On the way back out,
 * clearing the frozen flag alone is enough - update() is no longer cancelled, so its real body runs
 * again and recomputes live rotationalSpeed itself.
 */
@Mixin(SingleAxisRotatingVisual.class)
public abstract class FlywheelVisualMixin {

    @Unique
    private boolean createstatic$frozen = false;

    @Inject(method = "<init>(Ldev/engine_room/flywheel/api/visualization/VisualizationContext;Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;FLnet/minecraft/core/Direction;Ldev/engine_room/flywheel/api/model/Model;)V",
            at = @At("RETURN"), remap = false)
    private void createstatic$onInit(VisualizationContext context, KineticBlockEntity blockEntity, float partialTick,
                                       Direction direction, Model model, CallbackInfo ci) {
        if (CreateStaticConfig.isRelaysFrozen() && !((Object) this instanceof ChainConveyorVisual)) {
            createstatic$freeze();
        }
    }

    @Inject(method = "update", at = @At("HEAD"), cancellable = true, remap = false)
    private void createstatic$update(float partialTicks, CallbackInfo ci) {
        if ((Object) this instanceof ChainConveyorVisual) {
            return;
        }

        if (CreateStaticConfig.isRelaysFrozen()) {
            if (!createstatic$frozen) {
                createstatic$freeze();
            }
            ci.cancel();
        } else {
            createstatic$frozen = false;
        }
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, remap = false)
    private void createstatic$tick(TickableVisual.Context context, CallbackInfo ci) {
        if (CreateStaticConfig.isRelaysFrozen()) {
            ci.cancel();
        }
    }

    @Unique
    private void createstatic$freeze() {
        RotatingInstance rotatingModel = ((SingleAxisRotatingVisualAccessor) (Object) this).createstatic$getRotatingModel();
        if (rotatingModel != null) {
            rotatingModel.rotationalSpeed = 0f;
            rotatingModel.setChanged();
        }
        createstatic$frozen = true;
    }
}
