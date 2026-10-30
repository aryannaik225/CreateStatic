package com.createstatic.mixin;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraftforge.client.model.BakedModelWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * originalModel is declared on BakedModelWrapper itself, not on BracketedKineticBlockModel - a
 * plain @Shadow from BracketedKineticBlockModelMixin can't resolve a field that only exists in
 * the superclass's own bytecode. An accessor targeting the declaring class directly is the
 * reliable way to reach it.
 */
@Mixin(BakedModelWrapper.class)
public interface BakedModelWrapperAccessor {

    @Accessor(value = "originalModel", remap = false)
    BakedModel createstatic$getOriginalModel();
}
