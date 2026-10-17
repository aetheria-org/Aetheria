package io.hamlook.aetheria.utils.compat

//#if MC < 1.21
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.gui.GuiScreen
import net.minecraft.item.ItemStack
import org.lwjgl.opengl.GL11

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
    ): Int {
        return fr.drawString(text, x.toFloat(), y.toFloat(), color, shadow)
    }

    fun getStringWidth(fr: FontRenderer, text: String): Int =
        fr.getStringWidth(text)

    fun drawItem(item: ItemStack, x: Int, y: Int) {
        MinecraftCompat.getMinecraft().renderItem.renderItemIntoGUI(item, x, y)
    }

    fun drawItemInSlot(fr: FontRenderer, item: ItemStack, x: Int, y: Int) {
        MinecraftCompat.getMinecraft().renderItem.renderItemOverlayIntoGUI(fr, item, x, y, null)
    }

    fun fill(left: Int, top: Int, right: Int, bottom: Int, color: Int) {
        GuiScreen.drawRect(left, top, right, bottom, color)
    }

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
//#else
//$$ import net.minecraft.client.font.TextRenderer
//$$ import net.minecraft.client.gui.DrawContext as VanillaDrawContext
//$$ import net.minecraft.item.ItemStack
//$$ import org.lwjgl.opengl.GL11
//$$
//$$ class DrawContext(private val vanilla: VanillaDrawContext) {
//$$
//$$     val matrices get() = vanilla.matrices
//$$
//$$     val vertexConsumers get() = vanilla.vertexConsumers
//$$
//$$     fun drawText(fr: TextRenderer, text: String, x: Int, y: Int, color: Int, shadow: Boolean) {
//$$         vanilla.drawText(fr, text, x, y, color, shadow)
//$$     }
//$$
//$$     fun getStringWidth(fr: TextRenderer, text: String): Int = fr.getWidth(text)
//$$
    //$$     fun drawItem(item: ItemStack, x: Int, y: Int) {
    //$$         vanilla.drawItem(item, x, y)
    //$$     }
    //$$
    //$$     fun drawItemInSlot(fr: TextRenderer, item: ItemStack, x: Int, y: Int) {
    //$$         vanilla.drawItemInSlot(fr, item, x, y)
    //$$     }
//$$
//$$     fun fill(left: Int, top: Int, right: Int, bottom: Int, color: Int) {
//$$         vanilla.fill(left, top, right, bottom, color)
//$$     }
//$$
//$$     fun enableScissor(x: Int, y: Int, width: Int, height: Int) {
//$$         GL11.glEnable(GL11.GL_SCISSOR_TEST)
//$$         val factor = GuiScreenUtils.getScaleFactor()
//$$         val displayHeight = GuiScreenUtils.getDisplayHeight()
//$$         GL11.glScissor(x * factor, displayHeight - (y + height) * factor, width * factor, height * factor)
//$$     }
//$$
//$$     fun disableScissor() {
//$$         GL11.glDisable(GL11.GL_SCISSOR_TEST)
//$$     }
//$$ }
//#endif
