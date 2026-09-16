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
        val config = ATHRConfig.feature ?: return false
        if (!config.chat.chatCopyEnabled) return false
        if (mouseButton != 0) return false
        if (!KeyboardCompat.isKeyDown(Keyboard.KEY_LSHIFT) && !KeyboardCompat.isKeyDown(Keyboard.KEY_LCONTROL)) return false

        val chatGUI = MinecraftCompat.getIngameGui().chatGUI ?: return false
        val hook = chatGUI as? GuiNewChatHook ?: return false
        val line = hook.`athr$getCurrentHoveredLine`() ?: return false

        val formatted = config.chat.chatCopyFormatted
        val useFullMsg = KeyboardCompat.isKeyDown(Keyboard.KEY_LCONTROL)
        val text = ChatHook.copyChatLine(line, formatted, useFullMsg) ?: return false

        ClipboardCompat.setClipboard(text)
        return true
    }
}
