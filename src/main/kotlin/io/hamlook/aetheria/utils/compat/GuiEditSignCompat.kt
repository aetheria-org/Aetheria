package io.hamlook.aetheria.utils.compat

import net.minecraft.client.gui.inventory.GuiEditSign
import net.minecraft.util.IChatComponent
//#if MC < 1.16
import io.hamlook.aetheria.mixins.accessors.GuiEditSignAccessor
//#endif
//#if MC > 1.21
//$$ import io.hamlook.aetheria.mixins.accessors.SignEditScreenAccessor
//#endif

typealias GuiEditSignCompat = GuiEditSign

//#if MC < 1.16
fun GuiEditSignCompat.getSignTextCompat(): Array<IChatComponent> {
    val accessor = this as GuiEditSignAccessor
    return accessor.`ATHR$getTileSign`().signText
}

fun GuiEditSignCompat.getSignLineCompat(index: Int): String =
    getSignTextCompat()[index].unformattedText
//#else
//$$ fun GuiEditSignCompat.getSignLineCompat(index: Int): String {
//$$     val accessor = this as SignEditScreenAccessor
//$$     return TextCompat.getUnformattedText(accessor.`ATHR$getBlockEntity`().getFrontText().getMessage(index, false))
//$$ }
//#endif
