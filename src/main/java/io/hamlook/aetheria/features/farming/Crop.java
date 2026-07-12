package io.hamlook.aetheria.features.farming;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * One SkyBlock crop's raw -> enchanted -> compacted/enchanted-block chain,
 * plus a static registry of every tracked crop with the lookups FarmingTracker
 * needs (by raw id, by chat-drop name, by raw-crop-equivalent ratio).
 * <p>
 * This used to be three parallel maps living in FarmingTracker (RAW_EQUIVALENT,
 * RAW_IDS, CHAT_NAME_TO_ID), all built from the same CROPS array in separate
 * static blocks. Consolidating them here means adding/editing a crop only
 * touches one place, instead of risking the maps drifting out of sync with
 * each other.
 */
public class Crop {

    // Raw-crop equivalent multipliers, for a single combined "crops/hour" stat
    // (e.g. 1 Enchanted Melon = 160 Melon, so it contributes 160x its count).
    //
    // Both tiers confirmed at 160x via wiki.hypixel.net: Enchanted Wheat is
    // "crafted with 160 Wheat" (Wheat Collection V) and Enchanted Hay Bale is
    // "crafted with 160 Enchanted Wheat" (Wheat Collection XI) — the same 160
    // ratio applies at both the raw->enchanted and enchanted->block tiers.
    private static final long RAW_TO_ENCHANTED_RATIO = 160L;
    private static final long ENCHANTED_TO_BLOCK_RATIO = 160L;

    public final String rawId;
    public final String enchantedId;
    public final String enchantedChatName;
    public final String blockId;
    public final String blockChatName;
    public final String blockDisplayName;
    public final String displayName;

    private Crop(String rawId, String enchantedId, String enchantedChatName, String blockId, String blockChatName, String displayName) {
        this.rawId = rawId;
        this.enchantedId = enchantedId;
        this.enchantedChatName = enchantedChatName;
        this.blockId = blockId;
        this.blockChatName = blockChatName;
        // Overlay-only abbreviation ("Enchanted Hay Bale" -> "E. Hay Bale"), kept separate
        // from blockChatName since that field also has to match the game's exact
        // "RARE DROP!" chat text (see Crop.findByChatName / CHAT_NAME_TO_ID below).
        this.blockDisplayName = blockChatName != null && blockChatName.startsWith("Enchanted ")
                ? "E. " + blockChatName.substring("Enchanted ".length())
                : blockChatName;
        this.displayName = displayName;
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
    public ItemStack getIcon() {
        switch (rawId) {
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

    // ---- Registry ----

    // SkyBlock IDs verified against the Hypixel SkyBlock wiki. A few are
    // flagged below where Hypixel's legacy naming is less obvious than the
    // display name suggests (e.g. Cocoa Beans, Carrot, Potato, Nether Wart) —
    // worth a quick in-game check before merging if any tracking looks off.
    //
    // Wild Rose / Moonflower / Sunflower are Garden-only crops. Location.GARDEN
    // exists but its server prefixes are a PLACEHOLDER (see SkyblockData.java) —
    // location gating for these three won't work correctly until that's confirmed.
    private static final Crop[] ALL = new Crop[]{
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
            // Third tier is Mutant Nether Wart (item id MUTANT_NETHER_STALK), crafted
            // with 160 Enchanted Nether Wart — same 160x ratio as every other crop's
            // block tier. Verified against wiki.hypixel.net.
            new Crop("NETHER_STALK", "ENCHANTED_NETHER_STALK", "Enchanted Nether Wart", "MUTANT_NETHER_STALK", "Mutant Nether Wart", "Nether Wart"),
            // Garden-only (see caveat above). Compacted forms use "Compacted X" naming,
            // not "Enchanted X Block" like the other crops.
            new Crop("WILD_ROSE", "ENCHANTED_WILD_ROSE", "Enchanted Wild Rose", "COMPACTED_WILD_ROSE", "Compacted Wild Rose", "Wild Rose"),
            new Crop("MOONFLOWER", "ENCHANTED_MOONFLOWER", "Enchanted Moonflower", "COMPACTED_MOONFLOWER", "Compacted Moonflower", "Moonflower"),
            new Crop("SUNFLOWER", "ENCHANTED_SUNFLOWER", "Enchanted Sunflower", "COMPACTED_SUNFLOWER", "Compacted Sunflower", "Sunflower"),
    };

    private static final Map<String, Crop> BY_RAW_ID = new HashMap<>();
    private static final Map<String, String> CHAT_NAME_TO_ID = new HashMap<>();
    private static final Map<String, Long> RAW_EQUIVALENT = new HashMap<>();

    static {
        for (Crop crop : ALL) {
            BY_RAW_ID.put(crop.rawId, crop);

            CHAT_NAME_TO_ID.put(crop.enchantedChatName, crop.enchantedId);
            if (crop.blockId != null) {
                CHAT_NAME_TO_ID.put(crop.blockChatName, crop.blockId);
            }

            RAW_EQUIVALENT.put(crop.rawId, 1L);
            RAW_EQUIVALENT.put(crop.enchantedId, RAW_TO_ENCHANTED_RATIO);
            if (crop.blockId != null) {
                RAW_EQUIVALENT.put(crop.blockId, RAW_TO_ENCHANTED_RATIO * ENCHANTED_TO_BLOCK_RATIO);
            }
        }
    }

    /** Every tracked crop, in display order. */
    public static Crop[] all() {
        return ALL;
    }

    /** Looks up a crop by its raw (uncompacted) SkyBlock item id. Also doubles as "is this id tracked at all". */
    public static Crop findByRawId(String rawId) {
        return BY_RAW_ID.get(rawId);
    }

    /** Resolves a "RARE DROP!" chat line's item name to its internal id (enchanted/block tier only — raw crops come from the item log instead). */
    public static String findByChatName(String chatName) {
        return CHAT_NAME_TO_ID.get(chatName);
    }

    /** Raw-crop-equivalent multiplier for any tracked id (raw/enchanted/block, across all crops), or null if untracked. */
    public static Long rawEquivalentOf(String id) {
        return RAW_EQUIVALENT.get(id);
    }
}
