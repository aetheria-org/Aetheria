package com.jef.justenoughfakepixel.features.diana;

import com.jef.justenoughfakepixel.core.JefGsonBuilder;
import com.jef.justenoughfakepixel.core.JefStorageManager;
import com.jef.justenoughfakepixel.utils.data.SkyblockData;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StringUtils;

import java.io.*;

public class DianaStats implements JefStorageManager.Managed, JefStorageManager.AutoSaveable {

    private static final long INACTIVITY_LIMIT_MS = 90_000L;
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static DianaStats INSTANCE;
    public volatile String lastDropType = null;
    public volatile long lastDropAmount = 0L;
    public volatile long lastDropMs = 0L;
    private File file = null;
    @Getter
    private DianaData data = new DianaData();
    @Getter
    private volatile boolean trackingEnabled = true;
    private volatile long lastLootShareMs = 0L;
    private volatile boolean hasTrackedInqLs = false;
    private long sessionStartMs = -1L;
    private boolean timerRunning = false;
    private boolean timerStartedOnce = false;
    private boolean inactivityFlagged = false;
    private long timerStartTime = 0L;
    private long lastActivityTime = 0L;

    private DianaStats() {
    }

    public static DianaStats getInstance() {
        if (INSTANCE == null) INSTANCE = new DianaStats();
        return INSTANCE;
    }

    public static boolean hasSpadeInHotbar() {
        if (mc.thePlayer == null) return false;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.mainInventory[i];
            if (stack != null && stack.hasDisplayName() && StringUtils.stripControlCodes(stack.getDisplayName()).contains("Ancestral Spade")) {
                return true;
            }
        }
        return false;
    }

    public static String formatTime(long ms) {
        if (ms <= 0) return "0s";
        long totalSeconds = ms / 1000;
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0 || days > 0) sb.append(hours).append("h ");
        if (minutes > 0 || hours > 0 || days > 0) sb.append(minutes).append("m ");
        if (sb.length() == 0) sb.append(seconds).append("s");
        return sb.toString().trim();
    }

    public static String fmtCoins(long coins) {
        if (coins >= 1_000_000) return String.format("%.1fM", coins / 1_000_000.0);
        if (coins >= 1_000) return String.format("%.1fK", coins / 1_000.0);
        return String.valueOf(coins);
    }

    @Override
    public void initFile(File configDir) {
        this.file = new File(configDir, "diana_stats.json");
    }

    @Override
    public void load() {
        DianaData loaded = JefStorageManager.loadSafe(file, DianaData.class, JefGsonBuilder.GSON);
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
        data = new DianaData();
        lastDropType = null;
        lastDropAmount = 0L;
        lastDropMs = 0L;
        lastLootShareMs = 0L;
        hasTrackedInqLs = false;
        sessionStartMs = (sessionStartMs > 0) ? System.currentTimeMillis() : -1L;
        timerRunning = false;
        timerStartedOnce = false;
        inactivityFlagged = false;
        timerStartTime = 0L;
        lastActivityTime = 0L;
    }

    public boolean toggleTracking() {
        trackingEnabled = !trackingEnabled;
        return trackingEnabled;
    }

    public boolean isTracking() {
        return trackingEnabled && hasSpadeInHotbar() && SkyblockData.getCurrentLocation() == SkyblockData.Location.HUB;
    }

    public void onClientLogin() {
        sessionStartMs = System.currentTimeMillis();
    }

    public void onClientLogout() {
        pauseTimer();
        sessionStartMs = -1L;
    }

    public long getSessionTimeMs() {
        if (sessionStartMs <= 0) return 0L;
        return System.currentTimeMillis() - sessionStartMs;
    }

    public void onLootshare() {
        lastLootShareMs = System.currentTimeMillis();
    }

    public boolean gotLootShareRecently(long seconds) {
        return (System.currentTimeMillis() - lastLootShareMs) / 1000L <= seconds;
    }

    public void onInqDeath() {
        if (hasTrackedInqLs) return;
        if (!gotLootShareRecently(3)) return;

        hasTrackedInqLs = true;
        data.totalInqsLootshared++;
        save();

        new Thread(() -> {
            try {
                Thread.sleep(2_000L);
            } catch (InterruptedException ignored) {
            }
            hasTrackedInqLs = false;
        }).start();
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
        data.activeTimeMs += now - timerStartTime;
        timerStartTime = now;
        if (now - lastActivityTime > INACTIVITY_LIMIT_MS) {
            timerRunning = false;
            inactivityFlagged = true;
        }
    }

    public void pauseTimer() {
        if (!timerRunning) return;
        long now = System.currentTimeMillis();
        data.activeTimeMs += now - timerStartTime;
        timerRunning = false;
        save();
    }

    public double getBph() {
        if (data.activeTimeMs < 1_000L || data.totalBorrows == 0) return 0.0;
        return data.totalBorrows / (data.activeTimeMs / 3_600_000.0);
    }

    public double getInqChance() {
        if (data.totalInqs == 0 || data.totalMobs == 0) return -1.0;
        return (double) data.totalInqs / data.totalMobs * 100.0;
    }

    public double getMobPercent(int mobCount) {
        if (mobCount == 0 || data.totalMobs == 0) return 0.0;
        return (double) mobCount / data.totalMobs * 100.0;
    }

    public String formatMobPct(int count) {
        return data.totalMobs > 0 ? String.format("%.2f%%", getMobPercent(count)) : "-.--%%";
    }
}