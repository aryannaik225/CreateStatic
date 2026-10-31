package com.createstatic.mixin;

import com.createstatic.CreateStaticConfig;
import com.simibubi.create.content.logistics.vault.ItemVaultBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ItemVaultBlockEntity declares its own tick() (calling super.tick() - SmartBlockEntityMixin - first),
 * then runs its multiblock connection validation afterward on every tick regardless - this is the
 * "create:item_vault" cost the profiler flagged. Cancelling the shared base alone does not stop any
 * of that, since it happens in this subclass's own method body after the super call returns - this
 * needs its own HEAD cancel. Server-side ticks are left untouched.
 */
@Mixin(ItemVaultBlockEntity.class)
public abstract class ItemVaultBlockEntityMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, remap = false)
    private void createstatic$tick(CallbackInfo ci) {
        Level level = ((BlockEntity) (Object) this).getLevel();
        if (level != null && level.isClientSide && CreateStaticConfig.STATIC_MODE_ENABLED) {
            ci.cancel();
        }
    }
}
