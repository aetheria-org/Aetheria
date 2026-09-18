package io.hamlook.aetheria.mixins.chat;

import io.hamlook.aetheria.features.chat.ChatCompactHandler;
import io.hamlook.aetheria.features.chat.ChatUtilsState;
import io.hamlook.aetheria.features.chat.GuiNewChatHook;
import io.hamlook.aetheria.utils.compat.GuiScreenUtils;
import io.hamlook.aetheria.utils.hooks.ChatHook;
//#if MC < 1.16
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.util.IChatComponent;
//#else
//$$ import net.minecraft.client.MinecraftClient;
//$$ import net.minecraft.client.gui.hud.ChatHudLine;
//$$ import net.minecraft.client.font.TextRenderer;
//$$ import net.minecraft.client.gui.DrawContext;
//$$ import net.minecraft.client.gui.hud.ChatHud;
//$$ import net.minecraft.client.gui.hud.MessageIndicator;
//$$ import net.minecraft.network.message.MessageSignatureData;
//$$ import net.minecraft.text.OrderedText;
//$$ import net.minecraft.text.Text;
//#endif
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

//#if MC < 1.16
@Mixin(GuiNewChat.class)
public abstract class MixinGuiNewChat extends Gui implements GuiNewChatHook {

    @Shadow @Final private List<ChatLine> chatLines;
    @Shadow @Final private List<ChatLine> drawnChatLines;
    @Shadow @Final private Minecraft mc;
    @Shadow private int scrollPos;

    @Shadow public abstract boolean getChatOpen();
    @Shadow public abstract int getLineCount();
    @Shadow public abstract float getChatScale();

    @ModifyVariable(method = "setChatLine", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private IChatComponent athr$injectTimestamp(IChatComponent component) {
        return ChatCompactHandler.applyTimestamp(component);
    }

    @Inject(method = "setChatLine", at = @At("HEAD"))
    private void athr$beforeSetChatLine(IChatComponent component, int chatLineId,
                                         int updateCounter, boolean refresh, CallbackInfo ci) {
        ChatUtilsState.currentFullMessage = component;
        ChatCompactHandler.handleChatMessage(component, refresh, this.chatLines, this.drawnChatLines);
    }

    @Inject(method = "setChatLine", at = @At("TAIL"))
    private void athr$afterSetChatLine(IChatComponent component, int chatLineId,
                                        int updateCounter, boolean refresh, CallbackInfo ci) {
        ChatCompactHandler.resetMessageHash();
        ChatUtilsState.currentFullMessage = null;
    }

    @Redirect(
            method = "setChatLine",
            at = @At(value = "INVOKE", target = "Ljava/util/List;add(ILjava/lang/Object;)V", remap = false)
    )
    private void athr$trackChatLine(List<Object> list, int index, Object line) {
        list.add(index, line);
        if (line instanceof ChatLine) {
            ChatCompactHandler.trackChatLine((ChatLine) line);
        }
    }

    @ModifyConstant(method = "setChatLine", constant = @Constant(intValue = 100), expect = 2)
    private int athr$expandHistory(int original) {
        return 16384;
    }

    /**
     * @author Aetheria
     * @reason Preserve chat history across GUI reopens
     */
    @Overwrite
    public void clearChatMessages() { }

    @Inject(method = "setChatLine", at = @At("HEAD"))
    private void athr$resetAnimation(IChatComponent component, int chatLineId,
                                      int updateCounter, boolean refresh, CallbackInfo ci) {
        ChatHook.shouldResetAnimation(refresh);
    }

    @Inject(method = "drawChat", at = @At("HEAD"))
    private void athr$applyAnimation(int updateCounter, CallbackInfo ci) {
        ChatHook.applyAnimation(updateCounter);
    }

    @Inject(method = "drawChat", at = @At("HEAD"))
    private void athr$computeHoveredLine(int updateCounter, CallbackInfo ci) {
        ChatHook.computeHoveredLine(this.drawnChatLines, this.scrollPos, getChatScale(), getLineCount(), org.lwjgl.input.Mouse.getX(), org.lwjgl.input.Mouse.getY());
    }

    @Redirect(
            method = "drawChat",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/gui/GuiNewChat;drawRect(IIIII)V",
                     ordinal = 0)
    )
    private void athr$clearBackground(int left, int top, int right, int bottom, int color) {
        ChatHook.getBackgroundColor(left, top, right, bottom, color, getChatOpen());
    }

    @ModifyVariable(method = "drawChat", at = @At("STORE"))
    private ChatLine athr$captureRenderLine(ChatLine line) {
        ChatHook.setRenderLine(line);
        return line;
    }

