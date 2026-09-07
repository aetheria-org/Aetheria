package io.hamlook.aetheria.utils.hooks

import io.hamlook.aetheria.core.ATHRConfig
import io.hamlook.aetheria.features.chat.ChatLineHook
import io.hamlook.aetheria.features.chat.GuiChatHook
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat
import io.hamlook.aetheria.utils.compat.GuiScreenUtils
import io.hamlook.aetheria.utils.compat.MinecraftCompat
import net.minecraft.client.gui.ChatLine
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.util.EnumChatFormatting
import net.minecraft.util.MathHelper

object ChatHook {

    @JvmStatic
    var renderLine: ChatLine? = null
    @JvmStatic
    var hoveredLine: ChatLine? = null
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

        Gui.drawRect(left, top, newRight, bottom, newColor)
        return newColor
    }

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

        Gui.drawScaledCustomSizeModalRect(
            x.toInt(), (y - 1f).toInt(), 8f, 8f, 8, 8, 8, 8, 64f, 64f
        )
        Gui.drawScaledCustomSizeModalRect(
            x.toInt(), (y - 1f).toInt(), 40f, 8f, 8, 8, 8, 8, 64f, 64f
        )

        GlStateManagerCompat.color(1.0f, 1.0f, 1.0f, 1.0f)
        return x + 10f
    }

    @JvmStatic
    fun offsetClickX(mouseX: Int): Int {
        val config = ATHRConfig.feature ?: return mouseX
        return if (config.chat.chatHeads) mouseX - 10 else mouseX
    }

    @JvmStatic
    fun getHoveredChatLine(
        drawnChatLines: List<ChatLine>, scrollPos: Int, chatScale: Float, lineCount: Int, rawMouseX: Int, rawMouseY: Int
    ): ChatLine? {
        val sr: ScaledResolution = GuiScreenUtils.getScaledResolution()
        val scaleFactor = sr.scaleFactor
        val mouseY = rawMouseY / scaleFactor
        var y = (sr.scaledHeight - 27) - mouseY
        y = MathHelper.floor_float(y.toFloat() / chatScale)
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
            val hook = line as? ChatLineHook
            val fullMsg = hook?.`athr$getFullMessage`()
            val src = fullMsg ?: line.chatComponent
            val raw = src.formattedText
            if (formatted) raw else EnumChatFormatting.getTextWithoutFormattingCodes(raw)
        } else {
            val raw = line.chatComponent.formattedText
            if (formatted) raw else EnumChatFormatting.getTextWithoutFormattingCodes(raw)
        }
    }
}
