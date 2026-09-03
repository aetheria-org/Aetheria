package io.hamlook.aetheria.utils.compat

import org.lwjgl.input.Mouse

object MouseCompat {
    @JvmStatic fun isButtonDown(button: Int): Boolean = Mouse.isButtonDown(button)
    @JvmStatic fun getX(): Int = Mouse.getX()
    @JvmStatic fun getY(): Int = Mouse.getY()
    @JvmStatic fun getEventButtonState(): Boolean = Mouse.getEventButtonState()
    @JvmStatic fun getEventButton(): Int = Mouse.getEventButton()
    @JvmStatic fun getDWheel(): Int = Mouse.getDWheel()
    @JvmStatic fun getEventDWheel(): Int = Mouse.getEventDWheel()
    @JvmStatic fun getEventX(): Int = Mouse.getEventX()
    @JvmStatic fun getEventY(): Int = Mouse.getEventY()
    @JvmStatic fun getEventDY(): Int = Mouse.getEventDY()
    @JvmStatic fun getScrollDelta(): Int = Mouse.getEventDWheel()
    @JvmStatic fun getEventNanoseconds(): Long = Mouse.getEventNanoseconds()
    @JvmStatic fun next(): Boolean = Mouse.next()
    @JvmStatic fun setCursorPosition(x: Int, y: Int) = Mouse.setCursorPosition(x, y)
    @JvmStatic fun isGrabbed(): Boolean = Mouse.isGrabbed()
    @JvmStatic fun setGrabbed(grabbed: Boolean) = Mouse.setGrabbed(grabbed)
}
