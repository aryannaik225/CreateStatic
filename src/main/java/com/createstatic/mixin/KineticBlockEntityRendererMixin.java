package com.createstatic.mixin;

import com.createstatic.CreateStaticConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.chainDrive.ChainDriveBlock;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedShaftBlock;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KineticBlockEntityRenderer.class)
public abstract class KineticBlockEntityRendererMixin {

    @Inject(method = "renderSafe", at = @At("HEAD"), cancellable = true, remap = false)
    private void createstatic$renderSafe(KineticBlockEntity blockEntity, float partialTicks, PoseStack poseStack,
                                          MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                                          CallbackInfo ci) {
        if (!CreateStaticConfig.STATIC_MODE_ENABLED) {
            return;
        }

        Block block = blockEntity.getBlockState().getBlock();
        if (block instanceof ShaftBlock || block instanceof CogWheelBlock
                || block instanceof EncasedShaftBlock || block instanceof ChainDriveBlock) {
            ci.cancel();
        }
    }
}
