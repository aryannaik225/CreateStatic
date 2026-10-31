package com.createstatic.mixin;

import com.createstatic.CreateStaticConfig;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogVisual;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Encased Cogwheel / Large Encased Cogwheel: `EncasedCogVisual` extends `KineticBlockEntityVisual`
 * directly (not `SingleAxisRotatingVisual`), so FlywheelVisualMixin never sees it at all - it was
 * spinning completely untouched. It drives three separate RotatingInstances every update(): the
 * casing's own `rotatingModel` (always present) plus up to two internal `rotatingTopShaft`/
 * `rotatingBottomShaft` half-shaft stubs (either may be null depending on which neighbours actually
 * have a shaft connection), all re-derived unconditionally from the live block entity on every call.
 *
 * One-shot-into-unconditional freeze, same defensive shape as LargeCogVisualMixin/SawVisualMixin:
 * zero all three instances' rotationalSpeed at construction time (covering a live cog caught
 * mid-spin on chunk load while already static) and again on every single update() call thereafter
 * (not just the first) so nothing re-derived from the block entity between calls can sneak a
 * nonzero speed back in; cancel the rest of update() outright to avoid the redundant
 * setup()/setChanged() work.
 */
@Mixin(EncasedCogVisual.class)
public abstract class EncasedCogVisualMixin {

    @Shadow(remap = false)
    @Final
    protected RotatingInstance rotatingModel;

    @Shadow(remap = false)
    @Final
    protected RotatingInstance rotatingTopShaft;

    @Shadow(remap = false)
    @Final
    protected RotatingInstance rotatingBottomShaft;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void createstatic$onInit(VisualizationContext context, KineticBlockEntity blockEntity, boolean large,
                                       float partialTick, Model model, CallbackInfo ci) {
        if (CreateStaticConfig.isTransmissionsFrozen()) {
            createstatic$freeze();
        }
    }

    @Inject(method = "update", at = @At("HEAD"), cancellable = true, remap = false)
    private void createstatic$update(float partialTicks, CallbackInfo ci) {
        if (CreateStaticConfig.isTransmissionsFrozen()) {
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
        if (rotatingTopShaft != null) {
            rotatingTopShaft.rotationalSpeed = 0f;
            rotatingTopShaft.setChanged();
        }
        if (rotatingBottomShaft != null) {
            rotatingBottomShaft.rotationalSpeed = 0f;
            rotatingBottomShaft.setChanged();
        }
    }
}
