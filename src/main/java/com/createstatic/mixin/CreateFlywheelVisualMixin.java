package com.createstatic.mixin;

import com.createstatic.CreateStaticConfig;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.kinetics.flywheel.FlywheelBlockEntity;
import com.simibubi.create.content.kinetics.flywheel.FlywheelVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Create's physical "Flywheel" machine block wheel visual (distinct from the Flywheel rendering
 * engine's own FlywheelVisualMixin, which targets dev.engine_room.flywheel's SingleAxisRotatingVisual
 * - naming collision only, see FlywheelBlockEntityMixin). FlywheelVisual extends
 * KineticBlockEntityVisual directly (not SingleAxisRotatingVisual), declaring its own `shaft`
 * RotatingInstance (the connecting shaft stub) and its own update(float) - so the generic
 * FlywheelVisualMixin never reaches it, same situation as Saw/Fan/Gearbox. The wheel itself
 * (`wheel`, a TransformedInstance) is repositioned every frame in beginFrame()/animate() purely from
 * the block entity's `angle` field, which FlywheelBlockEntityMixin freezes by cancelling tick() - no
 * separate handling needed for it here.
 *
 * `shaft` is frozen unconditionally on every update() call (same shape as SawVisualMixin) to avoid
 * the goggle-overlay resync respin bug documented there.
 */
@Mixin(FlywheelVisual.class)
public abstract class CreateFlywheelVisualMixin {

    @Shadow(remap = false)
    @Final
    protected RotatingInstance shaft;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void createstatic$onInit(VisualizationContext context, FlywheelBlockEntity blockEntity,
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
        if (shaft != null) {
            shaft.rotationalSpeed = 0f;
            shaft.setChanged();
        }
    }
}
