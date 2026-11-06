package io.hamlook.aetheria.utils.compat
//#if MC > 1.21
//$$ import net.minecraft.client.gui.DrawContext
//$$ import org.joml.Matrix4f
//$$ import org.joml.Quaternionf
//#endif
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.renderer.GlStateManager
//$$ import net.minecraft.text.OrderedText
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import java.nio.FloatBuffer

object DrawContextUtils {

    const val GL_MODELVIEW_MATRIX = 2982
    const val GL_PROJECTION_MATRIX = 2983
    const val GL_CURRENT_COLOR = 2816

    private var renderDepth = 0

    //#if MC < 1.21
    private var _drawContext: DrawContext? = DrawContext()
    @JvmStatic
    val drawContext: DrawContext get() = _drawContext ?: DrawContext()
    //#else
//$$ private var _drawContext: DrawContext? = null
    //$$ @JvmStatic
    //$$ val drawContext: DrawContext get() = _drawContext ?: error("DrawContext not set - missing render mixin?")
    //#endif

    //#if MC < 1.21
    //#else
    //$$ @JvmStatic
    //$$ val drawContextOrNull: DrawContext? get() = _drawContext
    //$$ @JvmStatic
    //$$ fun flushIfPresent() {
    //$$     drawContextOrNull?.draw()
    //$$ }
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
        //$$ drawContext.matrices.multiply(Quaternionf().rotationAxis(Math.toRadians(angle.toDouble()).toFloat(), x, y, z))
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
    fun drawText(
        fr: FontRenderer,
        text: String,
        x: Float,
        y: Float,
        color: Int,
        shadow: Boolean
    ) = drawContext.drawText(fr, text, x.toInt(), y.toInt(), color, shadow)

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

    //#if MC >= 1.21
    //$$ @JvmStatic
    //$$ fun drawOrderedText(
    //$$     fr: TextRenderer,
    //$$     text: OrderedText,
    //$$     x: Int,
    //$$     y: Int,
    //$$     color: Int,
    //$$     shadow: Boolean
    //$$ ): Int = drawContext.drawText(fr, text, x, y, color, shadow)
    //#endif

    @JvmStatic
    fun drawCenteredString(fr: FontRenderer, text: String, x: Int, y: Int, color: Int) =
        drawText(fr, text, x - getStringWidth(fr, text) / 2, y, color, false)

    @JvmStatic
    fun drawSplitString(fr: FontRenderer, text: String, x: Int, y: Int, wrapWidth: Int, color: Int) {
        //#if MC < 1.16
        fr.drawSplitString(text, x, y, wrapWidth, color)
        //#else
        //$$ var dy = y
        //$$ for (line in wrapFormattedText(fr, text, wrapWidth)) {
        //$$     drawText(fr, line, x, dy, color, false)
        //$$     dy += getFontHeight(fr)
        //$$ }
        //#endif
    }

    @JvmStatic
    fun getFontHeight(fr: FontRenderer): Int =
        //#if MC < 1.16
        fr.FONT_HEIGHT
        //#else
        //$$ fr.fontHeight
        //#endif

    @JvmStatic
    fun getStringWidth(fr: FontRenderer, text: String): Int =
        //#if MC < 1.21
        drawContext.getStringWidth(fr, text)
        //#else
        //$$ fr.getWidth(text)
        //#endif

    @JvmStatic
    fun getCharWidth(fr: FontRenderer, c: Char): Int =
        //#if MC < 1.16
        fr.getCharWidth(c)
        //#else
        //$$ fr.getWidth(java.lang.String.valueOf(c))
        //#endif

