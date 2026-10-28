package io.hamlook.aetheria.utils.hooks

import io.hamlook.aetheria.api.event.HandleEvent
import io.hamlook.aetheria.core.ATHRConfig
import io.hamlook.aetheria.core.features.chat.PlayerButtonsConfig
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour
import io.hamlook.aetheria.events.ASMGuiOpenEvent
import io.hamlook.aetheria.features.chat.PlayerNameButtonListener
import io.hamlook.aetheria.features.profile.viewer.ui.ProfileViewerGUI
import io.hamlook.aetheria.init.RegisterEvents
import io.hamlook.aetheria.utils.chat.ChatUtils
import io.hamlook.aetheria.utils.compat.DrawContextUtils
import io.hamlook.aetheria.utils.compat.MinecraftCompat
import io.hamlook.aetheria.utils.compat.MouseCompat
import io.hamlook.aetheria.utils.compat.TextCompat
import io.hamlook.aetheria.utils.render.MarqueeUtils
import io.hamlook.aetheria.utils.render.RenderUtils
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.gui.GuiTextField
import org.lwjgl.input.Keyboard
//#if MC < 1.21
//#else
//$$ import org.lwjgl.glfw.GLFW as Glfw
//#endif
import kotlin.math.max
import kotlin.math.min

@RegisterEvents
object PlayerButtonHook {

    private enum class MenuAction(val label: String, val command: String?, val special: Special?) {
        WHISPER("Whisper", "/msg {player}", Special.MESSAGE),
        VIEW_PROFILE("View Profile", "/viewprofile {player}", Special.VIEW_PROFILE),
        AUCTION_HOUSE("Auction House", "/ah {player}", null),
        PARTY_INVITE("Party Invite", "/p invite {player}", null),
        ADD_FRIEND("Add Friend", "/f add {player}", null),
        IGNORE("Ignore", "/ignore add {player}", null),
        VISIT_ISLAND("Visit Island", "/visit {player}", null),
        VISIT_GARDEN("Visit Garden", "/visitgarden {player}", null);

        enum class Special { MESSAGE, VIEW_PROFILE }
    }

    private class MenuLayout(screenWidth: Int, screenHeight: Int) {
        val left = menuLeft(screenWidth)
        val top = menuTop(screenHeight)
        val headerHeight = MENU_HEADER_H
        val rowHeight = MENU_ROW_H
        val height = headerHeight + MenuAction.entries.size * rowHeight + 4
        val cogLeft = left + MENU_WIDTH - headerHeight

        fun rowTop(index: Int) = top + headerHeight + 2 + index * rowHeight
    }

