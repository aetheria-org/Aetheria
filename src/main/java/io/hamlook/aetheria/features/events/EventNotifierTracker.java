package io.hamlook.aetheria.features.events;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.eventnotification.EventTypeConfig;
import io.hamlook.aetheria.init.RegisterEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RegisterEvents
public class EventNotifierTracker {

    private static final int TICK_INTERVAL = 20;

    /** Visible duration is configurable (Event Notification config, 1-10s); fade duration is
     *  fixed — {@link #FADE_MS} normally, {@link #COUNTDOWN_FADE_MS} for the live-countdown
     *  1-minute toast. Same lifecycle shape for every toast either way, driven off when it became
     *  {@link #current}, not off real event timing — the countdown toast's live seconds-remaining
     *  text just keeps ticking the whole time, including through the fade. */
    private static final long FADE_MS = 500L;
    private static final long COUNTDOWN_FADE_MS = 2000L;

    private static final int[] THRESHOLDS_MINUTES = {5, 1};

    /** Only one toast is ever shown at a time; if several fire together the rest queue up and
     *  play one after another, in the order they fired. */
    private static final Deque<EventToast> queue = new ArrayDeque<>();
    private static EventToast current = null;

    private static final Set<String> fired = new HashSet<>();

    private int tickCounter = 0;

    private static EventTypeConfig configFor(String apiTypeName) {
        if (apiTypeName == null) return null;
        switch (apiTypeName) {
            case "Election Booth Opens!":
            case "Election Over!":
                return ATHRConfig.feature.eventNotification.election;
            case "Traveling Zoo":
                return ATHRConfig.feature.eventNotification.oringo;
            case "Dark Auction":
                return ATHRConfig.feature.eventNotification.darkAuction;
            case "Farming Contest":
                return ATHRConfig.feature.eventNotification.farmingContest;
            case "Spooky Festival":
                return ATHRConfig.feature.eventNotification.spooky;
            case "Jerry Workshop Opens":
                return ATHRConfig.feature.eventNotification.jerry;
            case "Mining Fiesta":
                return ATHRConfig.feature.eventNotification.miningFiesta;
            case "Fishing Festival":
                return ATHRConfig.feature.eventNotification.fishingFestival;
            case "New Year":
                return ATHRConfig.feature.eventNotification.newYear;
            default:
                return null;
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if ((tickCounter = (tickCounter + 1) % TICK_INTERVAL) != 0) return;
        if (Minecraft.getMinecraft().thePlayer == null) return;
        if (ATHRConfig.feature == null || !ATHRConfig.feature.eventNotification.masterEnabled) return;

        EventUtils.maybeRefetch();
        checkThresholds();
    }

    private void checkThresholds() {
        Instant now = Instant.now();
        Set<String> validKeys = new HashSet<>();

        for (SkyblockEvent skyblockEvent : EventUtils.cachedEvents) {
            if (skyblockEvent == null || skyblockEvent.event == null || skyblockEvent.start == null) continue;

            String type = skyblockEvent.event.event;
            EventTypeConfig cfg = configFor(type);
            if (cfg == null || !cfg.enabled) continue;

            Instant start;
            try {
                start = Instant.parse(skyblockEvent.start);
            } catch (Exception e) {
                continue;
            }

            String key = type + ":" + skyblockEvent.start;
            long secsUntil = Duration.between(now, start).getSeconds();

            for (int minutes : THRESHOLDS_MINUTES) {
                String thresholdKey = key + ":" + minutes;
                validKeys.add(thresholdKey);
                if (!isNotifyEnabled(cfg, minutes)) continue;

                long upperBound = minutes * 60L;
                boolean inWindow = minutes == 1
                        ? (secsUntil <= upperBound && secsUntil >= 0)
                        : (secsUntil <= upperBound && secsUntil > upperBound - 60L);

                if (inWindow && fired.add(thresholdKey)) {
                    fire(skyblockEvent, start, minutes);
                }
            }
        }

        fired.retainAll(validKeys);
    }

    private static boolean isNotifyEnabled(EventTypeConfig cfg, int minutesThreshold) {
        switch (minutesThreshold) {
            case 5: return cfg.notify5Min;
            case 1: return cfg.notify1Min;
            default: return false;
        }
    }

    private void fire(SkyblockEvent skyblockEvent, Instant start, int minuteThreshold) {
        List<ItemStack> icons = EventIcons.iconsFor(skyblockEvent.event);
        String type = skyblockEvent.event.event;
        if (minuteThreshold == 1) {
            queue.add(EventToast.countdown(icons, type, start));
        } else {
            String text = type + " starts in " + minuteThreshold + " Minutes";
            queue.add(EventToast.staticText(icons, type, text));
        }
    }

    /** Debug/testing hook (see {@code AsmEventNotifTestCommand}) — queues a synthetic countdown
     *  toast starting {@code secondsFromNow} in the future, bypassing the per-type enable/notify
     *  toggles and the real API entirely. */
    public static void debugFireCountdown(String eventType, List<ItemStack> icons, int secondsFromNow) {
        queue.add(EventToast.countdown(icons, eventType, Instant.now().plusSeconds(secondsFromNow)));
    }

    /** Debug/testing hook (see {@code AsmEventTestAllCommand}) — queues a countdown toast anchored
     *  to a real event's actual start time, so the toast shows the genuine remaining time instead
     *  of a synthetic one, bypassing the per-type enable/notify toggles. */
    public static void debugFireCountdown(String eventType, List<ItemStack> icons, Instant realStart) {
        queue.add(EventToast.countdown(icons, eventType, realStart));
    }

    /** Advances the queue if the current toast has finished (visible + faded), then returns
     *  whatever should be on screen right now (may be {@code null}). Safe to call every frame. */
    public static EventToast activeToast() {
        long now = System.currentTimeMillis();
        if (current != null && isFinished(current, now)) {
            current = null;
        }
        if (current == null) {
            current = queue.poll();
            if (current != null) current.startTime = now;
        }
        return current;
    }

    private static boolean isFinished(EventToast toast, long nowMs) {
        return nowMs - toast.startTime >= visibleMs() + fadeMsFor(toast);
    }

    /** 1.0 while fully visible, linearly down to 0.0 across the fade-out window. */
    public static float activeAlpha() {
        if (current == null) return 0f;
        long visible = visibleMs();
        long elapsed = System.currentTimeMillis() - current.startTime;
        if (elapsed <= visible) return 1f;
        return Math.max(0f, 1f - (float) (elapsed - visible) / fadeMsFor(current));
    }

    private static long visibleMs() {
        if (ATHRConfig.feature == null) return 8000L;
        return (long) (ATHRConfig.feature.eventNotification.notificationDuration * 1000f);
    }

    private static long fadeMsFor(EventToast toast) {
        return toast.countdown ? COUNTDOWN_FADE_MS : FADE_MS;
    }
}
