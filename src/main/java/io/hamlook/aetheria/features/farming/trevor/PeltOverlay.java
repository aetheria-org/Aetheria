package io.hamlook.aetheria.features.farming.trevor;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.Position;
import io.hamlook.aetheria.utils.overlay.Overlay;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Session pelt tracker for Trevor's hunts: total pelts earned and pelts/hour,
 * counted from the reward chat message. The rate clock starts on the first
 * pelt of the session.
 */
@RegisterEvents
public class PeltOverlay extends Overlay {

    @Getter
    private static PeltOverlay instance;

    private static final long QUEST_COOLDOWN_MS = 20_000;

    private static int sessionPelts = 0;
    private static long firstPeltMs = 0;
    private static long cooldownStartMs = 0;

    public PeltOverlay() {
        super(90, 34);
        instance = this;
    }

    public static void addPelts(int amount) {
        if (firstPeltMs == 0) firstPeltMs = System.currentTimeMillis();
        sessionPelts += amount;
    }

    /** Arms the 20s new-quest cooldown; called when Trevor announces a hunt. */
    public static void startCooldown() {
        cooldownStartMs = System.currentTimeMillis();
    }

    public static void reset() {
        sessionPelts = 0;
        firstPeltMs = 0;
        cooldownStartMs = 0;
    }

    private static int peltsPerHour() {
        if (firstPeltMs == 0) return 0;
        double hours = (System.currentTimeMillis() - firstPeltMs) / 3_600_000.0;
        if (hours < 1.0 / 60.0) hours = 1.0 / 60.0; // clamp to 1 min so early rates aren't absurd
        return (int) Math.round(sessionPelts / hours);
    }

    @Override
    public Position getPosition() {
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
                && ATHRConfig.feature.farming.trevor.enabled
                && ATHRConfig.feature.farming.trevor.peltTracker;
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
        if (sessionPelts == 0 && cooldownStartMs == 0) return lines;
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
