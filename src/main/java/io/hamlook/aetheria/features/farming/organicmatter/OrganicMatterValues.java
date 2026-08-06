package io.hamlook.aetheria.features.farming.organicmatter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class OrganicMatterValues {

    private static final Map<String, Double> OM_PER_UNIT = new HashMap<>();

    static {

        put("WHEAT", 1);
        put("ENCHANTED_WHEAT", 160);
        put("ENCHANTED_HAY_BLOCK", 25_600);

        put("SEEDS", 1);
        put("ENCHANTED_SEEDS", 160);

        put("SEED_BOX", 25_600);

        put("CARROT_ITEM", 0.29);
        put("ENCHANTED_CARROT", 46.4);
        put("ENCHANTED_GOLDEN_CARROT", 5_939.2);

        put("POTATO_ITEM", 0.33);
        put("ENCHANTED_POTATO", 52.8);
        put("ENCHANTED_BAKED_POTATO", 8_448);

        put("PUMPKIN", 1);
        put("ENCHANTED_PUMPKIN", 160);
        put("POLISHED_PUMPKIN", 25_600);

        put("MELON", 0.2);
        put("ENCHANTED_MELON", 32);
        put("ENCHANTED_MELON_BLOCK", 5_120);

        put("RED_MUSHROOM", 1);
        put("ENCHANTED_RED_MUSHROOM", 160);
        put("ENCHANTED_RED_MUSHROOM_BLOCK", 5_184);

        put("BROWN_MUSHROOM", 1);
        put("ENCHANTED_BROWN_MUSHROOM", 160);
        put("ENCHANTED_BROWN_MUSHROOM_BLOCK", 5_184);

        put("INK_SACK__3", 0.4);
        put("ENCHANTED_COCOA", 64);

        put("CACTUS", 0.5);
        put("ENCHANTED_CACTUS_GREEN", 80);
        put("ENCHANTED_CACTUS", 12_800);

        put("SUGAR_CANE", 0.5);
        put("ENCHANTED_SUGAR", 80);
        put("ENCHANTED_SUGAR_CANE", 12_800);

        put("NETHER_STALK", 0.33);
        put("ENCHANTED_NETHER_STALK", 52.8);
        put("MUTANT_NETHER_STALK", 8_448);

        put("CROPIE", 2_500);
        put("SQUASH", 10_000);
        put("FERMENTO", 20_000);
        put("CONDENSED_FERMENTO", 180_000);
    }

    private static void put(String id, double value) {
        OM_PER_UNIT.put(id, value);
    }

    public static double getValue(String id) {
        if (id == null) return 0.0;
        Double v = OM_PER_UNIT.get(id);
        return v != null ? v : 0.0;
    }

    public static boolean hasValue(String id) {
        return id != null && OM_PER_UNIT.containsKey(id);
    }

    public static Map<String, Double> asMap() {
        return Collections.unmodifiableMap(OM_PER_UNIT);
    }
}

