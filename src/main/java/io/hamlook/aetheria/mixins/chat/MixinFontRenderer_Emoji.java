package io.hamlook.aetheria.mixins.chat;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.chat.emoji.EmojiManager;
import io.hamlook.aetheria.utils.render.RenderUtils;
import net.minecraft.client.gui.FontRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Swaps ":name:" chat tokens (Discord/Slack-style shortcodes) for the matching
 * emoji texture wherever text is drawn through FontRenderer - chat log, tab
 * list, signs, etc. This is a purely local render swap: nothing is sent over
 * the network, so a player without the mod (or with this feature off) just
 * sees the literal ":name:" text like normal.
 */
@Mixin(FontRenderer.class)
public abstract class MixinFontRenderer_Emoji {

    @Unique
    private static final Pattern EMOJI_PATTERN = Pattern.compile(":([a-zA-Z0-9_~+-]{2,}):");

    @Unique
    private static final ThreadLocal<Boolean> ATHR$processing = ThreadLocal.withInitial(() -> false);

    @Unique
    private boolean ATHR$processEmojis(String text, float x, float y, int color, boolean dropShadow, CallbackInfoReturnable<Integer> cir) {
        if (ATHR$processing.get()) return false;
        if (ATHRConfig.feature == null || !ATHRConfig.feature.chat.emojiConfig.enabled) return false;
        if (!EmojiManager.isLoaded() || text == null || !text.contains(":")) return false;
        Matcher matcher = EMOJI_PATTERN.matcher(text);
        if (!matcher.find()) return false;
        matcher.reset();

        ATHR$processing.set(true);
        try {
            FontRenderer fr = (FontRenderer) (Object) this;
            float cursorX = x;
            int lastEnd = 0;

            while (matcher.find()) {
                cursorX = ATHR$drawPlain(fr, text.substring(lastEnd, matcher.start()), cursorX, y, color, dropShadow);

                String key = matcher.group(1);
                float size = fr.FONT_HEIGHT;

                if (EmojiManager.exists(key) && RenderUtils.drawEmoji(key, cursorX, y, size)) {
                    cursorX += size + 1;
                } else {
                    cursorX = ATHR$drawPlain(fr, matcher.group(), cursorX, y, color, dropShadow);
                }

                lastEnd = matcher.end();
            }

            if (lastEnd < text.length()) {
                cursorX = ATHR$drawPlain(fr, text.substring(lastEnd), cursorX, y, color, dropShadow);
            }

            cir.setReturnValue(Math.round(cursorX));
            cir.cancel();
            return true;
        } finally {
            ATHR$processing.set(false);
        }
    }

    @Inject(method = "drawStringWithShadow(Ljava/lang/String;FFI)I", at = @At("HEAD"), cancellable = true)
    private void ATHR$drawStringWithShadow(String text, float x, float y, int color, CallbackInfoReturnable<Integer> cir) {
        if (ATHR$processEmojis(text, x, y, color, true, cir)) return;
    }

    @Inject(method = "drawString(Ljava/lang/String;III)I", at = @At("HEAD"), cancellable = true)
    private void ATHR$drawStringInt(String text, int x, int y, int color, CallbackInfoReturnable<Integer> cir) {
        if (ATHR$processEmojis(text, (float) x, (float) y, color, false, cir)) return;
    }

    @Unique
    private float ATHR$drawPlain(FontRenderer fr, String segment, float x, float y, int color, boolean dropShadow) {
        if (segment.isEmpty()) return x;
        if (dropShadow) {
            fr.drawStringWithShadow(segment, x, y, color);
        } else {
            fr.drawString(segment, (int) x, (int) y, color);
        }
        return x + fr.getStringWidth(segment);
    }
}
