package io.hamlook.aetheria.utils.compat

import io.hamlook.aetheria.Aetheria
import net.minecraft.client.gui.GuiScreen
import java.util.logging.Level

/**
 * Abstract base for Aetheria GUI screens. Wraps version-sensitive [GuiScreen] method signatures
 * behind [on*] hooks so preprocessor blocks live in one place when adding multi-version support.
 *
 * Subclasses override [onDrawScreen], [onMouseClicked], [onKeyTyped], etc. The base class handles
 * [super] calls and exception logging in [final] override methods. Hooks are [protected open] for
 * Java subclass access across packages.
 *
 * Do NOT override [drawScreen], [mouseClicked], [keyTyped], etc. directly — use the on* hooks.
 * [doesGuiPauseGame], [updateScreen], [actionPerformed], [handleKeyboardInput], [onResize] are
 * NOT wrapped — override them directly if needed (they don't change across MC versions).
 */
abstract class AetheriaBaseScreen : GuiScreen() {

    final override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        super.drawScreen(mouseX, mouseY, partialTicks)
        try {
            onDrawScreen(mouseX, mouseY, partialTicks)
        } catch (e: Exception) {
            Aetheria.logger.log(Level.WARNING, "Error while drawing screen", e)
        }
    }

    protected open fun onDrawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {}

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

    final override fun handleMouseInput() {
        super.handleMouseInput()
        try {
            onHandleMouseInput()
        } catch (e: Exception) {
            Aetheria.logger.log(Level.WARNING, "Error while handling mouse input", e)
        }
    }

    protected open fun onHandleMouseInput() {}

    final override fun onGuiClosed() {
        super.onGuiClosed()
        try {
            guiClosed()
        } catch (e: Exception) {
            Aetheria.logger.log(Level.WARNING, "Error while closing GUI", e)
        }
    }

    protected open fun guiClosed() {}

    final override fun initGui() {
        super.initGui()
        try {
            onInitGui()
        } catch (e: Exception) {
            Aetheria.logger.log(Level.WARNING, "Error while initializing GUI", e)
        }
    }

    protected open fun onInitGui() {}
}
