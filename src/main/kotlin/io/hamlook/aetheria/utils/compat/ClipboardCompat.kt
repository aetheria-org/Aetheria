package io.hamlook.aetheria.utils.compat

//#if MC < 1.16
import net.minecraft.client.gui.GuiScreen
//#endif

object ClipboardCompat {
    @JvmStatic
    fun setClipboard(text: String) {
        //#if MC < 1.16
        GuiScreen.setClipboardString(text)
        //#else
        //$$ MinecraftCompat.getMinecraft().keyboard.setClipboard(text)
        //#endif
    }

    @JvmStatic
    fun getClipboard(): String =
        //#if MC < 1.16
        GuiScreen.getClipboardString()
        //#else
        //$$ MinecraftCompat.getMinecraft().keyboard.getClipboard()
        //#endif
}
