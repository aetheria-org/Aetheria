package io.hamlook.aetheria.features.events;

import net.minecraft.item.ItemStack;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * One queued/active popup. {@code startTime} is set by {@link EventNotifierTracker} the moment
 * this toast becomes the one actively displayed, not when it's created/queued.
 * <p>
 * Two flavors: a plain static-text toast (used for every threshold except the final minute), and
 * a "countdown" toast (used for the 1-minute threshold, and by {@code AsmEventTestAllCommand}'s
 * real-time debug preview) whose {@link #text()} live-recomputes the time remaining until the real
 * event start every time it's read, so it visibly ticks down rather than showing a fixed string the
 * whole time. In production this is always under a minute, so it only ever prints seconds; the
 * debug preview can anchor it to an event that's hours away, so {@link #text()} also formats
 * minutes and hours for anything longer.
 * <p>
 * {@code eventType} (the raw API type name, e.g. "Dark Auction") is kept on every toast — not just
 * countdown ones — so the overlay can look up that event's {@link EventTheme} regardless of which
 * threshold fired.
 */
public class EventToast {

    public final List<ItemStack> icons;
    public final String eventType;
    private final String staticText;
    private final Instant eventStart;
    public final boolean countdown;

    long startTime = -1L;

    private EventToast(List<ItemStack> icons, String eventType, String staticText, Instant eventStart, boolean countdown) {
        this.icons = icons;
        this.eventType = eventType;
        this.staticText = staticText;
        this.eventStart = eventStart;
        this.countdown = countdown;
    }

    public static EventToast staticText(List<ItemStack> icons, String eventType, String text) {
        return new EventToast(icons, eventType, text, null, false);
    }

    public static EventToast countdown(List<ItemStack> icons, String eventType, Instant eventStart) {
        return new EventToast(icons, eventType, null, eventStart, true);
    }

    public String text() {
        if (!countdown) return staticText;
        long secsLeft = Math.max(0L, Duration.between(Instant.now(), eventStart).getSeconds());
        return eventType + " starts in " + formatRemaining(secsLeft);
    }

    private static String formatRemaining(long totalSeconds) {
        if (totalSeconds < 60) {
            return totalSeconds + (totalSeconds == 1 ? " Second" : " Seconds");
        }
        if (totalSeconds < 3600) {
            long minutes = totalSeconds / 60;
            return minutes + (minutes == 1 ? " Minute" : " Minutes");
        }
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        String hourPart = hours + (hours == 1 ? " Hour" : " Hours");
        return minutes == 0 ? hourPart : hourPart + " " + minutes + (minutes == 1 ? " Minute" : " Minutes");
    }
}
