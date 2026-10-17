package io.hamlook.aetheria.mixins.text;

import io.hamlook.aetheria.utils.render.ChromaTextRenderer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.TextColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TextRenderer.Drawer.class)
public abstract class MixinDrawer {

    @Accessor("x")
    abstract float athr$getX();

    @Accessor("y")
    abstract float athr$getY();

    @Redirect(
            method = "accept",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/text/TextColor;getRgb()I")
    )
    private int athr$chromaColor(TextColor color) {
        int rgb = color.getRgb();
        int tier = ChromaTextRenderer.tierOfSentinel(rgb);
        if (tier < 0) return rgb;
        int argb = ChromaTextRenderer.chromaArgbForTier(tier, athr$getX(), athr$getY());
        return argb != -1 ? argb & 0xFFFFFF : rgb;
    }
}
