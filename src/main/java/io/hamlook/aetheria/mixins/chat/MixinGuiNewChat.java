package io.hamlook.aetheria.mixins.chat;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.chat.ChatCompactHandler;
import io.hamlook.aetheria.features.chat.ChatLineHook;
import io.hamlook.aetheria.features.chat.ChatUtilsState;
import io.hamlook.aetheria.features.chat.GuiChatHook;
import io.hamlook.aetheria.features.chat.GuiNewChatHook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * The main chat-utils mixin.  Implements {@link GuiNewChatHook} and hooks into:
 *
 * <ul>
 *   <li>{@code setChatLine}  – timestamp injection, compacting, history-size expansion</li>
 *   <li>{@code drawChat}     – chat heads, transparent / animated chat, copy highlight</li>
 *   <li>{@code getChatComponent} – adjusts click-x when chat heads are enabled</li>
 *   <li>{@code clearChatMessages} – suppressed (overwritten to no-op)</li>
 * </ul>
 */
@Mixin(GuiNewChat.class)
public abstract class MixinGuiNewChat extends Gui implements GuiNewChatHook {

    // ── Shadows ───────────────────────────────────────────────────────────────

    @Shadow @Final private List<ChatLine> chatLines;
    @Shadow @Final private List<ChatLine> drawnChatLines;
    @Shadow @Final private Minecraft mc;
    @Shadow private int scrollPos;

    @Shadow public abstract boolean getChatOpen();
    @Shadow public abstract int getLineCount();
    @Shadow public abstract int getChatWidth();
    @Shadow public abstract float getChatScale();

    // ── Per-frame state ───────────────────────────────────────────────────────

    @Unique private ChatLine chatutils$renderLine   = null;
    @Unique private ChatLine chatutils$hoveredLine  = null;
    @Unique private long     chatutils$animationStart = 0L;

    // ═════════════════════════════════════════════════════════════════════════
    // setChatLine hooks
    // ═════════════════════════════════════════════════════════════════════════

    /** Step 1 – inject timestamp before line is processed. */
    @ModifyVariable(method = "setChatLine", at = @At("HEAD"), ordinal = 0)
    private IChatComponent chatutils$injectTimestamp(IChatComponent component) {
        return ChatCompactHandler.applyTimestamp(component);
    }

    /** Step 2 – store the full message on state and run compacting logic. */
    @Inject(method = "setChatLine", at = @At("HEAD"))
    private void chatutils$beforeSetChatLine(IChatComponent component, int chatLineId,
                                              int updateCounter, boolean refresh, CallbackInfo ci) {
        ChatUtilsState.currentFullMessage = component;
        ChatCompactHandler.handleChatMessage(component, refresh, chatLines, drawnChatLines);
    }

    /** Step 3 – clear per-message state after the line is stored. */
    @Inject(method = "setChatLine", at = @At("TAIL"))
    private void chatutils$afterSetChatLine(IChatComponent component, int chatLineId,
                                             int updateCounter, boolean refresh, CallbackInfo ci) {
        ChatCompactHandler.resetMessageHash();
        ChatUtilsState.currentFullMessage = null;
    }

    /** Step 4 – track each ChatLine added to the list for compacting bookkeeping. */
    @Redirect(
            method = "setChatLine",
            at = @At(value = "INVOKE", target = "Ljava/util/List;add(ILjava/lang/Object;)V", remap = false)
    )
    private void chatutils$trackChatLine(List<Object> list, int index, Object line) {
        list.add(index, line);
        if (line instanceof ChatLine) {
            ChatCompactHandler.trackChatLine((ChatLine) line);
        }
    }

    /** Expand the hard-coded 100-line history cap to 16 384 lines. */
    @ModifyConstant(method = "setChatLine", constant = @Constant(intValue = 100), expect = 2)
    private int chatutils$expandHistory(int original) {
        return 16384;
    }

    /** Suppress the vanilla clear so chat history survives GUI reopens. */
    @Overwrite
    public void clearChatMessages() { /* no-op */ }

    // ═════════════════════════════════════════════════════════════════════════
    // drawChat hooks
    // ═════════════════════════════════════════════════════════════════════════

    /** Trigger the slide-in animation reset when a new message arrives. */
    @Inject(method = "setChatLine", at = @At("HEAD"))
    private void chatutils$resetAnimation(IChatComponent component, int chatLineId,
                                           int updateCounter, boolean refresh, CallbackInfo ci) {
        if (ATHRConfig.feature != null && ATHRConfig.feature.chat.animatedChat && !refresh) {
            chatutils$animationStart = System.currentTimeMillis();
        }
    }

    /** Apply vertical slide animation at the start of drawChat. */
    @Inject(method = "drawChat", at = @At("HEAD"))
    private void chatutils$applyAnimation(int updateCounter, CallbackInfo ci) {
        if (ATHRConfig.feature == null || !ATHRConfig.feature.chat.animatedChat
                || chatutils$animationStart == 0L) return;

        double speed      = 20.0D;
        float  lineHeight = 9.0F;
        double shift      = ((double) System.currentTimeMillis()
                - (double) lineHeight * speed - chatutils$animationStart) / speed;
        if (shift > 0.0D) shift = 0.0D;
        GlStateManager.translate(0.0D, -shift, 0.0D);
    }

