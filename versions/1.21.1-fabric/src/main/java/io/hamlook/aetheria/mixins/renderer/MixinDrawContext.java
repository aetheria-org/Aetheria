package io.hamlook.aetheria.mixins.renderer;

import io.hamlook.aetheria.events.RenderItemOverlayEvent;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DrawContext.class)
public class MixinDrawContext {

    @Inject(method = "drawItemInSlot(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V", at = @At("TAIL"))
    private void athr$onItemOverlay(TextRenderer fr, ItemStack stack, int x, int y, String text, CallbackInfo ci) {
        if (stack != null)
            new RenderItemOverlayEvent((DrawContext) (Object) this, stack, x, y).post();
    }
}
