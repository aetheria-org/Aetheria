package com.jef.justenoughfakepixel.features.mining.powder;

import com.jef.justenoughfakepixel.core.JefGsonBuilder;
import com.jef.justenoughfakepixel.core.JefStorageManager;
import lombok.Getter;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class PowderStats implements JefStorageManager.Managed, JefStorageManager.AutoSaveable {

    private static PowderStats INSTANCE;
    public final RateInfo gemstoneInfo = new RateInfo();
    public final RateInfo chestInfo = new RateInfo();
    public final RateInfo hardStoneInfo = new RateInfo();
    private File file = null;
    @Getter
    private PowderData data = new PowderData();
    private boolean tracking = true;

    private PowderStats() {
    }

    public static PowderStats getInstance() {
        if (INSTANCE == null) INSTANCE = new PowderStats();
        return INSTANCE;
    }

    private static void tick(RateInfo info, long current) {
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

    public static String fmtRate(double perHour) {
        if (perHour <= 0) return "0";
        long v = (long) perHour;
        if (v >= 1_000_000) return String.format("%.1fM", v / 1_000_000.0);
        if (v >= 1_000) return String.format("%.1fK", v / 1_000.0);
        return String.valueOf(v);
    }

    public static String fmtNum(long n) {
        if (n >= 1_000_000) return String.format("%.2fM", n / 1_000_000.0);
        if (n >= 1_000) return String.format("%,d", n);
        return String.valueOf(n);
    }

    public boolean isTrackingEnabled() {
        return tracking;
    }

    public boolean toggleTracking() {
        tracking = !tracking;
        return tracking;
    }

    @Override
    public void initFile(File configDir) {
        this.file = new File(configDir, "powder_stats.json");
    }

    @Override
    public void load() {
        PowderData loaded = JefStorageManager.loadSafe(file, PowderData.class, JefGsonBuilder.GSON);
        if (loaded != null) data = loaded;
    }

    public void save() {
        JefStorageManager.saveAtomic(file, data, JefGsonBuilder.GSON);
    }

    @Override
    public void autoSave() {
        save();
    }

    public void reset() {
        data.reset();
        gemstoneInfo.clear();
        chestInfo.clear();
        hardStoneInfo.clear();
        save();
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