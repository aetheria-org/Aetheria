package io.hamlook.aetheria.features.farming;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.misc.itemlog.ItemPickupLog;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.data.SkyblockData;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Items;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
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
    // Wild Rose / Moonflower / Sunflower are Garden-only crops. Location.GARDEN
    // exists but its server prefixes are a PLACEHOLDER (see SkyblockData.java) —
    // location gating for these three won't work correctly until that's confirmed.
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

    /**
     * One representative icon per crop line (shown once at the start of the
     * line, not per raw/enchanted/block sub-form). This mod has no general
     * SkyBlock-ID -> texture registry, so most of these map straight to the
     * vanilla item Hypixel reuses under the hood (enchanted tiers look
     * identical to raw crops in-game, just with NBT lore/glint added).
     * Wild Rose / Moonflower / Sunflower have no vanilla equivalent at all
     * (Garden-exclusive custom textures) — using the closest-looking vanilla
     * flower as a stand-in sprite instead, per request.
     */
    public static ItemStack getCropIcon(Crop crop) {
        switch (crop.rawId) {
            case "WHEAT": return new ItemStack(Items.wheat);
            case "CARROT_ITEM": return new ItemStack(Items.carrot);
            case "POTATO_ITEM": return new ItemStack(Items.potato);
            case "PUMPKIN": return new ItemStack(Item.getItemFromBlock(Blocks.pumpkin));
            case "MELON": return new ItemStack(Items.melon);
            case "SUGAR_CANE": return new ItemStack(Items.reeds);
            case "INK_SACK:3": return new ItemStack(Items.dye, 1, 3); // Cocoa Beans
            case "CACTUS": return new ItemStack(Item.getItemFromBlock(Blocks.cactus));
            case "RED_MUSHROOM": return new ItemStack(Item.getItemFromBlock(Blocks.red_mushroom));
            case "BROWN_MUSHROOM": return new ItemStack(Item.getItemFromBlock(Blocks.brown_mushroom));
            case "NETHER_STALK": return new ItemStack(Items.nether_wart);
            case "WILD_ROSE": return new ItemStack(Item.getItemFromBlock(Blocks.double_plant), 1, 4); // Rose Bush
            case "MOONFLOWER": return new ItemStack(Item.getItemFromBlock(Blocks.red_flower), 1, 1); // Blue Orchid
            case "SUNFLOWER": return new ItemStack(Item.getItemFromBlock(Blocks.double_plant), 1, 0); // Sunflower
            default: return null;
        }
    }

    // Raw-crop equivalent multipliers, for a single combined "crops/hour" stat
    // (e.g. 1 Enchanted Melon = 160 Melon, so it contributes 160x its count).
    //
    // Both tiers confirmed at 160x via wiki.hypixel.net: Enchanted Wheat is
    // "crafted with 160 Wheat" (Wheat Collection V) and Enchanted Hay Bale is
    // "crafted with 160 Enchanted Wheat" (Wheat Collection XI) — the same 160
    // ratio applies at both the raw->enchanted and enchanted->block tiers.
    private static final long RAW_TO_ENCHANTED_RATIO = 160L;
    private static final long ENCHANTED_TO_BLOCK_RATIO = 160L;

    private static final Map<String, Long> RAW_EQUIVALENT = new HashMap<>();

    static {
        for (Crop crop : CROPS) {
            RAW_EQUIVALENT.put(crop.rawId, 1L);
            RAW_EQUIVALENT.put(crop.enchantedId, RAW_TO_ENCHANTED_RATIO);
            if (crop.blockId != null) {
                RAW_EQUIVALENT.put(crop.blockId, RAW_TO_ENCHANTED_RATIO * ENCHANTED_TO_BLOCK_RATIO);
            }
        }
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

    // Drop-line matching happens on the ColorUtils.stripColor()'d text
    private static final Pattern DROP_PATTERN = Pattern.compile("DROP! \\S+ dropped (\\d+)x (.+)!");

    private static boolean listenerRegistered = false;

    // Pauses the coins/hour rate calc after 7s of no crop activity, so
    // AFK/break time doesn't dilute the rate. Mirrors PowderStats' pattern,
    // just with a much shorter threshold suited to farming's rapid drops.
    private static final long INACTIVITY_LIMIT_MS = 7_000L;
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
                FarmingTrackerData.getInstance().setActiveTimeMs(
                        FarmingTrackerData.getInstance().getActiveTimeMs() - INACTIVITY_LIMIT_MS);
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
        if (!RAW_IDS.containsKey(internalId)) return;

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

        String id = CHAT_NAME_TO_ID.get(m.group(2));
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

    public static double currentValue() {
        double total = 0.0;
        for (Map.Entry<String, Long> entry : FarmingTrackerData.getInstance().getCounts().entrySet()) {
            Double price = PRICES.get(entry.getKey());
            if (price != null && price > 0) total += entry.getValue() * price;
        }
        return total;
    }

    public static double coinsPerHour() {
        double hours = FarmingTrackerData.getInstance().getActiveTimeMs() / 3_600_000.0;
        return hours <= 0.0 ? 0.0 : currentValue() / hours;
    }

    /** Total crops earned this session, with enchanted/compacted forms converted back to raw-crop-equivalent count. */
    public static long totalRawCrops() {
        long total = 0L;
        for (Map.Entry<String, Long> entry : FarmingTrackerData.getInstance().getCounts().entrySet()) {
            Long ratio = RAW_EQUIVALENT.get(entry.getKey());
            if (ratio != null) total += entry.getValue() * ratio;
        }
        return total;
    }

    public static double cropsPerHour() {
        double hours = FarmingTrackerData.getInstance().getActiveTimeMs() / 3_600_000.0;
        return hours <= 0.0 ? 0.0 : totalRawCrops() / hours;
    }
}
