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
    TextCompat.getFormattedText(this.chatComponent)
    //#else
    //$$ TextCompat.getFormattedText(this.content)
    //#endif
