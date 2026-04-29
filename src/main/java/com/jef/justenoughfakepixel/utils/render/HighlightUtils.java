package com.jef.justenoughfakepixel.utils.render;

import com.jef.justenoughfakepixel.core.JefConfig;
import com.jef.justenoughfakepixel.core.config.editors.ChromaColour;
import com.jef.justenoughfakepixel.features.misc.SearchBar;
import com.jef.justenoughfakepixel.utils.ColorUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiPredicate;

public class HighlightUtils {

    private static final List<SlotHighlighter> HIGHLIGHTERS = new ArrayList<>();

    static {
        registerHighlighter((gui, slot) -> {
            if (JefConfig.feature == null || !JefConfig.feature.misc.searchBarConfig.searchBar) return null;
            if (SearchBar.isCalcMode()) return null;
            String searchText = SearchBar.getSearchText();
            if (searchText == null || searchText.trim().isEmpty()) return null;
            ItemStack stack = slot.getStack();
            if (stack == null || stack.getItem() == null) return null;
            if (!matches(stack, searchText.trim().toLowerCase(Locale.ROOT))) return null;
            return ChromaColour.specialToChromaRGB(JefConfig.feature.misc.searchBarConfig.searchBarHighlightColor);
        });
    }

    public static void registerHighlighter(BiPredicate<GuiContainer, Slot> shouldHighlight, int color) {
        HIGHLIGHTERS.add(new SlotHighlighter(shouldHighlight, color));
    }

    public static void registerHighlighter(SlotHighlightFunction highlighter) {
        HIGHLIGHTERS.add(new SlotHighlighter(highlighter));
    }

    public static void renderAllHighlights(GuiContainer gui, Slot slot) {
        for (SlotHighlighter highlighter : HIGHLIGHTERS) {
            Integer color = highlighter.getColor(gui, slot);
            if (color != null) {
                renderSlotHighlight(slot.xDisplayPosition, slot.yDisplayPosition, color);
            }
        }
    }

    public static void renderSlotHighlight(int x, int y, int color) {
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.colorMask(true, true, true, false);

        Gui.drawRect(x, y, x + 16, y + 16, color);

        GlStateManager.colorMask(true, true, true, true);
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
    }

    public static void renderButtonHighlight(int x, int y) {
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.colorMask(true, true, true, false);
        Gui.drawRect(x, y, x + 18, y + 18, 0x80ffffff);
        GlStateManager.colorMask(true, true, true, true);
        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
    }

    private static boolean matches(ItemStack stack, String query) {
        String display = stack.getDisplayName();
        if (display != null && ColorUtils.stripColor(display).toLowerCase(Locale.ROOT).contains(query)) return true;

        List<String> tooltip = stack.getTooltip(Minecraft.getMinecraft().thePlayer, false);
        if (tooltip != null) for (String line : tooltip)
            if (line != null && ColorUtils.stripColor(line).toLowerCase(Locale.ROOT).contains(query)) return true;

        return false;
    }

    @FunctionalInterface
    public interface SlotHighlightFunction {
        Integer getColor(GuiContainer gui, Slot slot);
    }

    private static class SlotHighlighter {
        private final SlotHighlightFunction function;

        SlotHighlighter(SlotHighlightFunction function) {
            this.function = function;
        }

        SlotHighlighter(BiPredicate<GuiContainer, Slot> predicate, int color) {
            this.function = (gui, slot) -> predicate.test(gui, slot) ? color : null;
        }

        Integer getColor(GuiContainer gui, Slot slot) {
            return function.getColor(gui, slot);
        }
    }
}