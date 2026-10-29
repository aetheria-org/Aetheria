package io.hamlook.aetheria.utils.hooks

import io.hamlook.aetheria.core.ATHRConfig
import io.hamlook.aetheria.features.chat.ChatLineHook
import io.hamlook.aetheria.features.chat.GuiChatHook
import io.hamlook.aetheria.features.chat.PlayerNameButtonListener
import io.hamlook.aetheria.utils.compat.DrawContextUtils
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat
import io.hamlook.aetheria.utils.compat.GuiScreenUtils
import io.hamlook.aetheria.utils.compat.MinecraftCompat
import io.hamlook.aetheria.utils.compat.TextCompat
import io.hamlook.aetheria.utils.chat.ChatHoverUnderline
import net.minecraft.client.gui.ChatLine
import net.minecraft.client.gui.GuiChat
import net.minecraft.util.IChatComponent
//#if MC >= 1.21
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
    var hoveredVisible: Any? = null
    @JvmStatic
    var animationStart: Long = 0L

    private var hoveredNameClickValue: String? = null

    @JvmStatic
    fun clearHover() {
        hoveredLine = null
        hoveredVisible = null
        hoveredNameClickValue = null
    }

    @JvmStatic
    fun getHoveredNameClickValue(): String? = hoveredNameClickValue

    @JvmStatic
    fun applyAnimation(updateCounter: Int) {
        val config = ATHRConfig.feature ?: return
        if (!config.chat.appearance.animatedChat || animationStart == 0L) return

        val speed = 20.0
        //#if MC < 1.21
        val lineHeight = 9.0 * MinecraftCompat.getMinecraft().gameSettings.chatScale
        //#else
        //$$ val options = MinecraftCompat.getMinecraft().options
        //$$ val spacing = options.chatLineSpacing.value as? Double ?: 0.0
        //$$ val chatScale = options.chatScale.value as? Double ?: 1.0
        //$$ val lineHeight = 9.0 * (spacing + 1.0) * chatScale
        //#endif
        var shift = (System.currentTimeMillis().toDouble() - lineHeight * speed - animationStart) / speed
        if (shift > 0.0) shift = 0.0
        GlStateManagerCompat.translate(0.0, -shift, 0.0)
    }

    @JvmStatic
    fun computeHoveredLine(
        drawnChatLines: List<ChatLine>, scrollPos: Int, chatScale: Float, lineCount: Int, mouseX: Int, mouseY: Int
    ) {
        hoveredLine = null
        renderLine = null
        hoveredNameClickValue = null
        val config = ATHRConfig.feature ?: return
        if (!config.chat.copy.chatCopyEnabled && !config.chat.playerButtons.enabled) return
        if (MinecraftCompat.getCurrentScreen() !is GuiChat) return
        val screen = MinecraftCompat.getCurrentScreen() as? GuiChatHook ?: return
        if (!screen.`athr$isTypingMode`()) return
        // keep hover effects off the chat while the player menu sits under the cursor
        if (PlayerButtonHook.isMouseOverMenu(
                mouseX / GuiScreenUtils.getScaleFactor(),
                GuiScreenUtils.getScaledWindowHeight() - mouseY / GuiScreenUtils.getScaleFactor(),
                GuiScreenUtils.getScaledWindowWidth(), GuiScreenUtils.getScaledWindowHeight())) return
        hoveredLine = getHoveredChatLine(drawnChatLines, scrollPos, chatScale, lineCount, mouseX, mouseY)

        //#if MC < 1.21
        val leaf = MinecraftCompat.getIngameGui().chatGUI.getChatComponent(mouseX, mouseY)
        val click = leaf?.let { TextCompat.getClickValue(TextCompat.getChatStyle(it)) }
        hoveredNameClickValue = click?.takeIf { it.startsWith(PlayerNameButtonListener.MARKER_PREFIX) }
        //#endif
    }

    @JvmStatic
    fun shouldResetAnimation(refresh: Boolean): Boolean {
        val config = ATHRConfig.feature ?: return false
        if (config.chat.appearance.animatedChat && !refresh) {
            animationStart = System.currentTimeMillis()
            return true
        }
        return false
    }

    @JvmStatic
    fun scaleChatAlpha(color: Int, opacity: Int): Int {
        val alpha = (color ushr 24) * opacity / 100
        return (alpha shl 24) or (color and 0xFFFFFF)
    }

    @JvmStatic
    fun getBackgroundColor(left: Int, top: Int, right: Int, bottom: Int, color: Int, chatOpen: Boolean): Int {
        val config = ATHRConfig.feature ?: return color

        val newRight = if (config.chat.heads.chatHeads) right + 10 else right
        val opacity = config.chat.appearance.chatBackgroundOpacity
        var newColor = scaleChatAlpha(color, opacity)

        if (config.chat.copy.chatCopyEnabled && chatOpen && hoveredLine != null && renderLine == hoveredLine) {
            newColor = if (opacity < 100) 0x22AAAACC else 0x60AAAACC
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
        if (renderLine !is ChatLineHook) return x

        var textX = x
        if (config.chat.heads.chatHeads) {
            val hook = renderLine as ChatLineHook
            val info = hook.`athr$getPlayerInfo`()
            if (info == null) {
                if (hook.`athr$hasDetected`() || config.chat.heads.offsetNonPlayerMessages) {
                    textX = x + 10f
                }
            } else {
                val alpha = (color shr 24) and 0xFF
                val headAlpha = if (alpha == 0) 1.0f else alpha / 255f

                GlStateManagerCompat.enableBlend()
                GlStateManagerCompat.enableAlpha()
                GlStateManagerCompat.enableTexture2D()
                GlStateManagerCompat.tryBlendFuncSeparate(770, 771, 1, 0)
                GlStateManagerCompat.color(1.0f, 1.0f, 1.0f, headAlpha)

                DrawContextUtils.drawScaledCustomSizeModalRect(
                    info.locationSkin, x.toInt(), (y - 1f).toInt(), 8f, 8f, 8, 8, 8f, 8f, 64f, 64f
                )
                DrawContextUtils.drawScaledCustomSizeModalRect(
                    info.locationSkin, x.toInt(), (y - 1f).toInt(), 40f, 8f, 8, 8, 8f, 8f, 64f, 64f
                )

                GlStateManagerCompat.color(1.0f, 1.0f, 1.0f, 1.0f)
                textX = x + 10f
            }
        }

        return textX
    }

    @JvmStatic
    fun lineFormattedText(text: String, component: IChatComponent, line: ChatLine?): String {
        val clickValue = hoveredNameClickValue
        if (clickValue == null || line == null || line != hoveredLine) return text
        return ChatHoverUnderline.apply(text, component, clickValue)
    }
    //#else
    //$$ @JvmStatic
    //$$ fun drawChatHead(
    //$$     context: DrawContext, x: Float, y: Float, color: Int, renderLine: Any?
    //$$ ): Float {
    //$$     val config = ATHRConfig.feature ?: return x
    //$$     if (!config.chat.heads.chatHeads) return x
    //$$     if (renderLine !is ChatLineHook) return x
    //$$
    //$$     val parent = renderLine.`athr$getParentLine`() as? ChatLineHook ?: return x
    //$$     val info = parent.`athr$getPlayerInfo`() ?: run {
    //$$         if (parent.`athr$isHeadHidden`() || config.chat.heads.offsetNonPlayerMessages) {
    //$$             return x + 10f
    //$$         }
    //$$         return x
    //$$     }
    //$$     if (parent.`athr$isHeadHidden`()) return x + 10f
    //$$     if (!renderLine.`athr$isHeadRow`()) return x + 10f
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
    //$$     val newRight = if (config.chat.heads.chatHeads) right + 10 else right
    //$$     val opacity = config.chat.appearance.chatBackgroundOpacity
    //$$     var newColor = scaleChatAlpha(color, opacity)
    //$$
    //$$     if (config.chat.copy.chatCopyEnabled && chatOpen && hoveredVisible != null && renderVisible === hoveredVisible) {
    //$$         newColor = if (opacity < 100) 0x22AAAACC else 0x60AAAACC
    //$$     }
    //$$
    //$$     context.fill(left, top, newRight, bottom, newColor)
    //$$     return newColor
    //$$ }
    //#endif

    @JvmStatic
    fun offsetClickX(mouseX: Int): Int {
        val config = ATHRConfig.feature ?: return mouseX
        return if (config.chat.heads.chatHeads) mouseX - 10 else mouseX
    }

    @JvmStatic
    fun getHoveredChatLine(
        drawnChatLines: List<ChatLine>, scrollPos: Int, chatScale: Float, lineCount: Int, rawMouseX: Int, rawMouseY: Int
    ): ChatLine? {
        val scaleFactor = GuiScreenUtils.getScaleFactor()
        val mouseX = rawMouseX / scaleFactor
        //#if MC < 1.21
        val chatWidthPx = TextCompat.floor(MinecraftCompat.getMinecraft().gameSettings.chatWidth * 280f + 40f)
        //#else
        //$$ val chatWidthSetting = MinecraftCompat.getMinecraft().options.chatWidth.value as? Double ?: 1.0
        //$$ val chatWidthPx = Math.floor(chatWidthSetting * 280.0 + 40.0).toInt()
        //#endif
        val jx = TextCompat.floor((mouseX - 3) / chatScale)
        if (jx < 0 || jx > TextCompat.floor(chatWidthPx / chatScale)) return null
        val mouseY = rawMouseY / scaleFactor
        var y = mouseY - 27
        y = TextCompat.floor(y.toFloat() / chatScale)
        if (y < 0) return null

        val visibleLines = Math.min(lineCount, drawnChatLines.size)
        val fontH = MinecraftCompat.getFontRenderer().FONT_HEIGHT

        if (y < fontH * visibleLines + visibleLines) {
            val index = y / fontH + scrollPos
            if (index in drawnChatLines.indices) {
                return drawnChatLines[index]
            }
        }
        return null
    }

    @JvmStatic
    fun copyChatLine(line: ChatLine, formatted: Boolean, useFullMsg: Boolean): String? {
        val config = ATHRConfig.feature ?: return null
        if (!config.chat.copy.chatCopyEnabled) return null

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
    //$$     hoveredVisible = null
    //$$     renderVisible = null
    //$$     hoveredNameClickValue = null
    //$$     val config = ATHRConfig.feature ?: return
    //$$     if (!config.chat.copy.chatCopyEnabled && !config.chat.playerButtons.enabled) return
    //$$     if (MinecraftCompat.getCurrentScreen() !is ChatScreen) return
    //$$     val screen = MinecraftCompat.getCurrentScreen() as? GuiChatHook ?: return
    //$$     if (!screen.`athr$isTypingMode`()) return
    //$$     if (PlayerButtonHook.isMouseOverMenu(mouseX, mouseY,
    //$$             GuiScreenUtils.getScaledWindowWidth(), GuiScreenUtils.getScaledWindowHeight())) return
    //$$     if (mouseX < -4 || mouseX > TextCompat.floor(chatWidthModern() / chatScale)) return
    //$$     val lineHeight = chatLineHeightModern()
    //$$     val y = ((GuiScreenUtils.getScaledWindowHeight() - mouseY) - 40.0) / (chatScale * lineHeight)
    //$$     if (y < 0 || y >= Math.min(lineCount, visibles.size)) return
    //$$
    //$$     val index = TextCompat.floor(y.toFloat()) + scrollPos
    //$$     if (index in visibles.indices) {
    //$$         hoveredVisible = visibles[index]
    //$$         val hook = visibles[index] as? ChatLineHook
    //$$         if (hook != null) hoveredLine = hook.`athr$getParentLine`()
    //$$         val style = MinecraftCompat.getIngameGui().chatHud
    //$$             .getTextStyleAt(mouseX.toDouble(), mouseY.toDouble())
    //$$         val click = style?.clickEvent?.value
    //$$         hoveredNameClickValue = click?.takeIf {
    //$$             it.startsWith(PlayerNameButtonListener.MARKER_PREFIX)
    //$$         }
    //$$     }
    //$$ }
    //$$
    //$$ private fun chatLineHeightModern(): Int {
    //$$     val spacing = MinecraftCompat.getMinecraft().options.chatLineSpacing.value as? Double ?: 0.0
    //$$     return (9.0 * (spacing + 1.0)).toInt()
    //$$ }
    //$$
    //$$ private fun chatWidthModern(): Int {
    //$$     val width = MinecraftCompat.getMinecraft().options.chatWidth.value as? Double ?: 1.0
    //$$     return Math.floor(width * 280.0 + 40.0).toInt()
    //$$ }
    //#endif
}
