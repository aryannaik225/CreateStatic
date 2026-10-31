package com.createstatic.mixin;

import com.createstatic.CreateStaticConfig;
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Same situation as WindmillBearingBlockEntityMixin: MechanicalBearingBlockEntity declares its own
 * tick() (overriding GeneratingKineticBlockEntity#tick() rather than calling up through it) that
 * drives angle/applyRotation() bookkeeping and the attached ControlledContraptionEntity's movement -
 * none of that is reached by cancelling the shared SmartBlockEntity/KineticBlockEntity tick() bases.
 * Cancelling it here freezes the contraption's angle outright, which is also what keeps
 * OrientedRotatingVisualMixin's frozen pivot shaft and BearingVisual's angle-driven cap orientation
 * visually still - there's nothing left updating the angle for them to read.
 *
 * WindmillBearingBlockEntity extends this class and overrides tick() itself again, so its own
 * dedicated mixin (WindmillBearingBlockEntityMixin) is what actually fires for windmill instances;
 * this one covers plain Mechanical Bearing instances. Client-side only.
 */
@Mixin(MechanicalBearingBlockEntity.class)
public abstract class MechanicalBearingBlockEntityMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, remap = false)
    private void createstatic$tick(CallbackInfo ci) {
        Level level = ((BlockEntity) (Object) this).getLevel();
        if (level != null && level.isClientSide && CreateStaticConfig.isMiscFrozen()) {
            ci.cancel();
        }
    }
}
