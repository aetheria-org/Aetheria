package io.hamlook.aetheria.features.mining.hotm;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.ColorUtils;
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
public class HotmPowderDisplay {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final String HOTM_TITLE = "Heart of the Mountain";

    private static final Pattern LEVEL_PATTERN = Pattern.compile("§(.)Level (\\d+)");

    private static final Pattern COST_PATTERN = Pattern.compile("^(?:§.)*§7Cost$");

    private static final String SHIFT_HINT = EnumChatFormatting.DARK_GRAY + "[SHIFT: powder for next 10 levels]";

    private static String buildSpentLine(HotmPerkData perk, int rawLevel) {
        long spent = perk.calculateTotalCost(rawLevel);
        long max = perk.totalCostMaxLevel;
        boolean isMax = rawLevel >= perk.maxLevel;
        double pct = max > 0 ? (spent * 100.0 / max) : 100.0;

        String label = "§7" + perk.powderType.displayName + " spent: ";

        switch (ATHRConfig.feature.mining.hotmPowder.hotmPowderSpentDesign) {
            case 1:
                return isMax ? label + "§e" + fmt2(pct) + "% §7(§aMax level§7)" : label + "§e" + fmt2(pct) + "%§7 of max";
            case 2:
                return isMax ? label + "§e" + formatNumber(max) + " §7(§aMax level§7)" : label + "§e" + formatNumber(spent) + "§7/§e" + formatNumber(max) + "§7 (§e" + fmt2(pct) + "%§7)";
            default:
                return isMax ? label + "§e" + formatNumber(max) + " §7(§aMax level§7)" : label + "§e" + formatNumber(spent) + "§7 / §e" + formatNumber(max);
        }
    }

    // TODO: Uncomment when Core of the Mountain is updated
    /*
    private void handleCotm(List<String> tip) {
        int rawLevel = parseRawLevel(tip);
        if (rawLevel < 0) return;
        rawLevel = Math.min(rawLevel, CoreOfTheMountainData.MAX_LEVEL);

        if (ATHRConfig.feature.mining.hotmPowder.hotmPowderSpent) {
            java.util.Map<HotmPerkData.PowderType, Long> spent = CoreOfTheMountainData.cumulativeCost(rawLevel);
            boolean isMax = rawLevel >= CoreOfTheMountainData.MAX_LEVEL;
            java.util.Map<HotmPerkData.PowderType, Long> max   = CoreOfTheMountainData.cumulativeCost(CoreOfTheMountainData.MAX_LEVEL);

            int insertPos = 2;
            for (HotmPerkData.PowderType type : HotmPerkData.PowderType.values()) {
                long spentAmt = spent.getOrDefault(type, 0L);
                long maxAmt   = max.getOrDefault(type, 0L);
                if (maxAmt == 0) continue;
                String label = "§7" + type.displayName + " spent: ";
                String line;
                if (isMax) {
                    line = label + "§e" + formatNumber(spentAmt) + " §7(§aMax level§7)";
                } else {
                    line = label + "§e" + formatNumber(spentAmt) + "§7/§e" + formatNumber(maxAmt);
                }
                tip.add(insertPos++, line);
            }
        }

        if (ATHRConfig.feature.mining.hotmPowder.hotmPowderFor10Levels && rawLevel < CoreOfTheMountainData.MAX_LEVEL) {
            int costIndex = indexOfCostLine(tip);
            if (!Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) && !Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
                if (!tip.contains(SHIFT_HINT)) tip.add(SHIFT_HINT);
            } else {
                tip.remove(SHIFT_HINT);
                int targetLevel = Math.min(rawLevel + 10, CoreOfTheMountainData.MAX_LEVEL);
                int levels      = targetLevel - rawLevel;
                String levelWord = levels == 1 ? "level" : "levels";
                java.util.Map<HotmPerkData.PowderType, Long> range = CoreOfTheMountainData.rangeCost(rawLevel, targetLevel);
                int insertAt = costIndex >= 0 ? costIndex + 2 : tip.size();
                for (HotmPerkData.PowderType type : HotmPerkData.PowderType.values()) {
                    long cost = range.getOrDefault(type, 0L);
                    if (cost == 0) continue;
                    String line = "§7" + type.displayName + " for " + levels + " " + levelWord + ": §e" + formatNumber(cost);
                    if (insertAt > tip.size()) insertAt = tip.size();
                    tip.add(insertAt++, line);
                }
            }
        }
    }
    */

    private static int parseRawLevel(List<String> tooltip) {
        for (String line : tooltip) {
            Matcher m = LEVEL_PATTERN.matcher(line);
            if (!m.find()) continue;
            try {
                int level = Integer.parseInt(m.group(2));
                boolean blueEgg = m.group(1).equals("b");
                return blueEgg ? Math.max(0, level - 1) : level;
            } catch (NumberFormatException ignored) {
            }
        }
        return -1;
    }

    private static int indexOfCostLine(List<String> tooltip) {
        for (int i = 0; i < tooltip.size(); i++) {
            if (COST_PATTERN.matcher(tooltip.get(i)).matches()) return i;
        }
        return -1;
    }

    private static String getContainerName() {
        return ContainerUtils.getContainerName();
    }

    private static String formatNumber(long n) {
        return String.format("%,d", n);
    }

    private static String fmt2(double d) {
        return String.format("%.2f", d);
    }

    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        if (ATHRConfig.feature == null) return;
        if (!ATHRConfig.feature.mining.hotmPowder.hotmPowderSpent && !ATHRConfig.feature.mining.hotmPowder.hotmPowderFor10Levels) return;
        if (event.toolTip == null || event.itemStack == null) return;
        if (!HOTM_TITLE.equals(getContainerName())) return;

        String displayName = ColorUtils.stripColor(event.itemStack.getDisplayName());

        // TODO: Uncomment when Core of the Mountain is updated
        /*
        if (CoreOfTheMountainData.GUI_NAME.equals(displayName)) {
            handleCotm(event.toolTip);
            return;
        }
        */

        HotmPerkData perk = HotmPerkData.findByGuiName(displayName);
        if (perk == null) return;

        int rawLevel = parseRawLevel(event.toolTip);
        if (rawLevel < 0) return;
        rawLevel = Math.min(rawLevel, perk.maxLevel);

        List<String> tip = event.toolTip;

        if (ATHRConfig.feature.mining.hotmPowder.hotmPowderSpent) {
            tip.add(2, buildSpentLine(perk, rawLevel));
        }

        if (ATHRConfig.feature.mining.hotmPowder.hotmPowderFor10Levels && rawLevel < perk.maxLevel) {
            int costIndex = indexOfCostLine(tip);

            if (!Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) && !Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
                if (!tip.contains(SHIFT_HINT)) tip.add(SHIFT_HINT);
            } else {
                tip.remove(SHIFT_HINT);

                int targetLevel = Math.min(rawLevel + 10, perk.maxLevel);
                int levels = targetLevel - rawLevel;
                long cost10 = perk.calculateTotalCost(targetLevel) - perk.calculateTotalCost(rawLevel);
                String levelWord = levels == 1 ? "level" : "levels";

                String line = "§7" + perk.powderType.displayName + " for " + levels + " " + levelWord + ": §e" + formatNumber(cost10);

                int insertAt = costIndex >= 0 ? costIndex + 2 : tip.size();
                if (insertAt > tip.size()) insertAt = tip.size();
                tip.add(insertAt, line);
            }
        }
    }
}