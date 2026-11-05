package com.createstatic.mixin;

import com.createstatic.CreateStaticConfig;
import com.createstatic.util.CameraDistanceUtil;
import com.createstatic.util.FanLodTracker;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import com.simibubi.create.content.kinetics.fan.FanVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * v1.1 LOD sub-component culling for the Encased Fan. FanVisual's `shaft` and `fan`
 * RotatingInstances are the only Flywheel-managed parts of this block - the encased casing is a
 * static baked model that gets baked into the chunk mesh, not a Flywheel instance, so it's never
 * touched here and stays visible at any distance.
 *
 * TRACED: `Visual#update(float)` is NOT a per-frame or per-tick hook. Flywheel's
 * VisualManagerImpl only calls it via Storage#update(), which only runs when a `Transaction` with
 * Action.UPDATE is dequeued - i.e. only when the owning BlockEntity's data actually changes
 * (VisualManagerImpl.processQueue / queueUpdate). It is NOT part of the per-frame `DynamicVisual`
 * plan (Storage.framePlan() only walks `dynamicVisuals`, a PlanMap<DynamicVisual, ...> - FanVisual
 * never implements DynamicVisual). So once a frozen fan's one real update() call has run, nothing
 * ever calls update() again (our own Hard Logic Cull also cancels BlockEntity#tick(), which is what
 * would otherwise queue further updates as the block's data "changes") - injecting the LOD distance
 * check into update() means it is evaluated once and then never again as the player walks around.
 *
 * FIX: every live FanVisual registers a cached `this::createstatic$tickLod` reference with
 * {@link FanLodTracker} on construction and unregisters it on `_delete()`. CreateStaticClientEvents'
 * TickEvent.ClientTickEvent handler drives {@link FanLodTracker#tickAll()} once per client tick - a
 * real per-tick hook, independent of whatever cadence Flywheel calls update() on.
 *
 * COORDINATE SPACE (CRASH/MP-ONLY POSTMORTEM): AbstractBlockEntityVisual exposes two BlockPos
 * fields - `pos` (blockEntity.getBlockPos(), true absolute world coords) and `visualPos` (`pos`
 * minus the VisualizationContext's floating-origin `renderOrigin()`, used internally so Flywheel's
 * GPU-side matrices stay in small numbers far from world origin). `getVisualPosition()` returns the
 * latter. This mixin used to feed `getVisualPosition()` into {@link CameraDistanceUtil}, which
 * compares against `Camera.getPosition()` - always true absolute coords. That's an origin-relative
 * position diffed against an absolute one: wrong by a constant vector equal to `renderOrigin()`,
 * so the squared distance is wrong by the same amount at point-blank range as from across the map -
 * it never "resolves" as you approach. Harmless near world origin (small renderOrigin, easy to miss
 * in singleplayer testing near spawn) but permanently wrong anywhere renderOrigin is non-trivial,
 * which in practice means most multiplayer bases. `pos` is declared on AbstractBlockEntityVisual
 * itself, not on FanVisual, so a plain `@Shadow` here can't resolve it - that crashed the game with
 * a MixinTransformerError at sided_setup. Fixed via {@link AbstractBlockEntityVisualAccessor}, an
 * `@Accessor` mixin targeting the actual declaring class (same pattern as
 * SingleAxisRotatingVisualAccessor for rotatingModel) - this matches the pattern Flywheel's own
 * AbstractBlockEntityVisual#doDistanceLimitThisFrame() uses (raw `pos` vs `camera.getPosition()`).
 */
@Mixin(FanVisual.class)
public abstract class FanVisualLodMixin {

    @Shadow(remap = false)
    protected RotatingInstance shaft;

    @Shadow(remap = false)
    protected RotatingInstance fan;

    @Unique
    private boolean createstatic$lodHidden = false;

    @Unique
    private Runnable createstatic$tickRef;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void createstatic$onInit(VisualizationContext context, EncasedFanBlockEntity blockEntity,
                                       float partialTick, CallbackInfo ci) {
        createstatic$tickRef = this::createstatic$tickLod;
        FanLodTracker.register(createstatic$tickRef);
    }

    @Inject(method = "_delete", at = @At("HEAD"), remap = false)
    private void createstatic$onDelete(CallbackInfo ci) {
        FanLodTracker.unregister(createstatic$tickRef);
    }

    @Unique
    private void createstatic$tickLod() {
        if (!CreateStaticConfig.isMasterEnabled()) {
            createstatic$setSubComponentsHidden(false);
            return;
        }

        double threshold = CreateStaticConfig.LOD_DISTANCE_THRESHOLD.get();
        double thresholdSqr = threshold * threshold;
        BlockPos blockPos = ((AbstractBlockEntityVisualAccessor) this).createstatic$getPos();
        double distanceSqr = CameraDistanceUtil.getDistanceSqrToCamera(blockPos);

        createstatic$setSubComponentsHidden(distanceSqr > thresholdSqr);
    }

    @Unique
    private void createstatic$setSubComponentsHidden(boolean hidden) {
        if (hidden == createstatic$lodHidden) {
            return;
        }
        createstatic$lodHidden = hidden;

        boolean visible = !hidden;
        if (shaft != null) {
            shaft.setVisible(visible);
            shaft.setChanged();
        }
        if (fan != null) {
            fan.setVisible(visible);
            fan.setChanged();
        }
    }
}
