package com.createstatic.mixin;

import com.createstatic.CreateStaticConfig;
import com.simibubi.create.content.contraptions.bearing.BearingVisual;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Windmill/Mechanical Bearing: BearingVisual's own pivot shaft (`rotatingModel`, inherited from
 * OrientedRotatingVisual) is already frozen for free by OrientedRotatingVisualMixin, and
 * WindmillBearingBlockEntityMixin cancels the block entity's tick() so its live angle stops
 * advancing. But the top plate's separate `topInstance` is reoriented every render frame in
 * beginFrame() from `IBearingBlockEntity.getInterpolatedAngle(partialTick - 1)` - a lerp between
 * the block entity's previous/current angle across the last tick boundary. Even with tick()
 * cancelled, that previous/current pair isn't necessarily equal at the exact moment static mode is
 * toggled (mid-rotation), and any further server resync packet nudging "current" independently of
 * the frozen client tick re-opens a gap between the two - beginFrame() re-interpolates across that
 * gap every frame using a live partialTick that still cycles 0..1 every tick, producing the
 * observed twitch rather than a clean stop.
 *
 * Fix: let beginFrame() run normally exactly once after the transition into static mode (capturing
 * wherever the interpolated angle actually is at that instant, as the baseline's "freeze in place,
 * not reset" rule requires), then cancel every call after that - topInstance's rotation/setChanged()
 * from that single call is the last transform it will ever receive, so it stays visible and
 * completely still. Clearing the frozen flag on the way out resumes live interpolation immediately.
 */
@Mixin(BearingVisual.class)
public abstract class BearingVisualMixin {

    @Unique
    private boolean createstatic$frozen = false;

    @Inject(method = "beginFrame", at = @At("HEAD"), cancellable = true, remap = false)
    private void createstatic$beginFrame(DynamicVisual.Context context, CallbackInfo ci) {
        if (CreateStaticConfig.isMiscFrozen()) {
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
