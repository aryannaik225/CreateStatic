package com.createstatic.mixin;

import dev.engine_room.flywheel.lib.visual.util.InstanceRecycler;
import dev.engine_room.flywheel.lib.visual.util.SmartRecycler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * Exposes the per-key InstanceRecyclers backing a SmartRecycler, so every currently-recycled
 * instance can be reached and hidden without going through discardExtra()/delete().
 */
@Mixin(SmartRecycler.class)
public interface SmartRecyclerAccessor {

    @Accessor(value = "recyclers", remap = false)
    Map<Object, InstanceRecycler<?>> createstatic$getRecyclers();
}
