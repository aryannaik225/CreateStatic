package com.createstatic.mixin;

import com.createstatic.CreateStaticConfig;
import com.simibubi.create.content.kinetics.flywheel.FlywheelBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * This is the Create "Flywheel" machine block (com.simibubi.create.content.kinetics.flywheel),
 * unrelated to the Flywheel rendering engine mod (dev.engine_room.flywheel) that the rest of this mod
 * targets - naming collision only. FlywheelBlockEntity declares its own tick() (advancing the
 * visualSpeed LerpedFloat and angle used to spin the physical wheel model), shadowing the generic
 * KineticBlockEntity#tick() base. Cancelling it here freezes that progression; CreateFlywheelVisualMixin
 * freezes the connecting shaft stub independently at the Flywheel-visual layer. Client-side only.
 */
@Mixin(FlywheelBlockEntity.class)
public abstract class FlywheelBlockEntityMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, remap = false)
    private void createstatic$tick(CallbackInfo ci) {
        Level level = ((BlockEntity) (Object) this).getLevel();
        if (level != null && level.isClientSide && CreateStaticConfig.isMiscFrozen()) {
            ci.cancel();
        }
    }
}
