package com.createstatic.mixin;

import com.createstatic.CreateStaticConfig;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorVisual;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.visual.util.SmartRecycler;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * rotatingModel (the CHAIN_CONVEYOR_SHAFT sprocket/shaft piece) is inherited from
 * SingleAxisRotatingVisual and is GPU-animated the same way the belt pulley is - its speed
 * uniform has to be pinned to zero rather than deleted, since the guards/wheel geometry around it
 * must stay visible ("leave the static visual of the physical chain intact"). It's reached via
 * SingleAxisRotatingVisualAccessor rather than @Shadow since the field is only declared on the
 * superclass's own bytecode, not on ChainConveyorVisual. The guards List itself holds plain
 * (non-rotating) TransformedInstances built fresh by setupGuards() on every update(), so it needs
 * no special handling here.
 *
 * boxes/rigging are the cardboard-package and rigging-hook SmartRecyclers. beginFrame() normally
 * walks loopingPackages/travellingPackages and calls setupBoxVisual() per package, which pulls a
 * live instance out of each recycler via get() before discardExtra() removes anything that wasn't
 * touched this frame. Replacing the whole method with resetCount()+discardExtra() (no get() calls
 * in between) makes every previously-active box/rigging instance look unused this frame, so the
 * recyclers discard them - hiding the moving packages without deleting rotatingModel or the
 * guards.
 */
@Mixin(ChainConveyorVisual.class)
public abstract class ChainConveyorVisualMixin {

    @Shadow(remap = false)
    @Final
    private SmartRecycler<ResourceLocation, TransformedInstance> boxes;

    @Shadow(remap = false)
    @Final
    private SmartRecycler<ResourceLocation, TransformedInstance> rigging;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void createstatic$freezeShaftOnInit(VisualizationContext context, ChainConveyorBlockEntity blockEntity,
                                                  float partialTick, CallbackInfo ci) {
        createstatic$freezeShaft();
    }

    @Inject(method = "update", at = @At("RETURN"), remap = false)
    private void createstatic$freezeShaftOnUpdate(float partialTicks, CallbackInfo ci) {
        createstatic$freezeShaft();
    }

    @Inject(method = "beginFrame", at = @At("HEAD"), cancellable = true, remap = false)
    private void createstatic$hidePackages(DynamicVisual.Context context, CallbackInfo ci) {
        if (!CreateStaticConfig.STATIC_MODE_ENABLED) {
            return;
        }
        boxes.resetCount();
        rigging.resetCount();
        boxes.discardExtra();
        rigging.discardExtra();
        ci.cancel();
    }

    @Unique
    private void createstatic$freezeShaft() {
        if (!CreateStaticConfig.STATIC_MODE_ENABLED) {
            return;
        }
        RotatingInstance rotatingModel = ((SingleAxisRotatingVisualAccessor) (Object) this).createstatic$getRotatingModel();
        if (rotatingModel == null) {
            return;
        }
        rotatingModel.rotationalSpeed = 0f;
        rotatingModel.setChanged();
    }
}
