package io.hamlook.aetheria.utils.hooks

import io.hamlook.aetheria.core.ATHRConfig
import io.hamlook.aetheria.features.chat.ChatLineHook
import io.hamlook.aetheria.features.chat.GuiChatHook
import io.hamlook.aetheria.utils.compat.DrawContextUtils
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat
import io.hamlook.aetheria.utils.compat.GuiScreenUtils
import io.hamlook.aetheria.utils.compat.MinecraftCompat
import io.hamlook.aetheria.utils.compat.TextCompat
import net.minecraft.client.gui.ChatLine
import net.minecraft.client.gui.GuiChat
//#if MC < 1.21
//#else
//$$ import net.minecraft.client.gui.DrawContext
//$$ import net.minecraft.client.gui.PlayerSkinDrawer
//$$ import net.minecraft.client.gui.hud.ChatHudLine.Visible
//#endif

object ChatHook {

    @JvmStatic
    var renderLine: ChatLine? = null
    @JvmStatic
    var hoveredLine: ChatLine? = null
    @JvmStatic
    var renderVisible: Any? = null
    @JvmStatic
    var animationStart: Long = 0L

    @JvmStatic
    fun applyAnimation(updateCounter: Int) {
        val config = ATHRConfig.feature ?: return
        if (!config.chat.animatedChat || animationStart == 0L) return

        val speed = 20.0
        val lineHeight = 9.0f
        var shift = (System.currentTimeMillis().toDouble() - lineHeight.toDouble() * speed - animationStart) / speed
        if (shift > 0.0) shift = 0.0
        GlStateManagerCompat.translate(0.0, -shift, 0.0)
    }

    @JvmStatic
    fun computeHoveredLine(
        drawnChatLines: List<ChatLine>, scrollPos: Int, chatScale: Float, lineCount: Int, mouseX: Int, mouseY: Int
    ) {
        hoveredLine = null
        val config = ATHRConfig.feature ?: return
        if (!config.chat.chatCopyEnabled) return
        if (MinecraftCompat.getCurrentScreen() !is GuiChat) return
        val screen = MinecraftCompat.getCurrentScreen() as? GuiChatHook ?: return
        if (!screen.`athr$isTypingMode`()) return
        hoveredLine = getHoveredChatLine(drawnChatLines, scrollPos, chatScale, lineCount, mouseX, mouseY)
    }

    @JvmStatic
    fun shouldResetAnimation(refresh: Boolean): Boolean {
        val config = ATHRConfig.feature ?: return false
        if (config.chat.animatedChat && !refresh) {
            animationStart = System.currentTimeMillis()
            return true
        }
        return false
    }

    @JvmStatic
    fun getBackgroundColor(left: Int, top: Int, right: Int, bottom: Int, color: Int, chatOpen: Boolean): Int {
        val config = ATHRConfig.feature ?: return color

        val newRight = if (config.chat.chatHeads) right + 10 else right
        var newColor = if (config.chat.transparentChat) 0x00000000 else color

        if (config.chat.chatCopyEnabled && chatOpen && hoveredLine != null && renderLine == hoveredLine) {
            newColor = if (config.chat.transparentChat) 0x22AAAACC else 0x60AAAACC
        }

        DrawContextUtils.fill(left, top, newRight, bottom, newColor)
        return newColor
    }

    //#if MC < 1.21
    @JvmStatic
    fun drawChatHead(
        x: Float, y: Float, color: Int, renderLine: ChatLine?
    ): Float {
        val config = ATHRConfig.feature ?: return x
        if (!config.chat.chatHeads) return x
        if (renderLine !is ChatLineHook) return x

        val hook = renderLine as ChatLineHook
        val info = hook.`athr$getPlayerInfo`() ?: run {
            if (hook.`athr$hasDetected`() || config.chat.offsetNonPlayerMessages) {
                return x + 10f
            }
            return x
        }

        val alpha = (color shr 24) and 0xFF
        val headAlpha = if (alpha == 0) 1.0f else alpha / 255f

        val mc = MinecraftCompat.getMinecraft()
        GlStateManagerCompat.enableBlend()
        GlStateManagerCompat.enableAlpha()
        GlStateManagerCompat.enableTexture2D()
        mc.textureManager.bindTexture(info.locationSkin)
        GlStateManagerCompat.tryBlendFuncSeparate(770, 771, 1, 0)
        GlStateManagerCompat.color(1.0f, 1.0f, 1.0f, headAlpha)

        DrawContextUtils.drawScaledCustomSizeModalRect(
            x.toInt(), (y - 1f).toInt(), 8f, 8f, 8, 8, 8f, 8f, 64f, 64f
        )
        DrawContextUtils.drawScaledCustomSizeModalRect(
            x.toInt(), (y - 1f).toInt(), 40f, 8f, 8, 8, 8f, 8f, 64f, 64f
        )

        GlStateManagerCompat.color(1.0f, 1.0f, 1.0f, 1.0f)
        return x + 10f
    }
    //#else
    //$$ @JvmStatic
    //$$ fun drawChatHead(
    //$$     context: DrawContext, x: Float, y: Float, color: Int, renderLine: Any?
    //$$ ): Float {
    //$$     val config = ATHRConfig.feature ?: return x
    //$$     if (!config.chat.chatHeads) return x
    //$$     if (renderLine !is ChatLineHook) return x
    //$$
    //$$     val info = renderLine.`athr$getPlayerInfo`() ?: run {
    //$$         if (renderLine.`athr$isHeadHidden`() || config.chat.offsetNonPlayerMessages) {
    //$$             return x + 10f
    //$$         }
    //$$         return x
    //$$     }
    //$$     if (renderLine.`athr$isHeadHidden`()) return x + 10f
    //$$
    //$$     val alpha = (color shr 24) and 0xFF
    //$$     val headAlpha = if (alpha == 0) 1.0f else alpha / 255f
    //$$
    //$$     GlStateManagerCompat.enableBlend()
    //$$     GlStateManagerCompat.color(1.0f, 1.0f, 1.0f, headAlpha)
    //$$     PlayerSkinDrawer.draw(context, info.getSkinTextures(), x.toInt(), (y - 1f).toInt(), 8)
    //$$     GlStateManagerCompat.color(1.0f, 1.0f, 1.0f, 1.0f)
    //$$     return x + 10f
    //$$ }
    //#endif

