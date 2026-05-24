package io.hamlook.aetheria.features.misc.pet;

import com.google.gson.reflect.TypeToken;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import io.hamlook.aetheria.core.StorageManager;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class PetCache implements StorageManager.Managed {

    private static final int MAX_ENTRIES = 500;
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
        return name.replace("✦", "").replace("’", "'").trim();
    }

    public static String makeKey(String rarityColor, String baseName, String skinTag) {
        return rarityColor + baseName + (skinTag.isEmpty() ? "" : skinTag);
    }

    @Override
    public void initFile(File configDir) {
        file = new File(configDir, "pet_cache.json");
        PetFileValidator.deleteIfLegacy(file);
    }

    @Override
    public void load() {
        Type type = new TypeToken<Map<String, CachedPet>>() {
        }.getType();
        Map<String, CachedPet> loaded = PetFileValidator.load(file, type);
        if (loaded == null) return;
        loaded.forEach((key, pet) -> {
            if (key == null || pet == null) return;
            pet.sanitize();
            pets.put(key, pet);
        });
    }

    public void warmupTextures() {
        for (CachedPet pet : pets.values()) {
            if (pet.textureValue.isEmpty()) continue;
            GameProfile profile = new GameProfile(UUID.randomUUID(), "");
            profile.getProperties().put("textures", new Property("textures", pet.textureValue));
            Minecraft.getMinecraft().getSkinManager().loadProfileTextures(profile, null, false);
        }
    }

    private void save() {
        Map<String, CachedPet> toSave = new LinkedHashMap<>();
        pets.forEach((k, v) -> {
            if (k != null && v != null) toSave.put(k, v);
        });
        PetFileValidator.save(file, toSave);
    }

    private CachedPet getOrCreate(String key, String baseName) {
        return pets.computeIfAbsent(key, k -> {
            CachedPet p = new CachedPet();
            p.baseName = baseName;
            return p;
        });
    }

    public void update(String baseName, int level, String rarityColor, String skinTag, String textureValue) {
        baseName = normalizePetName(baseName);
        if (baseName.isEmpty()) return;
        String key = makeKey(rarityColor, baseName, skinTag);
        CachedPet pet = getOrCreate(key, baseName);
        pet.level = level;
        pet.rarityColor = rarityColor;
        pet.skinTag = skinTag;
        if (textureValue != null && !textureValue.isEmpty()) pet.textureValue = textureValue;
        pet.rebuildFormattedName();
        save();
    }

    public void updateFromChat(String baseName, int level, String rarityColor, String skinTag) {
        baseName = normalizePetName(baseName);
        if (baseName.isEmpty()) return;
        String key = makeKey(rarityColor, baseName, skinTag);
        CachedPet pet = getOrCreate(key, baseName);
        if (!rarityColor.isEmpty()) pet.rarityColor = rarityColor;
        if (level > 0) pet.level = level;
        pet.skinTag = skinTag;
        pet.rebuildFormattedName();
        save();
    }

    public CachedPet get(String key) {
        return pets.get(key);
    }

    public boolean hasTexture(String key) {
        CachedPet p = pets.get(key);
        return p != null && !p.textureValue.isEmpty();
    }
}
