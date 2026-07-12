package io.hamlook.aetheria.features.farming;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.misc.itemlog.ItemPickupLog;
import io.hamlook.aetheria.features.price.PriceMap;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.data.SkyblockData;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tracks farming crop value and coins/hour for the current session, across
 * every SkyBlock crop.
 * <p>
 * Raw crops are counted from the item log (ItemPickupLog), since that's what
 * actually lands in your inventory from farming. Enchanted / compacted forms
 * are counted from "RARE DROP!" style chat lines instead, since compacting
 * raw crops into them would otherwise double-count them once they appear in
 * the inventory too.
 * <p>
 * Crop metadata (raw/enchanted/block id chains, chat-name lookups, raw-crop
 * -equivalent ratios, icons) lives in {@link Crop}'s static registry. Prices
 * live in {@link PriceMap}. This class only owns count-tracking, per-crop
 * rate calculation, and session timing.
 */
@RegisterEvents
public class FarmingTracker {

    // Drop-line matching happens on the ColorUtils.stripColor()'d text
    private static final Pattern DROP_PATTERN = Pattern.compile("DROP! \\S+ dropped (\\d+)x (.+)!");

    // Divisor to convert accumulated active-session milliseconds into hours,
    // used by every .../hour rate calculation below (coins, total crops, and
    // per-crop rates).
    private static final double CROPS_PER_HOUR_THRESHOLD = 3_600_000.0;

    // Pauses the coins/hour rate calc after 7s of no crop activity, so
    // AFK/break time doesn't dilute the rate. Mirrors PowderStats' pattern,
    // just with a much shorter threshold suited to farming's rapid drops.
    private static final long INACTIVITY_LIMIT_MS = 7_000L;

    private static boolean listenerRegistered = false;
    private static boolean timerRunning = false;
    private static boolean timerStartedOnce = false;
    private static boolean inactivityFlagged = false;
    private static long timerStartTime = 0L;
    private static long lastActivityTime = 0L;

    /**
     * (Re)starts the running clock from "now" — covers both the very first
     * start and resuming after a pause in one place, instead of duplicating
     * the timerStartedOnce/timerRunning branching across every caller.
     * Rolls back the INACTIVITY_LIMIT_MS of dead time that got baked into
     * activeTimeMs when the pause was flagged, if any.
     */
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

    /** Total elapsed active (non-paused) session time, in milliseconds. */
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
        if (Crop.findByRawId(internalId) == null) return;

        FarmingTrackerData.getInstance().getCounts().merge(internalId, (long) delta, Long::sum);
        updateActivity();
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (!isEnabled()) return;
        if (!SkyblockData.isOnSkyblock()) return;
        if (!locationOk()) return;

        String raw = ChatUtils.clean(event);
        String stripped = ColorUtils.stripColor(raw);

        // Guard against whispers, party/guild chat, or any player-authored message
        // that happens to contain drop-like text (e.g. someone pasting/mocking a
        // drop line, or "Party > Steve: lol dropped 99x Enchanted Melon!"). The
        // regex below uses find(), not an anchored full-line match, so without
        // this it could pick up a fake drop embedded in a longer message.
        // isMsgReceived/isMsgSent need the RAW (still-colored) string, since their
        // patterns match literal §-codes; isPlayerMessage/isPartyMessage/
        // getGuildSender need the STRIPPED string, since theirs don't.
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

        FarmingTrackerData.getInstance().getCounts().merge(id, quantity, Long::sum);
        updateActivity();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!SkyblockData.isOnSkyblock()) return;
        ensureListenerRegistered();
        timerTick();
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (ATHRConfig.feature == null) return;
        if (!ATHRConfig.feature.farming.farmingTracker.persistAcrossSessions) {
            reset();
        }
    }

    public static long getCount(String id) {
        return FarmingTrackerData.getInstance().getCounts().getOrDefault(id, 0L);
    }

    public static Map<String, Long> getCounts() {
        return FarmingTrackerData.getInstance().getCounts();
    }

    /**
     * One crop's combined raw-crop-equivalent rate per hour for the current
     * session — raw + enchanted + block counts all folded into a single
     * number via {@link Crop#rawEquivalentOf}, the same conversion
     * {@link #totalRawCrops()} uses for the overall crops/hour stat.
     */
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
            double price = PriceMap.Cached.getPrice(entry.getKey());
            if (price > 0) total += entry.getValue() * price;
        }
        return total;
    }

    public static double coinsPerHour() {
        double hours = activeHours();
        return hours <= 0.0 ? 0.0 : currentValue() / hours;
    }

    /** Total crops earned this session, with enchanted/compacted forms converted back to raw-crop-equivalent count. */
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
