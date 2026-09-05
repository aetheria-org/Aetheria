package io.hamlook.aetheria.mixins.chat;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.chat.ChatLineHook;
import io.hamlook.aetheria.features.chat.GuiChatHook;
import io.hamlook.aetheria.features.chat.GuiNewChatHook;
import io.hamlook.aetheria.features.chat.emoji.EmojiSuggestionBar;
import io.hamlook.aetheria.features.qol.ChatStateManager;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.MouseCompat;
import io.hamlook.aetheria.utils.compat.ClipboardCompat;
import io.hamlook.aetheria.utils.compat.TextCompat;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiChat.class)
public abstract class MixinGuiChat implements GuiChatHook {

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

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void ATHR$onMouseClicked(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        if (!athr$disabled() && inputField != null && EmojiSuggestionBar.handleMouseClick(mouseX, mouseY, mouseButton, inputField)) {
            ci.cancel();
            return;
        }
        if (ATHRConfig.feature == null || !ATHRConfig.feature.chat.chatCopyEnabled) return;
        if (mouseButton != 0) return;
        if (!GuiScreen.isShiftKeyDown() && !GuiScreen.isCtrlKeyDown()) return;
        GuiNewChatHook chatGUI = (GuiNewChatHook) MinecraftCompat.getMinecraft().ingameGUI.getChatGUI();
        ChatLine line = chatGUI.athr$getCurrentHoveredLine();
        if (line == null) return;
        boolean formatted = ATHRConfig.feature.chat.chatCopyFormatted;
        String text;
        if (GuiScreen.isCtrlKeyDown()) {
            String raw = TextCompat.getFormattedText(line.getChatComponent());
            text = formatted ? raw : EnumChatFormatting.getTextWithoutFormattingCodes(raw);
        } else {
            IChatComponent fullMsg = ((ChatLineHook) line).athr$getFullMessage();
            IChatComponent src = (fullMsg != null) ? fullMsg : line.getChatComponent();
            String raw = TextCompat.getFormattedText(src);
            text = formatted ? raw : EnumChatFormatting.getTextWithoutFormattingCodes(raw);
        }
        ClipboardCompat.setClipboard(text);
        ci.cancel();
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
