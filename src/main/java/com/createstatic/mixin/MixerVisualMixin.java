package com.createstatic.mixin;

import com.createstatic.CreateStaticConfig;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.kinetics.mixer.MixerVisual;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * "Static Until In Use" machine (see CreateStaticConfig#isStaticUntilInUseMachine): the mixer's
 * upper cogwheel (the inherited `rotatingModel`, built from SHAFTLESS_COGWHEEL by the
 * SingleAxisRotatingVisual base) stays frozen forever, for free, via FlywheelVisualMixin - that part
 * never changes regardless of static mode.
 *
 * The whisk (`mixerHead`/`mixerPole`) is different: it's a genuine working part that needs to bob
 * and spin while the mixer is actually processing (MechanicalMixerBlockEntityMixin no longer exists
 * - its tick() is now exempted from the generic freeze, so `running`/`runningTicks` advance normally
 * and MixerVisual#beginFrame()/animate() is left completely untouched to position pressHead-style
 * every render frame exactly as vanilla does). The only thing static mode still needs to guarantee
 * is the *idle* look: MechanicalMixerBlockEntity#getRenderedHeadRotationSpeed() returns
 * getSpeed()/2 even while `running` is false (vanilla's always-on slow idle creep), which the
 * "stay static forever" baseline doesn't want.
 *
 * Injecting at the TAIL of animate(float) (after transformPole/transformHead have already run and
 * positioned everything live) lets the real per-frame positioning happen unconditionally, then
 * stomps mixerHead's rotationalSpeed back to 0 only when idle and static mode is on - spinning
 * freely the instant `running` flips true, and settling back to a dead stop the instant it flips
 * false again.
 */
@Mixin(MixerVisual.class)
public abstract class MixerVisualMixin {

    @Shadow(remap = false)
    @Final
    private RotatingInstance mixerHead;

    @Shadow(remap = false)
    @Final
    private MechanicalMixerBlockEntity mixer;

    @Inject(method = "animate", at = @At("RETURN"), remap = false)
    private void createstatic$afterAnimate(float partialTicks, CallbackInfo ci) {
        if (CreateStaticConfig.isProcessingFrozen() && mixer != null && !mixer.running && mixerHead != null) {
            mixerHead.rotationalSpeed = 0f;
            mixerHead.setChanged();
        }
    }
}
