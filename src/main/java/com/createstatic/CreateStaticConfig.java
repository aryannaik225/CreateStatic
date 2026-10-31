package com.createstatic;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.Set;

/**
 * Holds the global static-mode toggle and the F9 keybind that flips it.
 */
public class CreateStaticConfig {

    public static final String MOD_ID = "createstatic";

    public static boolean STATIC_MODE_ENABLED = true;

    public static KeyMapping STATIC_MODE_TOGGLE_KEY;

    /**
     * "Static Until In Use" machines: Mechanical Press, Mechanical Mixer, Deployer, Mechanical
     * Crafter and Mechanical Arm. Their own tick() (and, for Mixer/Press, the shared
     * BasinOperatingBlockEntity/KineticBlockEntity tick() they ride through) must NOT be cancelled
     * by the generic SmartBlockEntityTickerMixin/SmartBlockEntityMixin/KineticBlockEntityMixin
     * client-side freeze, unlike every other kinetic block entity - the recipe/movement state that
     * drives their Flywheel visuals (press head, whisk, deployer hand, arm segments) is advanced
     * inside tick() itself, and the trigger that flips them from idle into an active recipe
     * (DeferralBehaviour-driven basin/recipe scanning) also only ever runs there. Freezing tick()
     * unconditionally - as every other kinetic block entity still is - would both desync their
     * client-side animation from server state (causing the reported twitching) and permanently
     * strand them idle, since nothing would ever re-check for a recipe to start. Their *rotating*
     * kinetic parts (shafts/cogs) stay frozen regardless, purely at the Flywheel visual layer
     * (FlywheelVisualMixin and friends), independent of this tick exemption.
     *
     * Also covers four blocks whose own working animation would otherwise glitch: Steam Engine
     * (piston/linkage, driven by SteamEngineVisual#animate() off live tick() state), Hand Crank and
     * Valve Handle (the crank handle only swings while independentAngle is advancing during use), and
     * Cuckoo Clock (hourHand/minuteHand/door animation - a stopped clock is itself the bug). Their
     * generic rotating shaft stubs, where present, are still frozen at the Flywheel visual layer
     * exactly like everything else.
     *
     * CRASH/DESYNC POSTMORTEM (EncasedFanBlockEntity): originally given its own dedicated
     * EncasedFanBlockEntityMixin cancelling tick() unconditionally, same shape as Millstone/Saw - but
     * unlike those, EncasedFanBlockEntity#tick() isn't just driving a cosmetic countdown. It's what
     * (re)builds AirCurrent every tick, and Create's player-pushing/item-pushing is done via *client-
     * side physics prediction* reading that AirCurrent locally, not purely server-authoritative motion
     * - freezing tick() stopped the client from ever recomputing it, so the client no longer predicted
     * the push at all (players stopped feeling pushed) while the server kept applying it, and dropped
     * items visibly fought between the two every tick (the "violent twitch" correction). That dedicated
     * mixin has been deleted and EncasedFanBlockEntity is exempted here instead, so its tick() (and
     * thus AirCurrent) runs identically to vanilla on the client. NozzleBlockEntity is exempted
     * alongside it for the same reason - it redirects/reshapes the fan's AirCurrent into its own
     * pushing-entities list every tick, the same client-prediction-critical computation. Neither block
     * has a rotating kinetic part gated by this exemption: the fan's rotor/shaft stay frozen
     * regardless, purely at the Flywheel visual layer (FanVisualMixin), and NozzleBlockEntity has no
     * RotatingInstance at all.
     *
     * CRITICAL: this check used to be `blockEntity instanceof MechanicalPressBlockEntity || ...`,
     * which crashed the game on boot with a ClassCircularityError. SmartBlockEntityMixin and
     * KineticBlockEntityMixin are @Mixin(SmartBlockEntity.class)/@Mixin(KineticBlockEntity.class) -
     * their injected method bodies get merged directly into SmartBlockEntity's/KineticBlockEntity's
     * own classfiles. An `instanceof MechanicalPressBlockEntity` reference inside that merged
     * bytecode forces the JVM to resolve and load MechanicalPressBlockEntity while
     * SmartBlockEntity/KineticBlockEntity is itself still in the middle of loading - but
     * MechanicalPressBlockEntity extends BasinOperatingBlockEntity extends KineticBlockEntity
     * extends SmartBlockEntity, so that load immediately needs SmartBlockEntity/KineticBlockEntity
     * again, which the classloader is already in the process of defining. That's a textbook
     * circular class load, not just a style nit - using `getClass().getSimpleName()` string
     * comparison instead means SmartBlockEntity/KineticBlockEntity's merged bytecode (and this
     * class, which they transitively load) never contains a symbolic reference to any of these
     * subclasses at all, so loading them never re-enters the base class's own loading.
     */
    private static final Set<String> STATIC_UNTIL_IN_USE_CLASS_NAMES = Set.of(
            "MechanicalPressBlockEntity",
            "MechanicalMixerBlockEntity",
            "DeployerBlockEntity",
            "MechanicalCrafterBlockEntity",
            "ArmBlockEntity",
            "SteamEngineBlockEntity",
            "HandCrankBlockEntity",
            "ValveHandleBlockEntity",
            "CuckooClockBlockEntity",
            "EncasedFanBlockEntity",
            "NozzleBlockEntity"
    );

    public static boolean isStaticUntilInUseMachine(BlockEntity blockEntity) {
        if (blockEntity == null) {
            return false;
        }
        return STATIC_UNTIL_IN_USE_CLASS_NAMES.contains(blockEntity.getClass().getSimpleName());
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class KeyRegistration {
        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            STATIC_MODE_TOGGLE_KEY = new KeyMapping(
                    "key.createstatic.toggle_static_mode",
                    GLFW.GLFW_KEY_F9,
                    "Create Static FPS"
            );
            event.register(STATIC_MODE_TOGGLE_KEY);
        }
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientTickHandler {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            if (STATIC_MODE_TOGGLE_KEY == null) {
                return;
            }
            while (STATIC_MODE_TOGGLE_KEY.consumeClick()) {
                toggleStaticMode();
            }
        }

        private static void toggleStaticMode() {
            STATIC_MODE_ENABLED = !STATIC_MODE_ENABLED;

            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.levelRenderer != null) {
                minecraft.levelRenderer.allChanged();
            }

            if (minecraft.player != null) {
                String state = STATIC_MODE_ENABLED ? "ON" : "OFF";
                minecraft.player.displayClientMessage(Component.literal("Create Static Mode: " + state), true);
            }
        }
    }
}
