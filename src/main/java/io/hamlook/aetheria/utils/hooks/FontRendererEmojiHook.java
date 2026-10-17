package io.hamlook.aetheria.utils.hooks;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.chat.emoji.EmojiManager;
import io.hamlook.aetheria.utils.compat.DrawContextUtils;
import io.hamlook.aetheria.utils.render.RenderUtils;
import net.minecraft.client.gui.FontRenderer;
//#if MC < 1.21
//#else
//$$ import net.minecraft.client.font.TextRenderer;
//$$ import net.minecraft.text.OrderedText;
//$$ import net.minecraft.text.Style;
//#endif

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FontRendererEmojiHook {

    private static final Pattern EMOJI_PATTERN = Pattern.compile(":([a-zA-Z0-9_~+-]{1,}):");
    private static final ThreadLocal<Boolean> PROCESSING = ThreadLocal.withInitial(() -> false);

    /**
     * Processes emoji shortcodes in a text string, drawing emoji textures
     * in place of :shortcode: patterns.
     *
     * @return the final cursor X position (as int) if emojis were rendered, or -1 if not handled
     */
    public static int processEmojis(FontRenderer fr, String text, float x, float y, int color, boolean dropShadow) {
        if (PROCESSING.get()) return -1;
        if (ATHRConfig.feature == null || !ATHRConfig.feature.chat.emojiConfig.enabled) return -1;
        if (!EmojiManager.isLoaded() || text == null || !text.contains(":")) return -1;

        Matcher matcher = EMOJI_PATTERN.matcher(text);
        if (!matcher.find()) return -1;
        matcher.reset();

        PROCESSING.set(true);
        try {
            float cursorX = x;
            int lastEnd = 0;

            while (matcher.find()) {
                cursorX = drawPlain(fr, text.substring(lastEnd, matcher.start()), cursorX, y, color, dropShadow);

                String key = matcher.group(1);
                float size = fr.FONT_HEIGHT;
                if (EmojiManager.exists(key) && RenderUtils.drawEmoji(key, cursorX, y, size)) {
                    cursorX += size + 1;
                } else {
                    cursorX = drawPlain(fr, matcher.group(), cursorX, y, color, dropShadow);
                }
                lastEnd = matcher.end();
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
    //$$     if (!EmojiManager.isLoaded() || text == null || !text.contains(":")) return -1;
    //$$
    //$$     Matcher matcher = EMOJI_PATTERN.matcher(text);
    //$$     if (!matcher.find()) return -1;
    //$$     matcher.reset();
    //$$
    //$$     PROCESSING.set(true);
    //$$     try {
    //$$         float cursorX = x;
    //$$         int lastEnd = 0;
    //$$
    //$$         while (matcher.find()) {
    //$$             cursorX = drawPlainModern(fr, text.substring(lastEnd, matcher.start()), cursorX, y, color, dropShadow);
    //$$
    //$$             String key = matcher.group(1);
    //$$             float size = fr.fontHeight;
    //$$             if (EmojiManager.exists(key) && RenderUtils.drawEmoji(key, cursorX, y, size)) {
    //$$                 cursorX += size + 1;
    //$$             } else {
    //$$                 cursorX = drawPlainModern(fr, matcher.group(), cursorX, y, color, dropShadow);
    //$$             }
    //$$             lastEnd = matcher.end();
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
    //$$ private static boolean runsContainEmoji(List<StyledRun> runs) {
    //$$     for (StyledRun run : runs) {
    //$$         if (EMOJI_PATTERN.matcher(run.text).find()) return true;
    //$$     }
    //$$     return false;
    //$$ }
    //$$
    //$$ public static int processEmojisOrdered(TextRenderer fr, OrderedText text, float x, float y, int color, boolean shadow) {
    //$$     if (PROCESSING.get()) return -1;
    //$$     if (ATHRConfig.feature == null || !ATHRConfig.feature.chat.emojiConfig.enabled) return -1;
    //$$     if (!EmojiManager.isLoaded()) return -1;
    //$$
    //$$     List<StyledRun> runs = collectRuns(text);
    //$$     if (!runsContainEmoji(runs)) return -1;
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
    //$$     Matcher matcher = EMOJI_PATTERN.matcher(run.text);
    //$$     if (!matcher.find()) {
    //$$         return drawStyledSegment(fr, run.text, run.style, x, y, color, shadow);
    //$$     }
    //$$     matcher.reset();
    //$$     float cursorX = x;
    //$$     int lastEnd = 0;
    //$$     while (matcher.find()) {
    //$$         cursorX = drawStyledSegment(fr, run.text.substring(lastEnd, matcher.start()), run.style, cursorX, y, color, shadow);
    //$$         String key = matcher.group(1);
    //$$         float size = fr.fontHeight;
    //$$         if (EmojiManager.exists(key) && RenderUtils.drawEmoji(key, cursorX, y, size)) {
    //$$             cursorX += size + 1;
    //$$         } else {
    //$$             cursorX = drawStyledSegment(fr, matcher.group(), run.style, cursorX, y, color, shadow);
    //$$         }
    //$$         lastEnd = matcher.end();
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
    //$$
    //$$     List<StyledRun> runs = collectRuns(text);
    //$$     if (!runsContainEmoji(runs)) return -1;
    //$$
    //$$     boolean wasProcessing = PROCESSING.get();
    //$$     PROCESSING.set(true);
    //$$     try {
    //$$         float width = 0;
    //$$         for (StyledRun run : runs) {
    //$$             Matcher matcher = EMOJI_PATTERN.matcher(run.text);
    //$$             int lastEnd = 0;
    //$$             while (matcher.find()) {
    //$$                 width += segmentWidth(fr, run.text.substring(lastEnd, matcher.start()), run.style);
    //$$                 String key = matcher.group(1);
    //$$                 if (EmojiManager.exists(key)) {
    //$$                     width += fr.fontHeight + 1;
    //$$                 } else {
    //$$                     width += segmentWidth(fr, matcher.group(), run.style);
    //$$                 }
    //$$                 lastEnd = matcher.end();
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
