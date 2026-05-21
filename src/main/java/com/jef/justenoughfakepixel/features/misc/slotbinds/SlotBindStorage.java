package com.jef.justenoughfakepixel.features.misc.slotbinds;

import com.google.gson.reflect.TypeToken;
import com.jef.justenoughfakepixel.core.JefGsonBuilder;
import com.jef.justenoughfakepixel.core.JefStorageManager;

import java.io.File;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Persists slot-bind mappings (slotA → slotB) to disk.
 * The key is the "source" slot index; the value is the bound partner slot index.
 * One of the two must always be a hotbar slot (36–44) for the swap to work.
 */
public class SlotBindStorage implements JefStorageManager.Managed, JefStorageManager.AutoSaveable {

    private static final SlotBindStorage INSTANCE = new SlotBindStorage();
    private static final Type TYPE = new TypeToken<Map<Integer, Integer>>() {}.getType();

    private File storageFile;
    private Map<Integer, Integer> binds = new HashMap<>();

    private SlotBindStorage() {}

    public static SlotBindStorage getInstance() {
        return INSTANCE;
    }

    /** Returns the live bind map (do not hold a reference; always re-fetch). */
    public Map<Integer, Integer> getBinds() {
        return binds;
    }

    @Override
    public void initFile(File configDir) {
        this.storageFile = new File(configDir, "slotbinds.json");
    }

    @Override
    public void load() {
        if (storageFile == null || !storageFile.exists()) {
            binds = new HashMap<>();
            return;
        }
        Map<Integer, Integer> loaded = JefStorageManager.loadSafe(storageFile, TYPE, JefGsonBuilder.GSON_STRICT);
        binds = loaded != null ? loaded : new HashMap<>();
    }

    public void save() {
        if (storageFile == null) return;
        JefStorageManager.saveAtomic(storageFile, binds, JefGsonBuilder.GSON_STRICT);
    }

    @Override
    public void autoSave() {
        save();
    }
}
