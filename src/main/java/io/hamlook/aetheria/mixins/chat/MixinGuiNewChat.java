package io.hamlook.aetheria.mixins.chat;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.chat.*;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.GuiScreenUtils;
import io.hamlook.aetheria.utils.compat.MouseCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(GuiNewChat.class)
public abstract class MixinGuiNewChat extends Gui implements GuiNewChatHook {

    @Shadow @Final private List<ChatLine> chatLines;
    @Shadow @Final private List<ChatLine> drawnChatLines;
    @Shadow @Final private Minecraft mc;
    @Shadow private int scrollPos;

    @Shadow public abstract boolean getChatOpen();
    @Shadow public abstract int getLineCount();
    @Shadow public abstract float getChatScale();

    @Unique private ChatLine athr$renderLine   = null;
    @Unique private ChatLine athr$hoveredLine  = null;
    @Unique private long     athr$animationStart = 0L;

    @ModifyVariable(method = "setChatLine", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private IChatComponent athr$injectTimestamp(IChatComponent component) {
        return ChatCompactHandler.applyTimestamp(component);
    }

    @Inject(method = "setChatLine", at = @At("HEAD"))
    private void athr$beforeSetChatLine(IChatComponent component, int chatLineId,
                                         int updateCounter, boolean refresh, CallbackInfo ci) {
        ChatUtilsState.currentFullMessage = component;
        ChatCompactHandler.handleChatMessage(component, refresh, chatLines, drawnChatLines);
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
        if (ATHRConfig.feature != null && ATHRConfig.feature.chat.animatedChat && !refresh) {
            athr$animationStart = System.currentTimeMillis();
        }
    }

    @Inject(method = "drawChat", at = @At("HEAD"))
    private void athr$applyAnimation(int updateCounter, CallbackInfo ci) {
        if (ATHRConfig.feature == null || !ATHRConfig.feature.chat.animatedChat
                || athr$animationStart == 0L) return;

        double speed      = 20.0D;
        float  lineHeight = 9.0F;
        double shift      = ((double) System.currentTimeMillis()
                - (double) lineHeight * speed - athr$animationStart) / speed;
        if (shift > 0.0D) shift = 0.0D;
        GlStateManagerCompat.translate(0.0D, -shift, 0.0D);
    }

    @Inject(method = "drawChat", at = @At("HEAD"))
    private void athr$computeHoveredLine(int updateCounter, CallbackInfo ci) {
        athr$hoveredLine = null;
        if (ATHRConfig.feature == null || !ATHRConfig.feature.chat.chatCopyEnabled) return;
        if (!(mc.currentScreen instanceof GuiChat)) return;
        if (!((GuiChatHook) mc.currentScreen).athr$isTypingMode()) return;
        athr$hoveredLine = athr$getHoveredChatLine(
                MouseCompat.getX(), GuiScreenUtils.getDisplayHeight() - MouseCompat.getY() - 1);
    }

    @Redirect(
            method = "drawChat",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/gui/GuiNewChat;drawRect(IIIII)V",
                     ordinal = 0)
    )
    private void athr$clearBackground(int left, int top, int right, int bottom, int color) {
        if (ATHRConfig.feature == null) {
            drawRect(left, top, right, bottom, color);
            return;
        }

        int  newRight = ATHRConfig.feature.chat.chatHeads ? right + 10 : right;
        int  newColor = ATHRConfig.feature.chat.transparentChat ? 0x00000000 : color;

        if (ATHRConfig.feature.chat.chatCopyEnabled && getChatOpen()
                && athr$hoveredLine != null
                && athr$renderLine == athr$hoveredLine) {
            newColor = ATHRConfig.feature.chat.transparentChat ? 0x22AAAACC : 0x60AAAACC;
        }

        drawRect(left, top, newRight, bottom, newColor);
    }

    @ModifyVariable(method = "drawChat", at = @At("STORE"))
    private ChatLine athr$captureRenderLine(ChatLine line) {
        athr$renderLine = line;
        return line;
    }

    @Redirect(
            method = "drawChat",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;FFI)I")
    )
    private int athr$redirectDrawString(FontRenderer fr, String text, float x, float y, int color) {
        float drawX = x;

        if (ATHRConfig.feature != null && ATHRConfig.feature.chat.chatHeads
                && athr$renderLine instanceof ChatLineHook) {

            ChatLineHook hook = (ChatLineHook) athr$renderLine;
            NetworkPlayerInfo info = hook.athr$getPlayerInfo();

            if (info != null) {
                int   alpha     = (color >> 24) & 0xFF;
                float headAlpha = (alpha == 0) ? 1.0f : alpha / 255f;

                GlStateManagerCompat.enableBlend();
                GlStateManagerCompat.enableAlpha();
                GlStateManagerCompat.enableTexture2D();
                mc.getTextureManager().bindTexture(info.getLocationSkin());
                GlStateManagerCompat.tryBlendFuncSeparate(770, 771, 1, 0);
                GlStateManagerCompat.color(1.0f, 1.0f, 1.0f, headAlpha);

                Gui.drawScaledCustomSizeModalRect((int) x, (int) (y - 1f), 8f,  8f, 8, 8, 8, 8, 64f, 64f);
                Gui.drawScaledCustomSizeModalRect((int) x, (int) (y - 1f), 40f, 8f, 8, 8, 8, 8, 64f, 64f);

                GlStateManagerCompat.color(1.0f, 1.0f, 1.0f, 1.0f);
                drawX += 10f;

            } else if (hook.athr$hasDetected() || ATHRConfig.feature.chat.offsetNonPlayerMessages) {
                drawX += 10f;
            }
        }

        return fr.drawStringWithShadow(text, drawX, y, color);
    }

    @ModifyVariable(method = "getChatComponent", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private int athr$offsetClickX(int mouseX) {
        if (ATHRConfig.feature != null && ATHRConfig.feature.chat.chatHeads) {
            return mouseX - 10;
        }
        return mouseX;
    }

    @Override
    public ChatLine athr$getCurrentHoveredLine() {
        return athr$hoveredLine;
    }

    @Override
    public ChatLine athr$getHoveredChatLine(int rawMouseX, int rawMouseY) {
        if (!getChatOpen()) return null;

        ScaledResolution sr          = GuiScreenUtils.getScaledResolution();
        int              scaleFactor = sr.getScaleFactor();
        float            chatScale   = getChatScale();

        int mouseY = rawMouseY / scaleFactor;
        int y      = (sr.getScaledHeight() - 27) - mouseY;
        y = MathHelper.floor_float((float) y / chatScale);
        if (y < 0) return null;

        int visibleLines = Math.min(getLineCount(), drawnChatLines.size());
        int lineHeight   = mc.fontRendererObj.FONT_HEIGHT + 1;

        if (y < mc.fontRendererObj.FONT_HEIGHT * visibleLines + visibleLines) {
            int index = y / lineHeight + scrollPos;
            if (index >= 0 && index < drawnChatLines.size()) {
                return drawnChatLines.get(index);
            }
        }

        return null;
    }
}
