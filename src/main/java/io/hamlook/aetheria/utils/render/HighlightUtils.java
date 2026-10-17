package io.hamlook.aetheria.utils.render;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import io.hamlook.aetheria.features.misc.SearchBar;
import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.compat.DrawContextUtils;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.ItemCompatKt;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.TextCompat;
import net.minecraft.client.gui.inventory.GuiContainer;
//#if MC < 1.21
//#else
//$$ import net.minecraft.client.gui.screen.ingame.HandledScreen;
//#endif
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiPredicate;

public class HighlightUtils {

    private static final List<SlotHighlighter> HIGHLIGHTERS = new ArrayList<>();

    static {
        registerHighlighter((gui, slot) -> {
            if (ATHRConfig.feature == null || !ATHRConfig.feature.misc.searchBarConfig.searchBar) return null;
            if (SearchBar.isCalcMode()) return null;
            String searchText = SearchBar.getSearchText();
            if (searchText == null || searchText.trim().isEmpty()) return null;
            ItemStack stack = slot.getStack();
            if (stack == null || stack.getItem() == null) return null;
            if (!matches(stack, searchText.trim().toLowerCase(Locale.ROOT))) return null;
            return ChromaColour.specialToChromaRGB(ATHRConfig.feature.misc.searchBarConfig.searchBarHighlightColor);
        });
    }

    //#if MC < 1.21
    public static void registerHighlighter(BiPredicate<GuiContainer, Slot> shouldHighlight, int color) {
        HIGHLIGHTERS.add(new SlotHighlighter(shouldHighlight, color));
    }
    //#else
    //$$ public static void registerHighlighter(BiPredicate<HandledScreen<?>, Slot> shouldHighlight, int color) {
    //$$     HIGHLIGHTERS.add(new SlotHighlighter(shouldHighlight, color));
    //$$ }
    //#endif

    public static void registerHighlighter(SlotHighlightFunction highlighter) {
        HIGHLIGHTERS.add(new SlotHighlighter(highlighter));
    }

    //#if MC < 1.21
    public static void renderAllHighlights(GuiContainer gui, Slot slot) {
        for (SlotHighlighter highlighter : HIGHLIGHTERS) {
            Integer color = highlighter.getColor(gui, slot);
            if (color != null) {
                renderSlotHighlight(slot.xDisplayPosition, slot.yDisplayPosition, color);
            }
        }
    }
    //#else
    //$$ public static void renderAllHandledHighlights(HandledScreen<?> gui, Slot slot) {
    //$$     for (SlotHighlighter highlighter : HIGHLIGHTERS) {
    //$$         Integer color = highlighter.getColor(gui, slot);
    //$$         if (color != null) {
    //$$             renderSlotHighlight(slot.x, slot.y, color);
    //$$         }
    //$$     }
    //$$ }
    //#endif

    public static void renderSlotHighlight(int x, int y, int color) {
        GlStateManagerCompat.disableLighting();
        GlStateManagerCompat.disableDepth();
        GlStateManagerCompat.disableTexture2D();
        GlStateManagerCompat.enableBlend();
        GlStateManagerCompat.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManagerCompat.colorMask(true, true, true, false);

        DrawContextUtils.fill(x, y, x + 16, y + 16, color);

        GlStateManagerCompat.colorMask(true, true, true, true);
        GlStateManagerCompat.disableBlend();
        GlStateManagerCompat.enableTexture2D();
        GlStateManagerCompat.enableDepth();
        GlStateManagerCompat.enableLighting();
    }

    public static void renderButtonHighlight(int x, int y) {
        boolean depthWasOn = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean lightingWasOn = GL11.glIsEnabled(GL11.GL_LIGHTING);

        GlStateManagerCompat.disableLighting();
        GlStateManagerCompat.disableDepth();
        GlStateManagerCompat.colorMask(true, true, true, false);
        DrawContextUtils.fill(x, y, x + 18, y + 18, 0x80ffffff);
        GlStateManagerCompat.colorMask(true, true, true, true);

        if (depthWasOn) GlStateManagerCompat.enableDepth();
        else GlStateManagerCompat.disableDepth();
        if (lightingWasOn) GlStateManagerCompat.enableLighting();
        else GlStateManagerCompat.disableLighting();
    }

    private static boolean matches(ItemStack stack, String query) {
        String display = TextCompat.getUnformattedText(stack.getDisplayName());
        if (display != null && ColorUtils.stripColor(display).toLowerCase(Locale.ROOT).contains(query)) return true;

        List<String> tooltip = ItemCompatKt.getTooltipCompat(stack, MinecraftCompat.getLocalPlayer(), false);
        if (tooltip != null) for (String line : tooltip)
            if (line != null && ColorUtils.stripColor(line).toLowerCase(Locale.ROOT).contains(query)) return true;

        return false;
    }

    @FunctionalInterface
    public interface SlotHighlightFunction {
        //#if MC < 1.21
        Integer getColor(GuiContainer gui, Slot slot);
        //#else
        //$$ Integer getColor(HandledScreen<?> gui, Slot slot);
        //#endif
    }

    private static class SlotHighlighter {
        private final SlotHighlightFunction function;

        SlotHighlighter(SlotHighlightFunction function) {
            this.function = function;
        }

        //#if MC < 1.21
        SlotHighlighter(BiPredicate<GuiContainer, Slot> predicate, int color) {
            this.function = (gui, slot) -> predicate.test(gui, slot) ? color : null;
        }

        Integer getColor(GuiContainer gui, Slot slot) {
            return function.getColor(gui, slot);
        }
        //#else
        //$$ SlotHighlighter(BiPredicate<HandledScreen<?>, Slot> predicate, int color) {
        //$$     this.function = (gui, slot) -> predicate.test(gui, slot) ? color : null;
        //$$ }
        //$$
        //$$ Integer getColor(HandledScreen<?> gui, Slot slot) {
        //$$     return function.getColor(gui, slot);
        //$$ }
        //#endif
    }
}