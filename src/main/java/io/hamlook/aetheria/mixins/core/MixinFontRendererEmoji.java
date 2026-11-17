package io.hamlook.aetheria.mixins.core;

import io.hamlook.aetheria.utils.hooks.FontRendererEmojiHook;
import net.minecraft.client.gui.FontRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FontRenderer.class)
public class MixinFontRendererEmoji {

    @Inject(method = "drawStringWithShadow(Ljava/lang/String;FFI)I", at = @At("HEAD"), cancellable = true)
    private void ATHR$drawStringWithShadow(String text, float x, float y, int color, CallbackInfoReturnable<Integer> cir) {
        FontRenderer fr = (FontRenderer) (Object) this;
        int result = FontRendererEmojiHook.processEmojis(fr, text, x, y, color, true);
        if (result != -1) {
            cir.setReturnValue(result);
        }
    }

    @Inject(method = "drawString(Ljava/lang/String;III)I", at = @At("HEAD"), cancellable = true)
    private void ATHR$drawStringInt(String text, int x, int y, int color, CallbackInfoReturnable<Integer> cir) {
        FontRenderer fr = (FontRenderer) (Object) this;
        int result = FontRendererEmojiHook.processEmojis(fr, text, (float) x, (float) y, color, false);
        if (result != -1) {
            cir.setReturnValue(result);
        }
    }
}
