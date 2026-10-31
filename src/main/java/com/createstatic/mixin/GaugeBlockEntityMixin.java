package com.createstatic.mixin;

import com.createstatic.CreateStaticConfig;
import com.simibubi.create.content.kinetics.gauge.GaugeBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * GaugeBlockEntity (abstract base for Speed Gauge and Stress Gauge) declares its own tick(), which
 * calls up through the already-cancelled KineticBlockEntity#tick() and then, with no isClientSide
 * guard of its own, advances prevDialState/dialState toward dialTarget every tick - the needle sweep
 * GaugeVisual's DialFace instances read every frame via beginFrame(). Same shape as
 * Millstone/Saw/EncasedFan. Neither SpeedGaugeBlockEntity nor StressGaugeBlockEntity override tick()
 * themselves, so cancelling it here on the shared abstract base covers both. Client-side only.
 */
@Mixin(GaugeBlockEntity.class)
public abstract class GaugeBlockEntityMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, remap = false)
    private void createstatic$tick(CallbackInfo ci) {
        Level level = ((BlockEntity) (Object) this).getLevel();
        if (level != null && level.isClientSide && CreateStaticConfig.STATIC_MODE_ENABLED) {
            ci.cancel();
        }
    }
}
