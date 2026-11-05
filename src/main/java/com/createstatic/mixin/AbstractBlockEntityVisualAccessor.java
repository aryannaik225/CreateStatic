package com.createstatic.mixin;

import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * pos is declared on AbstractBlockEntityVisual itself, not on FanVisual - a plain @Shadow from
 * FanVisualLodMixin can't resolve a field that only exists in the superclass's own bytecode (this
 * crashed the game with a MixinTransformerError at sided_setup; the AP's "Cannot find target for
 * @Shadow field" warning was not cosmetic after all). An accessor targeting the declaring class
 * directly is the reliable way to reach it, same as SingleAxisRotatingVisualAccessor does for
 * rotatingModel. `pos` is the true absolute-world BlockPos (blockEntity.getBlockPos()), distinct
 * from `visualPos`/getVisualPosition() which is render-origin-relative - see FanVisualLodMixin.
 */
@Mixin(AbstractBlockEntityVisual.class)
public interface AbstractBlockEntityVisualAccessor {

    @Accessor(value = "pos", remap = false)
    BlockPos createstatic$getPos();
}
