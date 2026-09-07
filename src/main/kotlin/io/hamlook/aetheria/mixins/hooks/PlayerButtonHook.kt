package io.hamlook.aetheria.mixins.hooks

import io.hamlook.aetheria.core.ATHRConfig
import io.hamlook.aetheria.core.features.chat.PlayerButtonsConfig
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour
import io.hamlook.aetheria.features.chat.PlayerNameButtonListener
import io.hamlook.aetheria.features.profile.viewer.ui.ProfileViewerGUI
import io.hamlook.aetheria.utils.chat.ChatUtils
import io.hamlook.aetheria.utils.compat.MinecraftCompat
import io.hamlook.aetheria.utils.compat.MouseCompat
import io.hamlook.aetheria.utils.compat.TextCompat
import io.hamlook.aetheria.utils.render.RenderUtils
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiTextField
import net.minecraft.event.ClickEvent
import net.minecraft.util.IChatComponent
import org.lwjgl.input.Keyboard
import kotlin.math.max
import kotlin.math.min

object PlayerButtonHook {

    private val MENU_LABELS = arrayOf(
        "Whisper",
        "View Profile",
        "Auction House",
        "Party Invite",
        "Add Friend",
        "Ignore",
        "Visit Island",
        "Visit Garden"
    )
    private const val MENU_PASTE_INDEX = 0
    private const val MENU_VIEW_PROFILE_INDEX = 1
    private const val MENU_ROW_H = 14
    private const val MENU_HEADER_H = 14
    private const val MENU_WIDTH = 110

    @JvmStatic
    var menuPlayer: String? = null
        private set

    private var menuX = 0
    private var menuY = 0

    @JvmStatic
    fun isMenuOpen(): Boolean = menuPlayer != null

    @JvmStatic
    fun openMenu(player: String, x: Int, y: Int) {
        menuPlayer = player
        menuX = x
        menuY = y
    }

    @JvmStatic
    fun closeMenu() {
        menuPlayer = null
    }

    @JvmStatic
    fun menuHeight(): Int = MENU_HEADER_H + MENU_LABELS.size * MENU_ROW_H + 4

    @JvmStatic
    fun menuLeft(screenWidth: Int): Int = max(2, min(menuX, screenWidth - MENU_WIDTH - 2))

    @JvmStatic
    fun menuTop(screenHeight: Int): Int = max(2, min(menuY, screenHeight - menuHeight() - 2))

    @JvmStatic
    fun cogLeft(screenWidth: Int): Int = menuLeft(screenWidth) + MENU_WIDTH - MENU_HEADER_H

    @JvmStatic
    fun menuBackgroundColor(): Int {
        val color = if (ATHRConfig.feature != null) ATHRConfig.feature.chat.playerButtons.backgroundColor
        else PlayerButtonsConfig.DEFAULT_BACKGROUND_COLOR
        return ChromaColour.specialToChromaRGB(color)
    }

    @JvmStatic
    fun menuAccentColor(): Int {
        val color = if (ATHRConfig.feature != null) ATHRConfig.feature.chat.playerButtons.accentColor
        else PlayerButtonsConfig.DEFAULT_ACCENT_COLOR
        return ChromaColour.specialToChromaRGB(color)
    }

