package io.hamlook.aetheria.utils;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SkullTextureCache {

    private static final Map<String, ResourceLocation> cache = new HashMap<>();

    private SkullTextureCache() {
    }

    public static void load(String textureValue) {
        if (textureValue == null || textureValue.isEmpty()) return;
        if (cache.containsKey(textureValue)) return;

        GameProfile profile = new GameProfile(UUID.randomUUID(), "");
        profile.getProperties().put("textures", new Property("textures", textureValue));

        MinecraftCompat.getMinecraft().getSkinManager().loadProfileTextures(profile, (type, location, texture) -> {
            if (type == MinecraftProfileTexture.Type.SKIN) {
                cache.put(textureValue, location);
            }
        }, false);
    }

    public static ResourceLocation get(String textureValue) {
        return cache.get(textureValue);
    }
}