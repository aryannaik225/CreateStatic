package com.createstatic.mixin;

import com.createstatic.CreateStaticConfig;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorVisual;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.TickableVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PIVOT: the CHAIN_CONVEYOR_SHAFT sprocket and the cardboard-package/rigging instances must all stay
 * visible and Flywheel-rendered rather than hidden - hiding them was only ever a stand-in for the GPU
 * rendering we're now keeping, and forcing everything onto the CPU chunk builder instead is exactly
 * what caused the ~29ms/frame Indigo/culling cost this pivot undoes.
 *
 * rotatingModel (the sprocket/shaft piece, inherited from SingleAxisRotatingVisual) is handled the
 * same way as the belt pulley and the plain shaft/cogwheel instances: its speed uniform is pinned to
 * zero rather than touching visibility at all. It's reached via SingleAxisRotatingVisualAccessor
 * rather than @Shadow since the field is only declared on the superclass's own bytecode, not on
 * ChainConveyorVisual.
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
 * above. Cancelling it stops that per-tick CPU cost; the packages simply stay wherever they last
 * were, still rendered by Flywheel.
 *
 * beginFrame() walks loopingPackages/travellingPackages and calls get() per visible package every
 * frame, then discardExtra() to recycle whatever wasn't touched - real per-frame CPU work that keeps
 * running regardless of visibility. Since the client-side block entity tick is already cancelled
 * (see ChainConveyorBlockEntityMixin / SmartBlockEntityMixin), package positions never change while
 * static, so there is nothing new for this walk to discover - cancelling it outright just removes
 * that redundant per-frame walk without touching which instances are visible or allocated.
 */
@Mixin(ChainConveyorVisual.class)
public abstract class ChainConveyorVisualMixin {

    @Unique
    private boolean createstatic$shaftFrozen = false;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void createstatic$freezeShaftOnInit(VisualizationContext context, ChainConveyorBlockEntity blockEntity,
                                                  float partialTick, CallbackInfo ci) {
        if (CreateStaticConfig.STATIC_MODE_ENABLED) {
            createstatic$freezeShaft();
        }
    }

    @Inject(method = "update", at = @At("HEAD"), cancellable = true, remap = false)
    private void createstatic$updateHead(float partialTicks, CallbackInfo ci) {
        if (CreateStaticConfig.STATIC_MODE_ENABLED) {
            if (!createstatic$shaftFrozen) {
                createstatic$freezeShaft();
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
    private void createstatic$freezeBeginFrame(DynamicVisual.Context context, CallbackInfo ci) {
        if (CreateStaticConfig.STATIC_MODE_ENABLED) {
            ci.cancel();
        }
    }

    @Unique
    private void createstatic$freezeShaft() {
        RotatingInstance rotatingModel = ((SingleAxisRotatingVisualAccessor) (Object) this).createstatic$getRotatingModel();
        if (rotatingModel != null) {
            rotatingModel.rotationalSpeed = 0f;
            rotatingModel.setChanged();
        }
        createstatic$shaftFrozen = true;
    }
}
