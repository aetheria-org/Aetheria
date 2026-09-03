package io.hamlook.aetheria.utils.compat

import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.WorldRenderer
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.client.renderer.vertex.VertexFormat
import org.lwjgl.opengl.GL11

object TessellatorCompat {

    @JvmField val QUADS: Int = GL11.GL_QUADS
    @JvmField val LINES: Int = GL11.GL_LINES
    @JvmField val TRIANGLES: Int = GL11.GL_TRIANGLES
    @JvmField val LINE_STRIP: Int = GL11.GL_LINE_STRIP

    @JvmField val POSITION_TEX = DefaultVertexFormats.POSITION_TEX
    @JvmField val POSITION_COLOR = DefaultVertexFormats.POSITION_COLOR
    @JvmField val POSITION_TEX_COLOR = DefaultVertexFormats.POSITION_TEX_COLOR
    @JvmField val POSITION = DefaultVertexFormats.POSITION

    @JvmStatic
    fun beginDraw(mode: Int, format: VertexFormat): VertexBuilder {
        val tess = Tessellator.getInstance()
        val wr = tess.worldRenderer
        wr.begin(mode, format)
        return VertexBuilder(wr, tess)
    }
}

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
