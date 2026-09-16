package io.hamlook.aetheria.utils.compat
//#if MC < 1.16
import net.minecraft.util.ChatComponentText
//#else
//$$ import net.minecraft.network.chat.Component
//#endif
import net.minecraft.util.IChatComponent

class Text private constructor(val text: String) {

    companion object {
        @JvmStatic
        fun of(string: String): Text = Text(string)
    }

    override fun toString(): String = this.text

    val string get() = this.text

    fun toComponent(): IChatComponent =
        //#if MC < 1.16
        ChatComponentText(text)
        //#else
        //$$ Component.literal(text)
        //#endif

    fun append(string: String): Text = Text(this.text + string)

    fun append(newText: Text): Text = Text(this.text + newText.text)
}
