package io.hamlook.aetheria.features.farming.farmingtracker;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.farming.FarmingApi;
import io.hamlook.aetheria.features.farming.data.Crop;
import io.hamlook.aetheria.features.misc.itemlog.ItemPickupLog;
import io.hamlook.aetheria.features.price.PriceMap;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.data.SkyblockData;
import io.hamlook.aetheria.api.event.HandleEvent;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import io.hamlook.aetheria.events.ASMTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import io.hamlook.aetheria.events.ASMChatEvent;
import io.hamlook.aetheria.events.ASMWorldUnloadEvent;

@RegisterEvents
public class FarmingTracker {

    private static final Pattern DROP_PATTERN = Pattern.compile("DROP! \\S+ dropped (\\d+)x (.+)!");

    private static final double CROPS_PER_HOUR_THRESHOLD = 3_600_000.0;

    private static final long INACTIVITY_LIMIT_MS = 7_000L;

    private static boolean listenerRegistered = false;
    private static boolean timerRunning = false;
    private static boolean timerStartedOnce = false;
    private static boolean inactivityFlagged = false;
    private static long timerStartTime = 0L;
    private static long lastActivityTime = 0L;

    private static void ensureTimerInitialized() {
        if (timerRunning) return;

        if (timerStartedOnce && inactivityFlagged) {
            FarmingTrackerData data = FarmingTrackerData.getInstance();
            data.setActiveTimeMs(data.getActiveTimeMs() - INACTIVITY_LIMIT_MS);
            inactivityFlagged = false;
        }

        timerStartTime = System.currentTimeMillis();
        timerRunning = true;
        timerStartedOnce = true;
    }

    private static void updateActivity() {
        ensureTimerInitialized();
        lastActivityTime = System.currentTimeMillis();
    }

    private static void timerTick() {
        if (!timerRunning) return;
        long now = System.currentTimeMillis();
        FarmingTrackerData data = FarmingTrackerData.getInstance();
        if (ATHRConfig.feature != null && ATHRConfig.feature.farming.farmingTracker.pauseOnChat && ChatUtils.isChatOpen()) {
            data.setActiveTimeMs(data.getActiveTimeMs() + (now - timerStartTime));
            timerRunning = false;
            return;
        }
        data.setActiveTimeMs(data.getActiveTimeMs() + (now - timerStartTime));
        timerStartTime = now;
        if (now - lastActivityTime > INACTIVITY_LIMIT_MS) {
            timerRunning = false;
            inactivityFlagged = true;
        }
    }

    public static boolean isPaused() {
        return timerStartedOnce && !timerRunning;
    }

    public static long getActiveTimeMs() {
        return FarmingTrackerData.getInstance().getActiveTimeMs();
    }

    private static double activeHours() {
        return getActiveTimeMs() / CROPS_PER_HOUR_THRESHOLD;
    }

    private static boolean isEnabled() {
        return ATHRConfig.feature != null && ATHRConfig.feature.farming.farmingTracker.enabled;
    }

    private static boolean isInFarmingLocation() {
        SkyblockData.Location location = SkyblockData.getCurrentLocation();
        return location == SkyblockData.Location.BARN
                || location == SkyblockData.Location.PRIVATE_ISLAND
                || location == SkyblockData.Location.GARDEN;
    }

    private static boolean locationOk() {
        return !ATHRConfig.feature.farming.farmingTracker.requireFarmingIsland || isInFarmingLocation();
    }

    public static void reset() {
        FarmingTrackerData.getInstance().reset();
        timerRunning = false;
        timerStartedOnce = false;
        inactivityFlagged = false;
        lastActivityTime = 0L;
    }

    private static void ensureListenerRegistered() {
        if (listenerRegistered) return;
        ItemPickupLog log = ItemPickupLog.getInstance();
        if (log == null) return;
        log.addRichItemChangeListener(FarmingTracker::onItemLogChange);
        listenerRegistered = true;
    }

    private static void onItemLogChange(String internalId, String displayName, int delta) {
        if (!isEnabled()) return;
        if (!locationOk()) return;
        if (delta <= 0) return;
        String key = resolveTrackedId(internalId, displayName);
        if (key == null) return;
        if (!toolHoldOk()) return;

        FarmingTrackerData.getInstance().getCounts().merge(key, (long) delta, Long::sum);
        updateActivity();
    }

