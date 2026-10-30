package com.createstatic.mixin;

import com.createstatic.CreateStaticConfig;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogwheelBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

/**
 * ShaftBlock, CogWheelBlock and EncasedCogwheelBlock never override getRenderShape() themselves -
 * it is only ever inherited as EntityBlock's default ENTITYBLOCK_ANIMATED implementation, and
 * Mixin's @Inject can't target a method that isn't declared in the target class's own bytecode
 * (confirmed via javap against the real jar), nor inside an interface mixin (Mixin 0.8.5 here
 * rejects injectors in interfaces). Instead this merges a real override straight into all three
 * leaf classes at once - Mixin's standard way of implementing an inherited-but-undeclared
 * interface method - so the logic doesn't need instanceof checks at all.
 */
@Mixin({ShaftBlock.class, CogWheelBlock.class, EncasedCogwheelBlock.class})
public abstract class KineticBlockMixin {

    public RenderShape getRenderShape(BlockState state) {
        return CreateStaticConfig.STATIC_MODE_ENABLED ? RenderShape.MODEL : RenderShape.ENTITYBLOCK_ANIMATED;
    }
}