    @JvmStatic
    fun drawMenu(mouseX: Int, mouseY: Int, screenWidth: Int, screenHeight: Int, fr: FontRenderer) {
        val player = menuPlayer ?: return

        val bx = menuLeft(screenWidth)
        val by = menuTop(screenHeight)
        val boxH = menuHeight()
        val cogX = cogLeft(screenWidth)

        val bg = menuBackgroundColor()
        val bgAlpha = (bg ushr 24) and 0xFF
        val bgFade = ((bgAlpha / 2) shl 24) or (bg and 0x00FFFFFF)
        val accent = menuAccentColor()
        val hoverColor = (accent and 0x00FFFFFF) or 0x80000000.toInt()

        RenderUtils.drawGradientRect(0, bx, by, bx + MENU_WIDTH, by + boxH, bg, bgFade)
        Gui.drawRect(bx, by, bx + MENU_WIDTH, by + MENU_HEADER_H, accent)
        fr.drawStringWithShadow(
            player,
            (bx + (MENU_WIDTH - fr.getStringWidth(player)) / 2).toFloat(),
            (by + 3).toFloat(),
            0xFFFFFFFF.toInt()
        )

        val cogHovered = mouseX in cogX..bx + MENU_WIDTH && mouseY in by until by + MENU_HEADER_H
        if (cogHovered) Gui.drawRect(cogX, by, bx + MENU_WIDTH, by + MENU_HEADER_H, hoverColor)
        fr.drawStringWithShadow(
            "\u2699",
            (cogX + MENU_HEADER_H / 2 - fr.getStringWidth("\u2699") / 2).toFloat(),
            (by + 3).toFloat(),
            0xFFFFFFFF.toInt()
        )

        for (i in MENU_LABELS.indices) {
            val ry = by + MENU_HEADER_H + 2 + i * MENU_ROW_H
            val hovered = mouseX in bx..bx + MENU_WIDTH && mouseY in ry until ry + MENU_ROW_H
            if (hovered) Gui.drawRect(bx + 1, ry, bx + MENU_WIDTH - 1, ry + MENU_ROW_H, hoverColor)
            fr.drawStringWithShadow(
                MENU_LABELS[i],
                (bx + 6).toFloat(),
                (ry + 3).toFloat(),
                if (hovered) 0xFFFFFFFF.toInt() else 0xFFB5BAC1.toInt()
            )
        }
    }

    @JvmStatic
    fun handleKeyTyped(keyCode: Int): Boolean {
        if (menuPlayer != null && keyCode == Keyboard.KEY_ESCAPE) {
            menuPlayer = null
            return true
        }
        return false
    }

    @JvmStatic
    fun handlePlayerMenuClick(
        mouseX: Int,
        mouseY: Int,
        mouseButton: Int,
        screenWidth: Int,
        screenHeight: Int,
        inputField: GuiTextField
    ): Boolean {
        if (menuPlayer != null) {
            if (mouseButton == 0) {
                val bx = menuLeft(screenWidth)
                val by = menuTop(screenHeight)
                val cogX = cogLeft(screenWidth)

                if (mouseX in cogX..bx + MENU_WIDTH && mouseY in by until by + MENU_HEADER_H) {
                    ATHRConfig.openSubcategory("Chat Utils", "playerButtons")
                } else {
                    for (i in MENU_LABELS.indices) {
                        val ry = by + MENU_HEADER_H + 2 + i * MENU_ROW_H
                        if (mouseX in bx..bx + MENU_WIDTH && mouseY in ry until ry + MENU_ROW_H) {
                            executeAction(i, menuPlayer!!, inputField)
                            break
                        }
                    }
                }
            }
            menuPlayer = null
            return true
        }

        if (ATHRConfig.feature == null || !ATHRConfig.feature.chat.playerButtons.enabled) return false
        if (mouseButton != 0) return false

        val comp = MinecraftCompat.getMinecraft().ingameGUI.chatGUI
            .getChatComponent(MouseCompat.getX(), MouseCompat.getY()) ?: return false

        val style = TextCompat.getChatStyle(comp)
        val click = TextCompat.getClickEvent(style) ?: return false
        if (click.action != ClickEvent.Action.SUGGEST_COMMAND) return false

        val value = click.value ?: return false
        if (!value.startsWith(PlayerNameButtonListener.MARKER_PREFIX)) return false

        val player = value.substring(PlayerNameButtonListener.MARKER_PREFIX.length)
        openMenu(player, mouseX, mouseY)
        return true
    }

