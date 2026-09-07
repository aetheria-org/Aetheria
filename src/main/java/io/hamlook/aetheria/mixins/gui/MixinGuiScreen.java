package io.hamlook.aetheria.mixins.gui;

import io.hamlook.aetheria.utils.hooks.GuiScreenHook;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.gui.GuiScreen.class)
public class MixinGuiScreen {

    @Inject(method = "renderToolTip", at = @At("HEAD"), cancellable = true)
    public void ATHR$storageTooltip(ItemStack stack, int x, int y, CallbackInfo ci) {
        if (GuiScreenHook.shouldCancelStorageTooltip()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderToolTip", at = @At("HEAD"), cancellable = true)
    public void ATHR$visitorTooltip(ItemStack stack, int x, int y, CallbackInfo ci) {
        if (GuiScreenHook.visitorTooltip(stack, x, y)) {
            ci.cancel();
        }
    }
}
