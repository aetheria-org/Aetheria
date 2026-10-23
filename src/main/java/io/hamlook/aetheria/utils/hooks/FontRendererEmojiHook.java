package io.hamlook.aetheria.utils.hooks;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.chat.emoji.EmojiManager;
import io.hamlook.aetheria.utils.compat.DrawContextUtils;
import io.hamlook.aetheria.utils.render.RenderUtils;
import net.minecraft.client.gui.FontRenderer;
//#if MC < 1.21
//#else
//$$ import net.minecraft.client.font.TextRenderer;
//$$ import net.minecraft.text.CharacterVisitor;
//$$ import net.minecraft.text.OrderedText;
//$$ import net.minecraft.text.Style;
//#endif

import java.util.ArrayList;
import java.util.List;

public class FontRendererEmojiHook {

    private static final ThreadLocal<Boolean> PROCESSING = ThreadLocal.withInitial(() -> false);

    // index of the colon opening the next :shortcode: at or after from, -1 if none
    static int findShortcode(String text, int from) {
        int length = text.length();
        for (int i = text.indexOf(':', from); i >= 0; i = text.indexOf(':', i + 1)) {
            int end = i + 1;
            while (end < length && isShortcodeChar(text.charAt(end))) end++;
            if (end > i + 1 && end < length && text.charAt(end) == ':') return i;
        }
        return -1;
    }

    // index of the colon closing the shortcode opened at start, -1 if none
    static int shortcodeEnd(String text, int start) {
        int length = text.length();
        int end = start + 1;
        while (end < length && isShortcodeChar(text.charAt(end))) end++;
        return end > start + 1 && end < length && text.charAt(end) == ':' ? end : -1;
    }

