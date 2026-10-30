package com.createstatic.client.model;

import com.createstatic.CreateStaticConfig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreateStaticConfig.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class StaticShaftModelRegistration {

    @SubscribeEvent
    public static void onRegisterGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register("static_shaft_overlay", StaticShaftOverlayLoader.INSTANCE);
    }
}
