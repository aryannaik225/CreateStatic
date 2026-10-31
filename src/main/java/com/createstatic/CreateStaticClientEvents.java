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
 * Registers the F9 keybind and flips {@link CreateStaticConfig#MASTER_TOGGLE} when it's pressed,
 * persisting the change to disk and forcing a chunk rebake so Flywheel instances pick it up
 * immediately.
 */
public class CreateStaticClientEvents {

    public static KeyMapping STATIC_MODE_TOGGLE_KEY;

    @Mod.EventBusSubscriber(modid = CreateStaticConfig.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class KeyRegistration {
        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            STATIC_MODE_TOGGLE_KEY = new KeyMapping(
                    "key.createstatic.toggle_static_mode",
                    GLFW.GLFW_KEY_F9,
                    "key.categories.createstatic"
            );
            event.register(STATIC_MODE_TOGGLE_KEY);
        }
    }

    @Mod.EventBusSubscriber(modid = CreateStaticConfig.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientTickHandler {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END || STATIC_MODE_TOGGLE_KEY == null) {
                return;
            }
            while (STATIC_MODE_TOGGLE_KEY.consumeClick()) {
                toggleStaticMode();
            }
        }

        private static void toggleStaticMode() {
            boolean newState = !CreateStaticConfig.MASTER_TOGGLE.get();
            CreateStaticConfig.MASTER_TOGGLE.set(newState);
            CreateStaticConfig.CLIENT_SPEC.save();

            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.levelRenderer != null) {
                minecraft.levelRenderer.allChanged();
            }

            if (minecraft.player != null) {
                String state = newState ? "Enabled" : "Disabled";
                minecraft.player.displayClientMessage(
                        Component.literal("CreateStatic: High-FPS Mode " + state), true);
            }
        }
    }
}
