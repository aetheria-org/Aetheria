package io.hamlook.aetheria.features.farming.trevor;

import net.minecraft.util.BlockPos;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Fixed Trapper animal spawn spots on the Mushroom Desert, grouped by the area
 * name Trevor announces in his quest start message.
 */
public final class TrevorSpots {

    private static final Map<String, List<BlockPos>> SPOTS = new HashMap<>();

    static {
        SPOTS.put("desert settlement", Arrays.asList(
                new BlockPos(184, 77, -352),
                new BlockPos(139, 77, -375)));
        SPOTS.put("oasis", Arrays.asList(
                new BlockPos(104, 65, -473),
                new BlockPos(116, 65, -416),
                new BlockPos(165, 77, -464)));
        SPOTS.put("mushroom gorge", Arrays.asList(
                new BlockPos(220, 41, -578),
                new BlockPos(234, 54, -500),
                new BlockPos(265, 55, -436),
                new BlockPos(187, 42, -520),
                new BlockPos(303, 51, -409),
                new BlockPos(172, 48, -459),
                new BlockPos(189, 43, -443)));
        SPOTS.put("overgrown mushroom cave", Arrays.asList(
                new BlockPos(247, 57, -421),
                new BlockPos(248, 58, -369)));
    }

    private TrevorSpots() {
    }

    public static List<BlockPos> forLocation(String location) {
        if (location == null) return Collections.emptyList();
        return SPOTS.getOrDefault(location.toLowerCase(Locale.ROOT).trim(), Collections.emptyList());
    }
}
