package io.hamlook.aetheria.utils.compat

//#if MC < 1.16
import org.lwjgl.input.Mouse
//#else
//$$ import org.lwjgl.glfw.GLFW
//#endif

object MouseCompat {
    @JvmStatic
    fun isButtonDown(button: Int): Boolean =
        //#if MC < 1.16
        Mouse.isButtonDown(button)
        //#else
        //$$ GLFW.glfwGetMouseButton(MinecraftCompat.getMinecraft().window.handle, button) == GLFW.GLFW_PRESS
        //#endif

    @JvmStatic
    fun getX(): Int =
        //#if MC < 1.16
        Mouse.getX()
        //#else
        //$$ MinecraftCompat.getMinecraft().mouse.x.toInt()
        //#endif

    @JvmStatic
    fun getY(): Int =
        //#if MC < 1.16
        Mouse.getY()
        //#else
        //$$ MinecraftCompat.getMinecraft().mouse.y.toInt()
        //#endif

    @JvmStatic
    fun getEventButtonState(): Boolean =
        //#if MC < 1.16
        Mouse.getEventButtonState()
        //#else
        //$$ InputEventQueue.mouseButtonState
        //#endif

    @JvmStatic
    fun getEventButton(): Int =
        //#if MC < 1.16
        Mouse.getEventButton()
        //#else
        //$$ InputEventQueue.mouseButton
        //#endif

    @JvmStatic
    fun getDWheel(): Int =
        //#if MC < 1.16
        Mouse.getDWheel()
        //#else
        //$$ InputEventQueue.consumeWheel()
        //#endif

    @JvmStatic
    fun getEventDWheel(): Int =
        //#if MC < 1.16
        Mouse.getEventDWheel()
        //#else
        //$$ InputEventQueue.consumeWheel()
        //#endif

    @JvmStatic
    fun getEventX(): Int =
        //#if MC < 1.16
        Mouse.getEventX()
        //#else
        //$$ getX()
        //#endif

    @JvmStatic
    fun getEventY(): Int =
        //#if MC < 1.16
        Mouse.getEventY()
        //#else
        //$$ getY()
        //#endif

    @JvmStatic
    fun getEventDY(): Int =
        //#if MC < 1.16
        Mouse.getEventDY()
        //#else
        //$$ InputEventQueue.mouseDY
        //#endif

    @JvmStatic
    fun getScrollDelta(): Int =
        //#if MC < 1.16
        Mouse.getEventDWheel()
        //#else
        //$$ InputEventQueue.consumeWheel()
        //#endif

    @JvmStatic
    fun getEventNanoseconds(): Long =
        //#if MC < 1.16
        Mouse.getEventNanoseconds()
        //#else
        //$$ InputEventQueue.mouseNanos
        //#endif

    @JvmStatic
    fun next(): Boolean =
        //#if MC < 1.16
        Mouse.next()
        //#else
        //$$ InputEventQueue.pollMouse()
        //#endif

    @JvmStatic
    fun setCursorPosition(x: Int, y: Int) {
        //#if MC < 1.16
        Mouse.setCursorPosition(x, y)
        //#else
        //$$ GLFW.glfwSetCursorPos(MinecraftCompat.getMinecraft().window.handle, x.toDouble(), y.toDouble())
        //#endif
    }

    @JvmStatic
    fun isGrabbed(): Boolean =
        //#if MC < 1.16
        Mouse.isGrabbed()
        //#else
        //$$ GLFW.glfwGetInputMode(MinecraftCompat.getMinecraft().window.handle, GLFW.GLFW_CURSOR) == GLFW.GLFW_CURSOR_DISABLED
        //#endif

    @JvmStatic
    fun setGrabbed(grabbed: Boolean) {
        //#if MC < 1.16
        Mouse.setGrabbed(grabbed)
        //#else
        //$$ val mode = if (grabbed) GLFW.GLFW_CURSOR_DISABLED else GLFW.GLFW_CURSOR_NORMAL
        //$$ GLFW.glfwSetInputMode(MinecraftCompat.getMinecraft().window.handle, GLFW.GLFW_CURSOR, mode)
        //#endif
    }
}
