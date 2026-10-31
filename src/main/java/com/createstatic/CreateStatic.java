package com.createstatic;

import com.createstatic.client.CreateStaticConfigScreen;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.client.ConfigScreenHandler;

@Mod("createstatic")
public class CreateStatic {
    public CreateStatic() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CreateStaticConfig.CLIENT_SPEC);

        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (client, parent) -> CreateStaticConfigScreen.buildScreen(parent)));
    }
}