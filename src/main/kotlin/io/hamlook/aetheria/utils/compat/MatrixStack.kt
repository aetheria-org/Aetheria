package io.hamlook.aetheria.utils.compat

//#if MC < 1.21
import net.minecraft.client.renderer.GlStateManager

/**
 * Shim MatrixStack that wraps GlStateManager matrix operations on 1.8.9.
 */
class MatrixStack {

    fun pushMatrix() {
        GlStateManager.pushMatrix()
    }

    fun popMatrix() {
        GlStateManager.popMatrix()
    }

    fun translate(x: Float, y: Float, z: Float) {
        GlStateManager.translate(x, y, z)
    }

    fun translate(x: Double, y: Double, z: Double) {
        GlStateManager.translate(x, y, z)
    }

    fun scale(x: Float, y: Float, z: Float) {
        GlStateManager.scale(x, y, z)
    }

    fun rotate(angle: Float, x: Float, y: Float, z: Float) {
        GlStateManager.rotate(angle, x, y, z)
    }

    fun loadIdentity() {
        GlStateManager.loadIdentity()
    }
}
//#else
//$$ import net.minecraft.client.util.math.MatrixStack as VanillaMatrixStack
//$$ import org.joml.Quaternionf
//$$
//$$ class MatrixStack(private val vanilla: VanillaMatrixStack) {
//$$
//$$     fun pushMatrix() {
//$$         vanilla.push()
//$$     }
//$$
//$$     fun popMatrix() {
//$$         vanilla.pop()
//$$     }
//$$
//$$     fun translate(x: Float, y: Float, z: Float) {
//$$         vanilla.translate(x, y, z)
//$$     }
//$$
//$$     fun translate(x: Double, y: Double, z: Double) {
//$$         vanilla.translate(x, y, z)
//$$     }
//$$
//$$     fun scale(x: Float, y: Float, z: Float) {
//$$         vanilla.scale(x, y, z)
//$$     }
//$$
//$$     fun rotate(angle: Float, x: Float, y: Float, z: Float) {
//$$         vanilla.multiply(Quaternionf().rotationAxis(angle, x, y, z))
//$$     }
//$$
//$$     fun loadIdentity() {
//$$         vanilla.loadIdentity()
//$$     }
//$$ }
//#endif
