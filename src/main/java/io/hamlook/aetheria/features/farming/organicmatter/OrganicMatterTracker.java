package io.hamlook.aetheria.features.farming.organicmatter;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.farming.OrganicMatterTrackerConfig;
import io.hamlook.aetheria.features.farming.data.Crop;
import io.hamlook.aetheria.features.misc.itemlog.ItemPickupLog;
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
public class OrganicMatterTracker {

    private static final Pattern DROP_PATTERN = Pattern.compile("DROP! \\S+ dropped (\\d+)x (.+)!");

    private static final double MS_PER_HOUR = 3_600_000.0;
    private static final long INACTIVITY_LIMIT_MS = 7_000L;

    private static boolean timerRunning = false;
    private static boolean timerStartedOnce = false;
    private static boolean inactivityFlagged = false;
    private static long timerStartTime = 0L;
    private static long lastActivityTime = 0L;
    private static long sessionActiveTimeMs = 0L;
    private static boolean listenerRegistered = false;

    private static void ensureTimerInitialized() {
        if (timerRunning) return;

        if (timerStartedOnce && inactivityFlagged) {
            OrganicMatterTrackerData data = OrganicMatterTrackerData.getInstance();
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

    private static void flushSegment(long now, OrganicMatterTrackerData data) {
        long delta = now - timerStartTime;
        data.setActiveTimeMs(data.getActiveTimeMs() + delta);
        sessionActiveTimeMs += delta;
    }

    private static void timerTick() {
        if (!timerRunning) return;
        long now = System.currentTimeMillis();
        OrganicMatterTrackerData data = OrganicMatterTrackerData.getInstance();
        if (ATHRConfig.feature != null && config().pauseOnChat && ChatUtils.isChatOpen()) {
            flushSegment(now, data);
            timerRunning = false;
            return;
        }
        flushSegment(now, data);
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
        return OrganicMatterTrackerData.getInstance().getActiveTimeMs();
    }

    public static long getSessionTimeMs() {
        return sessionActiveTimeMs;
    }

    private static double activeHours() {
        return getActiveTimeMs() / MS_PER_HOUR;
    }

    private static OrganicMatterTrackerConfig config() {
        return ATHRConfig.feature.farming.organicMatterTracker;
    }

    private static boolean isEnabled() {
        return ATHRConfig.feature != null && config().enabled;
    }

    private static boolean isInFarmingLocation() {
        SkyblockData.Location location = SkyblockData.getCurrentLocation();
        return location == SkyblockData.Location.BARN
                || location == SkyblockData.Location.PRIVATE_ISLAND
                || location == SkyblockData.Location.GARDEN;
    }

    private static boolean locationOk() {
        return !config().requireFarmingIsland || isInFarmingLocation();
    }

    public static boolean isTracked(OrganicMatterCrop crop) {
        OrganicMatterCrop[] all = OrganicMatterCrop.all();
        int index = -1;
        for (int i = 0; i < all.length; i++) {
            if (all[i] == crop) {
                index = i;
                break;
            }
        }
        return index >= 0 && config().trackedCrops.contains(index);
    }

    private static boolean isIdTracked(String id) {
        if (id == null) return false;
        for (OrganicMatterCrop crop : OrganicMatterCrop.all()) {
            if (!isTracked(crop)) continue;
            if (id.equals(crop.rawId) || id.equals(crop.enchantedId) || id.equals(crop.blockId)) return true;
        }
        return false;
    }

    public static void reset() {
        OrganicMatterTrackerData.getInstance().reset();
        timerRunning = false;
        timerStartedOnce = false;
        inactivityFlagged = false;
        lastActivityTime = 0L;
        sessionActiveTimeMs = 0L;
    }

    private static void ensureListenerRegistered() {
        if (listenerRegistered) return;
        ItemPickupLog log = ItemPickupLog.getInstance();
        if (log == null) return;
        log.addRichItemChangeListener(OrganicMatterTracker::onItemLogChange);
        listenerRegistered = true;
    }

    private static void onItemLogChange(String internalId, String displayName, int delta) {
        if (!isEnabled()) return;
        if (!locationOk()) return;
        if (delta <= 0) return;
        if (Crop.findByRawId(internalId) == null) return;
        if (!isIdTracked(internalId)) return;

        OrganicMatterTrackerData.getInstance().getCounts().merge(internalId, (long) delta, Long::sum);
        updateActivity();
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

        String itemName = m.group(2);

        String id = OrganicMatterCrop.findByChatName(itemName);

        if (id == null) id = OrganicMatterCrop.findRawByChatName(itemName);
        if (id == null) return;
        if (!isIdTracked(id)) return;

        OrganicMatterTrackerData.getInstance().getCounts().merge(id, quantity, Long::sum);
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
        if (!config().persistAcrossSessions) {
            reset();
        }
    }

    public static long getCount(String id) {
        return OrganicMatterTrackerData.getInstance().getCounts().getOrDefault(id, 0L);
    }

    public static Map<String, Long> getCounts() {
        return OrganicMatterTrackerData.getInstance().getCounts();
    }

    public static double totalOrganicMatter() {
        double total = 0.0;
        for (Map.Entry<String, Long> entry : OrganicMatterTrackerData.getInstance().getCounts().entrySet()) {
            total += entry.getValue() * OrganicMatterValues.getValue(entry.getKey());
        }
        return total;
    }

    public static double organicMatterPerHour() {
        double hours = activeHours();
        return hours <= 0.0 ? 0.0 : totalOrganicMatter() / hours;
    }

    public static double getCropOmRate(OrganicMatterCrop crop) {
        double hours = activeHours();
        if (hours <= 0.0) return 0.0;

        double total = 0.0;
        total += getCount(crop.rawId) * OrganicMatterValues.getValue(crop.rawId);
        if (crop.enchantedId != null) total += getCount(crop.enchantedId) * OrganicMatterValues.getValue(crop.enchantedId);
        if (crop.blockId != null) total += getCount(crop.blockId) * OrganicMatterValues.getValue(crop.blockId);

        return total / hours;
    }

    public static long totalItems() {
        long total = 0L;
        for (Map.Entry<String, Long> entry : OrganicMatterTrackerData.getInstance().getCounts().entrySet()) {
            total += entry.getValue();
        }
        return total;
    }
}

