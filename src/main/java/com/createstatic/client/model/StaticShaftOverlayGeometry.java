package com.createstatic.client.model;

import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;

import java.util.function.Function;

/**
 * Bakes the same (rotated) ModelState twice against two different model files - `base` (the
 * original, untouched casing, e.g. create:block/gearbox/block) and `withShaft` (our merged
 * casing+inner-shaft model, e.g. createstatic:block/gearbox_overlay/block_with_shaft) - and wraps
 * both results in StaticShaftBakedModel. Baking through the ModelBaker/ModelState the blockstate
 * pipeline hands us here is what makes this free of manual per-axis rotation math: whichever x/y
 * rotation Create's own blockstate.json already specifies for a given variant (axis=x, part=end,
 * etc.) is applied identically to both submodels, so they stay in perfect alignment with each
 * other and with the original casing's orientation.
 */
public class StaticShaftOverlayGeometry implements IUnbakedGeometry<StaticShaftOverlayGeometry> {

    private final ResourceLocation base;
    private final ResourceLocation withShaft;

    public StaticShaftOverlayGeometry(ResourceLocation base, ResourceLocation withShaft) {
        this.base = base;
        this.withShaft = withShaft;
    }

    @Override
    public BakedModel bake(IGeometryBakingContext context, ModelBaker baker,
                            Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState,
                            ItemOverrides overrides, ResourceLocation modelLocation) {
        BakedModel bakedBase = baker.bake(base, modelState);
        BakedModel bakedWithShaft = baker.bake(withShaft, modelState);
        return new StaticShaftBakedModel(bakedBase, bakedWithShaft);
    }
}
