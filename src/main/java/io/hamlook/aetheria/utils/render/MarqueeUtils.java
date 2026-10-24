package io.hamlook.aetheria.utils.render;

import io.hamlook.aetheria.utils.compat.DrawContextUtils;
import net.minecraft.client.gui.FontRenderer;

/**
 * Scrolls text inside a fixed-width window. The offset is derived from wall-clock time,
 * so no per-call state is kept, and only whole glyphs are drawn, so nothing renders
 * outside the window.
 */
public final class MarqueeUtils {

    private static final long HOLD_MS = 1000L;
    private static final float SPEED_PX_PER_SEC = 30f;

    private MarqueeUtils() {}

    public static void draw(FontRenderer font, String text, int x, int y, int windowWidth, int color, boolean shadow) {
        if (text == null || text.isEmpty() || windowWidth <= 0) return;
        int textWidth = DrawContextUtils.getStringWidth(font, text);
        if (textWidth <= windowWidth) {
            DrawContextUtils.drawText(font, text, x, y, color, shadow);
            return;
        }
        int offset = offsetFor(textWidth, windowWidth);
        int[] ends = prefixWidths(font, text);
        int start = firstVisible(text, ends, offset);
        int end = lastVisible(text, ends, offset + windowWidth, start);
        if (end <= start) return;
        String slice = activeCodes(text, start) + text.substring(start, end);
        DrawContextUtils.drawText(font, slice, x + ends[start] - offset, y, color, shadow);
    }

    private static int offsetFor(int textWidth, int windowWidth) {
        int travel = textWidth - windowWidth;
        if (travel <= 0) return 0;
        return Math.round(travel * progressFor(textWidth));
    }

    private static float progressFor(int textWidth) {
        long travelMs = Math.max(1L, (long) (textWidth * 1000f / SPEED_PX_PER_SEC));
        long period = (HOLD_MS + travelMs) * 2L;
        long t = System.currentTimeMillis() % period;
        if (t < HOLD_MS) return 0f;
        t -= HOLD_MS;
        if (t < travelMs) return (float) t / travelMs;
        t -= travelMs;
        if (t < HOLD_MS) return 1f;
        t -= HOLD_MS;
        return 1f - (float) t / travelMs;
    }

    private static int[] prefixWidths(FontRenderer font, String text) {
        int[] ends = new int[text.length() + 1];
        for (int i = 1; i <= text.length(); i++) {
            ends[i] = DrawContextUtils.getStringWidth(font, text.substring(0, i));
        }
        return ends;
    }

    private static int firstVisible(String text, int[] ends, int offset) {
        int i = 0;
        while (i < text.length()) {
            if (text.charAt(i) == '§') {
                i += 2;
                continue;
            }
            if (ends[i] >= offset) break;
            i++;
        }
        return Math.min(i, text.length());
    }

    private static int lastVisible(String text, int[] ends, int limit, int start) {
        int i = start;
        while (i < text.length()) {
            int next = text.charAt(i) == '§' ? i + 2 : i + 1;
            if (next > text.length()) break;
            if (ends[next] > limit) break;
            i = next;
        }
        return i;
    }

    private static String activeCodes(String text, int index) {
        char color = 0;
        boolean obfuscated = false;
        boolean bold = false;
        boolean strike = false;
        boolean underline = false;
        boolean italic = false;
        for (int i = 0; i + 1 < index; i++) {
            if (text.charAt(i) != '§') continue;
            char code = Character.toLowerCase(text.charAt(i + 1));
            switch (code) {
                case 'k': obfuscated = true; break;
                case 'l': bold = true; break;
                case 'm': strike = true; break;
                case 'n': underline = true; break;
                case 'o': italic = true; break;
                case 'r':
                    color = 0;
                    obfuscated = bold = strike = underline = italic = false;
                    break;
                default:
                    color = code;
                    obfuscated = bold = strike = underline = italic = false;
                    break;
            }
        }
        StringBuilder out = new StringBuilder();
        if (color != 0) out.append('§').append(color);
        if (obfuscated) out.append("§k");
        if (bold) out.append("§l");
        if (strike) out.append("§m");
        if (underline) out.append("§n");
        if (italic) out.append("§o");
        return out.toString();
    }
}
