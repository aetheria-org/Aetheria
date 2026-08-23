package io.hamlook.aetheria.features.events;

import java.util.HashMap;
import java.util.Map;

/**
 * Background/border color pair per SkyBlock event type, so each popup reads as its own "themed
 * window" instead of one generic box for everything. Colors are fully opaque — the overlay fades
 * them by scaling the alpha byte at render time.
 */
public class EventTheme {

    public final int bgColor;
    public final int borderColor;

    private EventTheme(int bgColor, int borderColor) {
        this.bgColor = bgColor;
        this.borderColor = borderColor;
    }

    /** Generic terracotta/dark-oak fallback for any future/unmapped event type. */
    private static final EventTheme DEFAULT = new EventTheme(0xFFD87F33, 0xFF664C33);

    private static final Map<String, EventTheme> THEMES = new HashMap<>();

    static {
        // Regal purple + gold: electing a mayor is civic pageantry.
        THEMES.put("Election Booth Opens!", new EventTheme(0xFF3D2459, 0xFFC9A227));
        THEMES.put("Election Over!", new EventTheme(0xFF3D2459, 0xFFC9A227));
        // Circus red-orange + golden-yellow: Oringo's Traveling Zoo tent.
        THEMES.put("Traveling Zoo", new EventTheme(0xFFB33A1E, 0xFFF2C14E));
        // Near-black + pumpkin orange: Halloween jack-o'-lantern.
        THEMES.put("Spooky Festival", new EventTheme(0xFF1A1210, 0xFFD87F33));
        // Pine green + candy-cane red: Jerry's winter workshop.
        THEMES.put("Jerry Workshop Opens", new EventTheme(0xFF14432B, 0xFFC1272D));
        // Grass green + soil brown: the farm itself.
        THEMES.put("Farming Contest", new EventTheme(0xFF3F6B2E, 0xFF6B4423));
        // Near-black purple + antique gold: Sirius's shadowy, high-stakes auction house.
        THEMES.put("Dark Auction", new EventTheme(0xFF150B1F, 0xFFB8912B));
        // Stone grey + lava orange: the mines.
        THEMES.put("Mining Fiesta", new EventTheme(0xFF3A3A3D, 0xFFD9581E));
        // Ocean teal + sandy tan: fishing docks.
        THEMES.put("Fishing Festival", new EventTheme(0xFF0F5C6B, 0xFFE0C48C));
        // Midnight blue + fireworks gold: New Year's Eve sky.
        THEMES.put("New Year", new EventTheme(0xFF16213E, 0xFFD4AF37));
    }

    public static EventTheme forType(String type) {
        EventTheme theme = THEMES.get(type);
        return theme != null ? theme : DEFAULT;
    }
}
