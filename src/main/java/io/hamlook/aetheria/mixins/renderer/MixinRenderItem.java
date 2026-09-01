package io.hamlook.aetheria.mixins.renderer;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.events.RenderItemOverlayEvent;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderItem.class)
public class MixinRenderItem {

    @Inject(method = "renderEffect", at = @At("HEAD"), cancellable = true)
    private void ATHR$disableEnchantGlint(CallbackInfo ci) {
        if (ATHRConfig.feature != null && ATHRConfig.feature.qol.disableEnchantGlint)
            ci.cancel();
    }

    @Inject(method = "renderItemOverlayIntoGUI", at = @At("TAIL"))
    private void ATHR$onItemOverlay(FontRenderer fr, ItemStack stack, int x, int y, String text, CallbackInfo ci) {
        if (stack != null)
            new RenderItemOverlayEvent(stack, x, y).post();
    }
}
