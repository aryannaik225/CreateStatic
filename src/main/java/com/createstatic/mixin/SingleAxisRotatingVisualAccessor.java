package com.createstatic.mixin;

import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * rotatingModel is declared on SingleAxisRotatingVisual itself, not on ChainConveyorVisual - a
 * plain @Shadow from ChainConveyorVisualMixin can't resolve a field that only exists in the
 * superclass's own bytecode (confirmed by the Mixin annotation processor's "Cannot find target
 * for @Shadow field" warning). An accessor targeting the declaring class directly is the reliable
 * way to reach it, same as BakedModelWrapperAccessor does for originalModel.
 */
@Mixin(SingleAxisRotatingVisual.class)
public interface SingleAxisRotatingVisualAccessor {

    @Accessor(value = "rotatingModel", remap = false)
    RotatingInstance createstatic$getRotatingModel();
}
