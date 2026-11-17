package io.hamlook.aetheria.utils.compat

import io.hamlook.aetheria.features.chat.GuiNewChatHook

object ChatHudCompat {

    @JvmStatic
    fun rebuildChatView() {
        //#if MC < 1.21
        val chat = MinecraftCompat.getIngameGui().getChatGUI()
        //#else
        //$$ val chat = MinecraftCompat.getIngameGui().getChatHud()
        //#endif
        val hook = chat as? GuiNewChatHook ?: return
        hook.`athr$refreshView`()
    }
}
