package com.createstatic.mixin;

import com.createstatic.CreateStaticConfig;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * BeltBlockEntity declares its own tick() (calling super.tick() - KineticBlockEntityMixin - as its
 * first statement), then goes on to run belt-specific client work every tick regardless: lazily
 * initializing belt segment length, transported-entity/item movement and sound handling, etc.
 * Cancelling KineticBlockEntityMixin's copy alone does not stop any of that, since it all happens
 * in this subclass's own method body after the super call returns - this needs its own HEAD cancel.
 * Server-side ticks are left untouched; belts must keep moving items/entities for players without
 * the mod, and for this client's own server-authoritative state.
 */
@Mixin(BeltBlockEntity.class)
public abstract class BeltBlockEntityMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, remap = false)
    private void createstatic$tick(CallbackInfo ci) {
        Level level = ((BlockEntity) (Object) this).getLevel();
        if (level != null && level.isClientSide && CreateStaticConfig.STATIC_MODE_ENABLED) {
            ci.cancel();
        }
    }
}
