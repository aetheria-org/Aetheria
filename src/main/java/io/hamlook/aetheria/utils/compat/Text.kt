package io.hamlook.aetheria.utils.compat

import net.minecraft.util.ChatComponentText
import net.minecraft.util.IChatComponent

class Text private constructor(val text: String) {

    companion object {
        @JvmStatic
        fun of(string: String): Text = Text(string)
    }

    override fun toString(): String = this.text

    val string get() = this.text

    fun toComponent(): IChatComponent = ChatComponentText(text)

    fun append(string: String): Text = Text(this.text + string)

    fun append(newText: Text): Text = Text(this.text + newText.text)
}
