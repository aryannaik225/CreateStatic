package com.createstatic.mixin;

import com.createstatic.CreateStaticConfig;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.kinetics.gearbox.GearboxBlockEntity;
import com.simibubi.create.content.kinetics.gearbox.GearboxVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.EnumMap;

/**
 * PIVOT: these pegs must stay visible and Flywheel-rendered (see FlywheelVisualMixin) rather than
 * hidden - hiding them was only ever a stand-in for the GPU rendering we're now keeping. `keys` holds
 * up to two RotatingInstance pegs (one per perpendicular axis actually connected to a neighbour); as
 * with the shaft/cogwheel/chain-drive instances, only their rotationalSpeed needs pinning to zero so
 * Flywheel has nothing to animate and the CPU has nothing to recompute.
 *
 * FIXED (was a one-shot): the original approach cancelled update() outright while frozen, which also
 * skipped `updateSourceFacing()` forever. `sourceFacing` is what `getSpeed(Direction)` uses to flip
 * the sign of each perpendicular peg's speed relative to the source side; with it stuck null/stale
 * (e.g. frozen before a neighbour ever attached, or a neighbour changed while frozen) `getSpeed()`
 * falls back to returning the gearbox's raw network speed unmodified for every direction instead of
 * alternating sign per output - visually every peg spins the same way, looking like shafts are
 * "active"/connected on sides that never had a neighbour. `updateSourceFacing()` is two field reads
 * and a Direction lookup - cheap - so it now runs unconditionally every tick regardless of frozen
 * state; only the actually expensive part (the per-key RotatingInstance#setup()+setChanged() loop)
 * is skipped by keeping every peg pinned to rotationalSpeed=0 instead.
 */
@Mixin(GearboxVisual.class)
public abstract class GearboxVisualMixin {

    @Shadow(remap = false)
    @Final
    protected EnumMap<Direction, RotatingInstance> keys;

    @Shadow(remap = false)
    protected abstract void updateSourceFacing();

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void createstatic$onInit(VisualizationContext context, GearboxBlockEntity blockEntity,
                                       float partialTick, CallbackInfo ci) {
        if (CreateStaticConfig.isTransmissionsFrozen()) {
            createstatic$freeze();
        }
    }

    @Inject(method = "update", at = @At("HEAD"), cancellable = true, remap = false)
    private void createstatic$updateHead(float partialTicks, CallbackInfo ci) {
        updateSourceFacing();

        if (CreateStaticConfig.isTransmissionsFrozen()) {
            createstatic$freeze();
            ci.cancel();
        }
    }

    @Unique
    private void createstatic$freeze() {
        for (RotatingInstance rotatingModel : keys.values()) {
            if (rotatingModel != null) {
                rotatingModel.rotationalSpeed = 0f;
                rotatingModel.setChanged();
            }
        }
    }
}
