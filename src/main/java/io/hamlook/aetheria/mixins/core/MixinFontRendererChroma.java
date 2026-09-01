package io.hamlook.aetheria.mixins.core;

import io.hamlook.aetheria.utils.render.ChromaTextRenderer;
import net.minecraft.client.gui.FontRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FontRenderer.class)
public class MixinFontRendererChroma {

    @Redirect(method = "renderStringAtPos", at = @At(value = "INVOKE", target = "Ljava/lang/String;indexOf(I)I", ordinal = 0))
    private int ATHR$interceptFormatCodeRender(String formatCodes, int c) {
        int idx = formatCodes.indexOf(c);
        if (idx == -1 && (c == 'z' || c == 'Z')) {
            ChromaTextRenderer.onChromaCode();
            return 22;
        }
        if (idx < 16 || idx == 21) ChromaTextRenderer.onColorCode();
        return idx;
    }

    @Inject(method = "renderStringAtPos", at = @At("HEAD"))
    private void ATHR$beginRenderString(String text, boolean shadow, CallbackInfo ci) {
        ChromaTextRenderer.beginRenderString(text, shadow);
    }

    @Inject(method = "renderChar", at = @At("HEAD"))
    private void ATHR$changeTextColor(char ch, boolean italic, CallbackInfoReturnable<Float> cir) {
        ChromaTextRenderer.changeTextColor((FontRenderer) (Object) this, ch);
    }

    @Inject(method = "renderStringAtPos", at = @At("RETURN"))
    private void ATHR$endRenderString(String text, boolean shadow, CallbackInfo ci) {
        ChromaTextRenderer.endRenderString();
    }
}
