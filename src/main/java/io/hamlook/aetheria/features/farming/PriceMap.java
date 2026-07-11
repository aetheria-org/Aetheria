package io.hamlook.aetheria.features.farming;

import java.util.HashMap;
import java.util.Map;

/**
 * Hardcoded crop sell prices, kept separate from FarmingTracker so pricing
 * concerns don't get tangled up with count-tracking/session logic. If this
 * ever moves to a live bazaar lookup, only this class needs to change —
 * {@link #getCachedPrice(String)} is the one entry point FarmingTracker calls.
 * <p>
 * Hardcoded rather than a live bazaar lookup, so this can be pre-seeded with
 * guessed/manual values for items not yet added to the game (upcoming crops,
 * new enchanted variants, etc). Verify these against current bazaar sell
 * prices before merging — only WHEAT's value below came from a live check;
 * the rest are 0.0 placeholders (untracked value) until filled in.
 */
public class PriceMap {

    private static final Map<String, Double> HARDCODED_PRICES = new HashMap<>();

    static {
        HARDCODED_PRICES.put("WHEAT", 3.1);
        HARDCODED_PRICES.put("ENCHANTED_WHEAT", 0.0); // TODO: fill in current bazaar sell price
        HARDCODED_PRICES.put("ENCHANTED_HAY_BLOCK", 0.0); // TODO

        HARDCODED_PRICES.put("CARROT_ITEM", 0.0); // TODO
        HARDCODED_PRICES.put("ENCHANTED_CARROT", 0.0); // TODO
        HARDCODED_PRICES.put("ENCHANTED_GOLDEN_CARROT", 0.0); // TODO

        HARDCODED_PRICES.put("POTATO_ITEM", 0.0); // TODO
        HARDCODED_PRICES.put("ENCHANTED_POTATO", 0.0); // TODO
        HARDCODED_PRICES.put("ENCHANTED_BAKED_POTATO", 0.0); // TODO

        HARDCODED_PRICES.put("PUMPKIN", 0.0); // TODO
        HARDCODED_PRICES.put("ENCHANTED_PUMPKIN", 0.0); // TODO
        HARDCODED_PRICES.put("POLISHED_PUMPKIN", 0.0); // TODO

        HARDCODED_PRICES.put("MELON", 2.0);
        HARDCODED_PRICES.put("ENCHANTED_MELON", 320.0);
        HARDCODED_PRICES.put("ENCHANTED_MELON_BLOCK", 51_200.0);

        HARDCODED_PRICES.put("SUGAR_CANE", 0.0); // TODO
        HARDCODED_PRICES.put("ENCHANTED_SUGAR", 0.0); // TODO
        HARDCODED_PRICES.put("ENCHANTED_SUGAR_CANE", 0.0); // TODO

        HARDCODED_PRICES.put("INK_SACK:3", 0.0); // TODO (Cocoa Beans)
        HARDCODED_PRICES.put("ENCHANTED_COCOA", 0.0); // TODO
        HARDCODED_PRICES.put("ENCHANTED_COOKIE", 0.0); // TODO

        HARDCODED_PRICES.put("CACTUS", 0.0); // TODO
        HARDCODED_PRICES.put("ENCHANTED_CACTUS_GREEN", 0.0); // TODO
        HARDCODED_PRICES.put("ENCHANTED_CACTUS", 0.0); // TODO

        HARDCODED_PRICES.put("RED_MUSHROOM", 0.0); // TODO
        HARDCODED_PRICES.put("ENCHANTED_RED_MUSHROOM", 0.0); // TODO
        HARDCODED_PRICES.put("ENCHANTED_RED_MUSHROOM_BLOCK", 0.0); // TODO

        HARDCODED_PRICES.put("BROWN_MUSHROOM", 0.0); // TODO
        HARDCODED_PRICES.put("ENCHANTED_BROWN_MUSHROOM", 0.0); // TODO
        HARDCODED_PRICES.put("ENCHANTED_BROWN_MUSHROOM_BLOCK", 0.0); // TODO

        HARDCODED_PRICES.put("NETHER_STALK", 0.0); // TODO
        HARDCODED_PRICES.put("ENCHANTED_NETHER_STALK", 0.0); // TODO

        HARDCODED_PRICES.put("WILD_ROSE", 0.0); // TODO
        HARDCODED_PRICES.put("ENCHANTED_WILD_ROSE", 0.0); // TODO
        HARDCODED_PRICES.put("COMPACTED_WILD_ROSE", 0.0); // TODO

        HARDCODED_PRICES.put("MOONFLOWER", 0.0); // TODO
        HARDCODED_PRICES.put("ENCHANTED_MOONFLOWER", 0.0); // TODO
        HARDCODED_PRICES.put("COMPACTED_MOONFLOWER", 0.0); // TODO

        HARDCODED_PRICES.put("SUNFLOWER", 0.0); // TODO
        HARDCODED_PRICES.put("ENCHANTED_SUNFLOWER", 0.0); // TODO
        HARDCODED_PRICES.put("COMPACTED_SUNFLOWER", 0.0); // TODO

        // Add future/unreleased items here ahead of time, e.g.:
        // HARDCODED_PRICES.put("SOME_NEW_CROP_ID", 100.0);
    }

    /** Cached sell price for a SkyBlock item id, or 0.0 if untracked/not yet filled in. */
    public static double getCachedPrice(String cropId) {
        Double price = HARDCODED_PRICES.get(cropId);
        return price != null ? price : 0.0;
    }
}
