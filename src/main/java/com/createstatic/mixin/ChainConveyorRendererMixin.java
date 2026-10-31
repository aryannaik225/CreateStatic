package com.createstatic.mixin;

import com.createstatic.CreateStaticConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorPackage;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorRenderer;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * renderSafe always calls renderChains() on the CPU (the dangling chain segments between
 * conveyors are never Flywheel-instanced), then - only when Flywheel is unsupported - falls
 * through to the CPU wheel buffer plus a loopingPackages/travellingPackages renderBox() loop as a
 * fallback for when ChainConveyorVisual isn't active. Cancelling renderBox hides that fallback's
 * cardboard packages; the normal, Flywheel-driven package path is handled separately in
 * ChainConveyorVisualMixin.
 *
 * renderChains derives its scroll phase as
 * (AnimationTickHolder.getRenderTime(level) / (360f / abs(getSpeed()))) % 1f, i.e. the only time
 * input is that single getRenderTime() call - redirecting it to a frozen 0f when static mode is
 * on stops the dangling chain texture from crawling without touching the rest of the segment
 * geometry math (which still needs the real belt speed/direction to lay out correctly).
 */
@Mixin(ChainConveyorRenderer.class)
public abstract class ChainConveyorRendererMixin {

    @Inject(method = "renderBox", at = @At("HEAD"), cancellable = true, remap = false)
    private void createstatic$hideBox(ChainConveyorBlockEntity blockEntity, PoseStack poseStack,
                                       MultiBufferSource bufferSource, int overlay, BlockPos pos,
                                       ChainConveyorPackage pkg, float partialTicks, CallbackInfo ci) {
        if (CreateStaticConfig.isBeltsFrozen()) {
            ci.cancel();
        }
    }

    @Redirect(method = "renderChains", at = @At(value = "INVOKE",
            target = "Lnet/createmod/catnip/animation/AnimationTickHolder;getRenderTime(Lnet/minecraft/world/level/LevelAccessor;)F"),
            remap = false)
    private float createstatic$freezeChainScroll(LevelAccessor level) {
        if (CreateStaticConfig.isBeltsFrozen()) {
            return 0f;
        }
        return AnimationTickHolder.getRenderTime(level);
    }
}
