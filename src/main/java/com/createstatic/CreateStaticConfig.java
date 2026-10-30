package com.createstatic;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * Holds the global static-mode toggle and the F9 keybind that flips it.
 */
public class CreateStaticConfig {

    public static final String MOD_ID = "createstatic";

    public static boolean STATIC_MODE_ENABLED = true;

    public static KeyMapping STATIC_MODE_TOGGLE_KEY;

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
