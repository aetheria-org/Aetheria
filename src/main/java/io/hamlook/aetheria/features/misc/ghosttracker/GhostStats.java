package io.hamlook.aetheria.features.misc.ghosttracker;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.GsonBuilder;
import io.hamlook.aetheria.core.ProfileManagedStorage;
import io.hamlook.aetheria.core.StorageManager;
import io.hamlook.aetheria.features.price.PriceMap;
import io.hamlook.aetheria.utils.chat.ChatUtils;

import java.io.File;

public class GhostStats extends ProfileManagedStorage implements StorageManager.AutoSaveable {

    private static final long INACTIVITY_LIMIT_MS = 120_000L; // 2 minutes
    private static GhostStats INSTANCE;
    @Expose
    public int totalKills = 0;
    @Expose
    public long activeTimeMs = 0L;
    @Expose
    public int totalSorrow = 0;
    @Expose
    public int totalVolta = 0;
    @Expose
    public int totalPlasma = 0;
    @Expose
    public int totalBoots = 0;
    @Expose
    public int totalBagOfCash = 0;
    @Expose
    public int totalCoins = 0;
    @Expose
    public int totalScavenger = 0;
    @Expose
    public int totalMagicFind = 0;
    @Expose
    public int mfDropCount = 0;
    @Expose
    public int highestMagicFind = 0;
    @Expose
    public long totalXp = 0;
    @Expose
    public int killsSinceLastSorrow = 0;
    @Expose
    public int killsSinceLastVolta = 0;
    @Expose
    public int killsSinceLastPlasma = 0;
    @Expose
    public int killsSinceLastBoots = 0;
    @Expose
    public int killsSinceLastBagOfCash = 0;
    @Expose
    public int killsSinceLastCoins = 0;
    private boolean timerRunning = false;
    private boolean timerStartedOnce = false;
    private boolean inactivityFlagged = false;
    private long timerStartTime = 0L;
    private long lastActivityTime = 0L;

    private GhostStats() {
        super("ghost_stats.json");
    }

    public static GhostStats getInstance() {
        if (INSTANCE == null) INSTANCE = new GhostStats();
        return INSTANCE;
    }

    @Override
    public void load() {
        File f = resolveFile();
        if (f == null) return;
        GhostStats loaded = StorageManager.loadSafe(f, GhostStats.class, GsonBuilder.GSON);
        if (loaded != null) {
            this.totalKills = loaded.totalKills;
            this.activeTimeMs = loaded.activeTimeMs;
            this.totalSorrow = loaded.totalSorrow;
            this.totalVolta = loaded.totalVolta;
            this.totalPlasma = loaded.totalPlasma;
            this.totalBoots = loaded.totalBoots;
            this.totalBagOfCash = loaded.totalBagOfCash;
            this.totalCoins = loaded.totalCoins;
            this.totalScavenger = loaded.totalScavenger;
            this.totalMagicFind = loaded.totalMagicFind;
            this.mfDropCount = loaded.mfDropCount;
            this.highestMagicFind = loaded.highestMagicFind;
            this.totalXp = loaded.totalXp;
            this.killsSinceLastSorrow = loaded.killsSinceLastSorrow;
            this.killsSinceLastVolta = loaded.killsSinceLastVolta;
            this.killsSinceLastPlasma = loaded.killsSinceLastPlasma;
            this.killsSinceLastBoots = loaded.killsSinceLastBoots;
            this.killsSinceLastBagOfCash = loaded.killsSinceLastBagOfCash;
            this.killsSinceLastCoins = loaded.killsSinceLastCoins;
        }
    }

    @Override
    public void autoSave() {
        save();
    }

    public void save() {
        File f = resolveFile();
        if (f == null) return;
        StorageManager.saveAtomic(f, this, GsonBuilder.GSON);
    }

