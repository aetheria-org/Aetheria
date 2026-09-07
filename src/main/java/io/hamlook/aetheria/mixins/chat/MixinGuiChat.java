package io.hamlook.aetheria.mixins.chat;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.chat.GuiChatHook;
import io.hamlook.aetheria.features.chat.emoji.EmojiSuggestionBar;
import io.hamlook.aetheria.features.qol.ChatStateManager;
import io.hamlook.aetheria.mixins.hooks.GuiChatMixinHook;
import io.hamlook.aetheria.mixins.hooks.PlayerButtonHook;
import io.hamlook.aetheria.utils.compat.MouseCompat;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiChat.class)
public abstract class MixinGuiChat extends GuiScreen implements GuiChatHook {

    @Shadow
    protected GuiTextField inputField;

    @Unique
    private static boolean athr$disabled() {
        return ATHRConfig.feature == null || !ATHRConfig.feature.chat.emojiConfig.enabled || !ATHRConfig.feature.chat.emojiConfig.suggestionsEnabled;
    }

    @Override
    public boolean athr$isTypingMode() {
        return inputField != null && inputField.isFocused();
    }

    @Inject(method = "drawScreen", at = @At("TAIL"))
    private void athr$drawPlayerMenu(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        PlayerButtonHook.drawMenu(mouseX, mouseY, this.width, this.height, this.fontRendererObj);
    }

    @Inject(method = "keyTyped", at = @At("HEAD"), cancellable = true)
    private void athr$onKeyTypedMenu(char typedChar, int keyCode, CallbackInfo ci) {
        if (PlayerButtonHook.handleKeyTyped(keyCode)) ci.cancel();
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void athr$onPlayerMenuClick(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        if (PlayerButtonHook.handlePlayerMenuClick(mouseX, mouseY, mouseButton, this.width, this.height, this.inputField))
            ci.cancel();
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void ATHR$onMouseClicked(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        if (!athr$disabled() && inputField != null && EmojiSuggestionBar.handleMouseClick(mouseX, mouseY, mouseButton, inputField)) {
            ci.cancel();
            return;
        }
        if (GuiChatMixinHook.handleChatCopy(mouseButton)) {
            ci.cancel();
        }
    }

    @Inject(method = "keyTyped", at = @At("HEAD"), cancellable = true)
    private void ATHR$onKeyTypedHead(char typedChar, int keyCode, CallbackInfo ci) {
        if (athr$disabled() || inputField == null) return;
        if (EmojiSuggestionBar.handleKeyTypedPre(keyCode, inputField)) {
            ci.cancel();
        }
    }

    @Inject(method = "keyTyped", at = @At("RETURN"))
    private void ATHR$onKeyTypedReturn(char typedChar, int keyCode, CallbackInfo ci) {
        if (ATHRConfig.feature != null && ATHRConfig.feature.qol.chatStateRestore) {
            if (keyCode != Keyboard.KEY_ESCAPE && keyCode != Keyboard.KEY_RETURN) {
                ChatStateManager.getInstance().updateState(inputField.getText());
            } else {
                ChatStateManager.getInstance().resetState();
            }
        }
        if (!athr$disabled() && inputField != null) {
            EmojiSuggestionBar.handleKeyTypedPost(keyCode, inputField);
        }
    }

    @Inject(method = "handleMouseInput", at = @At("HEAD"), cancellable = true)
    private void ATHR$onHandleMouseInput(CallbackInfo ci) {
        if (athr$disabled()) return;
        if (EmojiSuggestionBar.handleMouseWheel(MouseCompat.getEventDWheel())) {
            ci.cancel();
        }
    }

    @Inject(method = "drawScreen", at = @At("RETURN"))
    private void ATHR$drawSuggestions(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (athr$disabled()) return;
        EmojiSuggestionBar.render(inputField, mouseX, mouseY);
        EmojiSuggestionBar.tickDrag(mouseY);
    }

    @Inject(method = "initGui", at = @At("RETURN"))
    public void ATHR$chatStateInit(CallbackInfo ci) {
        if (ATHRConfig.feature == null || !ATHRConfig.feature.qol.chatStateRestore) return;
        if (ChatStateManager.getInstance().shouldRestore()) {
            inputField.setText(ChatStateManager.getInstance().getSavedText());
        }
    }
}
