package io.hamlook.aetheria.mixins.chat;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.chat.GuiChatHook;
import io.hamlook.aetheria.features.chat.emoji.EmojiSuggestionBar;
import io.hamlook.aetheria.features.qol.ChatStateManager;
import io.hamlook.aetheria.utils.hooks.GuiChatMixinHook;
import io.hamlook.aetheria.utils.compat.DrawContextUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.hooks.PlayerButtonHook;
import io.hamlook.aetheria.utils.compat.MouseCompat;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
//#if MC < 1.21
//#else
//$$ import net.minecraft.client.gui.screen.ChatScreen;
//$$ import net.minecraft.client.gui.screen.Screen;
//$$ import net.minecraft.client.gui.widget.TextFieldWidget;
//$$ import net.minecraft.text.Text;
//$$ import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
//$$ import static org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;
//$$ import static org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER;
//#endif
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//#if MC < 1.21
@Mixin(GuiChat.class)
public abstract class MixinGuiChat extends GuiScreen implements GuiChatHook {
//#else
//$$ @Mixin(ChatScreen.class)
//$$ public abstract class MixinGuiChat extends Screen implements GuiChatHook {
//$$     public MixinGuiChat() { super(Text.empty()); }
//#endif

    //#if MC < 1.21
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
        PlayerButtonHook.drawMenu(mouseX, mouseY, this.width, this.height, MinecraftCompat.getFontRenderer());
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
    //#else
//$$     @Shadow
//$$     protected TextFieldWidget chatField;
//$$
//$$     @Unique
//$$     private static boolean athr$disabled() {
//$$         return ATHRConfig.feature == null || !ATHRConfig.feature.chat.emojiConfig.enabled || !ATHRConfig.feature.chat.emojiConfig.suggestionsEnabled;
//$$     }
//$$
//$$     @Override
//$$     public boolean athr$isTypingMode() {
//$$         return chatField != null && chatField.isFocused();
//$$     }
//$$
//$$     @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At("TAIL"))
//$$     private void athr$drawPlayerMenu(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
//$$         DrawContextUtils.setContext(context);
//$$         try {
//$$             PlayerButtonHook.drawMenu(mouseX, mouseY, this.width, this.height, MinecraftCompat.getFontRenderer());
//$$         } finally {
//$$             DrawContextUtils.clearContext();
//$$         }
//$$     }
//$$
//$$     @Inject(method = "keyPressed(III)Z", at = @At("HEAD"), cancellable = true)
//$$     private void athr$onKeyTypedMenu(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
//$$         if (PlayerButtonHook.handleKeyTyped(keyCode)) cir.setReturnValue(true);
//$$     }
//$$
//$$     @Inject(method = "mouseClicked(DDI)Z", at = @At("HEAD"), cancellable = true)
//$$     private void athr$onPlayerMenuClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
//$$         int mx = (int) mouseX, my = (int) mouseY;
//$$         if (PlayerButtonHook.handlePlayerMenuClick(mx, my, button, this.width, this.height, this.chatField))
//$$             cir.setReturnValue(true);
//$$     }
//$$
//$$     @Inject(method = "mouseClicked(DDI)Z", at = @At("HEAD"), cancellable = true)
//$$     private void ATHR$onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
//$$         int mx = (int) mouseX, my = (int) mouseY;
//$$         if (!athr$disabled() && chatField != null && EmojiSuggestionBar.handleMouseClick(mx, my, button, chatField)) {
//$$             cir.setReturnValue(true);
//$$             return;
//$$         }
//$$         if (GuiChatMixinHook.handleChatCopy(button)) {
//$$             cir.setReturnValue(true);
//$$         }
//$$     }
//$$
//$$     @Inject(method = "keyPressed(III)Z", at = @At("HEAD"), cancellable = true)
//$$     private void ATHR$onKeyTypedHead(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
//$$         if (athr$disabled() || chatField == null) return;
//$$         if (EmojiSuggestionBar.handleKeyTypedPre(keyCode, chatField)) {
//$$             cir.setReturnValue(true);
//$$         }
//$$     }
//$$
//$$     @Inject(method = "keyPressed(III)Z", at = @At("RETURN"))
//$$     private void ATHR$onKeyTypedReturn(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
//$$         if (ATHRConfig.feature != null && ATHRConfig.feature.qol.chatStateRestore) {
//$$             if (keyCode != GLFW_KEY_ESCAPE && keyCode != GLFW_KEY_ENTER && keyCode != GLFW_KEY_KP_ENTER) {
//$$                 ChatStateManager.getInstance().updateState(chatField.getText());
//$$             } else {
//$$                 ChatStateManager.getInstance().resetState();
//$$             }
//$$         }
//$$         if (!athr$disabled() && chatField != null) {
//$$             EmojiSuggestionBar.handleKeyTypedPost(keyCode, chatField);
//$$         }
//$$     }
//$$
//$$     @Inject(method = "mouseScrolled(DDDD)Z", at = @At("HEAD"), cancellable = true)
//$$     private void ATHR$onHandleMouseInput(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
//$$         if (athr$disabled()) return;
//$$         if (EmojiSuggestionBar.handleMouseWheel((int) verticalAmount)) {
//$$             cir.setReturnValue(true);
//$$         }
//$$     }
//$$
//$$     @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At("RETURN"))
//$$     private void ATHR$drawSuggestions(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
//$$         if (athr$disabled()) return;
//$$         DrawContextUtils.setContext(context);
//$$         try {
//$$             EmojiSuggestionBar.render(chatField, mouseX, mouseY);
//$$             EmojiSuggestionBar.tickDrag(mouseY);
//$$         } finally {
//$$             DrawContextUtils.clearContext();
//$$         }
//$$     }
//$$
//$$     @Inject(method = "init()V", at = @At("RETURN"))
//$$     public void ATHR$chatStateInit(CallbackInfo ci) {
//$$         if (ATHRConfig.feature == null || !ATHRConfig.feature.qol.chatStateRestore) return;
//$$         if (ChatStateManager.getInstance().shouldRestore()) {
//$$             chatField.setText(ChatStateManager.getInstance().getSavedText());
//$$         }
//$$     }
//#endif
}
