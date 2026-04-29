package com.jef.justenoughfakepixel.features.misc.pet;

import com.google.gson.reflect.TypeToken;
import com.jef.justenoughfakepixel.core.JefGsonBuilder;
import com.jef.justenoughfakepixel.core.JefStorageManager;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.client.Minecraft;

import java.io.*;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class PetCache implements JefStorageManager.Managed {

    private static final int MAX_ENTRIES = 200;
    private static PetCache INSTANCE;
    private final Map<String, CachedPet> pets = new LinkedHashMap<String, CachedPet>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CachedPet> eldest) {
            return size() > MAX_ENTRIES;
        }
    };
    private File file;

    private PetCache() {
    }

    public static PetCache getInstance() {
        if (INSTANCE == null) INSTANCE = new PetCache();
        return INSTANCE;
    }

    public static String normalizePetName(String name) {
        if (name == null) return "";
        return name.replace("✦", "")
                .replace("\u2019", "'")
                .trim();
    }

    @Override
    public void initFile(File configDir) {
        file = new File(configDir, "pet_cache.json");
    }

    @Override
    public void load() {
        Type type = new TypeToken<Map<String, CachedPet>>() {}.getType();
        Map<String, CachedPet> loaded = JefStorageManager.loadSafe(file, type, JefGsonBuilder.GSON);
        if (loaded != null) {
            // sanitize corrupted § on load
            for (CachedPet pet : loaded.values()) {
                if (pet.formattedName != null) pet.formattedName = pet.formattedName.replace("Â§", "§");
            }
            pets.putAll(loaded);
        }
    }

    public void warmupTextures() {
        for (CachedPet pet : pets.values()) {
            if (pet.textureValue == null || pet.textureValue.isEmpty()) continue;
            GameProfile profile = new GameProfile(UUID.randomUUID(), "");
            profile.getProperties().put("textures", new Property("textures", pet.textureValue));
            Minecraft.getMinecraft().getSkinManager().loadProfileTextures(profile, null, false);
        }
    }

    private void save() {
        JefStorageManager.saveAtomic(file, pets, JefGsonBuilder.GSON);
    }

    public void update(String baseName, String formattedName, String textureValue) {
        baseName = normalizePetName(baseName);

        String starSuffix = "";
        int starIndex = formattedName.indexOf("✦");
        if (starIndex > 0) {
            String before = formattedName.substring(0, starIndex);
            if (before.length() >= 2 && before.charAt(before.length() - 2) == '§') {
                starSuffix = " " + before.substring(before.length() - 2) + "✦";
            } else {
                starSuffix = " ✦";
            }
        }

        formattedName = formattedName.replace("✦", "").trim();
        if (!starSuffix.isEmpty()) formattedName = formattedName + starSuffix;

        CachedPet existing = pets.get(baseName);
        if (existing != null && existing.formattedName.equals(formattedName) && existing.textureValue.equals(textureValue))
            return;

        CachedPet pet = new CachedPet();
        pet.baseName = baseName;
        pet.formattedName = formattedName;
        pet.textureValue = textureValue;
        pets.put(baseName, pet);
        save();
    }

    public CachedPet get(String baseName) {
        return pets.get(normalizePetName(baseName));
    }

    public boolean hasTexture(String baseName) {
        CachedPet p = pets.get(normalizePetName(baseName));
        return p != null && p.textureValue != null && !p.textureValue.isEmpty();
    }
}