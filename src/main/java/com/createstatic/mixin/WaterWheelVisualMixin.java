package com.createstatic.mixin;

import com.createstatic.CreateStaticConfig;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.kinetics.waterwheel.WaterWheelBlockEntity;
import com.simibubi.create.content.kinetics.waterwheel.WaterWheelVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Shared Flywheel visual for both WaterWheelBlock and LargeWaterWheelBlock (the `large` boolean and
 * the `standard`/`large` factory methods are the only difference between them, both built on this one
 * class) - one mixin covers both entries on the checklist.
 *
 * PIVOT: `rotatingModel` must stay visible and Flywheel-rendered (see FlywheelVisualMixin) rather than
 * hidden - only its rotationalSpeed needs pinning to zero so Flywheel has nothing to animate and the
 * CPU has nothing to recompute.
 *
 * This is a one-shot: HEAD zeroes rotatingModel's rotationalSpeed exactly once on the transition into
 * static mode and cancels the rest of update() outright (which would otherwise re-run
 * RotatingInstance#setup()+setChanged() every tick). On the way back out, clearing the frozen flag
 * alone is enough - update() is no longer cancelled, so its real body resumes computing live rotation
 * itself.
 */
@Mixin(WaterWheelVisual.class)
public abstract class WaterWheelVisualMixin {

    @Shadow(remap = false)
    protected RotatingInstance rotatingModel;

    @Unique
    private boolean createstatic$frozen = false;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void createstatic$onInit(VisualizationContext context, WaterWheelBlockEntity blockEntity,
                                       boolean large, float partialTick, CallbackInfo ci) {
        if (CreateStaticConfig.STATIC_MODE_ENABLED) {
            createstatic$freeze();
        }
    }

    @Inject(method = "update", at = @At("HEAD"), cancellable = true, remap = false)
    private void createstatic$updateHead(float partialTicks, CallbackInfo ci) {
        if (CreateStaticConfig.STATIC_MODE_ENABLED) {
            if (!createstatic$frozen) {
                createstatic$freeze();
            }
            ci.cancel();
        } else {
            createstatic$frozen = false;
        }
    }

    @Unique
    private void createstatic$freeze() {
        if (rotatingModel != null) {
            rotatingModel.rotationalSpeed = 0f;
            rotatingModel.setChanged();
        }
        createstatic$frozen = true;
    }
}
