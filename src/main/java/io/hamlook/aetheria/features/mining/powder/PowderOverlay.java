package io.hamlook.aetheria.features.mining.powder;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.Position;
import io.hamlook.aetheria.utils.Utils;
import io.hamlook.aetheria.utils.data.SkyblockData;
import io.hamlook.aetheria.utils.overlay.Overlay;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@RegisterEvents
public class PowderOverlay extends Overlay {

    // ordinal, gem name, color code
    private static final String[][] GEM_ENTRIES = {{"Ruby", "§c"}, {"Sapphire", "§b"}, {"Amber", "§6"}, {"Amethyst", "§5"}, {"Jade", "§a"}, {"Topaz", "§e"}, {"Jasper", "§c"}, {"Opal", "§f"}, {"Citrine", "§6"}, {"Aquamarine", "§3"}, {"Peridot", "§a"}, {"Onyx", "§8"},};

    @Getter
    private static PowderOverlay instance;

    public PowderOverlay() {
        super(200, 20);
        instance = this;
    }

    private static String gemLine(String gem, String color, PowderData d, boolean preview) {
        if (preview) {
            return String.format("§5%s§7-§9%s§7-§a%s§7-§f%s %s%s Gemstone", 1, 3, 4, 0, color, gem);
        }
        long[] bd = PowderStats.getGemBreakdown(d, gem);
        if (bd[0] + bd[1] + bd[2] + bd[3] == 0) return null;
        return String.format("§5%s§7-§9%s§7-§a%s§7-§f%s %s%s Gemstone", bd[0], bd[1], bd[2], bd[3], color, gem);
    }

    @Override
    public Position getPosition() {
        return ATHRConfig.feature.mining.powderTrackerConfig.powderOverlayPos;
    }

    @Override
    public float getScale() {
        return ATHRConfig.feature.mining.powderTrackerConfig.powderOverlayScale;
    }

    @Override
    public int getBgColor() {
        return ChromaColour.specialToChromaRGB(ATHRConfig.feature.mining.powderTrackerConfig.powderBgColor);
    }

    @Override
    public int getCornerRadius() {
        return ATHRConfig.feature.mining.powderTrackerConfig.powderCornerRadius;
    }

    @Override
    protected int getBaseWidth() {
        return 200;
    }

    @Override
    protected boolean isEnabled() {
        return ATHRConfig.feature.mining.powderTrackerConfig.powderTracker && PowderStats.getInstance().isTrackingEnabled() && SkyblockData.getCurrentLocation() == SkyblockData.Location.CRYSTAL_HOLLOWS;
    }

    private String lineForEntry(int ordinal, PowderData d, PowderStats stats, boolean preview) {
        switch (ordinal) {
            case 0:
                return "§b§lPowder Tracker" + (!preview && !PowderStats.getInstance().isTrackingEnabled() ? " §7[Paused]" : "");
            case 1: {
                String rate = preview ? "120" : Utils.shortNumberFormat(stats.getChestsPerHour(), 0);
                long n = preview ? 420L : d.totalChestsPicked;
                return String.format("§7%s Chests §7(%s/h)", Utils.shortNumberFormat(n, 0), rate);
            }
            case 2: {
                if (preview) return "§b2x Powder: §aActive! §7(5m 20s)";
                boolean dp = PowderTracker.isDoublePowder();
                String timeLeft = PowderTracker.getDoublePowderTimeLeft();
                String suffix = (dp && timeLeft != null) ? " §7(" + timeLeft + ")" : "";
                return "§b2x Powder: " + (dp ? "§aActive!" + suffix : "§cInactive!");
            }
            case 3: {
                String rate = preview ? "2.5K" : Utils.shortNumberFormat(stats.getGemstonePerHour(), 0);
                long n = preview ? 1337L : d.gemstonePowder;
                return String.format("§d%s Gemstone Powder §7(%s/h)", Utils.shortNumberFormat(n, 0), rate);
            }
            case 4: {
                long n = preview ? 12L : d.diamondEssence;
                if (!preview && n == 0) return null;
                return String.format("§b%s Diamond Essence", Utils.shortNumberFormat(n, 0));
            }
            case 5: {
                long n = preview ? 66L : d.goldEssence;
                if (!preview && n == 0) return null;
                return String.format("§6%s Gold Essence", Utils.shortNumberFormat(n, 0));
            }
            case 6: {
                long n = preview ? 8L : d.oilBarrels;
                if (!preview && n == 0) return null;
                return String.format("§8%s Oil Barrel%s", Utils.shortNumberFormat(n, 0), n == 1 ? "" : "s");
            }
            case 7: {
                long n = preview ? 3L : d.ascensionRopes;
                if (!preview && n == 0) return null;
                return String.format("§5%s Ascension Rope%s", Utils.shortNumberFormat(n, 0), n == 1 ? "" : "s");
            }
            case 8: {
                long n = preview ? 2L : d.wishingCompasses;
                if (!preview && n == 0) return null;
                return String.format("§9%s Wishing Compass%s", Utils.shortNumberFormat(n, 0), n == 1 ? "" : "es");
            }
            case 9: {
                long n = preview ? 1L : d.jungleHearts;
                if (!preview && n == 0) return null;
                return String.format("§6%s Jungle Heart%s", Utils.shortNumberFormat(n, 0), n == 1 ? "" : "s");
            }
            case 10: {
                long raw = preview ? 512L : d.hardStone;
                long compacted = preview ? 5L : d.hardStoneCompacted;
                String rate = preview ? "1.5K" : Utils.shortNumberFormat(stats.getHardStonePerHour(), 0);
                if (!preview && raw + compacted == 0) return null;
                return String.format("§a%s Enchanted Hard Stone §8(%s compact) §7(%s/h)", Utils.shortNumberFormat(raw, 0), Utils.shortNumberFormat(compacted, 0), rate);
            }
            case 24: {
                if (preview) return "§1Playtime: §f2h 30m  §1Session: §f45m";
                long total = d.activeTimeMs;
                long session = stats.getSessionTimeMs();
                if (total == 0 && session == 0) return null;
                return String.format("§1Playtime: §f%s  §1Session: §f%s", Utils.formatDuration(total), Utils.formatDuration(session));
            }
            default: {
                int gemIndex = ordinal - 11;
                if (gemIndex < 0 || gemIndex >= GEM_ENTRIES.length) return null;
                return gemLine(GEM_ENTRIES[gemIndex][0], GEM_ENTRIES[gemIndex][1], d, preview);
            }
        }
    }

    @Override
    public List<String> getLines(boolean preview) {
        List<String> lines = new ArrayList<>();
        PowderStats stats = PowderStats.getInstance();
        PowderData d = stats.getData();

        for (int ordinal : ATHRConfig.feature.mining.powderTrackerConfig.powderDisplayLines) {
            String line = lineForEntry(ordinal, d, stats, preview);
            if (line != null) lines.add(line);
        }
        return lines;
    }
}