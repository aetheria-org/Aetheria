// SPDX-License-Identifier: LGPL-3.0-only
// Derived from MoulConfig (https://github.com/NotEnoughUpdates/MoulConfig)

package io.hamlook.aetheria.core.moulconfig.editors;

import com.google.gson.JsonObject;
import io.hamlook.aetheria.utils.compat.TextCompat;
import net.minecraft.util.ResourceLocation;

import java.util.Arrays;

public class TextureObject {

    public String displayName;

    public TextureObject(String displayName) {
        this.displayName = displayName;
    }

    public static TextureObject decode(JsonObject json) {
        TextureObject textureObject = new TextureObject(json.get("displayName").getAsString());
        Arrays
                .stream(textureObject.getClass().getDeclaredFields())
                .filter(field -> field.getType().equals(ResourceLocation.class))
                .forEach(field -> {
                    try {
                        field.set(textureObject, TextCompat.createResourceLocation(json.get(field.getName()).getAsString()));
                    } catch (Exception ignored) {
                    }
                });
        return textureObject;
    }

    private static ResourceLocation resource(String path) {
        return TextCompat.createResourceLocation("notenoughfakepixel", path);
    }
}