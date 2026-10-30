package com.createstatic.mixin;

import com.createstatic.CreateStaticConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.gearbox.GearboxBlockEntity;
import com.simibubi.create.content.kinetics.gearbox.GearboxRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Gearbox's blockstate model (create:block/gearbox/block) already bakes the full andesite-cased
 * body statically for every axis variant, so unlike the bare ShaftBlock/CogWheelBlock there is no
 * need to force getRenderShape() here - it already defaults to MODEL and the casing renders fine
 * with static mode off. GearboxRenderer only ever draws the two perpendicular SHAFT_HALF pegs (the
 * connector stubs poking out toward neighbours not aligned with the block's own axis); it declares
 * its own renderSafe(GearboxBlockEntity, ...) override rather than relying on the inherited
 * KineticBlockEntityRenderer#renderSafe body, so the generic injection in
 * KineticBlockEntityRendererMixin never actually runs for this renderer - it has to be cancelled
 * here directly instead.
 */
@Mixin(GearboxRenderer.class)
public abstract class GearboxRendererMixin {

    @Inject(method = "renderSafe(Lcom/simibubi/create/content/kinetics/gearbox/GearboxBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void createstatic$renderSafe(GearboxBlockEntity blockEntity, float partialTicks, PoseStack poseStack,
                                          MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                                          CallbackInfo ci) {
        if (CreateStaticConfig.STATIC_MODE_ENABLED) {
            ci.cancel();
        }
    }
}
