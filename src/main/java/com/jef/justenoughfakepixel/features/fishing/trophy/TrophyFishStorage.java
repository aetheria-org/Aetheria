package com.jef.justenoughfakepixel.features.fishing.trophy;

import com.jef.justenoughfakepixel.core.JefGsonBuilder;
import com.jef.justenoughfakepixel.core.JefStorageManager;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class TrophyFishStorage implements JefStorageManager.Managed, JefStorageManager.AutoSaveable {

    private static TrophyFishStorage INSTANCE;
    private File file;
    private StoredData data = new StoredData();

    private TrophyFishStorage() {
    }

    public static TrophyFishStorage getInstance() {
        if (INSTANCE == null) INSTANCE = new TrophyFishStorage();
        return INSTANCE;
    }

    @Override
    public void initFile(File configDir) {
        this.file = new File(configDir, "trophy_fish.json");
    }

    @Override
    public void load() {
        StoredData loaded = JefStorageManager.loadSafe(file, StoredData.class, JefGsonBuilder.GSON);
        if (loaded != null) data = loaded;
    }

    public void save() {
        JefStorageManager.saveAtomic(file, data, JefGsonBuilder.GSON);
    }

    @Override
    public void autoSave() {
        save();
    }

    public Map<String, Map<String, Integer>> getFish() {
        return data.fish;
    }

    public int getCount(String fishName, TrophyRarity rarity) {
        Map<String, Integer> counts = data.fish.get(fishName);
        if (counts == null) return 0;
        return counts.getOrDefault(rarity.name(), 0);
    }

    public int incrementCount(String fishName, TrophyRarity rarity) {
        Map<String, Integer> counts = data.fish.computeIfAbsent(fishName, k -> new LinkedHashMap<>());
        int next = counts.getOrDefault(rarity.name(), 0) + 1;
        counts.put(rarity.name(), next);
        return next;
    }

    public void setCount(String fishName, TrophyRarity rarity, int count) {
        data.fish.computeIfAbsent(fishName, k -> new LinkedHashMap<>()).put(rarity.name(), count);
    }

    public int getTotal(String fishName) {
        Map<String, Integer> counts = data.fish.get(fishName);
        if (counts == null) return 0;
        return counts.values().stream().mapToInt(Integer::intValue).sum();
    }

    public TrophyRarity getBestRarity(String fishName) {
        TrophyRarity best = TrophyRarity.BRONZE;
        for (TrophyRarity r : TrophyRarity.values()) {
            if (getCount(fishName, r) > 0) best = r;
        }
        return best;
    }

    private static class StoredData {
        public Map<String, Map<String, Integer>> fish = new LinkedHashMap<>();
    }
}