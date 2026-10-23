package io.hamlook.aetheria.utils.compat

import net.minecraft.client.gui.ChatLine

typealias ChatLineCompat = ChatLine

fun ChatLineCompat.getChatComponentCompat(): Any =
    //#if MC < 1.16
    this.chatComponent
    //#else
    //$$ this.content
    //#endif

fun ChatLineCompat.getChatStringCompat(): String =
    //#if MC < 1.16
    TextCompat.getFormattedTextWithResets(this.chatComponent)
    //#else
    //$$ TextCompat.getFormattedTextWithResets(this.content)
    //#endif
