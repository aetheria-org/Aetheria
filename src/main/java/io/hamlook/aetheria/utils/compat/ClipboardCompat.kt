package io.hamlook.aetheria.utils.compat

import net.minecraft.client.gui.GuiScreen

object ClipboardCompat {
    @JvmStatic
    fun setClipboard(text: String) {
        GuiScreen.setClipboardString(text)
    }

    @JvmStatic
    fun getClipboard(): String = GuiScreen.getClipboardString()
}
