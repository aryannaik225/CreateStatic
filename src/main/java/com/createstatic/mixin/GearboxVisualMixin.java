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
 * This is a one-shot: HEAD zeroes every peg's rotationalSpeed exactly once on the transition into
 * static mode and cancels the rest of update() outright (which would otherwise re-run
 * updateSourceFacing() and RotatingInstance#setup()+setChanged() for every entry in keys every tick).
 * On the way back out, clearing the frozen flag alone is enough - update() is no longer cancelled, so
 * its real body resumes computing live peg rotation itself.
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
        for (RotatingInstance rotatingModel : keys.values()) {
            if (rotatingModel != null) {
                rotatingModel.rotationalSpeed = 0f;
                rotatingModel.setChanged();
            }
        }
        createstatic$frozen = true;
    }
}