    private fun executeAction(index: Int, name: String, inputField: GuiTextField) {
        when (index) {
            MENU_VIEW_PROFILE_INDEX -> {
                if (ATHRConfig.feature == null || ATHRConfig.feature.chat.playerButtons.useIngameViewProfile) {
                    sendCommand(index, name)
                } else {
                    ATHRConfig.screenToOpen = ProfileViewerGUI(name)
                }
            }

            MENU_PASTE_INDEX -> {
                inputField.text = commandFor(index, name)
                inputField.isFocused = true
            }

            else -> sendCommand(index, name)
        }
    }

    private fun sendCommand(index: Int, name: String) {
        val cmd = commandFor(index, name) ?: return
        ChatUtils.sendChatCommand(cmd)
    }

    @JvmStatic
    fun commandFor(index: Int, name: String): String? {
        return when (index) {
            0 -> "/msg $name"
            1 -> "/viewprofile $name"
            2 -> "/ah $name"
            3 -> "/p invite $name"
            4 -> "/f add $name"
            5 -> "/ignore add $name"
            6 -> "/visit $name"
            7 -> "/visitgarden $name"
            else -> null
        }
    }

    @JvmStatic
    fun wrapIgn(root: IChatComponent, ign: String): Boolean {
        val siblings = root.siblings
        for (i in siblings.indices) {
            val sib = siblings[i]
            val sibText = TextCompat.getUnformattedTextForChat(sib)

            val idx = sibText.indexOf(ign, ignoreCase = true)
            if (idx != -1) {
                val namePrefix = activeFormatState(sibText, idx)
                val afterPrefix = activeFormatState(sibText, idx + ign.length)

                val before = sibText.substring(0, idx)
                val name = namePrefix + sibText.substring(idx, idx + ign.length)
                val after = afterPrefix + sibText.substring(idx + ign.length)

                val baseStyle = TextCompat.getChatStyle(sib)

                val beforeComp = TextCompat.createText(before)
                TextCompat.setChatStyle(beforeComp, baseStyle.createDeepCopy())

                val nameComp = TextCompat.createText(name)
                val nameStyle = baseStyle.createDeepCopy()
                nameStyle.underlined = true
                TextCompat.setClickSuggestCommand(nameStyle, PlayerNameButtonListener.MARKER_PREFIX + ign)
                TextCompat.setHoverShowText(nameStyle, "\u00a7eClick for player options")
                TextCompat.setChatStyle(nameComp, nameStyle)

                val afterComp = TextCompat.createText(after)
                TextCompat.setChatStyle(afterComp, baseStyle.createDeepCopy())
                for (child in sib.siblings) {
                    afterComp.appendSibling(child)
                }

                siblings[i] = beforeComp
                siblings.add(i + 1, nameComp)
                siblings.add(i + 2, afterComp)
                return true
            }

            if (wrapIgn(sib, ign)) return true
        }
        return false
    }

    private fun activeFormatState(text: String, endExclusive: Int): String {
        var color: Char? = null
        var bold = false
        var italic = false
        var underline = false
        var strikethrough = false
        var obfuscated = false

        val limit = min(endExclusive, text.length) - 1
        var i = 0
        while (i < limit) {
            if (text[i] != '\u00a7') {
                i++; continue
            }
            when (val code = Character.toLowerCase(text[i + 1])) {
                'r' -> {
                    color = null; bold = false; italic = false; underline = false; strikethrough = false; obfuscated =
                        false
                }

                in "0123456789abcdef" -> {
                    color = code; bold = false; italic = false; underline = false; strikethrough = false; obfuscated =
                        false
                }

                'l' -> bold = true
                'o' -> italic = true
                'n' -> underline = true
                'm' -> strikethrough = true
                'k' -> obfuscated = true
            }
            i += 2
        }

        return buildString {
            if (color != null) append('\u00a7').append(color)
            if (bold) append("\u00a7l")
            if (strikethrough) append("\u00a7m")
            if (underline) append("\u00a7n")
            if (italic) append("\u00a7o")
            if (obfuscated) append("\u00a7k")
        }
    }
}
