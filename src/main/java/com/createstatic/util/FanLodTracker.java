package com.createstatic.util;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Plain (non-mixin) registry of per-instance LOD re-check callbacks, kept outside the mixin class on
 * purpose: a @Mixin class's static members aren't woven into the target the same way instance
 * members are, so calling a mixin class's own static methods/fields from unrelated code is not
 * reliable. FanVisualLodMixin registers/unregisters a cached `this::createstatic$tickLod` reference
 * here on construction/_delete(), and CreateStaticClientEvents' ClientTickEvent handler drives
 * {@link #tickAll()} once per client tick - a real per-tick hook, independent of whatever cadence
 * Flywheel calls update() on the visual itself (see FanVisualLodMixin's class comment for why that
 * cadence can't be trusted).
 */
public class FanLodTracker {

    private static final Set<Runnable> ACTIVE = ConcurrentHashMap.newKeySet();

    private FanLodTracker() {
    }

    public static void register(Runnable tickLod) {
        ACTIVE.add(tickLod);
    }

    public static void unregister(Runnable tickLod) {
        ACTIVE.remove(tickLod);
    }

    public static void tickAll() {
        for (Runnable tickLod : ACTIVE) {
            tickLod.run();
        }
    }
}
