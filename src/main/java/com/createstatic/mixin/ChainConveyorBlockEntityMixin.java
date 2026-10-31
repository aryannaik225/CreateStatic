package com.createstatic.mixin;

import com.createstatic.CreateStaticConfig;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Same situation as BeltBlockEntityMixin: ChainConveyorBlockEntity's own tick() calls super.tick()
 * (KineticBlockEntityMixin) first, then runs its own invalid-connection checks and
 * tickBoxVisuals()-adjacent package bookkeeping afterward - none of which is stopped by cancelling
 * the shared base. Cancelled client-side only, same as everywhere else here; the server keeps
 * driving package movement/connections regardless of what this client is rendering.
 */
@Mixin(ChainConveyorBlockEntity.class)
public abstract class ChainConveyorBlockEntityMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, remap = false)
    private void createstatic$tick(CallbackInfo ci) {
        Level level = ((BlockEntity) (Object) this).getLevel();
        if (level != null && level.isClientSide && CreateStaticConfig.isBeltsFrozen()) {
            ci.cancel();
        }
    }
}
