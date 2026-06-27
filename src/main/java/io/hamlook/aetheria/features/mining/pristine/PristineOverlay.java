package io.hamlook.aetheria.features.mining.pristine;

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
public class PristineOverlay extends Overlay {

    private static final String[][] GEM_ENTRIES = {{"Ruby", "§c"}, {"Sapphire", "§b"}, {"Amber", "§6"}, {"Amethyst", "§5"}, {"Jade", "§a"}, {"Topaz", "§e"}, {"Jasper", "§c"}, {"Opal", "§f"}, {"Citrine", "§6"}, {"Aquamarine", "§3"}, {"Peridot", "§a"}, {"Onyx", "§8"}};

    @Getter
    private static PristineOverlay instance;

    public PristineOverlay() {
        super(200, 20);
        instance = this;
    }

    private static String gemLine(String gem, String color, PristineData d, boolean preview, boolean compacted) {
        long flawed = d.gemstones.getOrDefault("Flawed_" + gem, 0L);
        long fine = d.gemstones.getOrDefault("Fine_" + gem, 0L);
        long flawless = d.gemstones.getOrDefault("Flawless_" + gem, 0L);
        long total = flawed + fine + flawless;
        if (!preview && total == 0) return null;

        if (preview) {
            return compacted ? String.format("§5%s§7-§9%s§7-§a%s %s%s Gemstone", 2, 10, 40, color, gem) : String.format("§a%s %s%s Gemstone", 200, color, gem);
        }

        if (compacted) {
            long[] bd = PristineStats.getGemBreakdown(d, gem);
            return String.format("§5%s§7-§9%s§7-§a%s %s%s Gemstone", bd[0], bd[1], bd[2], color, gem);
        }
        return String.format("§a%s %s%s Gemstone", Utils.shortNumberFormat(total, 0), color, gem);
    }

    @Override
    public Position getPosition() {
        return ATHRConfig.feature.mining.pristineTrackerConfig.pristineOverlayPos;
    }

    @Override
    public float getScale() {
        return ATHRConfig.feature.mining.pristineTrackerConfig.pristineOverlayScale;
    }

    @Override
    public int getBgColor() {
        return ChromaColour.specialToChromaRGB(ATHRConfig.feature.mining.pristineTrackerConfig.pristineBgColor);
    }

    @Override
    public int getCornerRadius() {
        return ATHRConfig.feature.mining.pristineTrackerConfig.pristineCornerRadius;
    }

    @Override
    protected int getBaseWidth() {
        return 200;
    }

    @Override
    protected boolean isEnabled() {
        return ATHRConfig.feature.mining.pristineTrackerConfig.pristineTracker && PristineStats.getInstance().isTrackingEnabled() && SkyblockData.getCurrentLocation() == SkyblockData.Location.CRYSTAL_HOLLOWS;
    }

    private String lineForEntry(int ordinal, PristineData d, PristineStats stats, boolean preview) {
        if (ordinal == 0) {
            return "§d§lPristine Tracker" + (!preview && !stats.isTrackingEnabled() ? " §7[Paused]" : "");
        }
        if (ordinal == 1) {
            long total = preview ? 1500L : d.gemstones.values().stream().mapToLong(Long::longValue).sum();
            String rate = preview ? "150" : Utils.shortNumberFormat(stats.getGemstonesPerHour(), 0);
            return String.format("§7Total Gems: §a%s §7(%s/h)", Utils.shortNumberFormat(total, 0), rate);
        }
        if (ordinal == 2) {
            int procs = preview ? 42 : d.totalProcs;
            String rate = preview ? "5" : Utils.shortNumberFormat(stats.getProcsPerHour(), 0);
            return String.format("§7Procs: §d%s §7(%s/h)", Utils.shortNumberFormat(procs, 0), rate);
        }
        if (ordinal == 15) {
            if (preview) return "§1Playtime: §f2h 30m  §1Session: §f45m";
            long total = d.activeTimeMs;
            long session = stats.getSessionTimeMs();
            if (total == 0 && session == 0) return null;
            return String.format("§1Playtime: §f%s  §1Session: §f%s", Utils.formatDuration(total), Utils.formatDuration(session));
        }
        int gemIndex = ordinal - 3;
        if (gemIndex < 0 || gemIndex >= GEM_ENTRIES.length) return null;
        return gemLine(GEM_ENTRIES[gemIndex][0], GEM_ENTRIES[gemIndex][1], d, preview, ATHRConfig.feature.mining.pristineTrackerConfig.showCompacted);
    }

    @Override
    public List<String> getLines(boolean preview) {
        List<String> lines = new ArrayList<>();
        PristineStats stats = PristineStats.getInstance();
        PristineData d = stats.getData();

        for (int ordinal : ATHRConfig.feature.mining.pristineTrackerConfig.pristineDisplayLines) {
            String line = lineForEntry(ordinal, d, stats, preview);
            if (line != null) lines.add(line);
        }
        return lines;
    }
}
