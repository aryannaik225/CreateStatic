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
 * GearboxVisual extends the generic KineticBlockEntityVisual rather than SingleAxisRotatingVisual,
 * so it never passes through FlywheelVisualMixin - it has to be handled on its own. `keys` holds up
 * to two RotatingInstance pegs (one per perpendicular axis actually connected to a neighbour).
 *
 * This used to re-apply visibility at update()'s RETURN every tick, which meant update()'s real
 * body - re-running updateSourceFacing() and RotatingInstance#setup()+setChanged() for every entry
 * in keys - still ran to completion first, unconditionally, static mode or not. It's now a one-shot:
 * HEAD hides every peg exactly once on the transition into static mode and cancels the rest of
 * update() outright, and un-hides once on the way back out so the real body resumes computing live
 * peg rotation.
 */
@Mixin(GearboxVisual.class)
public abstract class GearboxVisualMixin {

    @Shadow(remap = false)
    @Final
    protected EnumMap<Direction, RotatingInstance> keys;

    @Unique
    private boolean createstatic$frozen = false;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void createstatic$onInit(VisualizationContext context, GearboxBlockEntity blockEntity,
                                       float partialTick, CallbackInfo ci) {
        if (CreateStaticConfig.STATIC_MODE_ENABLED) {
            createstatic$applyVisibility(false);
            createstatic$frozen = true;
        }
    }

    @Inject(method = "update", at = @At("HEAD"), cancellable = true, remap = false)
    private void createstatic$updateHead(float partialTicks, CallbackInfo ci) {
        if (CreateStaticConfig.STATIC_MODE_ENABLED) {
            if (!createstatic$frozen) {
                createstatic$applyVisibility(false);
                createstatic$frozen = true;
            }
            ci.cancel();
        } else if (createstatic$frozen) {
            createstatic$applyVisibility(true);
            createstatic$frozen = false;
        }
    }

    @Unique
    private void createstatic$applyVisibility(boolean visible) {
        for (RotatingInstance rotatingModel : keys.values()) {
            rotatingModel.setVisible(visible);
        }
    }
}