    /** Determine which line is under the mouse once per frame (for copy highlight). */
    @Inject(method = "drawChat", at = @At("HEAD"))
    private void chatutils$computeHoveredLine(int updateCounter, CallbackInfo ci) {
        chatutils$hoveredLine = null;
        if (ATHRConfig.feature == null || !ATHRConfig.feature.chat.chatCopyEnabled) return;
        if (!(mc.currentScreen instanceof GuiChat)) return;
        if (!((GuiChatHook) mc.currentScreen).chatutils$isTypingMode()) return;
        chatutils$hoveredLine = chatutils$getHoveredChatLine(
                Mouse.getX(), mc.displayHeight - Mouse.getY() - 1);
    }

    /**
     * Redirect the background rect draw so we can:
     * <ul>
     *   <li>extend it rightward when chat heads are enabled,</li>
     *   <li>make it transparent,</li>
     *   <li>highlight the hovered line for copy.</li>
     * </ul>
     */
    @Redirect(
            method = "drawChat",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/gui/GuiNewChat;drawRect(IIIII)V",
                     ordinal = 0)
    )
    private void chatutils$maybeClearBackground(int left, int top, int right, int bottom, int color) {
        if (ATHRConfig.feature == null) {
            drawRect(left, top, right, bottom, color);
            return;
        }

        int  newRight = ATHRConfig.feature.chat.chatHeads ? right + 10 : right;
        int  newColor = ATHRConfig.feature.chat.transparentChat ? 0x00000000 : color;

        if (ATHRConfig.feature.chat.chatCopyEnabled && getChatOpen()
                && chatutils$hoveredLine != null
                && chatutils$renderLine == chatutils$hoveredLine) {
            newColor = ATHRConfig.feature.chat.transparentChat ? 0x22AAAACC : 0x60AAAACC;
        }

        drawRect(left, top, newRight, bottom, newColor);
    }

    /** Track which ChatLine is currently being rendered (for the highlight redirect). */
    @ModifyVariable(method = "drawChat", at = @At("STORE"))
    private ChatLine chatutils$captureRenderLine(ChatLine line) {
        chatutils$renderLine = line;
        return line;
    }

    /**
     * Replace the FontRenderer#drawStringWithShadow call to:
     * <ul>
     *   <li>draw a player head skin quad when available,</li>
     *   <li>indent the text if heads are enabled (player or offset for non-player lines).</li>
     * </ul>
     */
    @Redirect(
            method = "drawChat",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;FFI)I")
    )
    private int chatutils$redirectDrawString(FontRenderer fr, String text, float x, float y, int color) {
        float drawX = x;

        if (ATHRConfig.feature != null && ATHRConfig.feature.chat.chatHeads
                && chatutils$renderLine instanceof ChatLineHook) {

            ChatLineHook hook = (ChatLineHook) chatutils$renderLine;
            NetworkPlayerInfo info = hook.chatutils$getPlayerInfo();

            if (info != null) {
                // Compute alpha from the color argument (may be faded by vanilla)
                int   alpha     = (color >> 24) & 0xFF;
                float headAlpha = (alpha == 0) ? 1.0f : alpha / 255f;

                GlStateManager.enableBlend();
                GlStateManager.enableAlpha();
                GlStateManager.enableTexture2D();
                mc.getTextureManager().bindTexture(info.getLocationSkin());
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                GlStateManager.color(1.0f, 1.0f, 1.0f, headAlpha);

                // Base layer then hat layer
                Gui.drawScaledCustomSizeModalRect((int) x, (int) (y - 1f), 8f,  8f, 8, 8, 8, 8, 64f, 64f);
                Gui.drawScaledCustomSizeModalRect((int) x, (int) (y - 1f), 40f, 8f, 8, 8, 8, 8, 64f, 64f);

                GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
                drawX += 10f;

            } else if (hook.chatutils$hasDetected() || ATHRConfig.feature.chat.offsetNonPlayerMessages) {
                // Indent non-player lines so all text is aligned
                drawX += 10f;
            }
        }

        return fr.drawStringWithShadow(text, drawX, y, color);
    }

    /** Shift click-x back so chat-component link clicks still land correctly. */
    @ModifyVariable(method = "getChatComponent", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private int chatutils$offsetClickX(int mouseX) {
        if (ATHRConfig.feature != null && ATHRConfig.feature.chat.chatHeads) {
            return mouseX - 10;
        }
        return mouseX;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GuiNewChatHook implementation
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    public ChatLine chatutils$getCurrentHoveredLine() {
        return chatutils$hoveredLine;
    }

    @Override
    public ChatLine chatutils$getHoveredChatLine(int rawMouseX, int rawMouseY) {
        if (!getChatOpen()) return null;

        ScaledResolution sr          = new ScaledResolution(mc);
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
