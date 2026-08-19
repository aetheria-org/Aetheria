package io.hamlook.aetheria.features.farming.farmingtracker;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.farming.FarmingTrackerConfig;
import io.hamlook.aetheria.features.farming.FarmingApi;
import io.hamlook.aetheria.features.farming.data.Crop;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.Position;
import io.hamlook.aetheria.utils.Utils;
import io.hamlook.aetheria.utils.overlay.Overlay;
import lombok.Getter;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

@RegisterEvents
public class FarmingTrackerOverlay extends Overlay {

    @Getter
    private static FarmingTrackerOverlay instance;

    private static final int COUNT_LINES_START = 3;
    private static final int SESSION_TIMER_ORDINAL = COUNT_LINES_START + Crop.all().length;
    private static final int TOTAL_CROPS_ORDINAL = SESSION_TIMER_ORDINAL + 1;

    public FarmingTrackerOverlay() {
        super(160, 70);
        instance = this;
    }

    private static FarmingTrackerConfig config() {
        return ATHRConfig.feature.farming.farmingTracker;
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
        if (ordinal == 0) {
            String pausedTag = (!preview && FarmingTracker.isPaused()) ? " §7[Paused]" : "";
            return new Entry(null, "§a§lFarming Tracker" + pausedTag);
        }
        if (ordinal == 1) {
            if (preview) return new Entry(null, "§76,144,000 coins §7(3.2M/h)");
            return new Entry(null, "§7" + Utils.shortNumberFormat(FarmingTracker.currentValue(), 0)
                    + " coins §7(" + Utils.shortNumberFormat(FarmingTracker.coinsPerHour(), 0) + "/h)");
        }
        if (ordinal == 2) {
            if (preview) return new Entry(null, "§b12,480 crops/h");
            return new Entry(null, "§b" + Utils.shortNumberFormat(FarmingTracker.cropsPerHour(), 0) + " crops/h");
        }
        if (ordinal == SESSION_TIMER_ORDINAL) {
            if (preview) return new Entry(null, "§7Session: §f42:17");
            String pausedTag = FarmingTracker.isPaused() ? " §7[Paused]" : "";
            return new Entry(null, "§7Session: §f" + Utils.formatClock(FarmingTracker.getActiveTimeMs()) + pausedTag);
        }
        if (ordinal == TOTAL_CROPS_ORDINAL) {
            if (preview) return new Entry(null, "§bTotal: §f108,240 crops");
            long total = FarmingTracker.totalRawCrops();
            if (total <= 0L) return null;
            return new Entry(null, "§bTotal: §f" + Utils.shortNumberFormat((double) total, 0) + " crops");
        }

        Crop[] crops = Crop.all();

        int countIndex = ordinal - COUNT_LINES_START;
        if (countIndex >= 0 && countIndex < crops.length) {
            Crop crop = crops[countIndex];
            ItemStack icon = crop.getIcon();

            if (preview) {
                return new Entry(icon, "§a" + crop.displayName + ": §f12 §b(4,760/h)");
            }

            long raw = FarmingTracker.getCount(crop.rawId);
            long ench = FarmingTracker.getCount(crop.enchantedId);
            long block = crop.blockId != null ? FarmingTracker.getCount(crop.blockId) : 0L;

            if (raw == 0L && ench == 0L && block == 0L) return null;

            List<String> parts = new ArrayList<>();
            if (raw > 0) parts.add("§a" + crop.displayName + ": §f" + Utils.shortNumberFormat((double) raw, 0));
            if (ench > 0) parts.add("§7E." + crop.displayName + ": §f" + Utils.shortNumberFormat((double) ench, 0));
            if (block > 0 && crop.blockDisplayName != null) {
                parts.add("§7" + crop.blockDisplayName + ": §f" + Utils.shortNumberFormat((double) block, 0));
            }

            double rate = FarmingTracker.getCropRate(crop);
            if (rate > 0.0) parts.add("§b(" + Utils.shortNumberFormat(rate, 0) + "/h)");

            return new Entry(icon, String.join(" ", parts));
        }

        return null;
    }

    private List<Entry> buildEntries(boolean preview) {
        List<Entry> entries = new ArrayList<>();
        for (int ordinal : config().farmingDisplayLines) {
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
        return config().farmingTrackerPosition;
    }

    @Override
    public float getScale() {
        return config().farmingTrackerScale;
    }

    @Override
    public int getBgColor() {
        return config().farmingTrackerBgColor;
    }

    @Override
    public int getCornerRadius() {
        return config().farmingTrackerCornerRadius;
    }

    @Override
    protected boolean isEnabled() {
        if (!config().enabled) return false;
        if (config().requireFarmingIsland && !FarmingApi.isInFarmingLocation()) return false;
        if (config().hideWhenPaused && FarmingTracker.isPaused()) return false;
        if (config().showOnlyWhileFarming && !FarmingApi.isCurrentlyFarming()) return false;
        return !config().showOnlyWhileHoldingFarmingTool || FarmingApi.isHoldingFarmingTool();
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
