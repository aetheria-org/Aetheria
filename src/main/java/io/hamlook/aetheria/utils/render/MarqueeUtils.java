package io.hamlook.aetheria.utils.render;

import io.hamlook.aetheria.utils.chat.ChatUtils;
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
        String slice = ChatUtils.formatStateAt(text, start) + text.substring(start, end);
        DrawContextUtils.drawText(font, slice, x + ends[start] - offset, y, color, shadow);
    }

    public static void drawCentered(FontRenderer font, String text, int centerX, int y,
                                    int windowWidth, int color, boolean shadow) {
        if (text == null || text.isEmpty() || windowWidth <= 0) return;
        int textWidth = DrawContextUtils.getStringWidth(font, text);
        if (textWidth <= windowWidth) {
            DrawContextUtils.drawText(font, text, centerX - textWidth / 2, y, color, shadow);
            return;
        }
        draw(font, text, centerX - windowWidth / 2, y, windowWidth, color, shadow);
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

}
