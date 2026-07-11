package io.hamlook.aetheria.features.farming;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.misc.itemlog.ItemPickupLog;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.data.SkyblockData;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.LinkedHashMap;
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
 * Prices are hardcoded in {@link #PRICES} below rather than pulled from a
 * live bazaar lookup, so future/unreleased items can be pre-seeded with a
 * manual value before they exist in any price API.
 */
@RegisterEvents
public class FarmingTracker {

    /** One crop's raw -> enchanted -> compacted/enchanted-block chain. */
    public static class Crop {
        public final String rawId;
        public final String enchantedId;
        public final String enchantedChatName;
        public final String blockId;
        public final String blockChatName;
        public final String displayName;

        Crop(String rawId, String enchantedId, String enchantedChatName, String blockId, String blockChatName, String displayName) {
            this.rawId = rawId;
            this.enchantedId = enchantedId;
            this.enchantedChatName = enchantedChatName;
            this.blockId = blockId;
            this.blockChatName = blockChatName;
            this.displayName = displayName;
        }
    }

    // SkyBlock IDs verified against the Hypixel SkyBlock wiki. A few are
    // flagged below where Hypixel's legacy naming is less obvious than the
    // display name suggests (e.g. Cocoa Beans, Carrot, Potato, Nether Wart) —
    // worth a quick in-game check before merging if any tracking looks off.
    //
    // Wild Rose / Moonflower / Sunflower are Garden-only crops and cannot be
    // farmed in the Barn or on your Private Island. SkyblockData.Location has
    // no GARDEN entry yet, so with "Require Farming Location" enabled these
    // three will never register — either disable that setting while farming
    // flowers, or add GARDEN to the Location enum as a follow-up.
    private static final Crop[] CROPS = new Crop[]{
            new Crop("WHEAT", "ENCHANTED_WHEAT", "Enchanted Wheat", "ENCHANTED_HAY_BLOCK", "Enchanted Hay Bale", "Wheat"),
            // CARROT_ITEM/POTATO_ITEM are Hypixel's legacy vanilla item IDs (pre-1.16 carrot/potato
            // were sub-items of a shared crop block); double check against your item log's raw output.
            new Crop("CARROT_ITEM", "ENCHANTED_CARROT", "Enchanted Carrot", "ENCHANTED_GOLDEN_CARROT", "Enchanted Golden Carrot", "Carrot"),
            new Crop("POTATO_ITEM", "ENCHANTED_POTATO", "Enchanted Potato", "ENCHANTED_BAKED_POTATO", "Enchanted Baked Potato", "Potato"),
            new Crop("PUMPKIN", "ENCHANTED_PUMPKIN", "Enchanted Pumpkin", "POLISHED_PUMPKIN", "Polished Pumpkin", "Pumpkin"),
            new Crop("MELON", "ENCHANTED_MELON", "Enchanted Melon", "ENCHANTED_MELON_BLOCK", "Enchanted Melon Block", "Melon"),
            new Crop("SUGAR_CANE", "ENCHANTED_SUGAR", "Enchanted Sugar", "ENCHANTED_SUGAR_CANE", "Enchanted Sugar Cane", "Sugar Cane"),
            // Cocoa Beans is a leftover legacy dye sub-item, not a standalone item id.
            new Crop("INK_SACK:3", "ENCHANTED_COCOA", "Enchanted Cocoa Beans", "ENCHANTED_COOKIE", "Enchanted Cookie", "Cocoa Beans"),
            new Crop("CACTUS", "ENCHANTED_CACTUS_GREEN", "Enchanted Cactus Green", "ENCHANTED_CACTUS", "Enchanted Cactus", "Cactus"),
            new Crop("RED_MUSHROOM", "ENCHANTED_RED_MUSHROOM", "Enchanted Red Mushroom", "ENCHANTED_RED_MUSHROOM_BLOCK", "Enchanted Red Mushroom Block", "Red Mushroom"),
            new Crop("BROWN_MUSHROOM", "ENCHANTED_BROWN_MUSHROOM", "Enchanted Brown Mushroom", "ENCHANTED_BROWN_MUSHROOM_BLOCK", "Enchanted Brown Mushroom Block", "Brown Mushroom"),
            // Nether Wart's raw id is its own legacy name, not "NETHER_WART".
            new Crop("NETHER_STALK", "ENCHANTED_NETHER_STALK", "Enchanted Nether Wart", null, null, "Nether Wart"),
            // Garden-only (see caveat above). Compacted forms use "Compacted X" naming,
            // not "Enchanted X Block" like the other crops.
            new Crop("WILD_ROSE", "ENCHANTED_WILD_ROSE", "Enchanted Wild Rose", "COMPACTED_WILD_ROSE", "Compacted Wild Rose", "Wild Rose"),
            new Crop("MOONFLOWER", "ENCHANTED_MOONFLOWER", "Enchanted Moonflower", "COMPACTED_MOONFLOWER", "Compacted Moonflower", "Moonflower"),
            new Crop("SUNFLOWER", "ENCHANTED_SUNFLOWER", "Enchanted Sunflower", "COMPACTED_SUNFLOWER", "Compacted Sunflower", "Sunflower"),
    };

    /** Exposed so the overlay can build display lines without duplicating this table. */
    public static Crop[] getCrops() {
        return CROPS;
    }

    private static final Map<String, String> RAW_IDS = new HashMap<>();
    private static final Map<String, String> CHAT_NAME_TO_ID = new HashMap<>();

    static {
        for (Crop crop : CROPS) {
            RAW_IDS.put(crop.rawId, crop.rawId);
            CHAT_NAME_TO_ID.put(crop.enchantedChatName, crop.enchantedId);
            if (crop.blockId != null) {
                CHAT_NAME_TO_ID.put(crop.blockChatName, crop.blockId);
            }
        }
    }

    // Hardcoded rather than a live bazaar lookup, so this can be pre-seeded with
    // guessed/manual values for items not yet added to the game (upcoming crops,
    // new enchanted variants, etc). Verify these against current bazaar sell
    // prices before merging — only WHEAT's value below came from a live check;
    // the rest are 0.0 placeholders (untracked value) until filled in.
    private static final Map<String, Double> PRICES = new HashMap<>();

    static {
        PRICES.put("WHEAT", 3.1);
        PRICES.put("ENCHANTED_WHEAT", 0.0); // TODO: fill in current bazaar sell price
        PRICES.put("ENCHANTED_HAY_BLOCK", 0.0); // TODO

        PRICES.put("CARROT_ITEM", 0.0); // TODO
        PRICES.put("ENCHANTED_CARROT", 0.0); // TODO
        PRICES.put("ENCHANTED_GOLDEN_CARROT", 0.0); // TODO

        PRICES.put("POTATO_ITEM", 0.0); // TODO
        PRICES.put("ENCHANTED_POTATO", 0.0); // TODO
        PRICES.put("ENCHANTED_BAKED_POTATO", 0.0); // TODO

        PRICES.put("PUMPKIN", 0.0); // TODO
        PRICES.put("ENCHANTED_PUMPKIN", 0.0); // TODO
        PRICES.put("POLISHED_PUMPKIN", 0.0); // TODO

        PRICES.put("MELON", 2.0);
        PRICES.put("ENCHANTED_MELON", 320.0);
        PRICES.put("ENCHANTED_MELON_BLOCK", 51_200.0);

        PRICES.put("SUGAR_CANE", 0.0); // TODO
        PRICES.put("ENCHANTED_SUGAR", 0.0); // TODO
        PRICES.put("ENCHANTED_SUGAR_CANE", 0.0); // TODO

        PRICES.put("INK_SACK:3", 0.0); // TODO (Cocoa Beans)
        PRICES.put("ENCHANTED_COCOA", 0.0); // TODO
        PRICES.put("ENCHANTED_COOKIE", 0.0); // TODO

        PRICES.put("CACTUS", 0.0); // TODO
        PRICES.put("ENCHANTED_CACTUS_GREEN", 0.0); // TODO
        PRICES.put("ENCHANTED_CACTUS", 0.0); // TODO

        PRICES.put("RED_MUSHROOM", 0.0); // TODO
        PRICES.put("ENCHANTED_RED_MUSHROOM", 0.0); // TODO
        PRICES.put("ENCHANTED_RED_MUSHROOM_BLOCK", 0.0); // TODO

        PRICES.put("BROWN_MUSHROOM", 0.0); // TODO
        PRICES.put("ENCHANTED_BROWN_MUSHROOM", 0.0); // TODO
        PRICES.put("ENCHANTED_BROWN_MUSHROOM_BLOCK", 0.0); // TODO

        PRICES.put("NETHER_STALK", 0.0); // TODO
        PRICES.put("ENCHANTED_NETHER_STALK", 0.0); // TODO

        PRICES.put("WILD_ROSE", 0.0); // TODO
        PRICES.put("ENCHANTED_WILD_ROSE", 0.0); // TODO
        PRICES.put("COMPACTED_WILD_ROSE", 0.0); // TODO

        PRICES.put("MOONFLOWER", 0.0); // TODO
        PRICES.put("ENCHANTED_MOONFLOWER", 0.0); // TODO
        PRICES.put("COMPACTED_MOONFLOWER", 0.0); // TODO

        PRICES.put("SUNFLOWER", 0.0); // TODO
        PRICES.put("ENCHANTED_SUNFLOWER", 0.0); // TODO
        PRICES.put("COMPACTED_SUNFLOWER", 0.0); // TODO

        // Add future/unreleased items here ahead of time, e.g.:
        // PRICES.put("SOME_NEW_CROP_ID", 100.0);
    }

    // Strip every remaining §-code first (handles duplicated/garbled codes on
    // rarer drop tiers), then match "...DROP! <player> dropped <qty>x <item>!"
    private static final Pattern FORMAT_CODE = Pattern.compile("§.");
    private static final Pattern DROP_PATTERN = Pattern.compile("DROP! \\S+ dropped (\\d+)x (.+)!");

    private static final Map<String, Long> counts = new LinkedHashMap<>();
    private static boolean listenerRegistered = false;

    // Pauses the coins/hour rate calc after 7s of no crop activity, so
    // AFK/break time doesn't dilute the rate. Mirrors PowderStats' pattern,
    // just with a much shorter threshold suited to farming's rapid drops.
    private static final long INACTIVITY_LIMIT_MS = 7_000L;
    private static long activeTimeMs = 0L;
    private static boolean timerRunning = false;
    private static boolean timerStartedOnce = false;
    private static boolean inactivityFlagged = false;
    private static long timerStartTime = 0L;
    private static long lastActivityTime = 0L;

    private static void updateActivity() {
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

    private static void timerTick() {
        if (!timerRunning) return;
        long now = System.currentTimeMillis();
        activeTimeMs += now - timerStartTime;
        timerStartTime = now;
        if (now - lastActivityTime > INACTIVITY_LIMIT_MS) {
            timerRunning = false;
            inactivityFlagged = true;
        }
    }

    public static boolean isPaused() {
        return timerStartedOnce && !timerRunning;
    }

    private static boolean isEnabled() {
        return ATHRConfig.feature != null && ATHRConfig.feature.farming.farmingTracker.enabled;
    }

    private static boolean isInFarmingLocation() {
        SkyblockData.Location location = SkyblockData.getCurrentLocation();
        return location == SkyblockData.Location.BARN || location == SkyblockData.Location.PRIVATE_ISLAND;
    }

    private static boolean locationOk() {
        return !ATHRConfig.feature.farming.farmingTracker.requireFarmingIsland || isInFarmingLocation();
    }

    public static void reset() {
        counts.clear();
        activeTimeMs = 0L;
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
        if (!RAW_IDS.containsKey(internalId)) return;

        counts.merge(internalId, (long) delta, Long::sum);
        updateActivity();
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (!isEnabled()) return;
        if (!SkyblockData.isOnSkyblock()) return;
        if (!locationOk()) return;

        String msg = FORMAT_CODE.matcher(ChatUtils.clean(event)).replaceAll("");

        // Guard against whispers, party/guild chat, or any player-authored message
        // that happens to contain drop-like text (e.g. someone pasting/mocking a
        // drop line, or "Party > Steve: lol dropped 99x Enchanted Melon!"). The
        // regex below uses find(), not an anchored full-line match, so without
        // this it could pick up a fake drop embedded in a longer message. Real
        // SkyBlock system broadcasts never contain a colon; every whisper/party/
        // guild/DM format does ("From Steve:", "Party > Steve:", "Guild > Steve:").
        if (msg.contains(":")) return;

        Matcher m = DROP_PATTERN.matcher(msg);
        if (!m.find()) return;

        long quantity;
        try {
            quantity = Long.parseLong(m.group(1));
        } catch (NumberFormatException e) {
            return;
        }

        String id = CHAT_NAME_TO_ID.get(m.group(2));
        if (id == null) return;

        counts.merge(id, quantity, Long::sum);
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
        reset();
    }

    public static long getCount(String id) {
        return counts.getOrDefault(id, 0L);
    }

    public static Map<String, Long> getCounts() {
        return counts;
    }

    public static double currentValue() {
        double total = 0.0;
        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            Double price = PRICES.get(entry.getKey());
            if (price != null && price > 0) total += entry.getValue() * price;
        }
        return total;
    }

    public static double coinsPerHour() {
        double hours = activeTimeMs / 3_600_000.0;
        return hours <= 0.0 ? 0.0 : currentValue() / hours;
    }
}