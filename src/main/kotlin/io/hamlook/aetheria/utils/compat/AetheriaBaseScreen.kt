package io.hamlook.aetheria.utils.compat

import io.hamlook.aetheria.Aetheria
import net.minecraft.client.gui.GuiScreen
//#if MC > 1.21
//$$ import net.minecraft.client.gui.DrawContext
//$$ import net.minecraft.text.Text
//#endif
import java.util.logging.Level

//#if MC < 1.16
abstract class AetheriaBaseScreen : GuiScreen() {
//#else
//$$ abstract class AetheriaBaseScreen : Screen(Text.empty()) {
//#endif

    //#if MC < 1.21
    // inherits GuiScreen.mc — declare nothing
    //#else
    //$$ @JvmField val mc = MinecraftCompat.getMinecraft()
    //#endif

    //#if MC < 1.16
    final override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        super.drawScreen(mouseX, mouseY, partialTicks)
        postDrawScreen(DrawContext(), mouseX, mouseY, partialTicks)
    }
    //#else
    //$$ final override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
    //$$     super.render(context, mouseX, mouseY, delta)
    //$$     postDrawScreen(context, mouseX, mouseY, delta)
    //$$ }
    //#endif

    private var athrLastW = -1
    private var athrLastH = -1

    private fun postDrawScreen(context: DrawContext, mouseX: Int, mouseY: Int, partialTicks: Float) {
        if (athrLastW != width || athrLastH != height) {
            athrLastW = width
            athrLastH = height
            try {
                onResizeScreen(width, height)
            } catch (e: Exception) {
                Aetheria.logger.log(Level.WARNING, "Error while resizing screen", e)
            }
        }
        DrawContextUtils.setContext(context)
        try {
            onDrawScreen(mouseX, mouseY, partialTicks)
        } catch (e: Exception) {
            Aetheria.logger.log(Level.WARNING, "Error while drawing screen", e)
        }
        GlStateManagerCompat.resetColor()
        DrawContextUtils.clearContext()
    }

    protected open fun onDrawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {}

    //#if MC < 1.16
    final override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        super.mouseClicked(mouseX, mouseY, mouseButton)
        try {
            onMouseClicked(mouseX, mouseY, mouseButton)
        } catch (e: Exception) {
            Aetheria.logger.log(Level.WARNING, "Error while clicking mouse", e)
        }
    }

    protected open fun onMouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {}

    final override fun keyTyped(typedChar: Char, keyCode: Int) {
        super.keyTyped(typedChar, keyCode)
        try {
            onKeyTyped(typedChar, keyCode)
        } catch (e: Exception) {
            Aetheria.logger.log(Level.WARNING, "Error while typing key", e)
        }
    }

    protected open fun onKeyTyped(typedChar: Char, keyCode: Int) {}

    final override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        super.mouseReleased(mouseX, mouseY, state)
        try {
            onMouseReleased(mouseX, mouseY, state)
        } catch (e: Exception) {
            Aetheria.logger.log(Level.WARNING, "Error while releasing mouse", e)
        }
    }

    protected open fun onMouseReleased(mouseX: Int, mouseY: Int, state: Int) {}

    final override fun mouseClickMove(mouseX: Int, mouseY: Int, clickedMouseButton: Int, timeSinceLastClick: Long) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick)
        try {
            onMouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick)
        } catch (e: Exception) {
            Aetheria.logger.log(Level.WARNING, "Error while clicking and moving mouse", e)
        }
    }

    protected open fun onMouseClickMove(mouseX: Int, mouseY: Int, clickedMouseButton: Int, timeSinceLastClick: Long) {}
    //#else
    //$$ final override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
    //$$     val result = super.mouseClicked(mouseX, mouseY, button)
    //$$     try {
    //$$         onMouseClicked(mouseX.toInt(), mouseY.toInt(), button)
    //$$     } catch (e: Exception) {
    //$$         Aetheria.logger.log(Level.WARNING, "Error while clicking mouse", e)
    //$$     }
    //$$     return result
    //$$ }
    //$$
    //$$ protected open fun onMouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {}
    //$$
    //$$ final override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
    //$$     val result = super.keyPressed(keyCode, scanCode, modifiers)
    //$$     try {
    //$$         onKeyTyped('\u0000', keyCode)
    //$$     } catch (e: Exception) {
    //$$         Aetheria.logger.log(Level.WARNING, "Error while typing key", e)
    //$$     }
    //$$     return result
    //$$ }
    //$$
    //$$ protected open fun onKeyTyped(typedChar: Char, keyCode: Int) {}
    //$$
    //$$ final override fun charTyped(ch: Char, modifiers: Int): Boolean {
    //$$     val result = super.charTyped(ch, modifiers)
    //$$     try {
    //$$         onCharTyped(ch, modifiers)
    //$$     } catch (e: Exception) {
    //$$         Aetheria.logger.log(Level.WARNING, "Error while typing char", e)
    //$$     }
    //$$     return result
    //$$ }
    //$$
    //$$ protected open fun onCharTyped(typedChar: Char, modifiers: Int) {}
    //$$
    //$$ final override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
    //$$     val result = super.mouseReleased(mouseX, mouseY, button)
    //$$     try {
    //$$         onMouseReleased(mouseX.toInt(), mouseY.toInt(), button)
    //$$     } catch (e: Exception) {
    //$$         Aetheria.logger.log(Level.WARNING, "Error while releasing mouse", e)
    //$$     }
    //$$     return result
    //$$ }
    //$$
    //$$ protected open fun onMouseReleased(mouseX: Int, mouseY: Int, state: Int) {}
    //$$
    //$$ final override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
    //$$     val result = super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
    //$$     try {
    //$$         onMouseClickMove(mouseX.toInt(), mouseY.toInt(), button, 0L)
    //$$     } catch (e: Exception) {
    //$$         Aetheria.logger.log(Level.WARNING, "Error while clicking and moving mouse", e)
    //$$     }
    //$$     return result
    //$$ }
    //$$
    //$$ protected open fun onMouseClickMove(mouseX: Int, mouseY: Int, clickedMouseButton: Int, timeSinceLastClick: Long) {}
    //$$
    //$$ final override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
    //$$     val result = super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    //$$     try {
    //$$         onMouseScrolled(mouseX.toInt(), mouseY.toInt(), verticalAmount)
    //$$     } catch (e: Exception) {
    //$$         Aetheria.logger.log(Level.WARNING, "Error while scrolling mouse", e)
    //$$     }
    //$$     return result
    //$$ }
    //$$
    //$$ protected open fun onMouseScrolled(mouseX: Int, mouseY: Int, verticalAmount: Double) {
    //$$     onHandleMouseInput()
    //$$ }
    //#endif

    //#if MC < 1.16
    final override fun handleMouseInput() {
        super.handleMouseInput()
        try {
            onHandleMouseInput()
        } catch (e: Exception) {
            Aetheria.logger.log(Level.WARNING, "Error while handling mouse input", e)
        }
    }
    //#endif

    protected open fun onHandleMouseInput() {}

    //#if MC < 1.16
    final override fun onGuiClosed() {
        super.onGuiClosed()
        try {
            guiClosed()
        } catch (e: Exception) {
            Aetheria.logger.log(Level.WARNING, "Error while closing GUI", e)
        }
    }
    //#else
    //$$ final override fun close() {
    //$$     super.close()
    //$$     try {
    //$$         guiClosed()
    //$$     } catch (e: Exception) {
    //$$         Aetheria.logger.log(Level.WARNING, "Error while closing GUI", e)
    //$$     }
    //$$ }
    //#endif

    protected open fun guiClosed() {}

    //#if MC < 1.16
    final override fun initGui() {
        super.initGui()
        try {
            onInitGui()
        } catch (e: Exception) {
            Aetheria.logger.log(Level.WARNING, "Error while initializing GUI", e)
        }
    }
    //#else
    //$$ final override fun init() {
    //$$     super.init()
    //$$     try {
    //$$         onInitGui()
    //$$     } catch (e: Exception) {
    //$$         Aetheria.logger.log(Level.WARNING, "Error while initializing GUI", e)
    //$$     }
    //$$ }
    //#endif

    protected open fun onInitGui() {}

    protected open fun onResizeScreen(w: Int, h: Int) {}
}
