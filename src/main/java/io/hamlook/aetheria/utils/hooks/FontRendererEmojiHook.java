package io.hamlook.aetheria.utils.hooks;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.chat.emoji.EmojiManager;
import io.hamlook.aetheria.utils.compat.DrawContextUtils;
import io.hamlook.aetheria.utils.render.RenderUtils;
import net.minecraft.client.gui.FontRenderer;

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
}
