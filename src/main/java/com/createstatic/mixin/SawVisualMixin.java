package com.createstatic.mixin;

import com.createstatic.CreateStaticConfig;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import com.simibubi.create.content.kinetics.saw.SawVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * SawVisual extends KineticBlockEntityVisual directly (not SingleAxisRotatingVisual), declaring its
 * own `rotatingModel` (the internal shaft stub) and its own update(float) - so FlywheelVisualMixin's
 * injection into SingleAxisRotatingVisual#update() never reaches it, same situation as
 * GearboxVisual/SplitShaftVisual. The blade itself is drawn by SawRenderer (a plain BER, not a
 * Flywheel visual) and doesn't spin on its own - only this shaft stub needs freezing.
 *
 * BUG: hovering the crosshair over a placed saw respun it. Create's goggle-overlay lookup triggers a
 * block entity resync, which calls back into this update() via Flywheel's instance manager - but the
 * old implementation only (re)applied the zero on the FIRST call after transitioning into static
 * mode (gated behind a `frozen` flag) and simply cancelled every call after that without touching
 * rotatingModel again. Any such re-entry that manages to run RotatingInstance#setup() on
 * rotatingModel before this injection's HEAD fires - or any other external code path that pokes the
 * field directly - would leave a nonzero rotationalSpeed sitting there uncorrected, since the
 * `frozen == true` branch no longer re-zeroed it.
 *
 * Fixed shape: the freeze is now unconditional on every single update() call while static mode is
 * on, not just the first - rotatingModel's rotationalSpeed gets stomped back to 0 every time,
 * regardless of what nudged it in between, before cancelling the rest of the method. Same
 * unconditional-every-call shape as LargeCogVisualMixin/EncasedCogVisualMixin.
 */
@Mixin(SawVisual.class)
public abstract class SawVisualMixin {

    @Shadow(remap = false)
    @Final
    protected RotatingInstance rotatingModel;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void createstatic$onInit(VisualizationContext context, SawBlockEntity blockEntity,
                                       float partialTick, CallbackInfo ci) {
        if (CreateStaticConfig.STATIC_MODE_ENABLED) {
            createstatic$freeze();
        }
    }

    @Inject(method = "update", at = @At("HEAD"), cancellable = true, remap = false)
    private void createstatic$update(float partialTicks, CallbackInfo ci) {
        if (CreateStaticConfig.STATIC_MODE_ENABLED) {
            createstatic$freeze();
            ci.cancel();
        }
    }

    @Unique
    private void createstatic$freeze() {
        if (rotatingModel != null) {
            rotatingModel.rotationalSpeed = 0f;
            rotatingModel.setChanged();
        }
    }
}
