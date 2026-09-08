package io.hamlook.aetheria.utils.compat

/**
 * Central bridge for rendering operations.
 * Feature code calls DrawContextUtils for translate/scale/push/pop/rotate.
 * On 1.8.9, routes through the fake DrawContext → MatrixStack → GlStateManager.
 * DrawContext.matrices instead.
 */
object DrawContextUtils {

    private val _drawContext = DrawContext()
    val drawContext: DrawContext get() = _drawContext

    // Matrix operations
    @JvmStatic
    fun pushMatrix() = drawContext.matrices.pushMatrix()

    @JvmStatic
    fun popMatrix() = drawContext.matrices.popMatrix()

    @JvmStatic
    fun translate(x: Float, y: Float, z: Float) = drawContext.matrices.translate(x, y, z)

    @JvmStatic
    fun translate(x: Double, y: Double, z: Double) = drawContext.matrices.translate(x, y, z)

    @JvmStatic
    fun scale(x: Float, y: Float, z: Float) = drawContext.matrices.scale(x, y, z)

    @JvmStatic
    fun rotate(angle: Float, x: Float, y: Float, z: Float) =
        drawContext.matrices.rotate(angle, x, y, z)

    @JvmStatic
    fun loadIdentity() = drawContext.matrices.loadIdentity()

    // Scoped helpers
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

    // DrawContext delegation
    @JvmStatic
    fun fill(left: Int, top: Int, right: Int, bottom: Int, color: Int) =
        drawContext.fill(left, top, right, bottom, color)

    @JvmStatic
    fun enableScissor(x: Int, y: Int, width: Int, height: Int) =
        drawContext.enableScissor(x, y, width, height)

    @JvmStatic
    fun disableScissor() = drawContext.disableScissor()
}
