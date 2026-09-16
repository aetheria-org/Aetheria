package io.hamlook.aetheria.utils.compat
//#if MC < 1.21
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiTextField
import net.minecraft.client.gui.ScaledResolution
//#else
//$$ import net.minecraft.client.gui.screen.Screen
//$$ import net.minecraft.client.gui.widget.ClickableWidget
//$$ import net.minecraft.client.gui.widget.TextFieldWidget
//#endif

object GuiScreenUtils {
    //#if MC < 1.21
    private var cachedScaledResolution: ScaledResolution? = null
    private var cachedKey = 0

    private fun getOrCreateScaledResolution(): ScaledResolution {
        val mc = MinecraftCompat.getMinecraft()
        val key = MinecraftCompat.getDisplayWidth() * 31 + MinecraftCompat.getDisplayHeight() * 17 + MinecraftCompat.getGameSettings().guiScale * 7
        val cached = cachedScaledResolution
        if (cached == null || key != cachedKey) {
            cachedScaledResolution = ScaledResolution(mc)
            cachedKey = key
        }
        return cachedScaledResolution!!
    }
    //#endif

    @JvmStatic
    fun getScaledWindowWidth(): Int =
        //#if MC < 1.21
        getOrCreateScaledResolution().scaledWidth
        //#else
        //$$ MinecraftCompat.getMinecraft().window.getScaledWidth()
        //#endif

    @JvmStatic
    fun getScaledWindowHeight(): Int =
        //#if MC < 1.21
        getOrCreateScaledResolution().scaledHeight
        //#else
        //$$ MinecraftCompat.getMinecraft().window.getScaledHeight()
        //#endif

    @JvmStatic
    fun getScaleFactor(): Int =
        //#if MC < 1.21
        getOrCreateScaledResolution().scaleFactor
        //#else
        //$$ MinecraftCompat.getMinecraft().window.getScaleFactor().toInt()
        //#endif

    @JvmStatic
    fun getScaledWindowWidthDouble(): Double =
        //#if MC < 1.21
        getOrCreateScaledResolution().scaledWidth_double
        //#else
        //$$ MinecraftCompat.getMinecraft().window.getScaledWidth().toDouble()
        //#endif

    @JvmStatic
    fun getScaledWindowHeightDouble(): Double =
        //#if MC < 1.21
        getOrCreateScaledResolution().scaledHeight_double
        //#else
        //$$ MinecraftCompat.getMinecraft().window.getScaledHeight().toDouble()
        //#endif

    @JvmStatic
    fun getDisplayWidth(): Int =
        //#if MC < 1.21
        MinecraftCompat.getDisplayWidth()
        //#else
        //$$ MinecraftCompat.getMinecraft().window.getWidth()
        //#endif

    @JvmStatic
    fun getDisplayHeight(): Int =
        //#if MC < 1.21
        MinecraftCompat.getDisplayHeight()
        //#else
        //$$ MinecraftCompat.getMinecraft().window.getHeight()
        //#endif

    @JvmStatic
    fun getMouseX(): Int {
        val sw = getScaledWindowWidth()
        val x = MouseCompat.getX() * sw / getDisplayWidth()
        //#if MC < 1.21
        return x
        //#else
        //$$ val mc = MinecraftCompat.getMinecraft()
        //$$ if (mc.window.framebufferWidth > mc.window.width) return x * 2
        //$$ return x
        //#endif
    }

    @JvmStatic
    fun getMouseY(): Int {
        val h = getScaledWindowHeight()
        val y = MouseCompat.getY() * h / getDisplayHeight()
        //#if MC < 1.21
        return h - y - 1
        //#else
        //$$ val mc = MinecraftCompat.getMinecraft()
        //$$ if (mc.window.framebufferHeight > mc.window.height) return y * 2
        //$$ return y
        //#endif
    }

    @JvmStatic
    fun getMousePos(): Pair<Int, Int> = getMouseX() to getMouseY()

