package com.createstatic.mixin;

import com.createstatic.CreateStaticConfig;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorVisual;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.TickableVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.visual.util.InstanceRecycler;
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
 * must stay visible. It's reached via SingleAxisRotatingVisualAccessor rather than @Shadow since
 * the field is only declared on the superclass's own bytecode, not on ChainConveyorVisual.
 *
 * boxes/rigging are the cardboard-package and rigging-hook SmartRecyclers. The real beginFrame()
 * walks loopingPackages/travellingPackages and calls get() per visible package every frame, then
 * discardExtra() to recycle (delete()) whatever wasn't touched - that is the normal, intended use
 * of discardExtra(). Calling resetCount()+discardExtra() ourselves on every single frame (the
 * previous approach here) instead forced every already-allocated box/rigging instance through
 * delete() and reallocation on every frame regardless of whether anything changed, which is the
 * per-frame alloc/delete churn that caused the memory leak and lag spikes. Hiding is now done
 * exactly once per transition into/out of static mode: cancel beginFrame entirely so it can't
 * reallocate anything, and flip setVisible() on whatever instances already exist via
 * InstanceRecyclerAccessor/SmartRecyclerAccessor - no delete(), no allocation, no per-frame work.
 *
 * update() used to be left to run to completion every tick with the shaft re-frozen afterwards at
 * RETURN - that meant update()'s call into super.update() (SingleAxisRotatingVisual#update(),
 * re-running RotatingInstance#setup()+setChanged() every tick) and this class's own setupGuards()
 * rebuild still happened unconditionally, static mode or not; only the rotation value itself was
 * being zeroed after the fact. That's real per-tick CPU work surviving the "freeze" for every chain
 * conveyor in the world. It's now a one-shot: HEAD freezes the shaft exactly once on the transition
 * into static mode and then cancels the body outright (super.update() + setupGuards() never run
 * while static), and un-freezes once on the way back out so the real body resumes computing live
 * rotation/guard state.
 *
 * tick() is a separate per-game-tick entry point (ChainConveyorVisual implements
 * SimpleTickableVisual) that advances the package conveyor animation via
 * ChainConveyorBlockEntity#tickBoxVisuals() - independent of update() and not touched by anything
 * above. The packages it animates are already hidden via beginFrame below, so there's nothing left
 * for it to usefully compute while static; cancelling it removes that per-tick cost too.
 */
@Mixin(ChainConveyorVisual.class)
public abstract class ChainConveyorVisualMixin {

    @Shadow(remap = false)
    @Final
    private SmartRecycler<ResourceLocation, TransformedInstance> boxes;

    @Shadow(remap = false)
    @Final
    private SmartRecycler<ResourceLocation, TransformedInstance> rigging;

    @Unique
    private boolean createstatic$packagesHidden = false;

    @Unique
    private boolean createstatic$shaftFrozen = false;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void createstatic$freezeShaftOnInit(VisualizationContext context, ChainConveyorBlockEntity blockEntity,
                                                  float partialTick, CallbackInfo ci) {
        if (CreateStaticConfig.STATIC_MODE_ENABLED) {
            createstatic$freezeShaft();
            createstatic$shaftFrozen = true;
        }
    }

    @Inject(method = "update", at = @At("HEAD"), cancellable = true, remap = false)
    private void createstatic$updateHead(float partialTicks, CallbackInfo ci) {
        if (CreateStaticConfig.STATIC_MODE_ENABLED) {
            if (!createstatic$shaftFrozen) {
                createstatic$freezeShaft();
                createstatic$shaftFrozen = true;
            }
            ci.cancel();
        } else {
            createstatic$shaftFrozen = false;
        }
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, remap = false)
    private void createstatic$tick(TickableVisual.Context context, CallbackInfo ci) {
        if (CreateStaticConfig.STATIC_MODE_ENABLED) {
            ci.cancel();
        }
    }

    @Inject(method = "beginFrame", at = @At("HEAD"), cancellable = true, remap = false)
    private void createstatic$hidePackages(DynamicVisual.Context context, CallbackInfo ci) {
        if (!CreateStaticConfig.STATIC_MODE_ENABLED) {
            if (createstatic$packagesHidden) {
                createstatic$setVisible(boxes, true);
                createstatic$setVisible(rigging, true);
                createstatic$packagesHidden = false;
            }
            return;
        }

        if (!createstatic$packagesHidden) {
            createstatic$packagesHidden = true;
            createstatic$setVisible(boxes, false);
            createstatic$setVisible(rigging, false);
        }

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

    @Unique
    private static void createstatic$setVisible(SmartRecycler<?, ?> recycler, boolean visible) {
        for (InstanceRecycler<?> instanceRecycler : ((SmartRecyclerAccessor) (Object) recycler).createstatic$getRecyclers().values()) {
            for (Instance instance : ((InstanceRecyclerAccessor) (Object) instanceRecycler).createstatic$getInstances()) {
                instance.setVisible(visible);
            }
        }
    }
}
