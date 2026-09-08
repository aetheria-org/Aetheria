package io.hamlook.aetheria.utils.compat

import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.gui.GuiScreen
import net.minecraft.item.ItemStack
import org.lwjgl.opengl.GL11

/**
 * Fake DrawContext wrapping MatrixStack on 1.8.9.
 * When Phase 0 is added, this file gets an empty override in
 * versions/1.21.5/src/ so Minecraft's real DrawContext is used.
 */
class DrawContext {

    private val _matrices = MatrixStack()
    val matrices: MatrixStack get() = _matrices

    fun drawText(
        fr: FontRenderer,
        text: String,
        x: Int,
        y: Int,
        color: Int,
        shadow: Boolean
    ) = fr.drawString(text, x.toFloat(), y.toFloat(), color, shadow)

    fun drawItem(item: ItemStack, x: Int, y: Int) =
        MinecraftCompat.getMinecraft().renderItem.renderItemIntoGUI(item, x, y)

    fun fill(left: Int, top: Int, right: Int, bottom: Int, color: Int) =
        GuiScreen.drawRect(left, top, right, bottom, color)

    fun enableScissor(x: Int, y: Int, width: Int, height: Int) {
        GL11.glEnable(GL11.GL_SCISSOR_TEST)
        val factor = GuiScreenUtils.getScaleFactor()
        val displayHeight = GuiScreenUtils.getDisplayHeight()
        GL11.glScissor(x * factor, (displayHeight - (y + height) * factor), width * factor, height * factor)
    }

    fun disableScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST)
    }
}