    @JvmStatic
    fun wrapFormattedText(fr: FontRenderer, text: String, width: Int): List<String> =
        //#if MC < 1.16
        fr.listFormattedStringToWidth(text, width)
        //#else
        //$$ wrapWords(fr, text, width)
        //#endif

//#if MC > 1.21
//$$ private fun wrapWords(fr: TextRenderer, text: String, width: Int): List<String> {
//$$     val out = ArrayList<String>()
//$$     for (paragraph in text.split("\n")) {
//$$         var current = StringBuilder()
//$$         for (word in paragraph.split(" ")) {
//$$             if (word.isEmpty()) continue
//$$             val trial = if (current.isEmpty()) word else "$current $word"
//$$             if (current.isEmpty() || getStringWidth(fr, trial) <= width) {
//$$                 current = StringBuilder(trial)
//$$             } else {
//$$                 out.add(current.toString())
//$$                 current = StringBuilder(word)
//$$             }
//$$             while (getStringWidth(fr, current.toString()) > width && current.length > 1) {
//$$                 var cut = current.length - 1
//$$                 while (cut > 1 && current[cut - 1] == '§') cut--
//$$                 out.add(current.substring(0, cut))
//$$                 current = StringBuilder(current.substring(cut))
//$$             }
//$$         }
//$$         out.add(current.toString())
//$$     }
//$$     return out
//$$ }
//#endif

    @JvmStatic
    fun fill(left: Int, top: Int, right: Int, bottom: Int, color: Int) =
        drawContext.fill(left, top, right, bottom, color)

    @JvmStatic
    fun drawItem(item: ItemStack, x: Int, y: Int) =
        drawContext.drawItem(item, x, y)

    @JvmStatic
    fun drawItemInSlot(fr: FontRenderer, item: ItemStack, x: Int, y: Int) =
        drawContext.drawItemInSlot(fr, item, x, y)

    @JvmStatic
    fun drawScaledCustomSizeModalRect(
        texture: ResourceLocation,
        x: Int, y: Int, u: Float, v: Float,
        width: Int, height: Int,
        textureWidth: Float, textureHeight: Float,
        tileWidth: Float, tileHeight: Float
    ) {
        //#if MC < 1.21
        MinecraftCompat.getMinecraft().textureManager.bindTexture(texture)
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
        //#else
        //$$ drawContext.drawTexture(texture, x, y, width, height, u, v, textureWidth.toInt(), textureHeight.toInt(), tileWidth.toInt(), tileHeight.toInt())
        //#endif
    }

    //#if MC < 1.21
    @JvmStatic
    fun drawModalRectWithCustomSizedTexture(
        texture: ResourceLocation,
        x: Int, y: Int, u: Float, v: Float,
        width: Int, height: Int,
        textureWidth: Float, textureHeight: Float
    ) {
        MinecraftCompat.getMinecraft().getTextureManager().bindTexture(texture)
        drawModalRectWithCustomSizedTexture(x, y, u, v, width, height, textureWidth, textureHeight)
    }
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

    //#if MC < 1.21
    //#else
    //$$ /**
    //$$  * Draws [text] centred on ([centerX], [centerY]) at [scale].
    //$$  *
    //$$  * Mirrors the legacy `TextRenderUtils.drawCenteredStringScaleAware`
    //$$  * transform, which the modern render path was missing: widget
    //$$  * backgrounds were sized from the UI scale while their labels drew
    //$$  * straight through `drawText` at 1:1, so the text did not fill the box.
    //$$  */
    //$$ @JvmStatic
    //$$ fun drawCenteredTextScaled(
    //$$     fr: TextRenderer,
    //$$     text: OrderedText,
    //$$     centerX: Float,
    //$$     centerY: Float,
    //$$     scale: Float,
    //$$     color: Int,
    //$$ ) {
    //$$     pushMatrix()
    //$$     translate(centerX, centerY, 0f)
    //$$     scale(scale, scale, 1f)
    //$$     drawContext.drawText(fr, text, -fr.getWidth(text) / 2, -fr.fontHeight / 2, color, false)
    //$$     popMatrix()
    //$$ }
    //$$
    //$$ @JvmStatic
    //$$ fun drawTexturedRegion(
    //$$     texture: Identifier,
    //$$     x: Int, y: Int, width: Int, height: Int,
    //$$     u: Float, v: Float, regionWidth: Int, regionHeight: Int,
    //$$     textureWidth: Int, textureHeight: Int
    //$$ ) =
    //$$     drawContext.drawTexture(texture, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight)
    //#endif

    @JvmStatic
    fun enableScissor(x: Int, y: Int, width: Int, height: Int) =
        //#if MC < 1.21
        drawContext.enableScissor(x, y, width, height)
        //#else
        //$$ drawContext.enableScissor(x, y, x + width, y + height)
        //#endif

    @JvmStatic
    fun disableScissor() = drawContext.disableScissor()
}
