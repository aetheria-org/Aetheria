package io.hamlook.aetheria.utils.compat
//#if MC > 1.21
//$$ import net.minecraft.client.gui.DrawContext
//$$ import net.minecraft.util.Identifier
//$$ import org.joml.Matrix4f
//$$ import org.joml.Quaternionf
//$$ import org.lwjgl.opengl.GL11
//#endif
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.renderer.GlStateManager
import java.nio.FloatBuffer

object DrawContextUtils {

    const val GL_MODELVIEW_MATRIX = 2982
    const val GL_PROJECTION_MATRIX = 2983
    const val GL_CURRENT_COLOR = 2816

    private var renderDepth = 0

    //#if MC < 1.21
    private var _drawContext: DrawContext? = DrawContext()
    val drawContext: DrawContext get() = _drawContext ?: DrawContext()
    //#else
    //$$ private var _drawContext: DrawContext? = null
    //$$ val drawContext: DrawContext get() = _drawContext ?: error("DrawContext not set — missing render mixin?")
    //#endif

    @JvmStatic
    fun setContext(context: DrawContext) {
        renderDepth++
        if (_drawContext == null) {
            _drawContext = context
        }
    }

    @JvmStatic
    fun clearContext() {
        if (renderDepth > 1) {
            renderDepth--
            return
        }
        renderDepth = 0
        //#if MC < 1.21
        _drawContext = DrawContext()
        //#else
        //$$ _drawContext = null
        //#endif
    }

    @JvmStatic
    fun pushMatrix() =
        //#if MC < 1.21.6
        drawContext.matrices.pushMatrix()
        //#else
        //$$ drawContext.matrices.pushMatrix()
        //#endif

    @JvmStatic
    fun popMatrix() =
        //#if MC < 1.21.6
        drawContext.matrices.popMatrix()
        //#else
        //$$ drawContext.matrices.popMatrix()
        //#endif

    @JvmStatic
    fun translate(x: Float, y: Float, z: Float) {
        //#if MC < 1.21.6
        drawContext.matrices.translate(x, y, z)
        //#else
        //$$ drawContext.matrices.translate(x, y)
        //#endif
    }

    @JvmStatic
    fun translate(x: Double, y: Double, z: Double) {
        //#if MC < 1.21.6
        drawContext.matrices.translate(x, y, z)
        //#else
        //$$ drawContext.matrices.translate(x.toFloat(), y.toFloat())
        //#endif
    }

    @JvmStatic
    fun scale(x: Float, y: Float, z: Float) {
        //#if MC < 1.21.6
        drawContext.matrices.scale(x, y, z)
        //#else
        //$$ drawContext.matrices.scale(x, y)
        //#endif
    }

    @JvmStatic
    fun rotate(angle: Float, x: Float, y: Float, z: Float) {
        //#if MC < 1.21
        GlStateManager.rotate(angle, x, y, z)
        //#elseif MC < 1.21.6
        //$$ drawContext.matrices.multiply(Quaternionf().rotationAxis(angle, x, y, z))
        //#endif
    }

    @JvmStatic
    fun multMatrix(buffer: FloatBuffer) {
        //#if MC < 1.21
        GlStateManager.multMatrix(buffer)
        //#elseif MC < 1.21.6
        //$$ multMatrix(Matrix4f(buffer))
        //#endif
    }

    //#if MC > 1.21
    //#if MC < 1.21.6
    //$$ fun multMatrix(matrix: Matrix4f) = drawContext.matrices.multiplyPositionMatrix(matrix)
    //#endif
    //#endif

    @JvmStatic
    fun loadIdentity() {
        //#if MC < 1.21.6
        drawContext.matrices.loadIdentity()
        //#else
        //$$ drawContext.matrices.identity()
        //#endif
    }

    @JvmStatic
    inline fun pushPop(action: () -> Unit) {
        pushMatrix()
        try {
            action()
        } finally {
            popMatrix()
        }
    }

    @JvmStatic
    inline fun translated(x: Float, y: Float, z: Float, action: () -> Unit) {
        translate(x, y, z)
        try {
            action()
        } finally {
            translate(-x, -y, -z)
        }
    }

    @JvmStatic
    inline fun scaled(x: Float, y: Float, z: Float, action: () -> Unit) {
        scale(x, y, z)
        try {
            action()
        } finally {
            scale(1f / x, 1f / y, 1f / z)
        }
    }

    @JvmStatic
    fun drawText(
        fr: FontRenderer,
        text: String,
        x: Int,
        y: Int,
        color: Int,
        shadow: Boolean
    ) = drawContext.drawText(fr, text, x, y, color, shadow)

    @JvmStatic
    fun drawTextWithShadow(
        fr: FontRenderer,
        text: String,
        x: Int,
        y: Int,
        color: Int
    ) = drawContext.drawText(fr, text, x, y, color, true)

