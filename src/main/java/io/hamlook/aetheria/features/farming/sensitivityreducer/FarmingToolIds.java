package io.hamlook.aetheria.features.farming.sensitivityreducer;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class FarmingToolIds {

    private static final Set<String> IDS = new HashSet<>(Arrays.asList(
            "THEORETICAL_HOE_WHEAT_1", "THEORETICAL_HOE_WHEAT_2", "THEORETICAL_HOE_WHEAT_3",
            "THEORETICAL_HOE_CARROT_1", "THEORETICAL_HOE_CARROT_2", "THEORETICAL_HOE_CARROT_3",
            "THEORETICAL_HOE_POTATO_1", "THEORETICAL_HOE_POTATO_2", "THEORETICAL_HOE_POTATO_3",
            "THEORETICAL_HOE_CANE_1", "THEORETICAL_HOE_CANE_2", "THEORETICAL_HOE_CANE_3",
            "THEORETICAL_HOE_WARTS_1", "THEORETICAL_HOE_WARTS_2", "THEORETICAL_HOE_WARTS_3",
            "THEORETICAL_HOE_SUNFLOWER_1", "THEORETICAL_HOE_SUNFLOWER_2", "THEORETICAL_HOE_SUNFLOWER_3",
            "THEORETICAL_HOE_WILD_ROSE_1", "THEORETICAL_HOE_WILD_ROSE_2", "THEORETICAL_HOE_WILD_ROSE_3",
            "FUNGI_CUTTER", "FUNGI_CUTTER_2", "FUNGI_CUTTER_3",
            "COCO_CHOPPER", "COCO_CHOPPER_2", "COCO_CHOPPER_3",
            "CACTUS_KNIFE", "CACTUS_KNIFE_2", "CACTUS_KNIFE_3",
            "MELON_DICER", "MELON_DICER_2", "MELON_DICER_3",
            "PUMPKIN_DICER", "PUMPKIN_DICER_2", "PUMPKIN_DICER_3",
            "BASIC_GARDENING_HOE", "ADVANCED_GARDENING_HOE",
            "BASIC_GARDENING_AXE", "ADVANCED_GARDENING_AXE",
            "SQUEAKY_MOUSEMAT"
    ));

    private FarmingToolIds() {
    }

    public static boolean isFarmingTool(String internalId) {
        return internalId != null && IDS.contains(internalId);
    }

    public static Set<String> all() {
        return Collections.unmodifiableSet(IDS);
    }
}
