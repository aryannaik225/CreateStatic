package com.createstatic.mixin;

import com.createstatic.CreateStaticConfig;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.kinetics.crank.HandCrankBlockEntity;
import com.simibubi.create.content.kinetics.crank.HandCrankVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hand Crank / Valve Handle are "Static Until In Use" (HandCrankBlockEntity and
 * ValveHandleBlockEntity are both listed in CreateStaticConfig#STATIC_UNTIL_IN_USE_CLASS_NAMES): the
 * crank handle (`crank`, a TransformedInstance) must keep swinging live via beginFrame()/rotateCrank()
 * while the block entity's independentAngle is actively advancing during use, so its tick() is
 * exempted from the generic freeze - cancelling it would make right-clicking the crank do nothing
 * visually.
 *
 * `rotatingModel` is different: it's the generic connecting shaft stub underneath, which spins
 * continuously off the kinetic network's live speed (shouldRenderShaft()) regardless of whether the
 * crank is actively being turned - that part should stay frozen like every other shaft in the mod.
 * HandCrankVisual extends KineticBlockEntityVisual directly (not SingleAxisRotatingVisual) with its
 * own update(float), so the generic FlywheelVisualMixin never reaches it. beginFrame()/rotateCrank()
 * are left completely untouched so the handle animation keeps working.
 *
 * ValveHandleVisual (used for ValveHandleBlockEntity, a HandCrankBlockEntity subclass) has no
 * RotatingInstance field of its own - shouldRenderShaft() returns false for valve handles, so there's
 * no generic shaft stub to freeze there, and no mixin is needed for it.
 */
@Mixin(HandCrankVisual.class)
public abstract class HandCrankVisualMixin {

    @Shadow(remap = false)
    @Final
    private RotatingInstance rotatingModel;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void createstatic$onInit(VisualizationContext context, HandCrankBlockEntity blockEntity,
                                       float partialTick, CallbackInfo ci) {
        if (CreateStaticConfig.isMiscFrozen()) {
            createstatic$freeze();
        }
    }

    @Inject(method = "update", at = @At("HEAD"), cancellable = true, remap = false)
    private void createstatic$update(float partialTicks, CallbackInfo ci) {
        if (CreateStaticConfig.isMiscFrozen()) {
            createstatic$freeze();
            ci.cancel();
        }
    }

    @Unique
    private void createstatic$freeze() {
        if (rotatingModel != null) {
            rotatingModel.rotationalSpeed = 0f;
            rotatingModel.setChanged();
        }
    }
}
