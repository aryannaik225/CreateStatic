package com.createstatic.mixin;

import com.createstatic.CreateStaticConfig;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import com.simibubi.create.content.kinetics.fan.FanVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * FanVisual extends KineticBlockEntityVisual directly (not SingleAxisRotatingVisual), declaring its
 * own `shaft` (the connecting shaft stub) and `fan` (the visible spinning rotor) RotatingInstances and
 * its own update(float) - so FlywheelVisualMixin's injection into SingleAxisRotatingVisual#update()
 * never reaches it, same situation as Saw/Gearbox/SplitShaft. Both instances need pinning to zero;
 * unconditional on every update() call (same shape as SawVisualMixin) to avoid the goggle-overlay
 * resync respin bug documented there.
 *
 * EncasedFanBlockEntity is listed in CreateStaticConfig#STATIC_UNTIL_IN_USE_CLASS_NAMES - its tick()
 * now runs completely normally on the client (AirCurrent must keep recomputing every tick for
 * client-side push prediction to stay in sync with the server, see the postmortem on that constant).
 * That exemption is orthogonal to this mixin: nothing in EncasedFanBlockEntity#tick() touches the
 * Flywheel RotatingInstances directly, so the rotor/shaft stay pinned to zero here unconditionally
 * regardless of whether the block entity itself is ticking.
 */
@Mixin(FanVisual.class)
public abstract class FanVisualMixin {

    @Shadow(remap = false)
    @Final
    protected RotatingInstance shaft;

    @Shadow(remap = false)
    @Final
    protected RotatingInstance fan;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void createstatic$onInit(VisualizationContext context, EncasedFanBlockEntity blockEntity,
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
        if (shaft != null) {
            shaft.rotationalSpeed = 0f;
            shaft.setChanged();
        }
        if (fan != null) {
            fan.rotationalSpeed = 0f;
            fan.setChanged();
        }
    }
}
