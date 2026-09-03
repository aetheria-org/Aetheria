package io.hamlook.aetheria.utils.compat

import net.minecraft.client.gui.ScaledResolution

/**
 * Version-agnostic screen resolution and mouse coordinate helpers.
 * [getScaledResolution] is keyed-cached (displayWidth, displayHeight, guiScale) to avoid
 * per-frame allocation — call it instead of constructing [ScaledResolution] directly.
 */
object GuiScreenUtils {
    private var cachedScaledResolution: ScaledResolution? = null
    private var cachedKey = 0

    @JvmStatic
    fun getScaledResolution(): ScaledResolution {
        val mc = MinecraftCompat.getMinecraft()
        val key = MinecraftCompat.getDisplayWidth() * 31 + MinecraftCompat.getDisplayHeight() * 17 + MinecraftCompat.getGameSettings().guiScale * 7
        val cached = cachedScaledResolution
        if (cached == null || key != cachedKey) {
            cachedScaledResolution = ScaledResolution(mc)
            cachedKey = key
        }
        return cachedScaledResolution!!
    }

    @JvmStatic
    fun getScaledWindowWidth(): Int = getScaledResolution().scaledWidth

    @JvmStatic
    fun getScaledWindowHeight(): Int = getScaledResolution().scaledHeight

    @JvmStatic
    fun getScaleFactor(): Int = getScaledResolution().scaleFactor

    @JvmStatic
    fun getDisplayWidth(): Int = MinecraftCompat.getDisplayWidth()

    @JvmStatic
    fun getDisplayHeight(): Int = MinecraftCompat.getDisplayHeight()

    @JvmStatic
    fun getMouseX(): Int {
        val sr = getScaledResolution()
        return MouseCompat.getX() * sr.scaledWidth / MinecraftCompat.getDisplayWidth()
    }

    @JvmStatic
    fun getMouseY(): Int {
        val sr = getScaledResolution()
        val h = sr.scaledHeight
        return h - MouseCompat.getY() * h / MinecraftCompat.getDisplayHeight() - 1
    }

    @JvmStatic
    fun getMousePos(): Pair<Int, Int> = getMouseX() to getMouseY()
}
