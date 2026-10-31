package com.createstatic.mixin;

import com.createstatic.CreateStaticConfig;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * FrogportBlockEntity (Create's package frogport) declares its own tick() - calling
 * super.tick() (PackagePortBlockEntity -> SmartBlockEntityMixin) first, then running its own
 * package-animation and target-validation logic afterward on every tick regardless. Cancelling the
 * shared base alone does not stop any of that, since it happens in this subclass's own method body
 * after the super call returns - this needs its own HEAD cancel. Server-side ticks are left untouched.
 */
@Mixin(FrogportBlockEntity.class)
public abstract class FrogportBlockEntityMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, remap = false)
    private void createstatic$tick(CallbackInfo ci) {
        Level level = ((BlockEntity) (Object) this).getLevel();
        if (level != null && level.isClientSide && CreateStaticConfig.isBeltsFrozen()) {
            ci.cancel();
        }
    }
}
