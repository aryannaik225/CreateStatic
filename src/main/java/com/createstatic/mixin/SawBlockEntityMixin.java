package com.createstatic.mixin;

import com.createstatic.CreateStaticConfig;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * SawBlockEntity#tick() does NOT call super.tick() at HEAD the way Belt/ChainConveyor/Millstone do -
 * it runs its own early-destroy check first, then calls super.tick()
 * (BlockBreakingKineticBlockEntity#tick(), itself a no-op on the client past its own isClientSide
 * guard), and only after that unconditionally - on both sides, no isClientSide check anywhere in
 * this override - decrements ProcessingInventory#remainingTime and drives the cutting/processing
 * recipe cycle every tick the saw has nonzero speed. That item-processing countdown is what
 * SawRenderer#renderItems() draws every frame, so cancelling the whole override here at HEAD (client
 * + static mode only) freezes it in place.
 */
@Mixin(SawBlockEntity.class)
public abstract class SawBlockEntityMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, remap = false)
    private void createstatic$tick(CallbackInfo ci) {
        Level level = ((BlockEntity) (Object) this).getLevel();
        if (level != null && level.isClientSide && CreateStaticConfig.isProcessingFrozen()) {
            ci.cancel();
        }
    }
}
