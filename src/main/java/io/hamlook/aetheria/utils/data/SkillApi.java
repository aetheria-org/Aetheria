package io.hamlook.aetheria.utils.data;

import io.hamlook.aetheria.utils.Utils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SkillApi {

    private static final Pattern XP_LINE = Pattern.compile("^([\\d,]+)/[\\d.,]+[KMBk]?$");
    private static final Pattern PROGRESS_LINE = Pattern.compile("Progress to Level (\\d+):");
    private static final String AVERAGE_MARKER = "Skill Avg.";

    private static final long[] STANDARD_XP = {50, 125, 200, 300, 500, 750, 1000, 1500, 2000, 3500, 5000, 7500, 10000, 15000, 20000, 30000, 50000, 75000, 100000, 200000, 300000, 400000, 500000, 600000, 700000, 800000, 900000, 1000000, 1100000, 1200000, 1300000, 1400000, 1500000, 1600000, 1700000, 1800000, 1900000, 2000000, 2100000, 2200000, 2300000, 2400000, 2500000, 2600000, 2750000, 2900000, 3100000, 3400000, 3700000, 4000000, 4300000, 4600000, 4900000, 5200000, 5500000, 5800000, 6100000, 6400000, 6700000, 7000000};

    private static final long[] DUNGEON_XP = {50, 75, 110, 160, 230, 330, 470, 670, 950, 1340, 1890, 2665, 3760, 5260, 7380, 10300, 14400, 20000, 27600, 38000, 52500, 71500, 97000, 132000, 180000, 243000, 328000, 445000, 600000, 800000, 1065000, 1410000, 1900000, 2500000, 3300000, 4300000, 5600000, 7200000, 9200000, 12000000, 15000000, 19000000, 24000000, 30000000, 38000000, 48000000, 60000000, 75000000, 93000000, 116250000};

    private static final long[] RUNE_SOCIAL_XP = {50, 100, 125, 160, 200, 250, 315, 400, 500, 625, 785, 1000, 1250, 1565, 2000, 2500, 3125, 4000, 5000, 6250, 7850, 9800, 12250, 15300, 19100};

    private static final long[] CARPENTRY_XP = {50, 125, 200, 300, 500, 750, 1000, 1500, 2000, 3500, 5000, 7500, 10000, 15000, 20000, 30000, 50000, 75000, 100000, 200000, 300000, 400000, 500000, 600000, 700000, 800000, 900000, 1000000, 1100000, 1200000, 1300000, 1400000, 1500000, 1600000, 1700000, 1800000, 1900000, 2000000, 2100000, 2200000, 2300000, 2400000, 2500000, 2600000, 2750000, 2900000, 3100000, 3400000, 3700000, 4000000, 4300000, 4600000, 4900000, 5200000, 5500000, 5800000, 6100000, 6400000, 6700000, 7000000};

    private SkillApi() {
    }

    public static String stripColour(String line) {
        return line == null ? "" : line.replaceAll("(?i)§.", "");
    }

    public static int rawLevel(List<String> lore) {
        for (String line : lore) {
            Matcher matcher = PROGRESS_LINE.matcher(stripColour(line));
            if (matcher.find()) return Integer.parseInt(matcher.group(1));
        }
        return -1;
    }

    public static int level(List<String> lore) {
        int raw = rawLevel(lore);
        return raw < 0 ? -1 : raw - 1;
    }

    public static long currentXp(List<String> lore) {
        for (String line : lore) {
            String clean = stripColour(line).trim();
            if (clean.startsWith("—") || clean.startsWith("-")) clean = clean.substring(1).trim();
            Matcher matcher = XP_LINE.matcher(clean);
            if (!matcher.find()) continue;
            try {
                return Long.parseLong(matcher.group(1).replace(",", ""));
            } catch (NumberFormatException ignored) {
            }
        }
        return -1;
    }

    public static long requiredXp(List<String> lore) {
        for (String line : lore) {
            String clean = stripColour(line).trim();
            if (clean.startsWith("—") || clean.startsWith("-")) clean = clean.substring(1).trim();
            if (!XP_LINE.matcher(clean).find()) continue;
            String[] parts = clean.split("/");
            if (parts.length < 2) continue;
            return Utils.parseRawNumber(parts[1].trim());
        }
        return -1;
    }

    public static float average(List<String> lore) {
        for (String line : lore) {
            if (!line.contains(AVERAGE_MARKER)) continue;
            try {
                return Float.parseFloat(stripColour(line).split(" ")[0].trim());
            } catch (NumberFormatException ignored) {
                return -1;
            }
        }
        return -1;
    }

    public static long[] tableFor(String skillName) {
        if (skillName.contains("dungeon") || skillName.contains("dungeoneering")) return DUNGEON_XP;
        if (skillName.contains("social") || skillName.contains("runecrafting")) return RUNE_SOCIAL_XP;
        if (skillName.contains("carpentry")) return CARPENTRY_XP;
        return STANDARD_XP;
    }

    public static int maxLevelFor(long[] table) {
        return table == DUNGEON_XP ? 50 : table == RUNE_SOCIAL_XP ? 25 : 60;
    }

    public static long totalXpForLevel(long[] table, int level) {
        long total = 0;
        for (int i = 0; i < level && i < table.length; i++) total += table[i];
        return total;
    }
}