    @Redirect(
            method = "drawChat",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;FFI)I")
    )
    private int athr$redirectDrawString(FontRenderer fr, String text, float x, float y, int color) {
        float drawX = ChatHook.drawChatHead(x, y, color, ChatHook.getRenderLine());
        return fr.drawStringWithShadow(text, drawX, y, color);
    }

    @ModifyVariable(method = "getChatComponent", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private int athr$offsetClickX(int mouseX) {
        return ChatHook.offsetClickX(mouseX);
    }

    @Override
    public ChatLine athr$getCurrentHoveredLine() {
        return ChatHook.getHoveredLine();
    }

    @Override
    public ChatLine athr$getHoveredChatLine(int rawMouseX, int rawMouseY) {
        return ChatHook.getHoveredChatLine(this.drawnChatLines, this.scrollPos, getChatScale(), getLineCount(), rawMouseX, rawMouseY);
    }
}
//#else
//$$ @Mixin(ChatHud.class)
//$$ public abstract class MixinGuiNewChat implements GuiNewChatHook {
//$$
//$$     @Shadow @Final private List<ChatHudLine> messages;
//$$     @Shadow @Final private List visibleMessages;
//$$     @Shadow @Final private MinecraftClient client;
//$$     @Shadow private int scrolledLines;
//$$
//$$     @Shadow public abstract boolean isChatFocused();
//$$     @Shadow public abstract int getVisibleLineCount();
//$$     @Shadow public abstract double getChatScale();
//$$
//$$     @ModifyVariable(
//$$             method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
//$$             at = @At("HEAD"), ordinal = 0, argsOnly = true)
//$$     private Text athr$injectTimestamp(Text component) {
//$$         return ChatCompactHandler.applyTimestamp(component);
//$$     }
//$$
//$$     @Inject(
//$$             method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
//$$             at = @At("HEAD"))
//$$     private void athr$beforeAddMessage(Text component, MessageSignatureData signature, MessageIndicator indicator, CallbackInfo ci) {
//$$         ChatUtilsState.currentFullMessage = component;
//$$         ChatCompactHandler.handleChatMessage(component, false, this.messages, this.visibleMessages);
//$$         ChatHook.shouldResetAnimation(false);
//$$     }
//$$
//$$     @Inject(
//$$             method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
//$$             at = @At("TAIL"))
//$$     private void athr$afterAddMessage(Text component, MessageSignatureData signature, MessageIndicator indicator, CallbackInfo ci) {
//$$         ChatCompactHandler.resetMessageHash();
//$$         ChatUtilsState.currentFullMessage = null;
//$$     }
//$$
//$$     @Inject(
//$$             method = "addMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V",
//$$             at = @At("TAIL"))
//$$     private void athr$trackChatLine(ChatHudLine line) {
//$$         ChatCompactHandler.trackChatLine(line);
//$$     }
//$$
//$$     @ModifyConstant(
//$$             method = "addMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V",
//$$             constant = @Constant(intValue = 100))
//$$     private int athr$expandHistory(int original) {
//$$         return 16384;
//$$     }
//$$
//$$     /**
//$$      * @author Aetheria
//$$      * @reason Preserve chat history across GUI reopens
//$$      */
//$$     @Inject(method = "clear(Z)V", at = @At("HEAD"), cancellable = true)
//$$     private void athr$preserveHistory(boolean chatCleared, CallbackInfo ci) {
//$$         ci.cancel();
//$$     }
//$$
//$$     @Inject(method = "render", at = @At("HEAD"))
//$$     private void athr$applyAnimation(DrawContext context, int currentTick, int mouseX, int mouseY, boolean focused, CallbackInfo ci) {
//$$         ChatHook.applyAnimation(currentTick);
//$$     }
//$$
//$$     @Inject(method = "render", at = @At("HEAD"))
//$$     private void athr$computeHoveredLine(DrawContext context, int currentTick, int mouseX, int mouseY, boolean focused, CallbackInfo ci) {
//$$         ChatHook.computeHoveredVisible(this.visibleMessages, this.scrolledLines, (float) getChatScale(), getVisibleLineCount(), mouseX, mouseY);
//$$     }
//$$
//$$     @Redirect(
//$$             method = "render",
//$$             at = @At(value = "INVOKE",
//$$                      target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V",
//$$                      ordinal = 0)
//$$     )
//$$     private void athr$clearBackground(DrawContext context, int left, int top, int right, int bottom, int color) {
//$$         ChatHook.getBackgroundColor(context, left, top, right, bottom, color, isChatFocused());
//$$     }
//$$
//$$     @Redirect(
//$$             method = "render",
//$$             at = @At(value = "INVOKE",
//$$                      target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;III)I")
//$$     )
//$$     private int athr$redirectDrawString(DrawContext context, TextRenderer textRenderer, OrderedText text, int x, int y, int color) {
//$$         float drawX = ChatHook.drawChatHead(context, (float) x, (float) y, color, ChatHook.getRenderVisible());
//$$         return context.drawTextWithShadow(textRenderer, text, (int) drawX, y, color);
//$$     }
//$$
//$$     @ModifyVariable(method = "getTextStyleAt", at = @At("HEAD"), ordinal = 0, argsOnly = true)
//$$     private double athr$offsetClickX(double mouseX) {
//$$         return ChatHook.offsetClickX((int) mouseX);
//$$     }
//$$
//$$     @Override
//$$     public ChatHudLine athr$getCurrentHoveredLine() {
//$$         return ChatHook.getHoveredLine();
//$$     }
//$$
//$$     @Override
//$$     public ChatHudLine athr$getHoveredChatLine(int rawMouseX, int rawMouseY) {
//$$         int scaleFactor = GuiScreenUtils.getScaleFactor();
//$$         ChatHook.computeHoveredVisible(this.visibleMessages, this.scrolledLines, (float) getChatScale(), getVisibleLineCount(), rawMouseX / scaleFactor, rawMouseY / scaleFactor);
//$$         return ChatHook.getHoveredLine();
//$$     }
//$$ }
//#endif
