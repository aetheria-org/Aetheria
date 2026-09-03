package io.hamlook.aetheria.features.misc.itemList.recipe;

import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.features.misc.itemList.ItemRegistry;
import io.hamlook.aetheria.features.misc.itemList.SkyblockItem;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.render.NineSliceUtils;
import net.minecraft.client.gui.FontRenderer;

import java.util.ArrayList;
import java.util.List;

public final class RecipeUtils {

    private RecipeUtils() {}

    public static void drawSlot(int x, int y, int size) {
        NineSliceUtils.draw(Resources.storageSlot(1), x, y, size, size, 6, 18);
    }

    public static void drawAmount(FontRenderer fr, String amount, int sx, int sy) {
        if (amount == null || amount.equals("1") || amount.isEmpty()) return;

        GlStateManagerCompat.pushMatrix();
        GlStateManagerCompat.translate(0, 0, 300); // Render above the item stack
        int strW = fr.getStringWidth(amount);
        fr.drawStringWithShadow(amount, sx + 17 - strW, sy + 9, 0xFFFFFF);
        GlStateManagerCompat.popMatrix();
    }

    public static List<String> buildItemTooltip(SkyblockItem item) {
        List<String> tip = new ArrayList<>();
        if (item == null) return tip;
        tip.add(item.displayName);
        if (item.baseLore != null) tip.addAll(item.baseLore);
        return tip;
    }

    public static List<String> buildItemTooltipWithAmount(SkyblockItem item, String amt) {
        List<String> tip = new ArrayList<>();
        if (item == null) return tip;
        tip.add(item.displayName + " §8x" + amt);
        if (item.baseLore != null) tip.addAll(item.baseLore);
        return tip;
    }

    public static float parseChance(String c) {
        if (c == null) return 0f;
        if (c.startsWith("x")) return 100f;
        try {
            return Float.parseFloat(c.replace("%", ""));
        } catch (Exception e) {
            return 0f;
        }
    }

    public static String formatChance(String raw) {
        if (raw == null) return "100%";
        if (raw.startsWith("x")) {
            return raw.substring(1).replace("-", "% - ") + "%";
        }
        return raw;
    }

    public static String getChanceColor(String chanceStr) {
        float chance = 100f;
        try {
            String p = chanceStr;
            if (p.contains("-")) p = p.split("-")[0];
            p = p.replaceAll("[^0-9.]", "");
            if (!p.isEmpty()) chance = Float.parseFloat(p);
        } catch (Exception ignored) {}

        if (chance >= 20f)   return "§a"; // Green   – Common
        if (chance >= 5f)    return "§9"; // Blue    – Uncommon
        if (chance >= 1f)    return "§5"; // Purple  – Rare
        if (chance >= 0.1f)  return "§d"; // Pink    – Epic
        if (chance >= 0.01f) return "§6"; // Gold    – Legendary
        if (chance >= 0.001f) return "§c"; // Red    – Mythic
        return "§4";                        // Dark   – RNGesus
    }

    public static SkyblockItem resolve(String id) {
        return ItemRegistry.getItem(id);
    }
}