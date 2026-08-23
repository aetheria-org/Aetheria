package io.hamlook.aetheria.features.events;

import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Hand-drawn badge art for the two event types whose badge is otherwise a fetched NPC skull
 * (Sirius for Dark Auction, Oringo for Traveling Zoo) — every skull shares the same underlying
 * item+metadata, so unlike {@link EventItemTextures} this can't be keyed by item identity and is
 * keyed by event type instead. Checked before the badge falls through to
 * {@link EventNotifierOverlay#renderEventIcon}'s normal dispatch.
 */
public class EventBadgeTextures {

    private static ResourceLocation tex(String name) {
        return new ResourceLocation("aetheria", "eventnotification/" + name);
    }

    private static final Map<String, ResourceLocation> MAP = new HashMap<>();

    static {
        MAP.put("Dark Auction", tex("sirius_head.png"));
        MAP.put("Traveling Zoo", tex("traveling_zoo.png"));
    }

    private EventBadgeTextures() {}

    public static ResourceLocation forType(String eventType) {
        return eventType == null ? null : MAP.get(eventType);
    }
}
