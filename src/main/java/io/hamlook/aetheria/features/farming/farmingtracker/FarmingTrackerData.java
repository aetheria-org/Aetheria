package io.hamlook.aetheria.features.farming.farmingtracker;

import io.hamlook.aetheria.core.GsonBuilder;
import io.hamlook.aetheria.core.ProfileManagedStorage;
import io.hamlook.aetheria.core.StorageManager;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Persists FarmingTracker's crop counts and accumulated active time per
 * SkyBlock profile, so the session survives world unloads / game restarts.
 * <p>
 * Deliberately does NOT persist the pause/inactivity timer's running state
 * (timerRunning / timerStartedOnce / lastActivityTime) — that's runtime-only
 * bookkeeping that should just start fresh each time the game launches.
 */
public class FarmingTrackerData extends ProfileManagedStorage implements StorageManager.AutoSaveable {

    private static FarmingTrackerData INSTANCE;
    private StoredData data = new StoredData();

    private FarmingTrackerData() {
        super("farming_tracker.json");
    }

    public static FarmingTrackerData getInstance() {
        if (INSTANCE == null) INSTANCE = new FarmingTrackerData();
        return INSTANCE;
    }

    @Override
    public void load() {
        File f = resolveFile();
        if (f == null) return;
        StoredData loaded = StorageManager.loadSafe(f, StoredData.class, GsonBuilder.GSON);
        if (loaded != null) data = loaded;
    }

    public void save() {
        File f = resolveFile();
        if (f == null) return;
        StorageManager.saveAtomic(f, data, GsonBuilder.GSON);
    }

    @Override
    public void autoSave() {
        save();
    }

    public Map<String, Long> getCounts() {
        return data.counts;
    }

    public long getActiveTimeMs() {
        return data.activeTimeMs;
    }

    public void setActiveTimeMs(long ms) {
        data.activeTimeMs = ms;
    }

    public void reset() {
        data.counts.clear();
        data.activeTimeMs = 0L;
        save();
    }

    private static class StoredData {
        Map<String, Long> counts = new LinkedHashMap<>();
        long activeTimeMs = 0L;
    }
}
