package io.hamlook.aetheria.utils.compat

//#if MC < 1.16
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.WorldRenderer
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.client.renderer.vertex.VertexFormat
//#endif
//#if MC > 1.21
//$$ import com.mojang.blaze3d.systems.RenderSystem
//$$ import net.minecraft.client.render.BufferBuilder
//$$ import net.minecraft.client.render.BufferRenderer
//$$ import net.minecraft.client.render.GameRenderer
//$$ import net.minecraft.client.render.Tessellator
//$$ import net.minecraft.client.render.VertexFormat
//$$ import net.minecraft.client.render.VertexFormat.DrawMode
//$$ import net.minecraft.client.render.VertexFormats
//$$ import org.joml.Matrix4f
//#endif
import org.lwjgl.opengl.GL11

object TessellatorCompat {

    @JvmField val QUADS: Int = GL11.GL_QUADS
    @JvmField val LINES: Int = GL11.GL_LINES
    @JvmField val TRIANGLES: Int = GL11.GL_TRIANGLES
    @JvmField val TRIANGLE_FAN: Int = GL11.GL_TRIANGLE_FAN
    @JvmField val LINE_STRIP: Int = GL11.GL_LINE_STRIP

    @JvmField
    val POSITION_TEX: VertexFormat =
        //#if MC < 1.16
        DefaultVertexFormats.POSITION_TEX
        //#else
        //$$ VertexFormats.POSITION_TEXTURE
        //#endif

    @JvmField
    val POSITION_COLOR: VertexFormat =
        //#if MC < 1.16
        DefaultVertexFormats.POSITION_COLOR
        //#else
        //$$ VertexFormats.POSITION_COLOR
        //#endif

    @JvmField
    val POSITION_TEX_COLOR: VertexFormat =
        //#if MC < 1.16
        DefaultVertexFormats.POSITION_TEX_COLOR
        //#else
        //$$ VertexFormats.POSITION_TEXTURE_COLOR
        //#endif

    @JvmField
    val POSITION: VertexFormat =
        //#if MC < 1.16
        DefaultVertexFormats.POSITION
        //#else
        //$$ VertexFormats.POSITION
        //#endif

    @JvmStatic
    fun beginDraw(mode: Int, format: VertexFormat): VertexBuilder =
        //#if MC < 1.16
        beginDrawLegacy(mode, format)
        //#else
        //$$ beginDrawModern(mode, format)
        //#endif

    //#if MC < 1.16
    private fun beginDrawLegacy(mode: Int, format: VertexFormat): VertexBuilder {
        val tess = Tessellator.getInstance()
        val wr = tess.worldRenderer
        wr.begin(mode, format)
        return VertexBuilder(wr, tess)
    }
    //#endif

    //#if MC > 1.21
    //$$ private fun beginDrawModern(mode: Int, format: VertexFormat): VertexBuilder {
    //$$     val drawMode = when (mode) {
    //$$         GL11.GL_QUADS -> DrawMode.QUADS
    //$$         GL11.GL_LINES -> DrawMode.LINES
    //$$         GL11.GL_TRIANGLES -> DrawMode.TRIANGLES
    //$$         GL11.GL_TRIANGLE_FAN -> DrawMode.TRIANGLE_FAN
    //$$         GL11.GL_LINE_STRIP -> DrawMode.LINE_STRIP
    //$$         else -> DrawMode.QUADS
    //$$     }
    //$$     setShaderFor(format)
    //$$     return VertexBuilder(Tessellator.getInstance().begin(drawMode, format))
    //$$ }
    //$$
    //$$ private fun setShaderFor(format: VertexFormat) {
    //$$     val program = when (format) {
    //$$         POSITION_TEX -> GameRenderer.getPositionTexProgram()
    //$$         POSITION_COLOR -> GameRenderer.getPositionColorProgram()
    //$$         POSITION_TEX_COLOR -> GameRenderer.getPositionTexColorProgram()
    //$$         else -> GameRenderer.getPositionProgram()
    //$$     }
    //$$     RenderSystem.setShader { program }
    //$$ }
    //#endif
}

//#if MC < 1.16
class VertexBuilder internal constructor(
    val worldRenderer: WorldRenderer,
    private val tess: Tessellator
) {

    fun pos(x: Double, y: Double, z: Double): VertexBuilder {
        worldRenderer.pos(x, y, z)
        return this
    }

    fun tex(u: Double, v: Double): VertexBuilder {
        worldRenderer.tex(u, v)
        return this
    }

    fun color(r: Float, g: Float, b: Float, a: Float): VertexBuilder {
        worldRenderer.color(r, g, b, a)
        return this
    }

    fun color(r: Int, g: Int, b: Int, a: Int): VertexBuilder {
        worldRenderer.color(r, g, b, a)
        return this
    }

    fun endVertex() {
        worldRenderer.endVertex()
    }

    fun draw() {
        tess.draw()
    }
}
//#else
//$$ class VertexBuilder internal constructor(private val buffer: BufferBuilder) {
//$$
//$$     fun pos(x: Double, y: Double, z: Double): VertexBuilder {
//$$         buffer.vertex(x.toFloat(), y.toFloat(), z.toFloat())
//$$         return this
//$$     }
//$$
//$$     fun tex(u: Double, v: Double): VertexBuilder {
//$$         buffer.texture(u.toFloat(), v.toFloat())
//$$         return this
//$$     }
//$$
//$$     fun color(r: Float, g: Float, b: Float, a: Float): VertexBuilder {
//$$         buffer.color(r, g, b, a)
//$$         return this
//$$     }
//$$
//$$     fun color(r: Int, g: Int, b: Int, a: Int): VertexBuilder {
//$$         buffer.color(r, g, b, a)
//$$         return this
//$$     }
//$$
//$$     fun endVertex() {
//$$     }
//$$
//$$     fun draw() {
//$$         val context = DrawContextUtils.drawContextOrNull ?: return BufferRenderer.drawWithGlobalProgram(buffer.end())
//$$         val stack = RenderSystem.getModelViewStack()
//$$         val saved = Matrix4f(stack)
//$$         stack.set(saved).mul(context.matrices.peek().positionMatrix)
//$$         RenderSystem.applyModelViewMatrix()
//$$         try {
//$$             BufferRenderer.drawWithGlobalProgram(buffer.end())
//$$         } finally {
//$$             stack.set(saved)
//$$             RenderSystem.applyModelViewMatrix()
//$$         }
//$$     }
//$$ }
//#endif
