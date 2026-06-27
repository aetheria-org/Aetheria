package io.hamlook.aetheria.features.diana;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.GsonBuilder;
import io.hamlook.aetheria.core.ProfileManagedStorage;
import io.hamlook.aetheria.core.StorageManager;
import io.hamlook.aetheria.features.price.PriceMap;
import io.hamlook.aetheria.utils.Utils;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.data.SkyblockData;
import io.hamlook.aetheria.utils.data.TablistParser;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StringUtils;

import java.io.File;

public class DianaStats extends ProfileManagedStorage implements StorageManager.AutoSaveable {

    private static final long INACTIVITY_LIMIT_MS = 120_000L;
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static DianaStats INSTANCE;
    public volatile String lastDropType = null;
    public volatile long lastDropAmount = 0L;
    public volatile long lastDropMs = 0L;
    @Getter
    private DianaData data = new DianaData();
    @Getter
    private volatile boolean trackingEnabled = true;
    private volatile long lastLootShareMs = 0L;
    private volatile boolean hasTrackedInqLs = false;
    private long sessionStartMs = -1L;
    private long sessionActiveTimeMs = 0L;
    private boolean timerRunning = false;
    private boolean timerStartedOnce = false;
    private boolean inactivityFlagged = false;
    private long timerStartTime = 0L;
    private long lastActivityTime = 0L;

    private DianaStats() {
        super("diana_stats.json");
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
        return Utils.formatDuration(ms);
    }

    @Override
    public void load() {
        File f = resolveFile();
        if (f == null) return;
        DianaData loaded = StorageManager.loadSafe(f, DianaData.class, GsonBuilder.GSON);
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
        sessionActiveTimeMs = 0L;
    }

    public boolean toggleTracking() {
        trackingEnabled = !trackingEnabled;
        return trackingEnabled;
    }

    public boolean isTracking() {
        return trackingEnabled && hasSpadeInHotbar() && SkyblockData.getCurrentLocation() == SkyblockData.Location.HUB;
    }

    public boolean isDianaMayor() {
        return TablistParser.isDianaMayor();
    }

    public void onClientLogin() {
        sessionStartMs = System.currentTimeMillis();
        sessionActiveTimeMs = 0L;
    }

    public void onClientLogout() {
        pauseTimer();
        sessionStartMs = -1L;
    }

    public long getSessionTimeMs() {
        return sessionActiveTimeMs;
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
        if (ATHRConfig.feature != null && ATHRConfig.feature.diana.pauseOnChat && ChatUtils.isChatOpen()) {
            data.activeTimeMs += now - timerStartTime;
            sessionActiveTimeMs += now - timerStartTime;
            timerRunning = false;
            return;
        }
        if (isTracking() && isDianaMayor()) {
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
        return data.totalMobs > 0 ? String.format("%.2f%%", getMobPercent(count)) : "-.--%";
    }

    public long getEstimatedProfit() {
        long profit = 0;
        profit += (long) (data.totalChimeras * PriceMap.Cached.getPrice("CHIMERA_1"));
        profit += (long) (data.griffinFeathers * PriceMap.Cached.getPrice("GRIFFIN_FEATHER"));
        profit += (long) (data.dwarfTurtleShelmets * PriceMap.Cached.getPrice("DWARF_TURTLE_SHELMET"));
        profit += (long) (data.antiqueRemedies * PriceMap.Cached.getPrice("ANTIQUE_REMEDIES"));
        profit += (long) (data.crochetTigerPlushies * PriceMap.Cached.getPrice("CROCHET_TIGER_PLUSHIE"));
        profit += (long) (data.totalSticks * PriceMap.Cached.getPrice("DAEDALUS_STICK"));
        profit += (long) (data.totalRelics * PriceMap.Cached.getPrice("MINOS_RELIC"));
        profit += (long) (data.souvenirs * PriceMap.Cached.getPrice("WASHED_UP_SOUVENIR"));
        profit += (long) (data.crownsOfGreed * PriceMap.Cached.getPrice("CROWN_OF_GREED"));
        profit += data.totalCoins;
        return profit;
    }
}