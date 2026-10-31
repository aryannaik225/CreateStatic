package com.createstatic.client.model;

import com.createstatic.CreateStaticConfig;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Both `base` and `withShaft` are baked once, at resource-reload time, by
 * StaticShaftOverlayGeometry#bake() using the real ModelState the blockstate pipeline supplies for
 * that specific variant - so both already carry the correct per-axis rotation for free, the same
 * way the two halves of a stairs/door model would. Which one actually gets used is decided fresh on
 * every call by reading CreateStaticConfig.isRelaysFrozen() live, so flipping the F9 toggle (and
 * the levelRenderer.allChanged() it triggers) picks up the new choice on the very next chunk
 * rebuild without needing a resource reload.
 */
public class StaticShaftBakedModel implements BakedModel {

    private final BakedModel base;
    private final BakedModel withShaft;

    public StaticShaftBakedModel(BakedModel base, BakedModel withShaft) {
        this.base = base;
        this.withShaft = withShaft;
    }

    private BakedModel active() {
        return CreateStaticConfig.isRelaysFrozen() ? withShaft : base;
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction direction, RandomSource random) {
        return active().getQuads(state, direction, random);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return active().useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return active().isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return active().usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return active().isCustomRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return active().getParticleIcon();
    }

    @Override
    public ItemOverrides getOverrides() {
        return active().getOverrides();
    }
}
