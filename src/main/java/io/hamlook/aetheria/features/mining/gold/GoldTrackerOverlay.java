package io.hamlook.aetheria.features.mining.gold;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import io.hamlook.aetheria.core.features.mining.GoldTrackerConfig;
import io.hamlook.aetheria.features.misc.itemList.ItemRegistry;
import io.hamlook.aetheria.features.misc.itemList.SkyblockItem;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.Position;
import io.hamlook.aetheria.utils.Utils;
import io.hamlook.aetheria.utils.data.SkyblockData;
import io.hamlook.aetheria.utils.data.TablistParser;
import io.hamlook.aetheria.utils.overlay.Overlay;
import lombok.Getter;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

@RegisterEvents
public class GoldTrackerOverlay extends Overlay {

    @Getter
    private static GoldTrackerOverlay instance;

    public GoldTrackerOverlay() {
        super(200, 20);
        instance = this;
    }

    private static GoldTrackerConfig config() {
        if (ATHRConfig.feature == null) return null;
        return ATHRConfig.feature.mining.goldTracker;
    }

    @Override
    protected boolean isEnabled() {
        GoldTrackerConfig cfg = config();
        if (cfg == null || !cfg.enabled) return false;
        if (!SkyblockData.isOnSkyblock()) return false;
        if (!GoldStats.isGoldArea()) return false;
        if (cfg.hideWhenPaused && GoldStats.getInstance().isPaused()) return false;
        return !cfg.onlyTrackWhileMining || GoldTracker.isMiningGold();
    }

    @Override
    protected boolean extraGuard() {
        GoldStats stats = GoldStats.getInstance();
        return stats.getData().activeTimeMs > 0;
    }

    @Override
    public List<String> getLines(boolean preview) {
        clearLineIcons();

        GoldTrackerConfig cfg = config();
        if (cfg == null) cfg = new GoldTrackerConfig();
        GoldStats stats = GoldStats.getInstance();
        GoldData data = stats.getData();

        List<String> contentLines = new ArrayList<>();

        String pausedTag = (!preview && stats.isPaused()) ? " §7[Paused]" : "";
        contentLines.add("§6§lGold Tracker" + pausedTag);

        if (preview) {
            contentLines.add("§f1,200 gold ingots/h");
            contentLines.add("§f42.5K gold ingots");
            contentLines.add("§6Profit: §a12.3M coins §7(340K/h)");
            contentLines.add("§7Playtime: §e2h 30m  §7Session: §f45m");
            contentLines.add("§a42 compact §7(10/h)");
            contentLines.add("§fMining Speed: §6⸕2,940");
            contentLines.add("§fMining Fortune: §6☘1,134");
            contentLines.add("§fMining Spread: §e▚135");
        } else {
            boolean showEnchanted = "Show as Enchanted".equals(cfg.displayUnit);
            String color = showEnchanted ? "§a" : "§f";
            String unitName = showEnchanted ? "enchanted gold" : "gold ingots";

            long totalDisplay = showEnchanted ? stats.getTotalAsEnchanted() : stats.getTotalAsIngot();
            long rawTotal = stats.getTotalAsIngot();
            double rate = stats.getRate(totalDisplay);

            String rateLine = String.format("%s%s %s/h",
                color,
                Utils.shortNumberFormat((long) rate, 0), unitName);
            contentLines.add(rateLine);
            if (cfg.showIcons) {
                ItemStack icon = getItemIcon(cfg);
                if (icon != null) putLineIcon(rateLine, icon);
            }

            String totalLine;
            if (showEnchanted) {
                totalLine = String.format("%s%s %s §7(§f%s raw§7)",
                    color,
                    Utils.shortNumberFormat(totalDisplay, 0), unitName,
                    Utils.shortNumberFormat(rawTotal, 0));
            } else {
                totalLine = String.format("%s%s %s",
                    color,
                    Utils.shortNumberFormat(totalDisplay, 0), unitName);
            }
            contentLines.add(totalLine);
            if (cfg.showIcons) {
                ItemStack icon = getItemIcon(cfg);
                if (icon != null) putLineIcon(totalLine, icon);
            }

            long profit = stats.getProfit(cfg.profitUnit);
            double profitRate = stats.getRate(profit);
            String profitLine = String.format("§6Profit: §a%s coins §7(%s/h)",
                Utils.shortNumberFormat(profit, 0), Utils.shortNumberFormat(profitRate, 0));
            contentLines.add(profitLine);
            putLineIcon(profitLine, new ItemStack(Items.gold_ingot));

            contentLines.add(String.format("§7Playtime: §e%s  §7Session: §f%s",
                Utils.formatDuration(data.activeTimeMs, false),
                Utils.formatDuration(stats.getSessionTimeMs(), false)));

            if (cfg.showCompacts && data.compactCount > 0) {
                double compactRate = stats.getRate(data.compactCount);
                contentLines.add(String.format("§a%s compact §7(%s/h)",
                    Utils.shortNumberFormat(data.compactCount, 0),
                    Utils.shortNumberFormat((long) compactRate, 0)));
            }

            if (cfg.showStats) {
                addStatLine(contentLines, "Mining Speed", TablistParser.getMiningSpeed());
                addStatLine(contentLines, "Mining Fortune", TablistParser.getMiningFortune());
                addStatLine(contentLines, "Mining Spread", TablistParser.getMiningSpread());
            }
        }

        List<String> lines = new ArrayList<>();
        String header = contentLines.get(0);
        lines.add(header);

        for (int idx : cfg.displayLines) {
            if (idx >= 0 && idx + 1 < contentLines.size()) {
                String line = contentLines.get(idx + 1);
                lines.add(line);
                if (!preview && cfg.showIcons) {
                    ItemStack icon = getLineIcon(line);
                    if (icon != null) putLineIcon(line, icon);
                }
            }
        }
        return lines;
    }

    private static void addStatLine(List<String> lines, String label, String rawValue) {
        if (rawValue == null || rawValue.isEmpty()) return;
        lines.add("§f" + label + ": " + rawValue);
    }

    private ItemStack getItemIcon(GoldTrackerConfig cfg) {
        String id = "Track Ingot".equals(cfg.trackMode) ? "GOLD_INGOT" : "ENCHANTED_GOLD";
        SkyblockItem item = ItemRegistry.getItem(id);
        return item != null ? item.getStack() : null;
    }

    @Override
    public float getScale() {
        GoldTrackerConfig cfg = config();
        return cfg != null ? cfg.scale : 1f;
    }

    @Override
    public int getBgColor() {
        GoldTrackerConfig cfg = config();
        return cfg != null ? ChromaColour.specialToChromaRGB(cfg.bgColor) : 0x80000000;
    }

    @Override
    public int getCornerRadius() {
        GoldTrackerConfig cfg = config();
        return cfg != null ? cfg.cornerRadius : 4;
    }

    @Override
    public Position getPosition() {
        GoldTrackerConfig cfg = config();
        return cfg != null ? cfg.goldOverlayPos : new Position(4, 220);
    }
}
