package io.hamlook.aetheria.utils.compat

import net.minecraft.client.renderer.GlStateManager

object GlStateManagerCompat {
    // Matrix operations
    private val _matrices = MatrixStack()

    @JvmStatic fun pushMatrix() = _matrices.pushMatrix()
    @JvmStatic fun popMatrix() = _matrices.popMatrix()
    @JvmStatic fun translate(x: Double, y: Double, z: Double) = _matrices.translate(x, y, z)
    @JvmStatic fun scale(x: Float, y: Float, z: Float) = _matrices.scale(x, y, z)
    @JvmStatic fun rotate(angle: Float, x: Float, y: Float, z: Float) = _matrices.rotate(angle, x, y, z)
    @JvmStatic fun loadIdentity() = _matrices.loadIdentity()

    // Blend
    @JvmStatic fun enableBlend() = GlStateManager.enableBlend()
    @JvmStatic fun disableBlend() = GlStateManager.disableBlend()
    @JvmStatic fun tryBlendFuncSeparate(src: Int, dst: Int, srcAlpha: Int, dstAlpha: Int) =
        GlStateManager.tryBlendFuncSeparate(src, dst, srcAlpha, dstAlpha)
    @JvmStatic fun blendFunc(src: Int, dst: Int) = GlStateManager.blendFunc(src, dst)

    // Alpha
    @JvmStatic fun enableAlpha() = GlStateManager.enableAlpha()
    @JvmStatic fun disableAlpha() = GlStateManager.disableAlpha()

    // Texture
    @JvmStatic fun enableTexture2D() = GlStateManager.enableTexture2D()
    @JvmStatic fun disableTexture2D() = GlStateManager.disableTexture2D()
    @JvmStatic fun bindTexture(textureId: Int) = GlStateManager.bindTexture(textureId)

    // Color
    @JvmStatic fun color(r: Float, g: Float, b: Float, a: Float) = GlStateManager.color(r, g, b, a)
    @JvmStatic fun color(r: Float, g: Float, b: Float) = GlStateManager.color(r, g, b)
    @JvmStatic fun resetColor() = GlStateManager.resetColor()

    // Depth
    @JvmStatic fun enableDepth() = GlStateManager.enableDepth()
    @JvmStatic fun disableDepth() = GlStateManager.disableDepth()
    @JvmStatic fun depthFunc(func: Int) = GlStateManager.depthFunc(func)
    @JvmStatic fun depthMask(flag: Boolean) = GlStateManager.depthMask(flag)

    // Cull
    @JvmStatic fun enableCull() = GlStateManager.enableCull()
    @JvmStatic fun disableCull() = GlStateManager.disableCull()

    // Lighting
    @JvmStatic fun enableLighting() = GlStateManager.enableLighting()
    @JvmStatic fun disableLighting() = GlStateManager.disableLighting()
    @JvmStatic fun enableLight(light: Int) = GlStateManager.enableLight(light)
    @JvmStatic fun disableLight(light: Int) = GlStateManager.disableLight(light)

    // Color mask
    @JvmStatic fun colorMask(r: Boolean, g: Boolean, b: Boolean, a: Boolean) =
        GlStateManager.colorMask(r, g, b, a)

    // Fog
    @JvmStatic fun enableFog() = GlStateManager.enableFog()
    @JvmStatic fun disableFog() = GlStateManager.disableFog()

    // Attrib
    @JvmStatic fun pushAttrib() = GlStateManager.pushAttrib()
    @JvmStatic fun popAttrib() = GlStateManager.popAttrib()

    // Matrix mode
    @JvmStatic fun matrixMode(mode: Int) = GlStateManager.matrixMode(mode)

    // Shade model
    @JvmStatic fun shadeModel(mode: Int) = GlStateManager.shadeModel(mode)

    // Active texture
    @JvmStatic fun setActiveTexture(texture: Int) = GlStateManager.setActiveTexture(texture)

    // Ortho
    @JvmStatic fun ortho(
        left: Double, right: Double, bottom: Double, top: Double,
        zNear: Double, zFar: Double
    ) = GlStateManager.ortho(left, right, bottom, top, zNear, zFar)

    // Rescale normal
    @JvmStatic fun enableRescaleNormal() = GlStateManager.enableRescaleNormal()
    @JvmStatic fun disableRescaleNormal() = GlStateManager.disableRescaleNormal()

    // Color material
    @JvmStatic fun enableColorMaterial() = GlStateManager.enableColorMaterial()

    // Clear
    @JvmStatic fun clear(mask: Int) = GlStateManager.clear(mask)

    // Viewport
    @JvmStatic fun viewport(x: Int, y: Int, width: Int, height: Int) =
        GlStateManager.viewport(x, y, width, height)
}
