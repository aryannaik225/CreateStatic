package com.createstatic.mixin;

import com.createstatic.CreateStaticConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * BeltRenderer#renderSafe only ever draws the rubber-belt surface itself on the CPU fallback
 * path (when VisualizationManager.supportsVisualization() is false) - with Flywheel 1.0.5
 * bundled and active it jumps straight past that branch into renderItems(), which is the one
 * call that always runs regardless of Flywheel state and is solely responsible for drawing the
 * ItemStacks/packages riding on top of the belt. Cancelling renderItems (rather than the whole
 * renderSafe) hides only the moving cargo while leaving the belt surface itself - drawn
 * separately by BeltVisual in the Flywheel path, or by the untouched first half of renderSafe in
 * the CPU fallback path - fully intact.
 */
@Mixin(BeltRenderer.class)
public abstract class BeltRendererMixin {

    @Inject(method = "renderItems", at = @At("HEAD"), cancellable = true, remap = false)
    private void createstatic$renderItems(BeltBlockEntity blockEntity, float partialTicks, PoseStack poseStack,
                                           MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                                           CallbackInfo ci) {
        if (CreateStaticConfig.STATIC_MODE_ENABLED) {
            ci.cancel();
        }
    }
}