    public void reset() {
        totalKills = 0;
        activeTimeMs = 0L;
        totalSorrow = 0;
        totalVolta = 0;
        totalPlasma = 0;
        totalBoots = 0;
        totalBagOfCash = 0;
        totalCoins = 0;
        totalScavenger = 0;
        totalMagicFind = 0;
        mfDropCount = 0;
        highestMagicFind = 0;
        totalXp = 0;
        killsSinceLastSorrow = 0;
        killsSinceLastVolta = 0;
        killsSinceLastPlasma = 0;
        killsSinceLastBoots = 0;
        killsSinceLastBagOfCash = 0;
        killsSinceLastCoins = 0;
        timerRunning = false;
        timerStartedOnce = false;
        inactivityFlagged = false;
        timerStartTime = 0L;
        lastActivityTime = 0L;
        save();
    }

    public void updateActivity() {
        if (!timerStartedOnce) {
            timerStartTime = System.currentTimeMillis();
            timerRunning = true;
            timerStartedOnce = true;
        } else if (!timerRunning) {
            if (inactivityFlagged) {
                activeTimeMs -= INACTIVITY_LIMIT_MS;
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
        if (ATHRConfig.feature != null && ATHRConfig.feature.misc.ghostTrackerConfig.pauseOnChat && ChatUtils.isChatOpen()) {
            activeTimeMs += now - timerStartTime;
            timerRunning = false;
            return;
        }
        activeTimeMs += now - timerStartTime;
        timerStartTime = now;
        if (now - lastActivityTime > INACTIVITY_LIMIT_MS) {
            timerRunning = false;
            inactivityFlagged = true;
        }
    }

    public double getKillsPerHour() {
        if (activeTimeMs < 1_000L || totalKills == 0) return 0.0;
        return totalKills / (activeTimeMs / 3_600_000.0);
    }

    public double getXpPerHour() {
        if (activeTimeMs < 1_000L || totalXp == 0) return 0.0;
        return totalXp / (activeTimeMs / 3_600_000.0);
    }

    public float avgMagicFind() {
        if (mfDropCount > 0) return (float) totalMagicFind / mfDropCount;
        return 0f;
    }

    public void addKill(int kills) {
        updateActivity();
        totalKills += kills;
        killsSinceLastSorrow += kills;
        killsSinceLastVolta += kills;
        killsSinceLastPlasma += kills;
        killsSinceLastBoots += kills;
        killsSinceLastBagOfCash += kills;
        killsSinceLastCoins += kills;
    }

    public void addScavenger(int amount) {
        totalScavenger += amount;
    }

    public void recordMagicFind(int mf) {
        totalMagicFind += mf;
        mfDropCount++;
        if (mf > highestMagicFind) highestMagicFind = mf;
    }

    public void addDrop(String dropName) {
        switch (dropName) {
            case "Sorrow":
                totalSorrow++;
                killsSinceLastSorrow = 0;
                break;
            case "Volta":
                totalVolta++;
                killsSinceLastVolta = 0;
                break;
            case "Plasma":
                totalPlasma++;
                killsSinceLastPlasma = 0;
                break;
            case "Ghostly Boots":
                totalBoots++;
                killsSinceLastBoots = 0;
                break;
            case "Bag of Cash":
                totalBagOfCash++;
                killsSinceLastBagOfCash = 0;
                break;
            case "Coins":
                totalCoins++;
                killsSinceLastCoins = 0;
                break;
        }
    }

    public void addXp(long xp) {
        totalXp += xp;
    }

    public long getSessionDurationMs() {
        return activeTimeMs;
    }

    public long getEstimatedProfit() {
        long profit = 0;

        double sorrowPrice = PriceMap.Cached.getPrice("SORROW");
        double voltaPrice = PriceMap.Cached.getPrice("VOLTA");
        double plasmaPrice = PriceMap.Cached.getPrice("PLASMA");
        double bootsPrice = PriceMap.Cached.getPrice("GHOSTLY_BOOTS");
        double bagPrice = PriceMap.Cached.getPrice("BAG_OF_CASH");

        if (sorrowPrice > 0) profit += (long) (totalSorrow * sorrowPrice);
        if (voltaPrice > 0) profit += (long) (totalVolta * voltaPrice);
        if (plasmaPrice > 0) profit += (long) (totalPlasma * plasmaPrice);
        if (bootsPrice > 0) profit += (long) (totalBoots * bootsPrice);
        if (bagPrice > 0) profit += (long) (totalBagOfCash * bagPrice);

        profit += totalScavenger;

        return profit;
    }
}
