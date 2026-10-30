package com.createstatic.mixin;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.lib.visual.util.InstanceRecycler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/**
 * Exposes the live instance list backing an InstanceRecycler so it can be hidden in place
 * (setVisible(false)) instead of going through discardExtra()/delete(), which is only meant
 * to run when a slot is genuinely no longer needed, not every frame just to hide output.
 */
@Mixin(InstanceRecycler.class)
public interface InstanceRecyclerAccessor {

    @Accessor(value = "instances", remap = false)
    List<Instance> createstatic$getInstances();
}
