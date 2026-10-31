package com.createstatic.mixin;

import com.createstatic.CreateStaticConfig;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockEntityVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Large Cogwheel: `BracketedKineticBlockEntityVisual.create()` builds the private
 * `LargeCogVisual` inner class (a `SingleAxisRotatingVisual` built from SHAFTLESS_LARGE_COGWHEEL)
 * for `ICogWheel.isLargeCog()` states. Its outer `rotatingModel` is frozen for free by
 * FlywheelVisualMixin (constructor + update() HEAD cancel), but `LargeCogVisual` overrides
 * update(float) itself: it calls super.update() (where FlywheelVisualMixin's injection lives) and
 * then, completely unconditionally, re-runs `additionalShaft.setup(blockEntity)` - this re-derives
 * the center shaft's rotationalSpeed straight from the live block entity every single call,
 * regardless of whatever FlywheelVisualMixin did to the outer cog. That's the "inner shaft still
 * spins" bug: the outer cog's one-shot freeze never touches this separate instance.
 *
 * Cancelling this override entirely at HEAD when static (rather than only skipping the
 * additionalShaft tail) is safe: the outer `rotatingModel` already gets its own one-shot zero via
 * FlywheelVisualMixin's own constructor-time `<init>` hook, independent of whether this update()
 * body ever runs again, so nothing is lost by skipping the super.update() call here too. The freeze
 * is re-applied unconditionally on every single call (not gated behind a "first time only" check)
 * so a stray external re-corruption of `additionalShaft.rotationalSpeed` (e.g. anything that calls
 * RotatingInstance#setup() on it directly) gets stomped back to zero the very next frame rather than
 * being missed because a `frozen` flag already read true - same defensive shape now also applied to
 * SawVisualMixin after the Mechanical Saw hover-respin bug.
 */
@Mixin(BracketedKineticBlockEntityVisual.LargeCogVisual.class)
public abstract class LargeCogVisualMixin {

    @Shadow(remap = false)
    @Final
    protected RotatingInstance additionalShaft;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void createstatic$onInit(VisualizationContext context, BracketedKineticBlockEntity blockEntity,
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
        if (additionalShaft != null) {
            additionalShaft.rotationalSpeed = 0f;
            additionalShaft.setChanged();
        }
    }
}
