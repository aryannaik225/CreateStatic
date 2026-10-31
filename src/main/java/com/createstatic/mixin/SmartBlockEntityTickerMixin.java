package com.createstatic.mixin;

import com.createstatic.CreateStaticConfig;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntityTicker;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * KineticBlockEntityMixin/BeltBlockEntityMixin/ChainConveyorBlockEntityMixin cancel each entity's own
 * tick() body, but the level still has to look up and invoke a BlockEntityTickInvoker wrapping this
 * SmartBlockEntityTicker for every one of the ~10k registered block entities every level tick - that
 * invocation overhead (and whatever SmartBlockEntityTicker itself does before delegating to the
 * entity's tick(), e.g. behaviour ticking) is what the profiler's tick->level->blockEntities bucket
 * is actually measuring. Cancelling here, one level higher, short-circuits that per-entity dispatch
 * for every SmartBlockEntity (KineticBlockEntity, BeltBlockEntity and ChainConveyorBlockEntity all use
 * this same shared ticker - none of them override getTicker() with a different wrapper) before any of
 * it runs. Client-side only; the server keeps ticking normally for multiplayer compatibility.
 */
@Mixin(SmartBlockEntityTicker.class)
public abstract class SmartBlockEntityTickerMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, remap = false)
    private void createstatic$tick(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity, CallbackInfo ci) {
        if (level != null && level.isClientSide && CreateStaticConfig.STATIC_MODE_ENABLED) {
            ci.cancel();
        }
    }
}
