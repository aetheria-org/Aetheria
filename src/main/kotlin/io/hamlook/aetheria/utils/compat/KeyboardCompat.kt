package io.hamlook.aetheria.utils.compat

//#if MC < 1.16
import org.lwjgl.input.Keyboard
//#else
//$$ import org.lwjgl.glfw.GLFW
//#endif

object KeyboardCompat {
    @JvmStatic
    fun isKeyDown(key: Int): Boolean =
        //#if MC < 1.16
        Keyboard.isKeyDown(key)
        //#else
        //$$ GLFW.glfwGetKey(MinecraftCompat.getMinecraft().window.handle, key) == GLFW.GLFW_PRESS
        //#endif

    @JvmStatic
    fun getEventKey(): Int =
        //#if MC < 1.16
        Keyboard.getEventKey()
        //#else
        //$$ InputEventQueue.eventKey
        //#endif

    @JvmStatic
    fun getEventKeyState(): Boolean =
        //#if MC < 1.16
        Keyboard.getEventKeyState()
        //#else
        //$$ InputEventQueue.eventKeyState
        //#endif

    @JvmStatic
    fun getKeyName(key: Int): String =
        //#if MC < 1.16
        Keyboard.getKeyName(key)
        //#else
        //$$ GLFW.glfwGetKeyName(key, 0) ?: "UNKNOWN"
        //#endif

    @JvmStatic
    fun getEventCharacter(): Char =
        //#if MC < 1.16
        Keyboard.getEventCharacter()
        //#else
        //$$ InputEventQueue.eventChar
        //#endif

    @JvmStatic
    fun next(): Boolean =
        //#if MC < 1.16
        Keyboard.next()
        //#else
        //$$ InputEventQueue.pollKey()
        //#endif

    @JvmStatic
    fun isRepeatEvent(): Boolean =
        //#if MC < 1.16
        Keyboard.isRepeatEvent()
        //#else
        //$$ InputEventQueue.eventRepeat
        //#endif

    @JvmStatic
    fun enableRepeatEvents(enable: Boolean) {
        //#if MC < 1.16
        Keyboard.enableRepeatEvents(enable)
        //#endif
    }
}
