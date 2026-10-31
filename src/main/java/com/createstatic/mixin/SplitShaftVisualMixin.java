package com.createstatic.mixin;

import com.createstatic.CreateStaticConfig;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import com.simibubi.create.content.kinetics.transmission.SplitShaftVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

/**
 * Shared Flywheel visual for Clutch, Gearshift and SequencedGearshift (all three BlockEntities
 * extend SplitShaftBlockEntity and are visualized by this one class).
 *
 * PIVOT: the two internal half-shaft RotatingInstances in `keys` must stay visible and
 * Flywheel-rendered (see FlywheelVisualMixin) rather than hidden - only their rotationalSpeed needs
 * pinning to zero so Flywheel has nothing to animate and the CPU has nothing to recompute.
 *
 * This is a one-shot: HEAD zeroes both half-shaft stubs' rotationalSpeed exactly once on the
 * transition into static mode and cancels the rest of update() outright (which would otherwise
 * re-run RotatingInstance#setup()+setChanged() for every entry in keys every tick). On the way back
 * out, clearing the frozen flag alone is enough - update() is no longer cancelled, so its real body
 * resumes computing live rotation itself.
 */
@Mixin(SplitShaftVisual.class)
public abstract class SplitShaftVisualMixin {

    @Shadow(remap = false)
    @Final
    protected ArrayList<RotatingInstance> keys;

    @Unique
    private boolean createstatic$frozen = false;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void createstatic$onInit(VisualizationContext context, SplitShaftBlockEntity blockEntity,
                                       float partialTick, CallbackInfo ci) {
        if (CreateStaticConfig.isTransmissionsFrozen()) {
            createstatic$freeze();
        }
    }

    @Inject(method = "update", at = @At("HEAD"), cancellable = true, remap = false)
    private void createstatic$updateHead(float partialTicks, CallbackInfo ci) {
        if (CreateStaticConfig.isTransmissionsFrozen()) {
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
        for (RotatingInstance rotatingModel : keys) {
            if (rotatingModel != null) {
                rotatingModel.rotationalSpeed = 0f;
                rotatingModel.setChanged();
            }
        }
        createstatic$frozen = true;
    }
}
