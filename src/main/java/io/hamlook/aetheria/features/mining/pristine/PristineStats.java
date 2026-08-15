package io.hamlook.aetheria.features.mining.pristine;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.GsonBuilder;
import io.hamlook.aetheria.core.ProfileManagedStorage;
import io.hamlook.aetheria.core.StorageManager;
import io.hamlook.aetheria.features.mining.powder.PowderStats;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.data.SkyblockData;
import lombok.Getter;

import java.io.File;
import java.util.Map;

public class PristineStats extends ProfileManagedStorage implements StorageManager.AutoSaveable {

    private static final long INACTIVITY_LIMIT_MS = 120_000L;
    private static PristineStats INSTANCE;

    @Getter
    private PristineData data = new PristineData();

    @Getter
    private volatile boolean trackingEnabled = true;

    public final PowderStats.RateInfo rateInfo = new PowderStats.RateInfo();
    public final PowderStats.RateInfo procRateInfo = new PowderStats.RateInfo();
    public final PowderStats.RateInfo roughRateInfo = new PowderStats.RateInfo();

    private long sessionActiveTimeMs = 0L;
    private boolean timerRunning = false;
    private boolean timerStartedOnce = false;
    private boolean inactivityFlagged = false;
    private long timerStartTime = 0L;
    private long lastActivityTime = 0L;

    private PristineStats() {
        super("pristine_stats.json");
    }

    public static PristineStats getInstance() {
        if (INSTANCE == null) INSTANCE = new PristineStats();
        return INSTANCE;
    }

    public static long[] getGemBreakdown(PristineData data, String gem) {
        long flawed = data.gemstones.getOrDefault("Flawed_" + gem, 0L);
        long fine = data.gemstones.getOrDefault("Fine_" + gem, 0L);
        long flawless = data.gemstones.getOrDefault("Flawless_" + gem, 0L);
        long totalFlawed = flawed + fine * 80L + flawless * 6400L;
        long fl = totalFlawed / 6400L;
        long rem = totalFlawed % 6400L;
        long fi = rem / 80L;
        long fw = rem % 80L;
        return new long[]{fl, fi, fw};
    }

    public boolean toggleTracking() {
        trackingEnabled = !trackingEnabled;
        if (trackingEnabled) {
            data.lastPristineMs = System.currentTimeMillis();
        } else {
            pauseTimer();
        }
        return trackingEnabled;
    }

    public boolean shouldAutoStop() {
        return data.lastPristineMs > 0L && (System.currentTimeMillis() - data.lastPristineMs) > INACTIVITY_LIMIT_MS;
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
        if (ATHRConfig.feature != null && ATHRConfig.feature.mining.pristineTrackerConfig.pauseOnChat && ChatUtils.isChatOpen()) {
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
        return ATHRConfig.feature.mining.pristineTrackerConfig.pristineTracker && trackingEnabled && SkyblockData.getCurrentLocation() == SkyblockData.Location.CRYSTAL_HOLLOWS;
    }

    public long getSessionTimeMs() {
        return sessionActiveTimeMs;
    }

    public static long getRoughEquivTotal(PristineData data) {
        long total = 0L;
        for (Map.Entry<String, Long> entry : data.gemstones.entrySet()) {
            long v = entry.getValue();
            if (v == 0L) continue;
            if (entry.getKey().startsWith("Flawed_")) total += v * 80L;
            else if (entry.getKey().startsWith("Fine_")) total += v * 6400L;
            else if (entry.getKey().startsWith("Flawless_")) total += v * 512000L;
        }
        return total;
    }

    public double getGemstonesPerHour() {
        return rateInfo.perHour;
    }

    public double getProcsPerHour() {
        return procRateInfo.perHour;
    }

    public double getRoughGemstonesPerHour() {
        return roughRateInfo.perHour;
    }

    public void tickRates() {
        PowderStats.tick(rateInfo, data.gemstones.values().stream().mapToLong(Long::longValue).sum());
        PowderStats.tick(procRateInfo, data.totalProcs);
        PowderStats.tick(roughRateInfo, getRoughEquivTotal(data));
    }

    public void onWorldChange() {
        rateInfo.clear();
        procRateInfo.clear();
        roughRateInfo.clear();
    }

    @Override
    public void load() {
        File f = resolveFile();
        if (f == null) return;
        PristineData loaded = StorageManager.loadSafe(f, PristineData.class, GsonBuilder.GSON);
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
        data.reset();
        sessionActiveTimeMs = 0L;
        rateInfo.clear();
        procRateInfo.clear();
        roughRateInfo.clear();
        pauseTimer();
    }
}
