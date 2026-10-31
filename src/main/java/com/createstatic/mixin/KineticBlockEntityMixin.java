package com.createstatic.mixin;

import com.createstatic.CreateStaticConfig;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Freezing the Flywheel visuals (FlywheelVisualMixin, ChainConveyorVisualMixin, etc.) only stopped
 * the render-thread GPU-instance bookkeeping - KineticBlockEntity#tick() itself still runs in full
 * on the client 20x/second for every loaded kinetic block, regardless of whether anything it does
 * is visible: attachKinetics() (network attachment bookkeeping), SmartBlockEntity#tick() (behaviour
 * ticking), and KineticEffectHandler#tick() (the stress-glow/overstress particle and sound effects)
 * all run unconditionally. That's real client-thread work (the profiler's "tick" bucket) having
 * nothing to do with rendering at all.
 *
 * This only touches the CLIENT side - level.isClientSide is checked explicitly and the server-side
 * call path is left completely untouched, since the server is still authoritative for kinetic
 * network propagation, stress calculation, and power/item behaviour regardless of what any one
 * client is choosing to render.
 *
 * BeltBlockEntity and ChainConveyorBlockEntity both declare their own tick() override that calls
 * super.tick() (i.e. this method) as its first statement, so cancelling here also prevents their
 * share of the base KineticBlockEntity work - but cancelling here does NOT stop whatever each of
 * them does AFTER that super call (belt entity transport/sound, chain conveyor package logic), so
 * they still need their own dedicated cancel - see BeltBlockEntityMixin and
 * ChainConveyorBlockEntityMixin.
 *
 * Exempts the "Static Until In Use" machines (see CreateStaticConfig#isStaticUntilInUseMachine) -
 * Mechanical Press, Mechanical Mixer, Deployer, Mechanical Crafter and Mechanical Arm. Their recipe/
 * movement state (and, for Press/Mixer, the recipe-start trigger itself) is driven from inside
 * tick(), so cancelling it here would both desync their Flywheel visuals from server state and
 * permanently strand them idle. Their rotating parts (shafts/cogs) still freeze independently at
 * the Flywheel visual layer (FlywheelVisualMixin etc.), so this exemption only restores their
 * tick()-driven animation, not kinetic rotation.
 */
@Mixin(KineticBlockEntity.class)
public abstract class KineticBlockEntityMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, remap = false)
    private void createstatic$tick(CallbackInfo ci) {
        BlockEntity blockEntity = (BlockEntity) (Object) this;
        Level level = blockEntity.getLevel();
        if (level != null && level.isClientSide && CreateStaticConfig.STATIC_MODE_ENABLED
                && !CreateStaticConfig.isStaticUntilInUseMachine(blockEntity)) {
            ci.cancel();
        }
    }
}