    @JvmStatic
    fun drawTextWithShadow(
        fr: FontRenderer,
        text: String,
        x: Float,
        y: Float,
        color: Int
    ) = drawContext.drawText(fr, text, x.toInt(), y.toInt(), color, true)

    @JvmStatic
    fun getStringWidth(fr: FontRenderer, text: String): Int =
        //#if MC < 1.21
        drawContext.getStringWidth(fr, text)
        //#else
        //$$ fr.getWidth(text)
        //#endif

    @JvmStatic
    fun fill(left: Int, top: Int, right: Int, bottom: Int, color: Int) =
        drawContext.fill(left, top, right, bottom, color)

    //#if MC < 1.21
    @JvmStatic
    fun drawScaledCustomSizeModalRect(
        x: Int, y: Int, u: Float, v: Float,
        width: Int, height: Int,
        textureWidth: Float, textureHeight: Float,
        tileWidth: Float, tileHeight: Float
    ) {
        val f = 1.0f / tileWidth
        val g = 1.0f / tileHeight
        val tessellator = net.minecraft.client.renderer.Tessellator.getInstance()
        val worldRenderer = tessellator.worldRenderer
        worldRenderer.begin(7, net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_TEX)
        worldRenderer.pos(x.toDouble(), (y + height).toDouble(), 0.0)
            .tex((u * f).toDouble(), ((v + textureHeight) * g).toDouble()).endVertex()
        worldRenderer.pos((x + width).toDouble(), (y + height).toDouble(), 0.0)
            .tex(((u + textureWidth) * f).toDouble(), ((v + textureHeight) * g).toDouble()).endVertex()
        worldRenderer.pos((x + width).toDouble(), y.toDouble(), 0.0)
            .tex(((u + textureWidth) * f).toDouble(), (v * g).toDouble()).endVertex()
        worldRenderer.pos(x.toDouble(), y.toDouble(), 0.0)
            .tex((u * f).toDouble(), (v * g).toDouble()).endVertex()
        tessellator.draw()
    }
    //#else
    //$$ @JvmStatic
    //$$ fun drawScaledCustomSizeModalRect(
    //$$     texture: Identifier,
    //$$     x: Int, y: Int, u: Float, v: Float,
    //$$     width: Int, height: Int,
    //$$     tileWidth: Float, tileHeight: Float
    //$$ ) =
    //$$     drawContext.drawTexture(texture, x, y, u, v, width, height, tileWidth.toInt(), tileHeight.toInt())
    //#endif

    //#if MC < 1.21
    @JvmStatic
    fun drawModalRectWithCustomSizedTexture(
        x: Int, y: Int, u: Float, v: Float,
        width: Int, height: Int,
        textureWidth: Float, textureHeight: Float
    ) {
        val f = 1.0f / textureWidth
        val g = 1.0f / textureHeight
        val tessellator = net.minecraft.client.renderer.Tessellator.getInstance()
        val worldRenderer = tessellator.worldRenderer
        worldRenderer.begin(7, net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_TEX)
        worldRenderer.pos(x.toDouble(), (y + height).toDouble(), 0.0)
            .tex((u * f).toDouble(), ((v + height) * g).toDouble()).endVertex()
        worldRenderer.pos((x + width).toDouble(), (y + height).toDouble(), 0.0)
            .tex(((u + width) * f).toDouble(), ((v + height) * g).toDouble()).endVertex()
        worldRenderer.pos((x + width).toDouble(), y.toDouble(), 0.0)
            .tex(((u + width) * f).toDouble(), (v * g).toDouble()).endVertex()
        worldRenderer.pos(x.toDouble(), y.toDouble(), 0.0)
            .tex((u * f).toDouble(), (v * g).toDouble()).endVertex()
        tessellator.draw()
    }
    //#else
    //$$ @JvmStatic
    //$$ fun drawModalRectWithCustomSizedTexture(
    //$$     texture: Identifier,
    //$$     x: Int, y: Int, u: Float, v: Float,
    //$$     width: Int, height: Int,
    //$$     textureWidth: Float, textureHeight: Float
    //$$ ) =
    //$$     drawContext.drawTexture(texture, x, y, u, v, width, height, textureWidth.toInt(), textureHeight.toInt())
    //#endif

    @JvmStatic
    fun enableScissor(x: Int, y: Int, width: Int, height: Int) {
        //#if MC < 1.21
        drawContext.enableScissor(x, y, width, height)
        //#else
        //$$ GL11.glEnable(GL11.GL_SCISSOR_TEST)
        //$$ val factor = GuiScreenUtils.getScaleFactor()
        //$$ val displayHeight = GuiScreenUtils.getDisplayHeight()
        //$$ GL11.glScissor(x * factor, displayHeight - (y + height) * factor, width * factor, height * factor)
        //#endif
    }

    @JvmStatic
    fun disableScissor() {
        //#if MC < 1.21
        drawContext.disableScissor()
        //#else
        //$$ GL11.glDisable(GL11.GL_SCISSOR_TEST)
        //#endif
    }
}