    //#if MC < 1.21
    //#else
    //$$ @JvmStatic
    //$$ fun getBackgroundColor(context: DrawContext, left: Int, top: Int, right: Int, bottom: Int, color: Int, chatOpen: Boolean): Int {
    //$$     val config = ATHRConfig.feature ?: return color
    //$$
    //$$     val newRight = if (config.chat.chatHeads) right + 10 else right
    //$$     var newColor = if (config.chat.transparentChat) 0x00000000 else color
    //$$
    //$$     val renderParent = (renderVisible as? ChatLineHook)?.`athr$getParentLine`()
    //$$     if (config.chat.chatCopyEnabled && chatOpen && hoveredLine != null && renderParent == hoveredLine) {
    //$$         newColor = if (config.chat.transparentChat) 0x22AAAACC else 0x60AAAACC
    //$$     }
    //$$
    //$$     context.fill(left, top, newRight, bottom, newColor)
    //$$     return newColor
    //$$ }
    //#endif

    @JvmStatic
    fun offsetClickX(mouseX: Int): Int {
        val config = ATHRConfig.feature ?: return mouseX
        return if (config.chat.chatHeads) mouseX - 10 else mouseX
    }

    @JvmStatic
    fun getHoveredChatLine(
        drawnChatLines: List<ChatLine>, scrollPos: Int, chatScale: Float, lineCount: Int, rawMouseX: Int, rawMouseY: Int
    ): ChatLine? {
        val scaleFactor = GuiScreenUtils.getScaleFactor()
        val mouseY = rawMouseY / scaleFactor
        var y = (GuiScreenUtils.getScaledWindowHeight() - 27) - mouseY
        y = TextCompat.floor(y.toFloat() / chatScale)
        if (y < 0) return null

        val visibleLines = Math.min(lineCount, drawnChatLines.size)
        val lineHeight = MinecraftCompat.getFontRenderer().FONT_HEIGHT + 1

        if (y < MinecraftCompat.getFontRenderer().FONT_HEIGHT * visibleLines + visibleLines) {
            val index = y / lineHeight + scrollPos
            if (index in drawnChatLines.indices) {
                return drawnChatLines[index]
            }
        }
        return null
    }

    @JvmStatic
    fun copyChatLine(line: ChatLine, formatted: Boolean, useFullMsg: Boolean): String? {
        val config = ATHRConfig.feature ?: return null
        if (!config.chat.chatCopyEnabled) return null

        return if (useFullMsg) {
            //#if MC < 1.21
            val hook = line as? ChatLineHook
            val fullMsg = hook?.`athr$getFullMessage`()
            val src = fullMsg ?: TextCompat.getChatLineContent(line)
            val raw = TextCompat.getFormattedText(src)
            if (formatted) raw else TextCompat.getTextWithoutFormattingCodes(raw)
            //#else
            //$$ val raw = TextCompat.getFormattedText(TextCompat.getChatLineContent(line))
            //$$ if (formatted) raw else TextCompat.getTextWithoutFormattingCodes(raw)
            //#endif
        } else {
            val raw = TextCompat.getFormattedText(TextCompat.getChatLineContent(line))
            if (formatted) raw else TextCompat.getTextWithoutFormattingCodes(raw)
        }
    }

    //#if MC < 1.21
    //#else
    //$$ @JvmStatic
    //$$ fun computeHoveredVisible(
    //$$     visibles: List<Visible>, scrollPos: Int, chatScale: Float, lineCount: Int, mouseX: Int, mouseY: Int
    //$$ ) {
    //$$     hoveredLine = null
    //$$     val config = ATHRConfig.feature ?: return
    //$$     if (!config.chat.chatCopyEnabled) return
    //$$     if (MinecraftCompat.getCurrentScreen() !is ChatScreen) return
    //$$     val screen = MinecraftCompat.getCurrentScreen() as? GuiChatHook ?: return
    //$$     if (!screen.`athr$isTypingMode`()) return
    //$$     var y = (GuiScreenUtils.getScaledWindowHeight() - 27) - mouseY
    //$$     y = TextCompat.floor(y.toFloat() / chatScale)
    //$$     if (y < 0) return
    //$$
    //$$     val visibleLines = Math.min(lineCount, visibles.size)
    //$$     val lineHeight = MinecraftCompat.getFontRenderer().fontHeight + 1
    //$$
    //$$     if (y < MinecraftCompat.getFontRenderer().fontHeight * visibleLines + visibleLines) {
    //$$         val index = y / lineHeight + scrollPos
    //$$         if (index in visibles.indices) {
    //$$             val hook = visibles[index] as? ChatLineHook
    //$$             if (hook != null) hoveredLine = hook.`athr$getParentLine`()
    //$$         }
    //$$     }
    //$$ }
    //#endif
}
