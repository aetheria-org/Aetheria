package io.hamlook.aetheria.mixins.gui;

import io.hamlook.aetheria.utils.hooks.GuiIngameMenuHook;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.gui.GuiIngameMenu.class)
public abstract class MixinGuiIngameMenu extends GuiScreen {

    @Inject(method = "initGui", at = @At("TAIL"))
    private void ATHR$addButton(CallbackInfo ci) {
        GuiIngameMenuHook.addButton(this.buttonList, this.width, this.height);
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true)
    private void ATHR$actionPerformed(GuiButton button, CallbackInfo ci) {
        if (button.id == GuiIngameMenuHook.BTN_ATHR) {
            GuiIngameMenuHook.actionPerformed(button);
            ci.cancel();
        }
    }
}
