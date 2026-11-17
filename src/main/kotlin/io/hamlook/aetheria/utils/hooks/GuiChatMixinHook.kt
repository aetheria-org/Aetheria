package io.hamlook.aetheria.utils.hooks

import io.hamlook.aetheria.core.ATHRConfig
import io.hamlook.aetheria.features.chat.GuiNewChatHook
import io.hamlook.aetheria.utils.compat.ClipboardCompat
import io.hamlook.aetheria.utils.compat.KeyboardCompat
import io.hamlook.aetheria.utils.compat.MinecraftCompat
import org.lwjgl.input.Keyboard

object GuiChatMixinHook {

    @JvmStatic
    fun handleChatCopy(mouseButton: Int): Boolean {
        //#if MC < 1.21
        val config = ATHRConfig.feature ?: return false
        if (!config.chat.copy.chatCopyEnabled) return false
        if (mouseButton != 0) return false
        if (!KeyboardCompat.isKeyDown(Keyboard.KEY_LSHIFT) && !KeyboardCompat.isKeyDown(Keyboard.KEY_LCONTROL)) return false

        val chatGUI = MinecraftCompat.getIngameGui().chatGUI ?: return false
        val hook = chatGUI as? GuiNewChatHook ?: return false
        val line = hook.`athr$getCurrentHoveredLine`() ?: return false

        val formatted = config.chat.copy.chatCopyFormatted
        val useFullMsg = KeyboardCompat.isKeyDown(Keyboard.KEY_LCONTROL)
        val text = ChatHook.copyChatLine(line, formatted, useFullMsg) ?: return false

        ClipboardCompat.setClipboard(text)
        return true
        //#else
        //$$ val config = ATHRConfig.feature ?: return false
        //$$ if (!config.chat.copy.chatCopyEnabled) return false
        //$$ if (mouseButton != 0) return false
        //$$ if (!KeyboardCompat.isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT) && !KeyboardCompat.isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL)) return false
        //$$ val hud = MinecraftCompat.getIngameGui().chatHud ?: return false
        //$$ val hook = hud as? GuiNewChatHook ?: return false
        //$$ val line = hook.`athr$getCurrentHoveredLine`() ?: return false
        //$$ val formatted = config.chat.copy.chatCopyFormatted
        //$$ val useFullMsg = KeyboardCompat.isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL)
        //$$ val text = ChatHook.copyChatLine(line, formatted, useFullMsg) ?: return false
        //$$ ClipboardCompat.setClipboard(text)
        //$$ return true
        //#endif
    }
}
