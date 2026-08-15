package io.hamlook.aetheria.features.mining.powder;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.GsonBuilder;
import io.hamlook.aetheria.core.ProfileManagedStorage;
import io.hamlook.aetheria.core.StorageManager;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.data.SkyblockData;
import lombok.Getter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PowderStats extends ProfileManagedStorage implements StorageManager.AutoSaveable {

    private static final long INACTIVITY_LIMIT_MS = 120_000L;
    private static PowderStats INSTANCE;

    @Getter
    private PowderData data = new PowderData();

    public final RateInfo gemstoneInfo = new RateInfo();
    public final RateInfo chestInfo = new RateInfo();
    public final RateInfo hardStoneInfo = new RateInfo();

    private boolean tracking = true;

    private long sessionActiveTimeMs = 0L;
    private boolean timerRunning = false;
    private boolean timerStartedOnce = false;
    private boolean inactivityFlagged = false;
    private long timerStartTime = 0L;
    private long lastActivityTime = 0L;

    private PowderStats() {
        super("powder_stats.json");
    }

    public static PowderStats getInstance() {
        if (INSTANCE == null) INSTANCE = new PowderStats();
        return INSTANCE;
    }

    public static void tick(RateInfo info, long current) {
        info.estimated = current;
        long difference = info.estimated - info.lastEstimated;
        info.lastEstimated = info.estimated;

        if (difference == info.estimated) return;

        if (!info.perMin.isEmpty())
            info.perHour = info.perMin.stream().mapToLong(Long::longValue).average().orElse(0) * 3600;
        info.perMin.add(difference);

        if (difference == 0L) {
            info.stoppedChecks++;
            if (info.stoppedChecks >= 60) {
                info.stoppedChecks = 0;
                info.perMin.clear();
                info.perHour = 0.0;
            }
        } else {
            info.stoppedChecks = 0;
        }
    }

    public static String gemKey(String quality, String gem) {
        return quality + "_" + gem;
    }

    public static long[] getGemBreakdown(PowderData data, String gem) {
        long rough = data.gemstones.getOrDefault(gemKey("Rough", gem), 0L);
        long flawed = data.gemstones.getOrDefault(gemKey("Flawed", gem), 0L);
        long fine = data.gemstones.getOrDefault(gemKey("Fine", gem), 0L);
        long flawless = data.gemstones.getOrDefault(gemKey("Flawless", gem), 0L);

        long totalRough = rough + flawed * 80L + fine * 6400L + flawless * 512000L;

        long fl = totalRough / 512000L;
        long rem = totalRough % 512000L;
        long fi = rem / 6400L;
        rem = rem % 6400L;
        long fw = rem / 80L;
        long rgh = rem % 80L;
        return new long[]{fl, fi, fw, rgh};
    }

    public boolean isTrackingEnabled() {
        return tracking;
    }

    public boolean toggleTracking() {
        tracking = !tracking;
        if (!tracking) pauseTimer();
        return tracking;
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
        if (ATHRConfig.feature != null && ATHRConfig.feature.mining.powderTrackerConfig.pauseOnChat && ChatUtils.isChatOpen()) {
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
        return ATHRConfig.feature.mining.powderTrackerConfig.powderTracker && tracking && SkyblockData.getCurrentLocation() == SkyblockData.Location.CRYSTAL_HOLLOWS;
    }

    public long getSessionTimeMs() {
        return sessionActiveTimeMs;
    }

    public double getGemstonePerHour() {
        return gemstoneInfo.perHour;
    }

    public double getChestsPerHour() {
        return chestInfo.perHour;
    }

    public double getHardStonePerHour() {
        return hardStoneInfo.perHour;
    }

    public void tickRates() {
        tick(gemstoneInfo, data.gemstonePowder);
        tick(chestInfo, data.totalChestsPicked);
        tick(hardStoneInfo, data.hardStone + data.hardStoneCompacted * 9L);
    }

    public void onWorldChange() {
        gemstoneInfo.clear();
        chestInfo.clear();
        hardStoneInfo.clear();
    }

    @Override
    public void load() {
        File f = resolveFile();
        if (f == null) return;
        PowderData loaded = StorageManager.loadSafe(f, PowderData.class, GsonBuilder.GSON);
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
        gemstoneInfo.clear();
        chestInfo.clear();
        hardStoneInfo.clear();
        pauseTimer();
    }

    public static class RateInfo {
        public final List<Long> perMin = new ArrayList<>();
        public long estimated = 0L;
        public long lastEstimated = 0L;
        public int stoppedChecks = 0;
        public double perHour = 0.0;

        public void clear() {
            estimated = 0L;
            lastEstimated = 0L;
            stoppedChecks = 0;
            perHour = 0.0;
            perMin.clear();
        }
    }
}
