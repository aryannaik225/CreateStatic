package com.createstatic.mixin;

import com.createstatic.CreateStaticConfig;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltVisual;
import com.simibubi.create.content.processing.burner.ScrollInstance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import net.createmod.catnip.render.SpriteShiftEntry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Both the belts[] ScrollInstances and the pulley RotatingInstance are GPU-animated: speed() /
 * setRotationalSpeed() just write a uniform that the shader then integrates against render time
 * continuously, so cancelling update() the way FlywheelVisualMixin does for shafts would freeze
 * them mid-animation rather than hide them - and there is no chunk-baked substitute model to fall
 * back on here (the uncased belt's blockstate model is a bare particle/collision stub), so the
 * instances must stay alive and simply be pinned to zero speed instead of deleted.
 *
 * setup(ScrollInstance, boolean, SpriteShiftEntry) is the single private helper both the
 * constructor and update() funnel every belts[] entry through, so re-zeroing its result at
 * RETURN covers both paths in one place. The pulley has no such shared helper - both the
 * constructor and update() call pulley.setup(blockEntity) directly - so it is re-pinned from two
 * injection points instead.
 */
@Mixin(BeltVisual.class)
public abstract class BeltVisualMixin {

    @Shadow(remap = false)
    @Final
    protected RotatingInstance pulley;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void createstatic$freezePulleyOnInit(VisualizationContext context, BeltBlockEntity blockEntity,
                                                   float partialTick, CallbackInfo ci) {
        createstatic$freezePulley();
    }

    @Inject(method = "update", at = @At("RETURN"), remap = false)
    private void createstatic$freezePulleyOnUpdate(float partialTicks, CallbackInfo ci) {
        createstatic$freezePulley();
    }

    @Inject(method = "setup", at = @At("RETURN"), remap = false)
    private void createstatic$freezeScroll(ScrollInstance instance, boolean flip, SpriteShiftEntry spriteShift,
                                            CallbackInfoReturnable<ScrollInstance> cir) {
        if (!CreateStaticConfig.STATIC_MODE_ENABLED) {
            return;
        }
        ScrollInstance result = cir.getReturnValue();
        if (result != null) {
            result.speed(0f, 0f);
            result.setChanged();
        }
    }

    @Unique
    private void createstatic$freezePulley() {
        if (!CreateStaticConfig.STATIC_MODE_ENABLED || pulley == null) {
            return;
        }
        pulley.rotationalSpeed = 0f;
        pulley.setChanged();
    }
}
