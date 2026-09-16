package io.hamlook.aetheria.utils.compat

import net.minecraft.client.renderer.GlStateManager

object GlStateManagerCompat {
    // Matrix operations route through DrawContextUtils on every version: on
    // 1.8.9 that ends at these same GlStateManager calls via the shim, on
    // modern it drives the bound DrawContext matrices. Requires a bound
    // DrawContext on modern (DrawContextUtils.drawContext fails fast otherwise).
    @JvmStatic fun pushMatrix() = DrawContextUtils.pushMatrix()
    @JvmStatic fun popMatrix() = DrawContextUtils.popMatrix()
    @JvmStatic fun translate(x: Double, y: Double, z: Double) = DrawContextUtils.translate(x, y, z)
    @JvmStatic fun scale(x: Float, y: Float, z: Float) = DrawContextUtils.scale(x, y, z)
    @JvmStatic fun rotate(angle: Float, x: Float, y: Float, z: Float) = DrawContextUtils.rotate(angle, x, y, z)
    @JvmStatic fun loadIdentity() = DrawContextUtils.loadIdentity()

    // Blend
    @JvmStatic fun enableBlend() = GlStateManager.enableBlend()
    @JvmStatic fun disableBlend() = GlStateManager.disableBlend()
    @JvmStatic fun tryBlendFuncSeparate(src: Int, dst: Int, srcAlpha: Int, dstAlpha: Int) =
        GlStateManager.tryBlendFuncSeparate(src, dst, srcAlpha, dstAlpha)
    @JvmStatic fun blendFunc(src: Int, dst: Int) = GlStateManager.blendFunc(src, dst)

    // Alpha test does not exist on modern; paired blocks degrade to blend/color calls.
    @JvmStatic
    fun enableAlpha() {
        //#if MC < 1.21
        GlStateManager.enableAlpha()
        //#endif
    }

    @JvmStatic
    fun disableAlpha() {
        //#if MC < 1.21
        GlStateManager.disableAlpha()
        //#endif
    }

    // Texturing is unconditionally on for modern pipelines.
    @JvmStatic
    fun enableTexture2D() {
        //#if MC < 1.21
        GlStateManager.enableTexture2D()
        //#endif
    }

    @JvmStatic
    fun disableTexture2D() {
        //#if MC < 1.21
        GlStateManager.disableTexture2D()
        //#endif
    }

    @JvmStatic fun bindTexture(textureId: Int) = GlStateManager.bindTexture(textureId)

    // Tint goes through the shader color uniform on modern.
    @JvmStatic
    fun color(r: Float, g: Float, b: Float, a: Float) {
        //#if MC < 1.21
        GlStateManager.color(r, g, b, a)
        //#else
        //\$\$ GlStateManager.setShaderColor(r, g, b, a)
        //#endif
    }

    @JvmStatic
    fun color(r: Float, g: Float, b: Float) {
        //#if MC < 1.21
        GlStateManager.color(r, g, b)
        //#else
        //\$\$ GlStateManager.setShaderColor(r, g, b, 1f)
        //#endif
    }

    @JvmStatic
    fun resetColor() {
        //#if MC < 1.21
        GlStateManager.resetColor()
        //#else
        //\$\$ GlStateManager.setShaderColor(1f, 1f, 1f, 1f)
        //#endif
    }

    // Depth
    @JvmStatic fun enableDepth() = GlStateManager.enableDepth()
    @JvmStatic fun disableDepth() = GlStateManager.disableDepth()
    @JvmStatic fun depthFunc(func: Int) = GlStateManager.depthFunc(func)
    @JvmStatic fun depthMask(flag: Boolean) = GlStateManager.depthMask(flag)

    // Cull
    @JvmStatic fun enableCull() = GlStateManager.enableCull()
    @JvmStatic fun disableCull() = GlStateManager.disableCull()

    // Fixed-function lighting is gone; item lighting goes through DiffuseLighting.
    @JvmStatic
    fun enableLighting() {
        //#if MC < 1.21
        GlStateManager.enableLighting()
        //#endif
    }

    @JvmStatic
    fun disableLighting() {
        //#if MC < 1.21
        GlStateManager.disableLighting()
        //#endif
    }

    @JvmStatic
    fun enableLight(light: Int) {
        //#if MC < 1.21
        GlStateManager.enableLight(light)
        //#endif
    }

    @JvmStatic
    fun disableLight(light: Int) {
        //#if MC < 1.21
        GlStateManager.disableLight(light)
        //#endif
    }

    // Color mask
    @JvmStatic fun colorMask(r: Boolean, g: Boolean, b: Boolean, a: Boolean) =
        GlStateManager.colorMask(r, g, b, a)

    // Fog toggles have no modern equivalent; callers only ever disable fog.
    @JvmStatic
    fun enableFog() {
        //#if MC < 1.21
        GlStateManager.enableFog()
        //#endif
    }

    @JvmStatic
    fun disableFog() {
        //#if MC < 1.21
        GlStateManager.disableFog()
        //#endif
    }

    // Attrib stack does not exist on modern; sole caller is EntityHighlight.
    @JvmStatic
    fun pushAttrib() {
        //#if MC < 1.21
        GlStateManager.pushAttrib()
        //#endif
    }

    @JvmStatic
    fun popAttrib() {
        //#if MC < 1.21
        GlStateManager.popAttrib()
        //#endif
    }

    // Projection is owned by RenderSystem on modern; sole caller is the
    // Utils.pushGuiScale block, which degrades to viewport bookkeeping.
    @JvmStatic
    fun matrixMode(mode: Int) {
        //#if MC < 1.21
        GlStateManager.matrixMode(mode)
        //#endif
    }

    // Modern color pipelines always interpolate vertex colors.
    @JvmStatic
    fun shadeModel(mode: Int) {
        //#if MC < 1.21
        GlStateManager.shadeModel(mode)
        //#endif
    }

    // Active texture
    @JvmStatic
    fun setActiveTexture(texture: Int) {
        //#if MC < 1.21
        GlStateManager.setActiveTexture(texture)
        //#else
        //\$\$ GlStateManager.activeTexture(texture)
        //#endif
    }

    // Ortho
    @JvmStatic
    fun ortho(
        left: Double, right: Double, bottom: Double, top: Double,
        zNear: Double, zFar: Double
    ) {
        //#if MC < 1.21
        GlStateManager.ortho(left, right, bottom, top, zNear, zFar)
        //#endif
    }

    // Rescale normal
    @JvmStatic
    fun enableRescaleNormal() {
        //#if MC < 1.21
        GlStateManager.enableRescaleNormal()
        //#endif
    }

    @JvmStatic
    fun disableRescaleNormal() {
        //#if MC < 1.21
        GlStateManager.disableRescaleNormal()
        //#endif
    }

    // Color material
    @JvmStatic
    fun enableColorMaterial() {
        //#if MC < 1.21
        GlStateManager.enableColorMaterial()
        //#endif
    }

    // Clear takes an error-check flag on modern; legacy never error-checked.
    @JvmStatic
    fun clear(mask: Int) {
        //#if MC < 1.21
        GlStateManager.clear(mask)
        //#else
        //\$\$ GlStateManager.clear(mask, false)
        //#endif
    }

    // Viewport
    @JvmStatic fun viewport(x: Int, y: Int, width: Int, height: Int) =
        GlStateManager.viewport(x, y, width, height)
}
