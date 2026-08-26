package io.hamlook.aetheria.features.mining.gold;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.GsonBuilder;
import io.hamlook.aetheria.core.ProfileManagedStorage;
import io.hamlook.aetheria.core.StorageManager;
import io.hamlook.aetheria.features.price.PriceMap;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.data.SkyblockData;
import lombok.Getter;

import java.io.File;

public class GoldStats extends ProfileManagedStorage implements StorageManager.AutoSaveable {

    private static final long INACTIVITY_LIMIT_MS = 10_000L;
    private static GoldStats INSTANCE;

    @Getter
    private GoldData data = new GoldData();

    private long sessionActiveTimeMs = 0L;
    private boolean timerRunning = false;
    private boolean timerStartedOnce = false;
    private boolean inactivityFlagged = false;
    private long timerStartTime = 0L;
    private long lastActivityTime = 0L;

    private GoldStats() {
        super("gold_stats.json");
    }

    public static GoldStats getInstance() {
        if (INSTANCE == null) INSTANCE = new GoldStats();
        return INSTANCE;
    }

    public static boolean isGoldArea() {
        SkyblockData.Location loc = SkyblockData.getCurrentLocation();
        return loc == SkyblockData.Location.DWARVEN || loc == SkyblockData.Location.CRYSTAL_HOLLOWS;
    }

    public void updateActivity() {
        if (!timerStartedOnce) {
            timerStartTime = System.currentTimeMillis();
            timerRunning = true;
            timerStartedOnce = true;
        } else if (!timerRunning) {
            if (inactivityFlagged) {
                data.activeTimeMs -= INACTIVITY_LIMIT_MS;
                inactivityFlagged = false;
            }
            timerStartTime = System.currentTimeMillis();
            timerRunning = true;
        }
        lastActivityTime = System.currentTimeMillis();
    }

    public void timerTick() {
        if (!timerRunning) return;
        long now = System.currentTimeMillis();
        if (ATHRConfig.feature != null && ATHRConfig.feature.mining.goldTracker.pauseOnChat && ChatUtils.isChatOpen()) {
            data.activeTimeMs += now - timerStartTime;
            sessionActiveTimeMs += now - timerStartTime;
            timerRunning = false;
            return;
        }
        if (shouldTrack()) {
            data.activeTimeMs += now - timerStartTime;
            sessionActiveTimeMs += now - timerStartTime;
            timerStartTime = now;
            if (now - lastActivityTime > INACTIVITY_LIMIT_MS) {
                timerRunning = false;
                inactivityFlagged = true;
            }
        } else {
            timerStartTime = now;
            timerRunning = false;
            inactivityFlagged = false;
        }
    }

    public void pauseTimer() {
        if (!timerRunning) return;
        long now = System.currentTimeMillis();
        data.activeTimeMs += now - timerStartTime;
        sessionActiveTimeMs += now - timerStartTime;
        timerRunning = false;
        save();
    }

    private boolean shouldTrack() {
        if (ATHRConfig.feature == null) return false;
        return ATHRConfig.feature.mining.goldTracker.enabled && isGoldArea();
    }

    public boolean isPaused() {
        return timerStartedOnce && !timerRunning;
    }

    public long getSessionTimeMs() {
        return sessionActiveTimeMs;
    }

    public double getRate(long count) {
        if (data.activeTimeMs < 1_000L) return 0;
        return count / (data.activeTimeMs / 3_600_000.0);
    }

    public long getProfit() {
        long profit = 0;
        profit += (long) (data.ingotCount * PriceMap.Cached.getDPrice("GOLD_INGOT"));
        profit += (long) (data.enchantedCount * PriceMap.Cached.getDPrice("ENCHANTED_GOLD"));
        return profit;
    }

    public long getProfit(String profitUnit) {
        if ("As Enchanted".equals(profitUnit)) {
            long totalEnch = getTotalAsEnchanted();
            return (long) (totalEnch * PriceMap.Cached.getDPrice("ENCHANTED_GOLD"));
        } else if ("As Blocks".equals(profitUnit)) {
            long totalEnch = getTotalAsEnchanted();
            long totalBlocks = totalEnch / 160;
            return (long) (totalBlocks * PriceMap.Cached.getDPrice("ENCHANTED_GOLD_BLOCK"));
        }
        return getProfit();
    }

    public long getTotalAsIngot() {
        return data.ingotCount + data.enchantedCount * 160;
    }

    public long getTotalAsEnchanted() {
        return data.ingotCount / 160 + data.enchantedCount;
    }

    public void reset() {
        data.reset();
        sessionActiveTimeMs = 0L;
        timerRunning = false;
        timerStartedOnce = false;
        inactivityFlagged = false;
        lastActivityTime = 0L;
    }

    @Override
    public void load() {
        File f = resolveFile();
        if (f == null) return;
        GoldData loaded = StorageManager.loadSafe(f, GoldData.class, GsonBuilder.GSON);
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
}
