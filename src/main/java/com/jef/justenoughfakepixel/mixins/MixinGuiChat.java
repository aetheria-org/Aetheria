package com.jef.justenoughfakepixel.mixins;

import com.jef.justenoughfakepixel.core.JefConfig;
import com.jef.justenoughfakepixel.features.qol.ChatStateManager;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiChat.class)
public class MixinGuiChat {

    @Shadow
    protected GuiTextField inputField;

    @Inject(method = "keyTyped", at = @At("RETURN"))
    protected void jef$onKeyTyped(char typedChar, int keyCode, CallbackInfo ci) {
        if (JefConfig.feature == null || !JefConfig.feature.qol.chatStateRestore) return;

        if (keyCode != Keyboard.KEY_ESCAPE && keyCode != Keyboard.KEY_RETURN) {
            ChatStateManager.getInstance().updateState(inputField.getText());
        } else {
            ChatStateManager.getInstance().resetState();
        }
    }

    @Inject(method = "initGui", at = @At("RETURN"))
    public void jef$onInitGui(CallbackInfo ci) {
        if (JefConfig.feature == null || !JefConfig.feature.qol.chatStateRestore) return;

        if (ChatStateManager.getInstance().shouldRestore()) {
            inputField.setText(ChatStateManager.getInstance().getSavedText());
        }
    }
}
