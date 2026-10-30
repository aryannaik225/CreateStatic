package com.createstatic.mixin;

import com.createstatic.CreateStaticConfig;
import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * ShaftBlock and CogWheelBlock (small/large) are wrapped at bake time in
 * BracketedKineticBlockModel, which unconditionally returns Collections.emptyList() for normal
 * chunk rendering unless either (a) the render call is Ponder's "virtual" path, or (b) model
 * data carries a non-null bracket to draw instead. Since KineticBlockMixin now forces these
 * blocks to RenderShape.MODEL, chunk baking hits exactly the suppressed branch, so the real JSON
 * geometry never reaches the chunk mesh even though it baked fine underneath - hence "invisible"
 * rather than "wrong model". originalModel is the real baked model (our JSON geometry) that
 * BracketedKineticBlockModel wraps; calling it directly reproduces what the "virtual" branch
 * already does, just unconditionally while static mode is on.
 */
@Mixin(BracketedKineticBlockModel.class)
public abstract class BracketedKineticBlockModelMixin {

    @Inject(method = "getQuads", at = @At("HEAD"), cancellable = true, remap = false)
    private void createstatic$getQuads(BlockState state, Direction direction, RandomSource random,
                                        ModelData modelData, RenderType renderType,
                                        CallbackInfoReturnable<List<BakedQuad>> cir) {
        if (CreateStaticConfig.STATIC_MODE_ENABLED) {
            BakedModel originalModel = ((BakedModelWrapperAccessor) this).createstatic$getOriginalModel();
            cir.setReturnValue(originalModel.getQuads(state, direction, random, modelData, renderType));
        }
    }
}