    private const val MENU_ROW_H = 14
    private const val MENU_HEADER_H = 14
    private const val MENU_WIDTH = 110
    private const val MENU_Z = 300f

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
        ChatHook.clearHover()
    }

    @HandleEvent
    fun onGuiOpen(event: ASMGuiOpenEvent) {
        closeMenu()
    }

    @JvmStatic
    fun menuHeight(): Int = MENU_HEADER_H + MenuAction.entries.size * MENU_ROW_H + 4

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

        val layout = MenuLayout(screenWidth, screenHeight)
        val bx = layout.left
        val by = layout.top
        val boxH = layout.height
        val cogX = layout.cogLeft

        val bg = menuBackgroundColor()
        val bgAlpha = (bg ushr 24) and 0xFF
        val bgFade = ((bgAlpha / 2) shl 24) or (bg and 0x00FFFFFF)
        val accent = menuAccentColor()
        val hoverColor = (accent and 0x00FFFFFF) or 0x80000000.toInt()

        //#if MC >= 1.21
        //$$ DrawContextUtils.flushIfPresent()
        //#endif
        DrawContextUtils.pushPop {
            DrawContextUtils.translate(0f, 0f, MENU_Z)

            RenderUtils.drawGradientRect(0, bx, by, bx + MENU_WIDTH, by + boxH, bg, bgFade)
            DrawContextUtils.fill(bx, by, bx + MENU_WIDTH, by + MENU_HEADER_H, accent)
            MarqueeUtils.drawCentered(
                fr,
                player,
                bx + (MENU_WIDTH - MENU_HEADER_H) / 2,
                by + 3,
                MENU_WIDTH - MENU_HEADER_H - 4,
                0xFFFFFFFF.toInt(),
                true
            )

            val cogHovered = mouseX in cogX..bx + MENU_WIDTH && mouseY in by until by + MENU_HEADER_H
            if (cogHovered) DrawContextUtils.fill(cogX, by, bx + MENU_WIDTH, by + MENU_HEADER_H, hoverColor)
            DrawContextUtils.drawTextWithShadow(
                fr, "\u2699",
                cogX + MENU_HEADER_H / 2 - DrawContextUtils.getStringWidth(fr, "\u2699") / 2,
                by + 3,
                0xFFFFFFFF.toInt()
            )

            for (i in MenuAction.entries.indices) {
                val ry = layout.rowTop(i)
                val hovered = mouseX in bx..bx + MENU_WIDTH && mouseY in ry until ry + MENU_ROW_H
                if (hovered) DrawContextUtils.fill(bx + 1, ry, bx + MENU_WIDTH - 1, ry + MENU_ROW_H, hoverColor)
                DrawContextUtils.drawTextWithShadow(
                    fr, MenuAction.entries[i].label,
                    bx + 6,
                    ry + 3,
                    if (hovered) 0xFFFFFFFF.toInt() else 0xFFB5BAC1.toInt()
                )
            }
        }
    }

    @JvmStatic
    fun handleKeyTyped(keyCode: Int): Boolean {
        //#if MC < 1.21
        if (menuPlayer != null && keyCode == Keyboard.KEY_ESCAPE) {
        //#else
        //$$ if (menuPlayer != null && keyCode == Glfw.GLFW_KEY_ESCAPE) {
        //#endif
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
                val layout = MenuLayout(screenWidth, screenHeight)
                val bx = layout.left
                val by = layout.top
                val cogX = layout.cogLeft

                if (mouseX in cogX..bx + MENU_WIDTH && mouseY in by until by + layout.headerHeight) {
                    ATHRConfig.openSubcategory("Chat Utils", "playerButtons")
                } else {
                    for (i in MenuAction.entries.indices) {
                        val ry = layout.rowTop(i)
                        if (mouseX in bx..bx + MENU_WIDTH && mouseY in ry until ry + MENU_ROW_H) {
                            executeAction(MenuAction.entries[i], menuPlayer!!, inputField)
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

        //#if MC < 1.16
        val comp = MinecraftCompat.getMinecraft().ingameGUI.chatGUI
            .getChatComponent(MouseCompat.getX(), MouseCompat.getY()) ?: return false
        val style = TextCompat.getChatStyle(comp)
        //#else
        //$$ val style = MinecraftCompat.getMinecraft().inGameHud.chatHud
        //$$     .getTextStyleAt(MouseCompat.getX().toDouble(), MouseCompat.getY().toDouble()) ?: return false
        //#endif
        if (!TextCompat.isClickAction(style, "SUGGEST_COMMAND")) return false

        val value = TextCompat.getClickValue(style) ?: return false
        if (!value.startsWith(PlayerNameButtonListener.MARKER_PREFIX)) return false

        val player = value.substring(PlayerNameButtonListener.MARKER_PREFIX.length)
        openMenu(player, mouseX, mouseY)
        return true
    }

    private fun executeAction(action: MenuAction, name: String, inputField: GuiTextField) {
        when (action.special) {
            MenuAction.Special.MESSAGE -> {
                inputField.text = action.command!!.replace("{player}", name)
                inputField.isFocused = true
            }

            MenuAction.Special.VIEW_PROFILE -> {
                if (ATHRConfig.feature == null || ATHRConfig.feature.chat.playerButtons.useIngameViewProfile) {
                    sendCommand(action, name)
                } else {
                    ATHRConfig.screenToOpen = ProfileViewerGUI(name)
                }
            }

            null -> sendCommand(action, name)
        }
    }

    private fun sendCommand(action: MenuAction, name: String) {
        val command = action.command ?: return
        ChatUtils.sendChatCommand(command.replace("{player}", name))
    }
}
