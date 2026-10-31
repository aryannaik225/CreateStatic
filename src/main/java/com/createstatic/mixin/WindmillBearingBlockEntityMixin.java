package com.createstatic.mixin;

import com.createstatic.CreateStaticConfig;
import com.simibubi.create.content.contraptions.bearing.WindmillBearingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Same situation as BeltBlockEntityMixin/ChainConveyorBlockEntityMixin: WindmillBearingBlockEntity
 * declares its own tick() (inherited from MechanicalBearingBlockEntity, which itself overrides
 * GeneratingKineticBlockEntity#tick() rather than calling up through it) that drives
 * updateGeneratedRotation(), angle/applyRotation() bookkeeping and the attached
 * ControlledContraptionEntity's movement - none of that is reached by cancelling the shared
 * SmartBlockEntity/KineticBlockEntity tick() bases (SmartBlockEntityMixin/KineticBlockEntityMixin),
 * since this override never calls super.tick(). Cancelling it here freezes the sails' angle outright,
 * which is also what makes OrientedRotatingVisualMixin's frozen pivot shaft and BearingVisual's
 * angle-driven cap orientation stay visually still - there's nothing left updating the angle for them
 * to read.
 *
 * Client-side only, same as everywhere else here; the server keeps driving contraption assembly and
 * movement regardless of what this client is rendering.
 */
@Mixin(WindmillBearingBlockEntity.class)
public abstract class WindmillBearingBlockEntityMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, remap = false)
    private void createstatic$tick(CallbackInfo ci) {
        Level level = ((BlockEntity) (Object) this).getLevel();
        if (level != null && level.isClientSide && CreateStaticConfig.isGeneratorsFrozen()) {
            ci.cancel();
        }
    }
}
