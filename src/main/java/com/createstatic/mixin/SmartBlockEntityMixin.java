package com.createstatic.mixin;

import com.createstatic.CreateStaticConfig;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * SmartBlockEntityTickerMixin cancels the dispatch from the level's per-chunk BlockEntityTickInvoker
 * list, but Create 1.20.1 registers most of its tickers as anonymous lambdas (BlockEntityType ticker
 * factories), not as a direct reference to SmartBlockEntityTicker#tick - the profiler shows this
 * dispatch path as "unspecified" rather than attributing it to SmartBlockEntityTicker, so cancelling
 * there alone does not actually stop it. Cancelling here, at the one method practically every ticking
 * Create block entity shares as its common ancestor (kinetics, item vaults, fluid pipes, frogports),
 * catches the work regardless of which lambda/wrapper called in to get here. Client-side only; the
 * server keeps ticking normally for multiplayer compatibility.
 */
@Mixin(SmartBlockEntity.class)
public abstract class SmartBlockEntityMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, remap = false)
    private void createstatic$tick(CallbackInfo ci) {
        Level level = ((BlockEntity) (Object) this).getLevel();
        if (level != null && level.isClientSide && CreateStaticConfig.STATIC_MODE_ENABLED) {
            ci.cancel();
        }
    }
}
