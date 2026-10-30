package com.createstatic.mixin;

import com.createstatic.CreateStaticConfig;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockEntityVisual;
import dev.engine_room.flywheel.api.visual.BlockEntityVisual;
import dev.engine_room.flywheel.api.visual.Visual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Flywheel 1.0.5 instances bypass the normal BlockEntityRenderer path entirely, so cancelling
 * KineticBlockEntityRenderer#renderSafe (see KineticBlockEntityRendererMixin) has no effect on
 * GPU-instanced visuals - they have to be torn down here instead. delete()/_delete() follow
 * flywheel-lib's AbstractBlockEntityVisual template-method pair (delete() does bookkeeping and
 * calls the _delete() override we see on SingleAxisRotatingVisual), so delete() is safe to call
 * repeatedly - the createstatic$deleted guard just avoids doing it every single frame.
 */
@Mixin(SingleAxisRotatingVisual.class)
public abstract class FlywheelVisualMixin {

    @Unique
    private boolean createstatic$deleted = false;

    @Inject(method = "update", at = @At("HEAD"), cancellable = true, remap = false)
    private void createstatic$update(float partialTicks, CallbackInfo ci) {
        if (!CreateStaticConfig.STATIC_MODE_ENABLED) {
            createstatic$deleted = false;
            return;
        }

        if (!createstatic$deleted) {
            createstatic$deleted = true;
            ((Visual) (Object) this).delete();
        }

        ci.cancel();
    }
}

/**
 * create() is a static factory that assembles the bracketed-shaft/cog visual composite; the
 * concrete visual type it returns isn't exposed, so the only safe interception point is the
 * returned BlockEntityVisual itself - delete it immediately so it never spawns renderable
 * instances while static mode is on.
 */
@Mixin(BracketedKineticBlockEntityVisual.class)
abstract class BracketedKineticBlockEntityVisualMixin {

    @Inject(method = "create", at = @At("RETURN"), cancellable = true, remap = false)
    private static void createstatic$create(VisualizationContext context, BracketedKineticBlockEntity blockEntity,
                                              float partialTicks,
                                              CallbackInfoReturnable<BlockEntityVisual<BracketedKineticBlockEntity>> cir) {
        if (!CreateStaticConfig.STATIC_MODE_ENABLED) {
            return;
        }

        BlockEntityVisual<BracketedKineticBlockEntity> visual = cir.getReturnValue();
        if (visual != null) {
            visual.delete();
        }
    }
}
