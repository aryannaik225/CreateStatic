package com.createstatic.client.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.geometry.IGeometryLoader;

/**
 * Reads the two model references a wrapper model file points at under the "base" and "static"
 * keys, e.g.:
 * {
 *   "loader": "createstatic:static_shaft_overlay",
 *   "base": "create:block/gearbox/block",
 *   "static": "createstatic:block/gearbox_overlay/block_with_shaft"
 * }
 */
public class StaticShaftOverlayLoader implements IGeometryLoader<StaticShaftOverlayGeometry> {

    public static final StaticShaftOverlayLoader INSTANCE = new StaticShaftOverlayLoader();

    private StaticShaftOverlayLoader() {
    }

    @Override
    public StaticShaftOverlayGeometry read(JsonObject jsonObject, JsonDeserializationContext context) throws JsonParseException {
        ResourceLocation base = new ResourceLocation(jsonObject.get("base").getAsString());
        ResourceLocation withShaft = new ResourceLocation(jsonObject.get("static").getAsString());
        return new StaticShaftOverlayGeometry(base, withShaft);
    }
}
