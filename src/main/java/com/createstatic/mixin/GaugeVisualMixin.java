package com.createstatic.mixin;

import com.createstatic.CreateStaticConfig;
import com.simibubi.create.content.kinetics.gauge.GaugeVisual;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * GaugeVisual extends ShaftVisual (a SingleAxisRotatingVisual subclass that doesn't override update()
 * itself), so the connecting shaft stub is already frozen for free by the generic FlywheelVisualMixin.
 * The needle faces (`faces`, a list of DialFace instances) are different: beginFrame() reorients them
 * every render frame from GaugeBlockEntity's dialState/prevDialState lerp across the last tick
 * boundary (IHaveGoggleInformation-style interpolation) - same shape as BearingVisual's topInstance.
 * Even with GaugeBlockEntityMixin cancelling tick() (freezing dialState), prevDialState and dialState
 * aren't guaranteed equal at the instant static mode is toggled mid-sweep, and re-interpolating that
 * gap every frame against a live partialTick would produce a visible needle twitch instead of a clean
 * stop.
 *
 * Fix: same one-shot pattern as BearingVisualMixin - let beginFrame() run normally exactly once after
 * the transition into static mode (capturing wherever the interpolated needle position actually is),
 * then cancel every call after that. Clearing the frozen flag on the way out resumes live
 * interpolation immediately.
 */
@Mixin(GaugeVisual.class)
public abstract class GaugeVisualMixin {

    @Unique
    private boolean createstatic$frozen = false;

    @Inject(method = "beginFrame", at = @At("HEAD"), cancellable = true, remap = false)
    private void createstatic$beginFrame(DynamicVisual.Context context, CallbackInfo ci) {
        if (CreateStaticConfig.STATIC_MODE_ENABLED) {
            if (createstatic$frozen) {
                ci.cancel();
            } else {
                createstatic$frozen = true;
            }
        } else {
            createstatic$frozen = false;
        }
    }
}
