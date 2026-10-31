package com.createstatic.mixin;

import com.createstatic.CreateStaticConfig;
import com.simibubi.create.content.contraptions.bearing.ClockworkBearingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ClockworkBearingBlockEntity extends KineticBlockEntity directly and declares its own tick()
 * (driving hourAngle/minuteAngle and the attached hourHand/minuteHand ControlledContraptionEntity
 * movement via applyRotations()), so the generic KineticBlockEntityMixin/SmartBlockEntityMixin bases
 * never fire for it, same shape as MechanicalBearingBlockEntityMixin/WindmillBearingBlockEntityMixin.
 * Cancelling it here freezes both hands' angles outright; its pivot shaft (BearingVisual's inherited
 * `rotatingModel` from OrientedRotatingVisual) is already frozen independently and for free by
 * OrientedRotatingVisualMixin. Client-side only.
 */
@Mixin(ClockworkBearingBlockEntity.class)
public abstract class ClockworkBearingBlockEntityMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, remap = false)
    private void createstatic$tick(CallbackInfo ci) {
        Level level = ((BlockEntity) (Object) this).getLevel();
        if (level != null && level.isClientSide && CreateStaticConfig.isMiscFrozen()) {
            ci.cancel();
        }
    }
}
