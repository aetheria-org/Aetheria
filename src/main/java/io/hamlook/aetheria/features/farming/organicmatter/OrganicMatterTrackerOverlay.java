package io.hamlook.aetheria.features.farming.organicmatter;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.farming.OrganicMatterTrackerConfig;
import io.hamlook.aetheria.features.farming.FarmingApi;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.Position;
import io.hamlook.aetheria.utils.Utils;
import io.hamlook.aetheria.utils.overlay.Overlay;
import lombok.Getter;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

@RegisterEvents
public class OrganicMatterTrackerOverlay extends Overlay {

    @Getter
    private static OrganicMatterTrackerOverlay instance;

    private static final int TITLE_ORDINAL = 0;
    private static final int TOTAL_OM_ORDINAL = 1;
    private static final int OM_PER_HOUR_ORDINAL = 2;
    private static final int SESSION_TIMER_ORDINAL = 3;
    private static final int TOTAL_ITEMS_ORDINAL = 4;

    public OrganicMatterTrackerOverlay() {
        super(160, 70);
        instance = this;
    }

    private static OrganicMatterTrackerConfig config() {
        return ATHRConfig.feature.farming.organicMatterTracker;
    }

    private static final class Entry {
        final ItemStack icon;
        final String text;

        Entry(ItemStack icon, String text) {
            this.icon = icon;
            this.text = text;
        }
    }

    private Entry entryForOrdinal(int ordinal, boolean preview) {
        if (ordinal == TITLE_ORDINAL) {
            String pausedTag = (!preview && OrganicMatterTracker.isPaused()) ? " §7[Paused]" : "";
            return new Entry(null, "§a§lOrganic Matter Tracker" + pausedTag);
        }
        if (ordinal == TOTAL_OM_ORDINAL) {
            if (preview) return new Entry(null, "§aTotal Organic Matter: §f4,830,000");
            return new Entry(null, "§aTotal Organic Matter: §f"
                    + Utils.shortNumberFormat(OrganicMatterTracker.totalOrganicMatter(), 0));
        }
        if (ordinal == OM_PER_HOUR_ORDINAL) {
            if (preview) return new Entry(null, "§a402,500/h organic matter");
            return new Entry(null, "§a" + Utils.shortNumberFormat(OrganicMatterTracker.organicMatterPerHour(), 0) + "/h organic matter");
        }
        if (ordinal == SESSION_TIMER_ORDINAL) {
            if (preview) return new Entry(null, "§1Playtime: §f2h 30m  §1Session: §f45m");
            String pausedTag = OrganicMatterTracker.isPaused() ? " §7[Paused]" : "";
            return new Entry(null, "§1Playtime: §f" + Utils.formatDuration(OrganicMatterTracker.getActiveTimeMs())
                    + "  §1Session: §f" + Utils.formatDuration(OrganicMatterTracker.getSessionTimeMs()) + pausedTag);
        }
        if (ordinal == TOTAL_ITEMS_ORDINAL) {
            if (preview) return new Entry(null, "§bTotal: §f108,240 items");
            long total = OrganicMatterTracker.totalItems();
            if (total <= 0L) return null;
            return new Entry(null, "§bTotal: §f" + Utils.shortNumberFormat((double) total, 0) + " items");
        }

        return null;
    }

    private List<Entry> buildEntries(boolean preview) {
        List<Entry> entries = new ArrayList<>();
        for (int ordinal : config().organicMatterDisplayLines) {
            Entry entry = entryForOrdinal(ordinal, preview);
            if (entry != null) entries.add(entry);
        }
        return entries;
    }

    @Override
    public List<String> getLines(boolean preview) {
        List<String> lines = new ArrayList<>();
        clearLineIcons();
        for (Entry entry : buildEntries(preview)) {
            lines.add(entry.text);
            putLineIcon(entry.text, entry.icon);
        }
        return lines;
    }

    @Override
    public Position getPosition() {
        return config().organicMatterTrackerPosition;
    }

    @Override
    public float getScale() {
        return config().organicMatterTrackerScale;
    }

    @Override
    public int getBgColor() {
        return config().organicMatterTrackerBgColor;
    }

    @Override
    public int getCornerRadius() {
        return config().organicMatterTrackerCornerRadius;
    }

    @Override
    protected boolean isEnabled() {
        if (!config().enabled) return false;
        boolean locationOk = !config().requireFarmingIsland || FarmingApi.isInFarmingLocation();
        if (!locationOk) return false;
        if (config().hideWhenPaused && OrganicMatterTracker.isPaused()) return false;
        if (config().showOnlyWhileFarming && !FarmingApi.isCurrentlyFarming()) return false;
        if (config().showOnlyWhileHoldingFarmingTool && !FarmingApi.isHoldingFarmingTool()) return false;
        return true;
    }

    @Override
    protected boolean hideOnChat() {
        return config().hideOnChat;
    }

    @Override
    protected boolean hideOnTab() {
        return config().hideOnTab;
    }

    @Override
    protected boolean hideOnDebug() {
        return config().hideOnDebug;
    }
}

