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
        //$$ GLFW_KEY_NAMES[key] ?: GLFW.glfwGetKeyName(key, 0)?.uppercase() ?: "UNKNOWN"
        //#endif

    //#if MC < 1.16
    //#else
    //$$ private val GLFW_KEY_NAMES: Map<Int, String> = mapOf(
    //$$     GLFW.GLFW_KEY_ESCAPE to "ESCAPE",
    //$$     GLFW.GLFW_KEY_TAB to "TAB",
    //$$     GLFW.GLFW_KEY_CAPS_LOCK to "CAPITAL",
    //$$     GLFW.GLFW_KEY_BACKSPACE to "BACK",
    //$$     GLFW.GLFW_KEY_ENTER to "RETURN",
    //$$     GLFW.GLFW_KEY_KP_ENTER to "NUMPADENTER",
    //$$     GLFW.GLFW_KEY_LEFT_SHIFT to "LSHIFT",
    //$$     GLFW.GLFW_KEY_LEFT_CONTROL to "LCONTROL",
    //$$     GLFW.GLFW_KEY_LEFT_ALT to "LALT",
    //$$     GLFW.GLFW_KEY_LEFT_SUPER to "LMETA",
    //$$     GLFW.GLFW_KEY_RIGHT_SHIFT to "RSHIFT",
    //$$     GLFW.GLFW_KEY_RIGHT_CONTROL to "RCONTROL",
    //$$     GLFW.GLFW_KEY_RIGHT_ALT to "RALT",
    //$$     GLFW.GLFW_KEY_RIGHT_SUPER to "RMETA",
    //$$     GLFW.GLFW_KEY_SPACE to "SPACE",
    //$$     GLFW.GLFW_KEY_INSERT to "INSERT",
    //$$     GLFW.GLFW_KEY_DELETE to "DELETE",
    //$$     GLFW.GLFW_KEY_RIGHT to "RIGHT",
    //$$     GLFW.GLFW_KEY_LEFT to "LEFT",
    //$$     GLFW.GLFW_KEY_DOWN to "DOWN",
    //$$     GLFW.GLFW_KEY_UP to "UP",
    //$$     GLFW.GLFW_KEY_PAGE_UP to "PRIOR",
    //$$     GLFW.GLFW_KEY_PAGE_DOWN to "NEXT",
    //$$     GLFW.GLFW_KEY_HOME to "HOME",
    //$$     GLFW.GLFW_KEY_END to "END",
    //$$     GLFW.GLFW_KEY_PAUSE to "PAUSE",
    //$$     GLFW.GLFW_KEY_PRINT_SCREEN to "SYSRQ",
    //$$     GLFW.GLFW_KEY_F1 to "F1",
    //$$     GLFW.GLFW_KEY_F2 to "F2",
    //$$     GLFW.GLFW_KEY_F3 to "F3",
    //$$     GLFW.GLFW_KEY_F4 to "F4",
    //$$     GLFW.GLFW_KEY_F5 to "F5",
    //$$     GLFW.GLFW_KEY_F6 to "F6",
    //$$     GLFW.GLFW_KEY_F7 to "F7",
    //$$     GLFW.GLFW_KEY_F8 to "F8",
    //$$     GLFW.GLFW_KEY_F9 to "F9",
    //$$     GLFW.GLFW_KEY_F10 to "F10",
    //$$     GLFW.GLFW_KEY_F11 to "F11",
    //$$     GLFW.GLFW_KEY_F12 to "F12",
    //$$     GLFW.GLFW_KEY_KP_0 to "NUMPAD0",
    //$$     GLFW.GLFW_KEY_KP_1 to "NUMPAD1",
    //$$     GLFW.GLFW_KEY_KP_2 to "NUMPAD2",
    //$$     GLFW.GLFW_KEY_KP_3 to "NUMPAD3",
    //$$     GLFW.GLFW_KEY_KP_4 to "NUMPAD4",
    //$$     GLFW.GLFW_KEY_KP_5 to "NUMPAD5",
    //$$     GLFW.GLFW_KEY_KP_6 to "NUMPAD6",
    //$$     GLFW.GLFW_KEY_KP_7 to "NUMPAD7",
    //$$     GLFW.GLFW_KEY_KP_8 to "NUMPAD8",
    //$$     GLFW.GLFW_KEY_KP_9 to "NUMPAD9",
    //$$     GLFW.GLFW_KEY_KP_DECIMAL to "DECIMAL",
    //$$     GLFW.GLFW_KEY_KP_DIVIDE to "DIVIDE",
    //$$     GLFW.GLFW_KEY_KP_MULTIPLY to "MULTIPLY",
    //$$     GLFW.GLFW_KEY_KP_SUBTRACT to "SUBTRACT",
    //$$     GLFW.GLFW_KEY_KP_ADD to "ADD",
    //$$     GLFW.GLFW_KEY_GRAVE_ACCENT to "GRAVE",
    //$$     GLFW.GLFW_KEY_MINUS to "MINUS",
    //$$     GLFW.GLFW_KEY_EQUAL to "EQUALS",
    //$$     GLFW.GLFW_KEY_LEFT_BRACKET to "LBRACKET",
    //$$     GLFW.GLFW_KEY_RIGHT_BRACKET to "RBRACKET",
    //$$     GLFW.GLFW_KEY_SEMICOLON to "SEMICOLON",
    //$$     GLFW.GLFW_KEY_APOSTROPHE to "APOSTROPHE",
    //$$     GLFW.GLFW_KEY_BACKSLASH to "BACKSLASH",
    //$$     GLFW.GLFW_KEY_COMMA to "COMMA",
    //$$     GLFW.GLFW_KEY_PERIOD to "PERIOD",
    //$$     GLFW.GLFW_KEY_SLASH to "SLASH"
    //$$ )
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
