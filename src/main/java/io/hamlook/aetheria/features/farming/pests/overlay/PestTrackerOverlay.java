package io.hamlook.aetheria.features.farming.pests.overlay;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.farming.PestTrackerConfig;
import io.hamlook.aetheria.features.farming.FarmingApi;
import io.hamlook.aetheria.features.farming.data.Crop;
import io.hamlook.aetheria.features.farming.data.PestType;
import io.hamlook.aetheria.features.farming.pests.PestStats;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.Position;
import io.hamlook.aetheria.utils.Utils;
import io.hamlook.aetheria.utils.data.SkyblockData;
import io.hamlook.aetheria.utils.overlay.Overlay;
import lombok.Getter;
import net.minecraft.item.ItemStack;

import java.util.*;

@RegisterEvents
public class PestTrackerOverlay extends Overlay {

    @Getter
    private static PestTrackerOverlay instance;

    public PestTrackerOverlay() {
        super(120, 90);
        instance = this;
    }

    private static PestTrackerConfig config() {
        return ATHRConfig.feature.farming.pests.pestTracker;
    }

    private static double rate(PestStats stats, double count) {
        long basis = stats.getRateBasisMs();
        if (basis < 1_000L) return 0.0;
        return count / (basis / 3_600_000.0);
    }

    private static String rateSuffix(double rate) {
        return rate > 0.0 ? " §7(§b" + Utils.shortNumberFormat(rate, 0) + "/h§7)" : "";
    }

    private static Crop cropForId(String id) {
        Crop crop = Crop.findByRawId(id);
        if (crop != null) return crop;
        for (Crop c : Crop.all()) {
            if (id.equals(c.enchantedId) || id.equals(c.blockId)) return c;
        }
        return null;
    }

    private static String itemName(String id, Crop crop) {
        if (id.equals(crop.enchantedId)) return crop.enchantedChatName;
        if (id.equals(crop.blockId)) return crop.blockChatName;
        return crop.displayName;
    }

    private static List<Line> single(String text) {
        return Collections.singletonList(new Line(text, null));
    }

    private List<Line> entryLines(int ordinal, boolean preview, PestStats stats) {
        switch (ordinal) {
            case 0:
                if (preview) return single("§7Pests: §e125 §7(§b50/h§7)");
                return single("§7Pests: §e" + stats.getTotalPests() + rateSuffix(rate(stats, stats.getTotalPests())));
            case 1:
                if (preview) return single("§7Drops: §a1,523 §7(§b600/h§7)");
                return single("§7Drops: §a" + Utils.shortNumberFormat(stats.getTotalDrops(), 0) + rateSuffix(rate(stats, stats.getTotalDrops())));
            case 2:
                if (preview) return single("§7Session: §f42:17");
                return single("§7Session: §f" + Utils.formatClock(stats.getSessionTimeMs()));
            case 3:
                if (preview) return single("§7Total: §e42:17");
                String pausedTag = stats.isPaused() ? " §7[Paused]" : "";
                return single("§7Total: §e" + Utils.formatClock(stats.getActiveTimeMs()) + pausedTag);
            case 4:
                return pestsLines(preview, stats);
            case 5:
                return dropsLines(preview, stats);
            case 6:
                if (preview) return single("§71,234,567 coins §7(2.1M/h)");
                return single("§7" + Utils.shortNumberFormat(stats.getProfit(), 0) + " coins §7(" + Utils.shortNumberFormat(rate(stats, stats.getProfit()), 0) + "/h)");
            default:
                return null;
        }
    }

    private List<Line> pestsLines(boolean preview, PestStats stats) {
        if (preview)
            return Arrays.asList(new Line("§6Slug §7- §a125 §7(§b1k/h§7)", null), new Line("§6Moth §7- §a42 §7(§b20/h§7)", null));
        List<Line> lines = new ArrayList<>();
        for (PestType type : stats.getPestsSortedByKills()) {
            long kills = stats.getKills(type);
            lines.add(new Line("§6" + type.getChatName() + " §7- §a" + Utils.shortNumberFormat(kills, 0) + rateSuffix(rate(stats, kills)), null));
        }
        return lines;
    }

    private List<Line> dropsLines(boolean preview, PestStats stats) {
        if (preview)
            return Arrays.asList(new Line("§7- Enchanted Red Mushroom §a1523 §7(§b1k/h§7)", Crop.findByRawId("RED_MUSHROOM").getIcon()), new Line("§7- Enchanted Cocoa Beans §a58 §7(§b30/h§7)", Crop.findByRawId("INK_SACK__3").getIcon()));
        List<Line> lines = new ArrayList<>();
        for (Map.Entry<String, Long> entry : stats.getDropsMap().entrySet()) {
            Crop crop = cropForId(entry.getKey());
            if (crop == null) continue;
            long drops = entry.getValue();
            lines.add(new Line("§7- " + itemName(entry.getKey(), crop) + " §a" + Utils.shortNumberFormat(drops, 0) + rateSuffix(rate(stats, drops)), crop.getIcon()));
        }
        return lines;
    }

    @Override
    public List<String> getLines(boolean preview) {
        List<String> lines = new ArrayList<>();
        clearLineIcons();
        PestStats stats = PestStats.getInstance();
        String pausedTag = (!preview && stats.isPaused()) ? " §7[Paused]" : "";
        lines.add("§6§lPest Tracker" + pausedTag);
        for (int ordinal : config().pestTrackerLines) {
            List<Line> entry = entryLines(ordinal, preview, stats);
            if (entry == null) continue;
            for (Line line : entry) {
                lines.add(line.text);
                if (config().showIcons) putLineIcon(line.text, line.icon);
            }
        }
        return lines;
    }

    @Override
    public Position getPosition() {
        return config().pestOverlayPos;
    }

    @Override
    public float getScale() {
        return config().scale;
    }

    @Override
    public int getBgColor() {
        return config().bgColor;
    }

    @Override
    public int getCornerRadius() {
        return config().cornerRadius;
    }

    @Override
    protected boolean isEnabled() {
        if (!config().enabled || !SkyblockData.isOnSkyblock() || SkyblockData.getCurrentLocation() != SkyblockData.Location.GARDEN || !PestStats.getInstance().isOverlayVisible())
            return false;
        if (config().hideWhenPaused && PestStats.getInstance().isPaused()) return false;
        if (config().hideWhileFarming && FarmingApi.isCurrentlyFarming()) return false;
        return !config().hideOnFarmingTool || !FarmingApi.isHoldingFarmingTool();
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

    private static final class Line {
        final String text;
        final ItemStack icon;

        Line(String text, ItemStack icon) {
            this.text = text;
            this.icon = icon;
        }
    }
}