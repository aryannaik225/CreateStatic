package com.createstatic.client;

import com.createstatic.CreateStaticConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import me.shedaniel.clothconfig2.gui.entries.IntegerSliderEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * The CABIN modpack bundles Cloth Config, so this screen is built directly against its API rather
 * than behind a soft dependency. It's only classloaded when a player actually opens the mod's config
 * screen (see the lazy factory registered in {@code CreateStatic}), not at mod construction time.
 */
public class CreateStaticConfigScreen {

    public static Screen buildScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("CreateStatic Settings"))
                .setSavingRunnable(CreateStaticConfig.CLIENT_SPEC::save);

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));
        general.addEntry(booleanEntry(entryBuilder,
                "Enable CreateStatic (Master Toggle)",
                "Also bound to the F9 key in-game.",
                CreateStaticConfig.MASTER_TOGGLE));

        ConfigCategory categories = builder.getOrCreateCategory(Component.literal("Categories"));
        categories.addEntry(booleanEntry(entryBuilder,
                "Freeze Belts & Chains",
                "Belts, Chain Conveyors and Frogport Chains.",
                CreateStaticConfig.FREEZE_BELTS));
        categories.addEntry(booleanEntry(entryBuilder,
                "Freeze Shafts & Cogwheels",
                "Plain rotation relays.",
                CreateStaticConfig.FREEZE_RELAYS));
        categories.addEntry(booleanEntry(entryBuilder,
                "Freeze Gearboxes & Clutches",
                "Gearboxes, Gearshifts, Clutches and Encased Chain Drives.",
                CreateStaticConfig.FREEZE_TRANSMISSIONS));
        categories.addEntry(booleanEntry(entryBuilder,
                "Freeze Processing Machines",
                "Mechanical Mixers, Saws and Millstones.",
                CreateStaticConfig.FREEZE_PROCESSING));
        categories.addEntry(booleanEntry(entryBuilder,
                "Freeze Power Generators",
                "Water Wheels and Windmill Bearings.",
                CreateStaticConfig.FREEZE_GENERATORS));
        categories.addEntry(booleanEntry(entryBuilder,
                "Freeze Everything Else",
                "Fans, Gauges, Hand Cranks, Flywheels, Mechanical/Clockwork Bearings and Item Vaults.",
                CreateStaticConfig.FREEZE_MISC));

        ConfigCategory performance = builder.getOrCreateCategory(Component.literal("Performance & LOD"));
        performance.addEntry(intSliderEntry(entryBuilder,
                "LOD Distance Threshold",
                "Distance (in blocks) past which sub-component instances (fan blades, inner shafts, etc.) "
                        + "are culled, keeping only the outer casing instance active.",
                CreateStaticConfig.LOD_DISTANCE_THRESHOLD));

        return builder.build();
    }

    private static BooleanListEntry booleanEntry(ConfigEntryBuilder entryBuilder, String label,
            String tooltip, ForgeConfigSpec.BooleanValue value) {
        return entryBuilder.startBooleanToggle(Component.literal(label), value.get())
                .setDefaultValue(true)
                .setTooltip(Component.literal(tooltip))
                .setSaveConsumer(value::set)
                .build();
    }

    private static IntegerSliderEntry intSliderEntry(ConfigEntryBuilder entryBuilder, String label,
            String tooltip, ForgeConfigSpec.IntValue value) {
        return entryBuilder.startIntSlider(Component.literal(label), value.get(), 16, 128)
                .setDefaultValue(32)
                .setTooltip(Component.literal(tooltip))
                .setSaveConsumer(value::set)
                .build();
    }
}
