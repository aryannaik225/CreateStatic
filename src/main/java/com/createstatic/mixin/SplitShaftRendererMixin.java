package com.createstatic.mixin;

import com.createstatic.CreateStaticConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import com.simibubi.create.content.kinetics.transmission.SplitShaftRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * SplitShaftBlockEntity backs Clutch, Gearshift and SequencedGearshift alike - all three register
 * SplitShaftRenderer as their BlockEntityRenderer, and all three already have fully baked casing
 * blockstate models (clutch/block(_powered), gearshift/block(_powered),
 * sequenced_gearshift/idle|seq_1..5), so getRenderShape doesn't need forcing here any more than it
 * does for Gearbox or EncasedShaftBlock. SplitShaftRenderer declares its own
 * renderSafe(SplitShaftBlockEntity, ...) override (drawing just the internal half-shaft stub), so
 * - exactly like GearboxRenderer - the generic KineticBlockEntityRendererMixin injection into the
 * base class never fires for it; cancel this renderer's own method directly instead.
 */
@Mixin(SplitShaftRenderer.class)
public abstract class SplitShaftRendererMixin {

    @Inject(method = "renderSafe(Lcom/simibubi/create/content/kinetics/transmission/SplitShaftBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void createstatic$renderSafe(SplitShaftBlockEntity blockEntity, float partialTicks, PoseStack poseStack,
                                          MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                                          CallbackInfo ci) {
        if (CreateStaticConfig.STATIC_MODE_ENABLED) {
            ci.cancel();
        }
    }
}
