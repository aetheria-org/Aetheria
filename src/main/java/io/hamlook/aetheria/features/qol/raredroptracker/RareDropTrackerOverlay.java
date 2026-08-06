package io.hamlook.aetheria.features.qol.raredroptracker;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.qol.RareDropTrackerConfig;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.Position;
import io.hamlook.aetheria.utils.data.SkyblockData;
import io.hamlook.aetheria.utils.overlay.Overlay;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * HUD overlay for the Rare Drop Tracker. Shows every tracked item's running drop
 * count, and its progress toward a goal amount if one has been set (see
 * {@link RareDropTrackerGUI}).
 */
@RegisterEvents
public class RareDropTrackerOverlay extends Overlay {

    @Getter
    private static RareDropTrackerOverlay instance;

    public RareDropTrackerOverlay() {
        super(140, LINE_HEIGHT * 6 + PADDING * 2);
        instance = this;
    }

    @Override
    protected int getBaseWidth() {
        return 140;
    }

    @Override
    public Position getPosition() {
        return ATHRConfig.feature.qol.rareDropTracker.overlayPos;
    }

    @Override
    public float getScale() {
        return ATHRConfig.feature.qol.rareDropTracker.overlayScale;
    }

    @Override
    public int getBgColor() {
        return ChromaColour.specialToChromaRGB(ATHRConfig.feature.qol.rareDropTracker.overlayBgColor);
    }

    @Override
    public int getCornerRadius() {
        return ATHRConfig.feature.qol.rareDropTracker.overlayCornerRadius;
    }

    @Override
    protected boolean isEnabled() {
        return ATHRConfig.feature.qol.rareDropTracker.enabled
                && ATHRConfig.feature.qol.rareDropTracker.showOverlay
                && SkyblockData.isOnSkyblock()
                && hasAnyDrops();
    }

    private boolean hasAnyDrops() {
        return ATHRConfig.feature.qol.rareDropTracker.trackedItems.values().stream()
                .anyMatch(i -> i.count > 0);
    }

    @Override
    protected boolean hideOnChat()  { return ATHRConfig.feature.qol.rareDropTracker.hideOnChat; }
    @Override
    protected boolean hideOnTab()   { return ATHRConfig.feature.qol.rareDropTracker.hideOnTab; }
    @Override
    protected boolean hideOnDebug() { return ATHRConfig.feature.qol.rareDropTracker.hideOnDebug; }

    @Override
    public List<String> getLines(boolean preview) {
        List<String> lines = new ArrayList<>();
        lines.add("§d§lRare Drop Tracker");

        RareDropTrackerConfig config = ATHRConfig.feature.qol.rareDropTracker;

        if (preview) {
            lines.add("§dGiant's Sword §f3§7/§f10");
            lines.add("§dSummoning Eye §f27");
            lines.add("§dVoodoo Doll §f1§7/§f5");
            return lines;
        }

        if (config.trackedItems.isEmpty()) return lines;

        for (Map.Entry<String, RareDropTrackerConfig.TrackedItem> e : config.trackedItems.entrySet()) {
            RareDropTrackerConfig.TrackedItem item = e.getValue();
            boolean hasGoal = item.goal > 0;

            if (config.overlayOnlyShowGoals && !hasGoal) continue;
            if (config.overlayHideCompleted && hasGoal && item.count >= item.goal) continue;

            String name = item.displayName != null ? item.displayName : e.getKey();
            if (hasGoal) {
                String colour = item.count >= item.goal ? "§a" : "§d";
                lines.add(colour + name + " §f" + item.count + "§7/§f" + item.goal);
            } else {
                lines.add("§d" + name + " §f" + item.count);
            }
        }

        return lines;
    }
}
