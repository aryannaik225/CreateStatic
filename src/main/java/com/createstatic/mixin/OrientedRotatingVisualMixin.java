package com.createstatic.mixin;

import com.createstatic.CreateStaticConfig;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.OrientedRotatingVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * `BearingVisual` (WindmillBearingBlock's pivot shaft, shared with Mechanical/Clockwork Bearing) does
 * not override update() itself - it inherits this base, which also backs GantryShaftBlockEntity and
 * any other block built on OrientedRotatingVisual#of()/backHorizontal(). Freezing here therefore
 * covers the windmill bearing's own `rotatingModel` pivot shaft for free, the same way freezing
 * SingleAxisRotatingVisual (FlywheelVisualMixin) covers every one of its subclasses.
 *
 * This mixin intentionally does NOT touch BearingVisual's separate `topInstance` (the arm/cap that
 * tilts with the moving contraption) - that field has no rotationalSpeed to pin, it is re-oriented
 * every frame in beginFrame() from the block entity's live angle, and once WindmillBearingBlockEntity's
 * tick() is cancelled (see WindmillBearingBlockEntityMixin) that angle stops changing on its own, so
 * beginFrame() naturally keeps recomputing the same frozen orientation.
 *
 * One-shot freeze, same shape as every other Visual mixin here: HEAD zeroes rotationalSpeed exactly
 * once on the transition into static mode and cancels the rest of update(); clearing the frozen flag
 * on the way out is enough for update() to resume computing live rotation itself.
 */
@Mixin(OrientedRotatingVisual.class)
public abstract class OrientedRotatingVisualMixin {

    @Shadow(remap = false)
    @Final
    protected RotatingInstance rotatingModel;

    @Unique
    private boolean createstatic$frozen = false;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void createstatic$onInit(VisualizationContext context, KineticBlockEntity blockEntity, float partialTick,
                                       Direction direction1, Direction direction2, Model model, CallbackInfo ci) {
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
