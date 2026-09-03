package io.hamlook.aetheria.utils.compat

import org.lwjgl.input.Keyboard

object KeyboardCompat {
    @JvmStatic fun isKeyDown(key: Int): Boolean = Keyboard.isKeyDown(key)
    @JvmStatic fun getEventKey(): Int = Keyboard.getEventKey()
    @JvmStatic fun getEventKeyState(): Boolean = Keyboard.getEventKeyState()
    @JvmStatic fun getKeyName(key: Int): String = Keyboard.getKeyName(key)
    @JvmStatic fun getEventCharacter(): Char = Keyboard.getEventCharacter()
    @JvmStatic fun next(): Boolean = Keyboard.next()
    @JvmStatic fun isRepeatEvent(): Boolean = Keyboard.isRepeatEvent()
    @JvmStatic fun enableRepeatEvents(enable: Boolean) = Keyboard.enableRepeatEvents(enable)
}
