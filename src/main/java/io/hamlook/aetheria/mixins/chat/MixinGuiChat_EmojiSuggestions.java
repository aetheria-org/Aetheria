package io.hamlook.aetheria.mixins.chat;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.chat.emoji.EmojiSuggestionBar;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiChat.class)
public class MixinGuiChat_EmojiSuggestions {

    @Shadow
    protected GuiTextField inputField;

    @Unique
    private static boolean aetheria$disabled() {
        return ATHRConfig.feature == null || !ATHRConfig.feature.chat.emojiConfig.enabled || !ATHRConfig.feature.chat.emojiConfig.suggestionsEnabled;
    }

    @Inject(method = "keyTyped", at = @At("HEAD"), cancellable = true)
    private void ATHR$onKeyTypedHead(char typedChar, int keyCode, CallbackInfo ci) {
        if (aetheria$disabled() || inputField == null) return;
        if (EmojiSuggestionBar.handleKeyTypedPre(keyCode, inputField)) {
            ci.cancel();
        }
    }

    @Inject(method = "keyTyped", at = @At("RETURN"))
    private void ATHR$onKeyTypedReturn(char typedChar, int keyCode, CallbackInfo ci) {
        if (aetheria$disabled() || inputField == null) return;
        EmojiSuggestionBar.handleKeyTypedPost(keyCode, inputField);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void ATHR$onMouseClicked(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        if (aetheria$disabled() || inputField == null) return;
        if (EmojiSuggestionBar.handleMouseClick(mouseX, mouseY, mouseButton, inputField)) {
            ci.cancel();
        }
    }

    @Inject(method = "handleMouseInput", at = @At("HEAD"), cancellable = true)
    private void ATHR$onHandleMouseInput(CallbackInfo ci) {
        if (aetheria$disabled()) return;
        if (EmojiSuggestionBar.handleMouseWheel(Mouse.getEventDWheel())) {
            ci.cancel();
        }
    }

    @Inject(method = "drawScreen", at = @At("RETURN"))
    private void ATHR$drawSuggestions(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (aetheria$disabled()) return;
        EmojiSuggestionBar.render(inputField, mouseX, mouseY);
        EmojiSuggestionBar.tickDrag(mouseY);
    }
}
