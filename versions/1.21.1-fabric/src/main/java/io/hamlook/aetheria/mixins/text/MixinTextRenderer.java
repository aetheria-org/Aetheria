package io.hamlook.aetheria.mixins.text;

import io.hamlook.aetheria.utils.hooks.FontRendererEmojiHook;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.OrderedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TextRenderer.class)
public class MixinTextRenderer {

    @Inject(method = "getWidth(Lnet/minecraft/text/OrderedText;)I", at = @At("HEAD"), cancellable = true)
    private void athr$emojiWidth(OrderedText text, CallbackInfoReturnable<Integer> cir) {
        int width = FontRendererEmojiHook.emojiWidth((TextRenderer) (Object) this, text);
        if (width != -1) cir.setReturnValue(width);
    }
}
