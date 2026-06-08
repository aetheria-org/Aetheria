package io.hamlook.aetheria.features.misc;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.ContainerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RegisterEvents
public class SkillXpDisplay {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final Pattern XP_LINE = Pattern.compile("^([\\d,]+)/[\\d.,]+[KMBk]?$");
    private static final Pattern PROGRESS_LINE = Pattern.compile("Progress to Level (\\d+):");

    private static final String HINT = EnumChatFormatting.DARK_GRAY + "[SHIFT: XP to max level]";

    private static final long[] STANDARD_XP = {50, 125, 200, 300, 500, 750, 1000, 1500, 2000, 3500, 5000, 7500, 10000, 15000, 20000, 30000, 50000, 75000, 100000, 200000, 300000, 400000, 500000, 600000, 700000, 800000, 900000, 1000000, 1100000, 1200000, 1300000, 1400000, 1500000, 1600000, 1700000, 1800000, 1900000, 2000000, 2100000, 2200000, 2300000, 2400000, 2500000, 2600000, 2750000, 2900000, 3100000, 3400000, 3700000, 4000000, 4300000, 4600000, 4900000, 5200000, 5500000, 5800000, 6100000, 6400000, 6700000, 7000000};

    private static final long[] DUNGEON_XP = {50, 75, 110, 160, 230, 330, 470, 670, 950, 1340, 1890, 2665, 3760, 5260, 7380, 10300, 14400, 20000, 27600, 38000, 52500, 71500, 97000, 132000, 180000, 243000, 328000, 445000, 600000, 800000, 1065000, 1410000, 1900000, 2500000, 3300000, 4300000, 5600000, 7200000, 9200000, 12000000, 15000000, 19000000, 24000000, 30000000, 38000000, 48000000, 60000000, 75000000, 93000000, 116250000};

    private static final long[] RUNE_SOCIAL_XP = {50, 100, 125, 160, 200, 250, 315, 400, 500, 625, 785, 1000, 1250, 1565, 2000, 2500, 3125, 4000, 5000, 6250, 7850, 9800, 12250, 15300, 19100};

    private static final long[] CARPENTRY_XP = {50, 125, 200, 300, 500, 750, 1000, 1500, 2000, 3500, 5000, 7500, 10000, 15000, 20000, 30000, 50000, 75000, 100000, 200000, 300000, 400000, 500000, 600000, 700000, 800000, 900000, 1000000, 1100000, 1200000, 1300000, 1400000, 1500000, 1600000, 1700000, 1800000, 1900000, 2000000, 2100000, 2200000, 2300000, 2400000, 2500000, 2600000, 2750000, 2900000, 3100000, 3400000, 3700000, 4000000, 4300000, 4600000, 4900000, 5200000, 5500000, 5800000, 6100000, 6400000, 6700000, 7000000};

    private static long totalXpForLevel(long[] table, int level) {
        long total = 0;
        for (int i = 0; i < level && i < table.length; i++) total += table[i];
        return total;
    }

    private static int parseLevel(List<String> tooltip) {
        for (String line : tooltip) {
            Matcher m = PROGRESS_LINE.matcher(strip(line));
            if (m.find()) return Integer.parseInt(m.group(1)) - 1;
        }
        return -1;
    }

    private static long parseCurrentXp(List<String> tooltip) {
        for (String line : tooltip) {
            String clean = strip(line).trim();
            if (clean.startsWith("—") || clean.startsWith("-")) clean = clean.substring(1).trim();
            Matcher m = XP_LINE.matcher(clean);
            if (m.find()) {
                try {
                    return Long.parseLong(m.group(1).replace(",", ""));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return -1;
    }

    private static String formatXp(long xp) {
        if (xp <= 0) return "0";
        if (xp >= 1_000_000_000) return String.format("%.2fB", xp / 1e9);
        if (xp >= 1_000_000) return String.format("%.2fM", xp / 1e6);
        if (xp >= 1_000) return String.format("%.1fK", xp / 1e3);
        return String.valueOf(xp);
    }

    private static String strip(String s) {
        return s == null ? "" : s.replaceAll("(?i)§.", "");
    }

    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        if (ATHRConfig.feature == null || !ATHRConfig.feature.misc.skillXpDisplay) return;
        if (event.toolTip == null || event.itemStack == null) return;
        if (!ContainerUtils.isChestOpen()) return;

        String containerName = ContainerUtils.getContainerName();
        if (containerName == null) return;
        boolean isSkill = containerName.contains("View Skills") || containerName.contains("Your Skills");
        boolean isDungeon = containerName.contains("View Dungeon Stats") || containerName.contains("Dungeoneering");
        if (!isSkill && !isDungeon) return;

        List<String> tooltip = event.toolTip;
        if (tooltip.isEmpty()) return;

        String name = strip(tooltip.get(0)).toLowerCase();

        long[] table;
        int maxLevel;

        if (isDungeon) {
            table = DUNGEON_XP;
            maxLevel = 50;
        } else if (name.contains("social")) {
            table = RUNE_SOCIAL_XP;
            maxLevel = 25;
        } else if (name.contains("runecrafting")) {
            table = RUNE_SOCIAL_XP;
            maxLevel = 25;
        } else if (name.contains("carpentry")) {
            table = CARPENTRY_XP;
            maxLevel = 60;
        } else {
            table = STANDARD_XP;
            maxLevel = 60;
        }

        int currentLevel = parseLevel(tooltip);
        long currentXp = parseCurrentXp(tooltip);
        if (currentLevel < 0 || currentXp < 0) return;

        boolean shifting = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);

        if (!shifting) {
            if (!tooltip.contains(HINT)) tooltip.add(HINT);
            return;
        }

        tooltip.remove(HINT);

        if (currentLevel >= maxLevel) {
            tooltip.add(EnumChatFormatting.GREEN + "Max level reached!");
            return;
        }

        long totalAtCurrent = totalXpForLevel(table, currentLevel);
        long totalAbsolute = totalAtCurrent + currentXp;
        long totalAtMax = totalXpForLevel(table, maxLevel);
        long remaining = totalAtMax - totalAbsolute;

        tooltip.add(EnumChatFormatting.AQUA + "XP to max (Lv" + maxLevel + "): " + EnumChatFormatting.YELLOW + formatXp(remaining));
    }
}