    //#if MC < 1.21
    @JvmStatic fun getX(field: GuiTextField) = field.xPosition
    @JvmStatic fun setX(field: GuiTextField, v: Int) { field.xPosition = v }
    @JvmStatic fun getY(field: GuiTextField) = field.yPosition
    @JvmStatic fun setY(field: GuiTextField, v: Int) { field.yPosition = v }
    @JvmStatic fun getW(field: GuiTextField) = field.width
    @JvmStatic fun setW(field: GuiTextField, v: Int) { field.width = v }
    @JvmStatic fun getH(field: GuiTextField) = field.height
    @JvmStatic fun setH(field: GuiTextField, v: Int) { field.height = v }
    @JvmStatic fun getButtonX(b: GuiButton) = b.xPosition
    @JvmStatic fun getButtonY(b: GuiButton) = b.yPosition
    @JvmStatic fun getButtonW(b: GuiButton) = b.width
    @JvmStatic fun getButtonH(b: GuiButton) = b.height
    @JvmStatic fun setButtonX(b: GuiButton, v: Int) { b.xPosition = v }
    @JvmStatic fun setButtonY(b: GuiButton, v: Int) { b.yPosition = v }
    @JvmStatic fun setButtonW(b: GuiButton, v: Int) { b.width = v }
    @JvmStatic fun setButtonH(b: GuiButton, v: Int) { b.height = v }
    @JvmStatic fun getX(b: GuiButton) = b.xPosition
    @JvmStatic fun getY(b: GuiButton) = b.yPosition
    @JvmStatic fun getW(b: GuiButton) = b.width
    @JvmStatic fun getH(b: GuiButton) = b.height
    @JvmStatic fun setX(b: GuiButton, v: Int) { b.xPosition = v }
    @JvmStatic fun setY(b: GuiButton, v: Int) { b.yPosition = v }
    @JvmStatic fun setW(b: GuiButton, v: Int) { b.width = v }
    @JvmStatic fun setH(b: GuiButton, v: Int) { b.height = v }
    @JvmStatic fun addButton(screen: GuiScreen, btn: GuiButton) { screen.buttonList.add(btn) }
    @JvmStatic fun clearButtons(screen: GuiScreen) { screen.buttonList.clear() }
    @JvmStatic fun isButtonListEmpty(screen: GuiScreen) = screen.buttonList.isEmpty()
    //#else
    //$$ @JvmStatic fun getX(field: TextFieldWidget) = field.getX()
    //$$ @JvmStatic fun setX(field: TextFieldWidget, v: Int) = field.setX(v)
    //$$ @JvmStatic fun getY(field: TextFieldWidget) = field.getY()
    //$$ @JvmStatic fun setY(field: TextFieldWidget, v: Int) = field.setY(v)
    //$$ @JvmStatic fun getW(field: TextFieldWidget) = field.getWidth()
    //$$ @JvmStatic fun setW(field: TextFieldWidget, v: Int) = field.setWidth(v)
    //$$ @JvmStatic fun getH(field: TextFieldWidget) = field.getHeight()
    //$$ @JvmStatic fun setH(field: TextFieldWidget, v: Int) = field.setHeight(v)
    //$$ @JvmStatic fun getButtonX(b: ClickableWidget) = b.getX()
    //$$ @JvmStatic fun getButtonY(b: ClickableWidget) = b.getY()
    //$$ @JvmStatic fun getButtonW(b: ClickableWidget) = b.getWidth()
    //$$ @JvmStatic fun getButtonH(b: ClickableWidget) = b.getHeight()
    //$$ @JvmStatic fun setButtonX(b: ClickableWidget, v: Int) = b.setX(v)
    //$$ @JvmStatic fun setButtonY(b: ClickableWidget, v: Int) = b.setY(v)
    //$$ @JvmStatic fun setButtonW(b: ClickableWidget, v: Int) = b.setWidth(v)
    //$$ @JvmStatic fun setButtonH(b: ClickableWidget, v: Int) = b.setHeight(v)
    //$$ @JvmStatic fun addButton(screen: Screen, btn: ClickableWidget) { screen.addDrawableChild(btn) }
    //$$ @JvmStatic fun clearButtons(screen: Screen) = screen.clearChildren()
    //$$ @JvmStatic fun isButtonListEmpty(screen: Screen) = screen.children().isEmpty()
    //#endif
}
