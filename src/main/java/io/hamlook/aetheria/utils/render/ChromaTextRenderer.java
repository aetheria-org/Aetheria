package io.hamlook.aetheria.utils.render;

import io.hamlook.aetheria.core.moulconfig.editors.ChromaStyle;
import io.hamlook.aetheria.mixins.accessors.FontRendererAccessor;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import net.minecraft.client.gui.FontRenderer;

import java.util.ArrayDeque;
import java.util.Deque;

public class ChromaTextRenderer {

    private static final Deque<ChromaStyle> STYLE_STACK = new ArrayDeque<>();
    private static boolean chromaActive;
    private static boolean chromaOn;
    private static boolean renderingShadow;
    private static ChromaStyle defaultStyle = ChromaStyle.of("251:255:255:255:255");

    private ChromaTextRenderer() {
    }

    public static void setDefaultStyle(ChromaStyle style) {
        if (style != null) defaultStyle = style;
    }

    public static void pushStyle(ChromaStyle style) {
        if (style != null) STYLE_STACK.push(style);
    }

    public static void popStyle() {
        if (!STYLE_STACK.isEmpty()) STYLE_STACK.pop();
    }

    private static ChromaStyle currentStyle() {
        return STYLE_STACK.isEmpty() ? defaultStyle : STYLE_STACK.peek();
    }

    public static int drawString(FontRenderer fr, ChromaStyle style, String text, int x, int y, int color, boolean shadow) {
        pushStyle(style);
        try {
            return fr.drawString(text, x, y, color, shadow);
        } finally {
            popStyle();
        }
    }

    public static int drawStringWithShadow(FontRenderer fr, ChromaStyle style, String text, int x, int y, int color) {
        return drawString(fr, style, text, x, y, color, true);
    }

    public static void beginRenderString(String text, boolean shadow) {
        chromaOn = false;
        renderingShadow = shadow;
        chromaActive = text != null && (text.contains("§z") || text.contains("§Z"));
    }

    public static void onChromaCode() {
        if (chromaActive) chromaOn = true;
    }

    public static void onColorCode() {
        if (chromaActive) chromaOn = false;
    }

    public static void changeTextColor(FontRenderer fr, char ch) {
        if (!chromaActive || !chromaOn || fr == null) return;
        if (ch == 32) return;

        FontRendererAccessor accessor = (FontRendererAccessor) fr;
        int rgb = currentStyle().toArgb(accessor.ATHR$getPosX(), accessor.ATHR$getPosY());
        if (renderingShadow) {
            int a = (rgb >>> 24) & 255;
            int r = ((rgb >>> 16) & 255) / 4;
            int g = ((rgb >>> 8) & 255) / 4;
            int b = (rgb & 255) / 4;
            rgb = (a << 24) | (r << 16) | (g << 8) | b;
        }
        GlStateManagerCompat.color(((rgb >> 16) & 255) / 255F, ((rgb >> 8) & 255) / 255F, (rgb & 255) / 255F, ((rgb >> 24) & 255) / 255F);
    }

    public static void endRenderString() {
        chromaActive = false;
        chromaOn = false;
        renderingShadow = false;
        GlStateManagerCompat.color(1F, 1F, 1F, 1F);
    }
}
