package com.createstatic.mixin;

import com.createstatic.CreateStaticConfig;
import com.simibubi.create.content.kinetics.millstone.MillstoneBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MillstoneBlockEntity#tick() calls super.tick() (KineticBlockEntity#tick(), already cancelled by
 * KineticBlockEntityMixin) but then, with no isClientSide guard of its own, falls straight into
 * process() (recipe matching/grinding countdown) and spawnParticles() every tick the millstone has
 * nonzero speed - same shape as BeltBlockEntity/ChainConveyorBlockEntity overriding tick() and doing
 * work after the cancelled super call. Cancelling here stops that client-side grinding countdown and
 * particle spawning outright; the server keeps grinding normally for multiplayer compatibility.
 */
@Mixin(MillstoneBlockEntity.class)
public abstract class MillstoneBlockEntityMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, remap = false)
    private void createstatic$tick(CallbackInfo ci) {
        Level level = ((BlockEntity) (Object) this).getLevel();
        if (level != null && level.isClientSide && CreateStaticConfig.isProcessingFrozen()) {
            ci.cancel();
        }
    }
}