    /** Canonical crop id for a pickup, or null when it is not a tracked crop. */
    private static String resolveTrackedId(String internalId, String displayName) {
        Crop byId = Crop.findByRawId(internalId);
        if (byId != null) return byId.rawId;
        // Fallback: match by display name (covers enchanted/block tiers and
        // any future server id quirks).
        Crop byName = Crop.findByDropName(displayName);
        if (byName == null) return null;
        if (displayName.equals(byName.enchantedChatName) && byName.enchantedId != null) return byName.enchantedId;
        if (displayName.equals(byName.blockChatName) && byName.blockId != null) return byName.blockId;
        return byName.rawId;
    }

    private static boolean toolHoldOk() {
        if (ATHRConfig.feature == null) return false;
        if (!ATHRConfig.feature.farming.farmingTracker.requireFarmingToolHold) return true;
        return FarmingApi.heldFarmingToolRecently();
    }

    @HandleEvent
    public void onChat(ASMChatEvent event) {
        if (!isEnabled()) return;
        if (!SkyblockData.isOnSkyblock()) return;
        if (!locationOk()) return;

        String raw = ChatUtils.clean(event);
        String stripped = ColorUtils.stripColor(raw);

        if (ChatUtils.isPlayerMessage(stripped) || ChatUtils.isPartyMessage(stripped)
                || ChatUtils.getGuildSender(stripped) != null
                || ChatUtils.isMsgReceived(raw) || ChatUtils.isMsgSent(raw)) {
            return;
        }

        Matcher m = DROP_PATTERN.matcher(stripped);
        if (!m.find()) return;

        long quantity;
        try {
            quantity = Long.parseLong(m.group(1));
        } catch (NumberFormatException e) {
            return;
        }

        String id = Crop.findByChatName(m.group(2));
        if (id == null) return;
        if (!toolHoldOk()) return;

        FarmingTrackerData.getInstance().getCounts().merge(id, quantity, Long::sum);
        updateActivity();
    }

    @HandleEvent
    public void onTick(ASMTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!SkyblockData.isOnSkyblock()) return;
        ensureListenerRegistered();
        timerTick();
    }

    @HandleEvent
    public void onWorldUnload(ASMWorldUnloadEvent event) {
        if (ATHRConfig.feature == null) return;
        if (!ATHRConfig.feature.farming.farmingTracker.persistAcrossSessions) {
            reset();
        }
    }

    public static long getCount(String id) {
        return FarmingTrackerData.getInstance().getCounts().getOrDefault(id, 0L);
    }

    public static double getCropRate(Crop crop) {
        double hours = activeHours();
        if (hours <= 0.0) return 0.0;

        long total = 0L;
        Long rawRatio = Crop.rawEquivalentOf(crop.rawId);
        if (rawRatio != null) total += getCount(crop.rawId) * rawRatio;

        Long enchRatio = Crop.rawEquivalentOf(crop.enchantedId);
        if (enchRatio != null) total += getCount(crop.enchantedId) * enchRatio;

        if (crop.blockId != null) {
            Long blockRatio = Crop.rawEquivalentOf(crop.blockId);
            if (blockRatio != null) total += getCount(crop.blockId) * blockRatio;
        }

        return total / hours;
    }

    public static double currentValue() {
        double total = 0.0;
        for (Map.Entry<String, Long> entry : FarmingTrackerData.getInstance().getCounts().entrySet()) {
            double price = PriceMap.Cached.getDPrice(entry.getKey());
            if (price > 0) total += entry.getValue() * price;
        }
        return total;
    }

    public static double coinsPerHour() {
        double hours = activeHours();
        return hours <= 0.0 ? 0.0 : currentValue() / hours;
    }

    public static long totalRawCrops() {
        long total = 0L;
        for (Map.Entry<String, Long> entry : FarmingTrackerData.getInstance().getCounts().entrySet()) {
            Long ratio = Crop.rawEquivalentOf(entry.getKey());
            if (ratio != null) total += entry.getValue() * ratio;
        }
        return total;
    }

    public static double cropsPerHour() {
        double hours = activeHours();
        return hours <= 0.0 ? 0.0 : totalRawCrops() / hours;
    }
}