    private static boolean isShortcodeChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                || c == '_' || c == '~' || c == '+' || c == '-';
    }

    /**
     * Processes emoji shortcodes in a text string, drawing emoji textures
     * in place of :shortcode: patterns.
     *
     * @return the final cursor X position (as int) if emojis were rendered, or -1 if not handled
     */
    public static int processEmojis(FontRenderer fr, String text, float x, float y, int color, boolean dropShadow) {
        if (PROCESSING.get()) return -1;
        if (ATHRConfig.feature == null || !ATHRConfig.feature.chat.emojiConfig.enabled) return -1;
        if (!EmojiManager.isLoaded() || text == null) return -1;

        int cursor = findShortcode(text, 0);
        if (cursor < 0) return -1;

        PROCESSING.set(true);
        try {
            float cursorX = x;
            int lastEnd = 0;

            while (cursor >= 0) {
                int end = shortcodeEnd(text, cursor);
                cursorX = drawPlain(fr, text.substring(lastEnd, cursor), cursorX, y, color, dropShadow);

                String key = text.substring(cursor + 1, end);
                float size = fr.FONT_HEIGHT;
                if (EmojiManager.exists(key) && RenderUtils.drawEmoji(key, cursorX, y, size)) {
                    cursorX += size + 1;
                } else {
                    cursorX = drawPlain(fr, text.substring(cursor, end + 1), cursorX, y, color, dropShadow);
                }
                lastEnd = end + 1;
                cursor = findShortcode(text, lastEnd);
            }

            if (lastEnd < text.length()) {
                cursorX = drawPlain(fr, text.substring(lastEnd), cursorX, y, color, dropShadow);
            }
            return Math.round(cursorX);
        } finally {
            PROCESSING.set(false);
        }
    }

    private static float drawPlain(FontRenderer fr, String segment, float x, float y, int color, boolean dropShadow) {
        if (segment.isEmpty()) return x;
        if (dropShadow) {
            DrawContextUtils.drawTextWithShadow(fr, segment, (int) x, (int) y, color);
        } else {
            DrawContextUtils.drawText(fr, segment, (int) x, (int) y, color, false);
        }
        return x + DrawContextUtils.getStringWidth(fr, segment);
    }

    //#if MC < 1.21
    //#else
    //$$ public static int processEmojisModern(TextRenderer fr, String text, float x, float y, int color, boolean dropShadow) {
    //$$     if (PROCESSING.get()) return -1;
    //$$     if (ATHRConfig.feature == null || !ATHRConfig.feature.chat.emojiConfig.enabled) return -1;
    //$$     if (!EmojiManager.isLoaded() || text == null) return -1;
    //$$
    //$$     int cursor = findShortcode(text, 0);
    //$$     if (cursor < 0) return -1;
    //$$
    //$$     PROCESSING.set(true);
    //$$     try {
    //$$         float cursorX = x;
    //$$         int lastEnd = 0;
    //$$
    //$$         while (cursor >= 0) {
    //$$             int end = shortcodeEnd(text, cursor);
    //$$             cursorX = drawPlainModern(fr, text.substring(lastEnd, cursor), cursorX, y, color, dropShadow);
    //$$
    //$$             String key = text.substring(cursor + 1, end);
    //$$             float size = fr.fontHeight;
    //$$             if (EmojiManager.exists(key) && RenderUtils.drawEmoji(key, cursorX, y, size)) {
    //$$                 cursorX += size + 1;
    //$$             } else {
    //$$                 cursorX = drawPlainModern(fr, text.substring(cursor, end + 1), cursorX, y, color, dropShadow);
    //$$             }
    //$$             lastEnd = end + 1;
    //$$             cursor = findShortcode(text, lastEnd);
    //$$         }
    //$$
    //$$         if (lastEnd < text.length()) {
    //$$             cursorX = drawPlainModern(fr, text.substring(lastEnd), cursorX, y, color, dropShadow);
    //$$         }
    //$$         return Math.round(cursorX);
    //$$     } finally {
    //$$         PROCESSING.set(false);
    //$$     }
    //$$ }
    //$$
    //$$ private static float drawPlainModern(TextRenderer fr, String segment, float x, float y, int color, boolean dropShadow) {
    //$$     if (segment.isEmpty()) return x;
    //$$     if (dropShadow) {
    //$$         DrawContextUtils.drawTextWithShadow(fr, segment, (int) x, (int) y, color);
    //$$     } else {
    //$$         DrawContextUtils.drawText(fr, segment, (int) x, (int) y, color, false);
    //$$     }
    //$$     return x + DrawContextUtils.getStringWidth(fr, segment);
    //$$ }
    //$$
    //$$ private static final class StyledRun {
    //$$     final String text;
    //$$     final Style style;
    //$$     StyledRun(String text, Style style) {
    //$$         this.text = text;
    //$$         this.style = style;
    //$$     }
    //$$ }
    //$$
    //$$ private static final ThreadLocal<Boolean> COLON_FOUND = ThreadLocal.withInitial(() -> Boolean.FALSE);
    //$$
    //$$ private static final CharacterVisitor COLON_SCANNER = (index, style, codePoint) -> {
    //$$     if (codePoint == ':') {
    //$$         COLON_FOUND.set(Boolean.TRUE);
    //$$         return false;
    //$$     }
    //$$     return true;
    //$$ };
    //$$
    //$$ private static boolean containsColon(OrderedText text) {
    //$$     COLON_FOUND.set(Boolean.FALSE);
    //$$     text.accept(COLON_SCANNER);
    //$$     return COLON_FOUND.get();
    //$$ }
    //$$
    //$$ private static List<StyledRun> collectRuns(OrderedText text) {
    //$$     List<StyledRun> runs = new ArrayList<>();
    //$$     StringBuilder current = new StringBuilder();
    //$$     Style[] lastStyle = new Style[1];
    //$$     text.accept((index, style, codePoint) -> {
    //$$         if (lastStyle[0] != null && !style.equals(lastStyle[0]) && current.length() > 0) {
    //$$             runs.add(new StyledRun(current.toString(), lastStyle[0]));
    //$$             current.setLength(0);
    //$$         }
    //$$         lastStyle[0] = style;
    //$$         current.appendCodePoint(codePoint);
    //$$         return true;
    //$$     });
    //$$     if (current.length() > 0) runs.add(new StyledRun(current.toString(), lastStyle[0]));
    //$$     return runs;
    //$$ }
    //$$
    //$$ private static boolean runsContainShortcode(List<StyledRun> runs) {
    //$$     for (StyledRun run : runs) {
    //$$         if (findShortcode(run.text, 0) >= 0) return true;
    //$$     }
    //$$     return false;
    //$$ }
    //$$
    //$$ public static int processEmojisOrdered(TextRenderer fr, OrderedText text, float x, float y, int color, boolean shadow) {
    //$$     if (PROCESSING.get()) return -1;
    //$$     if (ATHRConfig.feature == null || !ATHRConfig.feature.chat.emojiConfig.enabled) return -1;
    //$$     if (!EmojiManager.isLoaded()) return -1;
    //$$     if (!containsColon(text)) return -1;
    //$$
    //$$     List<StyledRun> runs = collectRuns(text);
    //$$     if (!runsContainShortcode(runs)) return -1;
    //$$
    //$$     PROCESSING.set(true);
    //$$     try {
    //$$         float cursorX = x;
    //$$         for (StyledRun run : runs) {
    //$$             cursorX = drawRunWithEmojis(fr, run, cursorX, y, color, shadow);
    //$$         }
    //$$         return Math.round(cursorX);
    //$$     } finally {
    //$$         PROCESSING.set(false);
    //$$     }
    //$$ }
    //$$
    //$$ private static float drawRunWithEmojis(TextRenderer fr, StyledRun run, float x, float y, int color, boolean shadow) {
    //$$     int cursor = findShortcode(run.text, 0);
    //$$     if (cursor < 0) {
    //$$         return drawStyledSegment(fr, run.text, run.style, x, y, color, shadow);
    //$$     }
    //$$     float cursorX = x;
    //$$     int lastEnd = 0;
    //$$     while (cursor >= 0) {
    //$$         int end = shortcodeEnd(run.text, cursor);
    //$$         cursorX = drawStyledSegment(fr, run.text.substring(lastEnd, cursor), run.style, cursorX, y, color, shadow);
    //$$         String key = run.text.substring(cursor + 1, end);
    //$$         float size = fr.fontHeight;
    //$$         if (EmojiManager.exists(key) && RenderUtils.drawEmoji(key, cursorX, y, size)) {
    //$$             cursorX += size + 1;
    //$$         } else {
    //$$             cursorX = drawStyledSegment(fr, run.text.substring(cursor, end + 1), run.style, cursorX, y, color, shadow);
    //$$         }
    //$$         lastEnd = end + 1;
    //$$         cursor = findShortcode(run.text, lastEnd);
    //$$     }
    //$$     if (lastEnd < run.text.length()) {
    //$$         cursorX = drawStyledSegment(fr, run.text.substring(lastEnd), run.style, cursorX, y, color, shadow);
    //$$     }
    //$$     return cursorX;
    //$$ }
    //$$
    //$$ private static float drawStyledSegment(TextRenderer fr, String segment, Style style, float x, float y, int color, boolean shadow) {
    //$$     if (segment.isEmpty()) return x;
    //$$     OrderedText ordered = OrderedText.styledForwardsVisitedString(segment, style);
    //$$     DrawContextUtils.drawOrderedText(fr, ordered, (int) x, (int) y, color, shadow);
    //$$     return x + fr.getWidth(ordered);
    //$$ }
    //$$
    //$$ public static int emojiWidth(TextRenderer fr, OrderedText text) {
    //$$     if (PROCESSING.get()) return -1;
    //$$     if (ATHRConfig.feature == null || !ATHRConfig.feature.chat.emojiConfig.enabled) return -1;
    //$$     if (!EmojiManager.isLoaded()) return -1;
    //$$     if (!containsColon(text)) return -1;
    //$$
    //$$     List<StyledRun> runs = collectRuns(text);
    //$$     if (!runsContainShortcode(runs)) return -1;
    //$$
    //$$     boolean wasProcessing = PROCESSING.get();
    //$$     PROCESSING.set(true);
    //$$     try {
    //$$         int width = 0;
    //$$         for (StyledRun run : runs) {
    //$$             int cursor = findShortcode(run.text, 0);
    //$$             int lastEnd = 0;
    //$$             while (cursor >= 0) {
    //$$                 int end = shortcodeEnd(run.text, cursor);
    //$$                 width += segmentWidth(fr, run.text.substring(lastEnd, cursor), run.style);
    //$$                 String key = run.text.substring(cursor + 1, end);
    //$$                 if (EmojiManager.exists(key)) {
    //$$                     width += fr.fontHeight + 1;
    //$$                 } else {
    //$$                     width += segmentWidth(fr, run.text.substring(cursor, end + 1), run.style);
    //$$                 }
    //$$                 lastEnd = end + 1;
    //$$                 cursor = findShortcode(run.text, lastEnd);
    //$$             }
    //$$             if (lastEnd < run.text.length()) {
    //$$                 width += segmentWidth(fr, run.text.substring(lastEnd), run.style);
    //$$             }
    //$$         }
    //$$         return Math.round(width);
    //$$     } finally {
    //$$         PROCESSING.set(wasProcessing);
    //$$     }
    //$$ }
    //$$
    //$$ private static float segmentWidth(TextRenderer fr, String segment, Style style) {
    //$$     if (segment.isEmpty()) return 0;
    //$$     return fr.getWidth(OrderedText.styledForwardsVisitedString(segment, style));
    //$$ }
    //#endif
}
