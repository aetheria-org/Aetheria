package io.hamlook.aetheria.mixins.gui;

import io.hamlook.aetheria.features.storage.StorageManager;
import io.hamlook.aetheria.features.farming.visitors.VisitorTooltips;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(GuiScreen.class)
public class MixinGuiScreen {

    @Inject(method = "renderToolTip", at = @At("HEAD"), cancellable = true)
    public void ATHR$storageTooltip(ItemStack stack, int x, int y, CallbackInfo ci) {
        if (StorageManager.isOverlayActive() && StorageManager.isStorageChest()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderToolTip", at = @At("HEAD"), cancellable = true)
    public void ATHR$visitorTooltip(ItemStack stack, int x, int y, CallbackInfo ci) {
        List<String> lines = VisitorTooltips.replaceToolTip(stack);
        if (lines == null) return;
        ci.cancel();
        TextRenderUtils.drawHoveringText(lines, x, y, Minecraft.getMinecraft().fontRendererObj);
    }
}
