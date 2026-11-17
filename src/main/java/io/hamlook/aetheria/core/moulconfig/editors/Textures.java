// SPDX-License-Identifier: LGPL-3.0-only
// Derived from MoulConfig (https://github.com/NotEnoughUpdates/MoulConfig)

package io.hamlook.aetheria.core.moulconfig.editors;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.hamlook.aetheria.utils.compat.ResourceCompat;
import io.hamlook.aetheria.utils.compat.TextCompat;
//#if FORGE
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
//#else
//$$ import net.minecraft.resource.ResourceManager;
//#endif

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

//#if FORGE
public class Textures implements IResourceManagerReloadListener {
//#else
//$$ public class Textures {
//#endif

    private static final TextureObject DEFAULT_TEXTURE = new TextureObject("Default");

    private static final Gson gson = new GsonBuilder().create();
    public static final List<TextureObject> styles = Lists.newArrayList(DEFAULT_TEXTURE);
    public static TextureObject texture = DEFAULT_TEXTURE;

    public static void setTexture(int selected) {
        if (selected >= styles.size() || selected < 0) {
            texture = DEFAULT_TEXTURE;
        } else {
            texture = styles.get(selected);
        }
    }

    //#if FORGE
    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
    //#else
    //$$ public void onResourceManagerReload(ResourceManager resourceManager) {
    //#endif
        styles.clear();
        styles.add(DEFAULT_TEXTURE);
        DEFAULT_TEXTURE.displayName = "Default";
        try {
            for (byte[] data : ResourceCompat.openAllResources(TextCompat.createResourceLocation("notenoughfakepixel:data/styles.json"))) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(data), StandardCharsets.UTF_8))) {
                    JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);
                    for (JsonElement json : jsonObject.getAsJsonArray("styles")) {
                        styles.add(TextureObject.decode((JsonObject) json));
                    }
                    if (DEFAULT_TEXTURE.displayName.equals("Default") && jsonObject.has("defaultDisplayName") && jsonObject.get("defaultDisplayName").isJsonPrimitive()) {
                        DEFAULT_TEXTURE.displayName = jsonObject.get("defaultDisplayName").getAsString();
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }
}