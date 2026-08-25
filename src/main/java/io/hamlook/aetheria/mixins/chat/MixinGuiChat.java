package io.hamlook.aetheria.mixins.chat;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.chat.ChatLineHook;
import io.hamlook.aetheria.features.chat.GuiChatHook;
import io.hamlook.aetheria.features.chat.GuiNewChatHook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Implements {@link GuiChatHook} on {@link GuiChat} and handles the
 * Chat Copy feature: click while holding Ctrl or Shift to copy a line.
 *
 * <ul>
 *   <li>CTRL+Click → copies the hovered line's formatted/plain text</li>
 *   <li>SHIFT+Click → copies the full original multi-line message</li>
 * </ul>
 */
@Mixin(GuiChat.class)
public class MixinGuiChat implements GuiChatHook {

    @Shadow
    protected GuiTextField inputField;

    // ── GuiChatHook ───────────────────────────────────────────────────────────

    @Override
    public boolean chatutils$isTypingMode() {
        // Typing mode = input field exists AND is focused.
        // Scroll / view-only mode has the field present but not focused.
        return inputField != null && inputField.isFocused();
    }

    // ── Copy on click ─────────────────────────────────────────────────────────

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void chatutils$onMouseClicked(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        if (ATHRConfig.feature == null || !ATHRConfig.feature.chat.chatCopyEnabled) return;
        if (mouseButton != 0) return;
        if (!GuiScreen.isShiftKeyDown() && !GuiScreen.isCtrlKeyDown()) return;

        GuiNewChatHook chatGUI = (GuiNewChatHook) Minecraft.getMinecraft().ingameGUI.getChatGUI();
        ChatLine line = chatGUI.chatutils$getCurrentHoveredLine();
        if (line == null) return;

        boolean formatted = ATHRConfig.feature.chat.chatCopyFormatted;
        String text;

        if (GuiScreen.isCtrlKeyDown()) {
            // Ctrl: copy only this wrapped line
            String raw = line.getChatComponent().getFormattedText();
            text = formatted ? raw : EnumChatFormatting.getTextWithoutFormattingCodes(raw);
        } else {
            // Shift: copy the full original message
            IChatComponent fullMsg = ((ChatLineHook) line).chatutils$getFullMessage();
            IChatComponent src = (fullMsg != null) ? fullMsg : line.getChatComponent();
            String raw = src.getFormattedText();
            text = formatted ? raw : EnumChatFormatting.getTextWithoutFormattingCodes(raw);
        }

        GuiScreen.setClipboardString(text);
        ci.cancel();
    }
}
