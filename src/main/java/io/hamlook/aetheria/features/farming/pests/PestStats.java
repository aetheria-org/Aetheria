package io.hamlook.aetheria.features.farming.pests;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.GsonBuilder;
import io.hamlook.aetheria.core.ProfileManagedStorage;
import io.hamlook.aetheria.core.StorageManager;
import io.hamlook.aetheria.features.farming.data.PestType;
import io.hamlook.aetheria.features.price.PriceMap;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.data.SkyblockData;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PestStats extends ProfileManagedStorage implements StorageManager.AutoSaveable {

    private static final long INACTIVITY_LIMIT_MS = 120_000L;
    private static PestStats INSTANCE;

    private PestData data = new PestData();
    private volatile boolean trackingEnabled = true;
    private volatile boolean showOverlay = false;
    private long sessionStartMs = -1L;
    private long sessionActiveTimeMs = 0L;
    private boolean timerRunning = false;
    private boolean timerStartedOnce = false;
    private long timerStartTime = 0L;
    private long lastActivityTime = 0L;

    private PestStats() {
        super("pest_stats.json");
    }

    public static PestStats getInstance() {
        if (INSTANCE == null) INSTANCE = new PestStats();
        return INSTANCE;
    }

    @Override
    public void load() {
        File f = resolveFile();
        if (f == null) return;
        PestData loaded = StorageManager.loadSafe(f, PestData.class, GsonBuilder.GSON);
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

    public void reset() {
        data = new PestData();
        sessionStartMs = (sessionStartMs > 0) ? System.currentTimeMillis() : -1L;
        timerRunning = false;
        timerStartedOnce = false;
        timerStartTime = 0L;
        lastActivityTime = 0L;
        sessionActiveTimeMs = 0L;
        save();
    }

    public boolean toggleTracking() {
        trackingEnabled = !trackingEnabled;
        return trackingEnabled;
    }

    public boolean isTracking() {
        return trackingEnabled && SkyblockData.getCurrentLocation() == SkyblockData.Location.GARDEN;
    }

    public boolean isPaused() {
        return timerStartedOnce && !timerRunning;
    }

    public boolean isOverlayVisible() {
        return showOverlay;
    }

    public void setOverlayVisible(boolean visible) {
        showOverlay = visible;
    }

    public void recordPestKill(PestType type, long qty, String itemId) {
        data.kills.merge(type.name(), 1L, Long::sum);
        if (itemId != null) data.drops.merge(itemId, qty, Long::sum);
        updateActivity();
        showOverlay = true;
        save();
    }

    public long getTotalPests() {
        long total = 0L;
        for (long count : data.kills.values()) total += count;
        return total;
    }

    public long getTotalDrops() {
        long total = 0L;
        for (long count : data.drops.values()) total += count;
        return total;
    }

    public long getKills(PestType type) {
        return data.kills.getOrDefault(type.name(), 0L);
    }

    public Map<String, Long> getDropsMap() {
        return data.drops;
    }

    public double getProfit() {
        double total = 0.0;
        for (Map.Entry<String, Long> entry : data.drops.entrySet()) {
            double price = PriceMap.Cached.getDPrice(entry.getKey());
            if (price > 0) total += entry.getValue() * price;
        }
        return total;
    }

    public List<PestType> getPestsSortedByKills() {
        List<PestType> pests = new ArrayList<>();
        for (PestType type : PestType.values()) {
            if (getKills(type) > 0L) pests.add(type);
        }
        pests.sort((a, b) -> Long.compare(getKills(b), getKills(a)));
        return pests;
    }

    public long getActiveTimeMs() {
        return data.activeTimeMs;
    }

    public long getSessionTimeMs() {
        return sessionActiveTimeMs;
    }

    public long getRateBasisMs() {
        if (ATHRConfig.feature != null && ATHRConfig.feature.farming.pests.pestTracker.rateBasis == 1) {
            return sessionActiveTimeMs;
        }
        return data.activeTimeMs;
    }

    public void onClientLogin() {
        sessionStartMs = System.currentTimeMillis();
        sessionActiveTimeMs = 0L;
        showOverlay = false;
    }

    public void onClientLogout() {
        pauseTimer();
        sessionStartMs = -1L;
        showOverlay = false;
    }

    public void onWorldUnload() {
        pauseTimer();
        showOverlay = false;
    }

    public void updateActivity() {
        if (!timerStartedOnce) {
            timerStartTime = System.currentTimeMillis();
            timerRunning = true;
            timerStartedOnce = true;
        } else if (!timerRunning) {
            timerStartTime = System.currentTimeMillis();
            timerRunning = true;
        }
        lastActivityTime = System.currentTimeMillis();
    }

    public void timerTick() {
        if (!timerRunning) return;
        long now = System.currentTimeMillis();
        if (ATHRConfig.feature != null && ATHRConfig.feature.farming.pests.pestTracker.pauseOnChat && ChatUtils.isChatOpen()) {
            long elapsed = now - timerStartTime;
            data.activeTimeMs += elapsed;
            sessionActiveTimeMs += elapsed;
            timerRunning = false;
            return;
        }
        if (isTracking()) {
            if (now - lastActivityTime > INACTIVITY_LIMIT_MS) {
                long elapsed = now - timerStartTime;
                data.activeTimeMs += Math.max(0L, elapsed - INACTIVITY_LIMIT_MS);
                sessionActiveTimeMs += elapsed;
                timerRunning = false;
                return;
            }
            long elapsed = now - timerStartTime;
            data.activeTimeMs += elapsed;
            sessionActiveTimeMs += elapsed;
            timerStartTime = now;
        } else {
            timerStartTime = now;
            timerRunning = false;
        }
    }

    public void pauseTimer() {
        if (!timerRunning) return;
        long now = System.currentTimeMillis();
        long elapsed = now - timerStartTime;
        data.activeTimeMs += elapsed;
        sessionActiveTimeMs += elapsed;
        timerRunning = false;
        save();
    }
}