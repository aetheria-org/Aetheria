package io.hamlook.aetheria.utils.compat

import net.minecraft.client.renderer.GlStateManager

/**
 * Shim MatrixStack that wraps GlStateManager matrix operations on 1.8.9.
 * When preprocessor is added, this file gets an empty override
 * in versions/1.21.5/src/ so Minecraft's real MatrixStack is used instead.
 */
class MatrixStack {

    fun pushMatrix() = GlStateManager.pushMatrix()

    fun popMatrix() = GlStateManager.popMatrix()

    fun translate(x: Float, y: Float, z: Float) = GlStateManager.translate(x, y, z)

    fun translate(x: Double, y: Double, z: Double) = GlStateManager.translate(x, y, z)

    fun scale(x: Float, y: Float, z: Float) = GlStateManager.scale(x, y, z)

    fun rotate(angle: Float, x: Float, y: Float, z: Float) = GlStateManager.rotate(angle, x, y, z)

    fun loadIdentity() = GlStateManager.loadIdentity()
}
