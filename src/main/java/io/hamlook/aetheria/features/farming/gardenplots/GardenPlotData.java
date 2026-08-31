package io.hamlook.aetheria.features.farming.gardenplots;

import io.hamlook.aetheria.core.GsonBuilder;
import io.hamlook.aetheria.core.ProfileManagedStorage;
import io.hamlook.aetheria.core.StorageManager;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class GardenPlotData extends ProfileManagedStorage implements StorageManager.AutoSaveable {

    private static GardenPlotData INSTANCE;

    private static class Data {
        Set<Integer> unlockedPlots = new HashSet<>();
    }

    private Data data = new Data();
    private boolean dirty = false;

    private GardenPlotData() {
        super("garden_plots.json");
    }

    public static GardenPlotData getInstance() {
        if (INSTANCE == null) INSTANCE = new GardenPlotData();
        return INSTANCE;
    }

    @Override
    public void load() {
        File f = resolveFile();
        if (f == null) return;
        Data loaded = StorageManager.loadSafe(f, Data.class, GsonBuilder.GSON);
        if (loaded != null) data = loaded;
    }

    @Override
    public void save() {
        File f = resolveFile();
        if (f == null) return;
        StorageManager.saveAtomic(f, data, GsonBuilder.GSON);
    }

    @Override
    public void autoSave() {
        if (dirty) {
            save();
            dirty = false;
        }
    }

    public void updateFromChest(Set<Integer> newUnlocked) {
        if (!data.unlockedPlots.equals(newUnlocked)) {
            data.unlockedPlots = new HashSet<>(newUnlocked);
            dirty = true;
        }
    }

    public boolean isUnlocked(int plotId) {
        return data.unlockedPlots.contains(plotId);
    }
}
