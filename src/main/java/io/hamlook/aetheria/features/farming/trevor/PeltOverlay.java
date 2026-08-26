package io.hamlook.aetheria.features.farming.trevor;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.Position;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.overlay.Overlay;
import lombok.Getter;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Session pelt tracker for Trevor's hunts: total pelts earned and pelts/hour,
 * counted from the reward chat message. Only shown after the first hunt event
 * (quest announced or pelt reward) and hidden again after a few minutes of
 * inactivity; the rate is computed from active time, paused while chat is
 * open, so AFK/chat time doesn't deflate it.
 */
@RegisterEvents
public class PeltOverlay extends Overlay {

    @Getter
    private static PeltOverlay instance;

    private static final long QUEST_COOLDOWN_MS = 20_000;
    private static final long INACTIVITY_LIMIT_MS = 180_000;

    private static int sessionPelts = 0;
    private static long cooldownStartMs = 0;

    private static boolean timerRunning = false;
    private static boolean timerStartedOnce = false;
    private static boolean inactivityFlagged = false;
    private static long timerStartTime = 0;
    private static long lastActivityTime = 0;
    private static long activeTimeMs = 0;

    public PeltOverlay() {
        super(90, 34);
        instance = this;
    }

    public static void addPelts(int amount) {
        sessionPelts += amount;
        updateActivity();
    }

    /** Arms the 20s new-quest cooldown; called when Trevor announces a hunt. */
    public static void startCooldown() {
        cooldownStartMs = System.currentTimeMillis();
        updateActivity();
    }

    public static void reset() {
        sessionPelts = 0;
        cooldownStartMs = 0;
        timerRunning = false;
        timerStartedOnce = false;
        inactivityFlagged = false;
        timerStartTime = 0;
        lastActivityTime = 0;
        activeTimeMs = 0;
    }

    /** Marks a Trevor hunt event; (re)starts the active-time timer and refreshes the visibility window. */
    private static void updateActivity() {
        ensureTimerRunning();
        lastActivityTime = System.currentTimeMillis();
    }

    private static void ensureTimerRunning() {
        if (timerRunning) return;
        if (timerStartedOnce && inactivityFlagged) {
            activeTimeMs = Math.max(0, activeTimeMs - INACTIVITY_LIMIT_MS);
            inactivityFlagged = false;
        }
        timerStartTime = System.currentTimeMillis();
        timerRunning = true;
        timerStartedOnce = true;
    }

    private static void flushSegment(long now) {
        activeTimeMs += now - timerStartTime;
    }

    private static boolean pauseOnChat() {
        return ATHRConfig.feature != null && ATHRConfig.feature.farming != null
                && ATHRConfig.feature.farming.trevor != null
                && ATHRConfig.feature.farming.trevor.peltPauseOnChat;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!timerStartedOnce) return;
        long now = System.currentTimeMillis();

        if (timerRunning) {
            if (pauseOnChat() && ChatUtils.isChatOpen()) {
                flushSegment(now);
                timerRunning = false;
                return;
            }
            flushSegment(now);
            timerStartTime = now;
        }

        if (now - lastActivityTime > INACTIVITY_LIMIT_MS) {
            timerRunning = false;
            inactivityFlagged = true;
        }
    }

    private static boolean isVisible() {
        return timerStartedOnce && System.currentTimeMillis() - lastActivityTime <= INACTIVITY_LIMIT_MS;
    }

    private static int peltsPerHour() {
        double hours = activeTimeMs / 3_600_000.0;
        if (hours <= 0.0) return 0;
        return (int) Math.round(sessionPelts / hours);
    }

    @Override
    public Position getPosition() {
        if (ATHRConfig.feature == null || ATHRConfig.feature.farming == null
                || ATHRConfig.feature.farming.trevor == null
                || ATHRConfig.feature.farming.trevor.peltTrackerPos == null) {
            return new Position(2, 100, false, false);
        }
        return ATHRConfig.feature.farming.trevor.peltTrackerPos;
    }

    @Override
    public float getScale() {
        return 1f;
    }

    @Override
    public int getBgColor() {
        return ChromaColour.specialToChromaRGB("0:136:0:0:0");
    }

    @Override
    public int getCornerRadius() {
        return 4;
    }

    @Override
    protected boolean isEnabled() {
        return ATHRConfig.feature != null
                && ATHRConfig.feature.farming != null
                && ATHRConfig.feature.farming.trevor != null
                && ATHRConfig.feature.farming.trevor.enabled
                && ATHRConfig.feature.farming.trevor.peltTracker;
    }

    @Override
    protected boolean extraGuard() {
        // Only show while on the farming island (preview in the position
        // editor bypasses this).
        return TrevorSolver.isOnFarmingIsland();
    }

    @Override
    public List<String> getLines(boolean preview) {
        List<String> lines = new ArrayList<>();
        if (preview) {
            lines.add("§6§lPelt Tracker");
            lines.add("§ePelts: §642");
            lines.add("§ePelts/h: §6120");
            lines.add("§eCooldown: §aReady!");
            return lines;
        }
        if (!isVisible()) return lines;
        lines.add("§6§lPelt Tracker");
        lines.add("§ePelts: §6" + sessionPelts);
        lines.add("§ePelts/h: §6" + peltsPerHour());
        if (cooldownStartMs > 0) {
            long remainingMs = QUEST_COOLDOWN_MS - (System.currentTimeMillis() - cooldownStartMs);
            if (remainingMs > 0) {
                lines.add("§eCooldown: §c" + (long) Math.ceil(remainingMs / 1000.0) + "s");
            } else {
                lines.add("§eCooldown: §aReady!");
            }
        }
        return lines;
    }
}
