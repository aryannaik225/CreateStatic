package com.createstatic.mixin;

import com.createstatic.CreateStaticConfig;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltVisual;
import com.simibubi.create.content.processing.burner.ScrollInstance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Both the belts[] ScrollInstances and the pulley RotatingInstance are GPU-animated: speed() /
 * setRotationalSpeed() just write a uniform that the shader then integrates against render time
 * continuously, so hiding them outright is unnecessary and undesirable - there is no chunk-baked
 * substitute model to fall back on here (the uncased belt's blockstate model is a bare
 * particle/collision stub), so the instances must stay alive on the GPU and simply be pinned to
 * zero speed.
 *
 * This used to zero both at update()'s RETURN every tick, which meant update()'s real body -
 * rebuilding every belts[] ScrollInstance via setup(), and pulley.setup(blockEntity) - still ran to
 * completion first, unconditionally, static mode or not. Pinning the result afterwards didn't stop
 * that CPU work from happening; it just overwrote its output every single tick for every belt in
 * the world. It's now a one-shot pinned directly off the shadowed fields: HEAD freezes both arrays
 * exactly once on the transition into static mode and cancels the rest of update() outright, and
 * un-freezes once on the way back out so the real body resumes computing live scroll/rotation
 * state.
 */
@Mixin(BeltVisual.class)
public abstract class BeltVisualMixin {

    @Shadow(remap = false)
    @Final
    protected RotatingInstance pulley;

    @Shadow(remap = false)
    @Final
    protected ScrollInstance[] belts;

    @Unique
    private boolean createstatic$frozen = false;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void createstatic$freezeOnInit(VisualizationContext context, BeltBlockEntity blockEntity,
                                            float partialTick, CallbackInfo ci) {
        if (CreateStaticConfig.STATIC_MODE_ENABLED) {
            createstatic$freeze();
            createstatic$frozen = true;
        }
    }

    @Inject(method = "update", at = @At("HEAD"), cancellable = true, remap = false)
    private void createstatic$updateHead(float partialTicks, CallbackInfo ci) {
        if (CreateStaticConfig.STATIC_MODE_ENABLED) {
            if (!createstatic$frozen) {
                createstatic$freeze();
                createstatic$frozen = true;
            }
            ci.cancel();
        } else {
            createstatic$frozen = false;
        }
    }

    @Unique
    private void createstatic$freeze() {
        if (pulley != null) {
            pulley.rotationalSpeed = 0f;
            pulley.setChanged();
        }
        if (belts != null) {
            for (ScrollInstance belt : belts) {
                if (belt != null) {
                    belt.speed(0f, 0f);
                    belt.setChanged();
                }
            }
        }
    }
}
