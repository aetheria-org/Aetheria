package io.hamlook.aetheria.mixins.gui;

import io.hamlook.aetheria.utils.hooks.GuiIngameMenuHook;
//#if MC < 1.21
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiScreen;
//#else
//$$ import net.minecraft.client.gui.screen.GameMenuScreen;
//$$ import net.minecraft.client.gui.screen.Screen;
//#endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiIngameMenu.class)
public abstract class MixinGuiIngameMenu extends GuiScreen {

    //#if MC < 1.21
    @Inject(method = "initGui", at = @At("TAIL"))
    private void ATHR$addButton(CallbackInfo ci) {
        GuiIngameMenuHook.addButton(this.buttonList, this.width, this.height);
    }
    //#else
    //$$ @Inject(method = "init", at = @At("TAIL"))
    //$$ private void ATHR$addButton(CallbackInfo ci) {
    //$$     GuiIngameMenuHook.addButton((Screen)(Object)this, this.width, this.height);
    //$$ }
    //#endif

    //#if MC < 1.21
    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true)
    private void ATHR$actionPerformed(GuiButton button, CallbackInfo ci) {
        if (button.id == GuiIngameMenuHook.BTN_ATHR) {
            GuiIngameMenuHook.actionPerformed(button);
            ci.cancel();
        }
    }
    //#endif
}
