package com.createstatic.util;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * Flywheel's DynamicVisual#update() (what FanVisualLodMixin injects into) runs across a
 * ParallelTaskExecutor worker-thread pool, not the render thread - see
 * dev.engine_room.flywheel.impl.task.ParallelTaskExecutor. Reading Minecraft.getInstance()
 * .gameRenderer.getMainCamera().getPosition() directly from those worker threads races the render
 * thread's per-frame Camera#setup() writes with no synchronization, so a worker thread can latch a
 * stale Vec3 with no guarantee it ever observes a later update (classic JMM visibility hazard, not
 * just a one-frame glitch). That shows up worst on multiplayer: a dedicated-server join streams many
 * chunks/BlockEntities continuously while the player keeps moving, giving far more concurrent
 * worker-thread update() calls than singleplayer's comparatively front-loaded initial chunk load, so
 * the LOD-hidden state latches once and never re-syncs even when the camera is right next to the
 * block.
 *
 * Fix: snapshot the camera position exactly once per client tick, on the main thread, into a
 * `volatile` field. `volatile` establishes the happens-before edge the raw read was missing, so every
 * worker thread is guaranteed to observe the latest snapshot - cheap (one write per tick, not per
 * Flywheel instance) and keeps the hot path lock-free.
 */
public class CameraDistanceUtil {

    private static volatile Vec3 cachedCameraPos = null;

    public static void refreshCachedCameraPosition() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.gameRenderer == null) {
            cachedCameraPos = null;
            return;
        }

        Camera camera = minecraft.gameRenderer.getMainCamera();
        if (camera == null || !camera.isInitialized()) {
            cachedCameraPos = null;
            return;
        }

        cachedCameraPos = camera.getPosition();
    }

    public static double getDistanceSqrToCamera(BlockPos pos) {
        Vec3 cameraPos = cachedCameraPos;
        if (cameraPos == null) {
            return 0.0;
        }

        return cameraPos.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }
}
