package io.hamlook.aetheria.features.misc

import io.hamlook.aetheria.Resources
import io.hamlook.aetheria.core.ATHRConfig
import io.hamlook.aetheria.init.RegisterEvents
import io.hamlook.aetheria.utils.chat.ChatUtils
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat
import io.hamlook.aetheria.utils.compat.MinecraftCompat
import io.hamlook.aetheria.utils.overlay.SimpleOverlay
import io.hamlook.aetheria.utils.render.RenderUtils
import net.minecraft.client.gui.ScaledResolution
import java.awt.Color

@RegisterEvents
object DVD : SimpleOverlay() {

    private const val ASPECT_RATIO = 0.553
    private val mc = MinecraftCompat.getMinecraft()
    private var lastUpdateTime = System.nanoTime()
    private var color = 0xFFFFFF
    private var x = 100.0
    private var y = 100.0
    private var dx = 1.0
    private var dy = 1.0
    private var initialized = false
    private var justInitialized = false

    private fun getBoxWidth() = ATHRConfig.feature.misc.dvd.dvdSize
    private fun getBoxHeight() = (getBoxWidth() * ASPECT_RATIO).toInt()

    fun forceCornerHit() {
        if (!ATHRConfig.feature.misc.dvd.dvdScreensaver) {
            ChatUtils.sendMessage("§cDVD is not enabled!")
            return
        }
        x = 0.0
        y = 0.0
        dx = 1.0
        dy = 1.0
        initialized = true
        justInitialized = false
        lastUpdateTime = System.nanoTime()
        ChatUtils.sendMessage("§aForcing corner hit...")
    }

    override fun shouldRender(): Boolean {
        if (!ATHRConfig.feature.misc.dvd.dvdScreensaver) {
            initialized = false
            return false
        }
        return true
    }

    override fun hideOnChat() = ATHRConfig.feature.misc.dvd.hideOnChat
    override fun hideOnTab() = ATHRConfig.feature.misc.dvd.hideOnTab
    override fun hideOnDebug() = ATHRConfig.feature.misc.dvd.hideOnDebug

    override fun render(sr: ScaledResolution) {
        if (!initialized) {
            x = sr.scaledWidth / 2.0
            y = sr.scaledHeight / 2.0
            initialized = true
            justInitialized = true
            lastUpdateTime = System.nanoTime()
        }

        updatePosition()

        val boxWidth = getBoxWidth()
        val boxHeight = getBoxHeight()

        mc.textureManager.bindTexture(Resources.DVD_LOGO)

        val r = (color shr 16 and 0xFF) / 255f
        val g = (color shr 8 and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        GlStateManagerCompat.color(r, g, b, 1f)

        RenderUtils.drawTexturedRect(x.toFloat(), y.toFloat(), boxWidth.toFloat(), boxHeight.toFloat())

        GlStateManagerCompat.color(1f, 1f, 1f, 1f)
    }

    private fun updatePosition() {
        val currentTime = System.nanoTime()
        val deltaTime = (currentTime - lastUpdateTime) / 1_000_000_000.0
        lastUpdateTime = currentTime

        val speed = 30.0
        val movement = speed * deltaTime
        x += dx * movement
        y += dy * movement

        val sr = ScaledResolution(mc)
        val boxWidth = getBoxWidth()
        val boxHeight = getBoxHeight()

        if (x < 0) x = 0.0
        if (y < 0) y = 0.0
        if (x + boxWidth > sr.scaledWidth) x = (sr.scaledWidth - boxWidth).toDouble()
        if (y + boxHeight > sr.scaledHeight) y = (sr.scaledHeight - boxHeight).toDouble()

        val tolerance = 0.5
        var hitCorner = false

        if (x <= 0) {
            x = 0.0
            dx = -dx
            color = randomColor()
            hitCorner = y <= tolerance || y + boxHeight >= sr.scaledHeight - tolerance
        } else if (x + boxWidth >= sr.scaledWidth) {
            x = (sr.scaledWidth - boxWidth).toDouble()
            dx = -dx
            color = randomColor()
            hitCorner = y <= tolerance || y + boxHeight >= sr.scaledHeight - tolerance
        }

        if (y <= 0) {
            y = 0.0
            dy = -dy
            color = randomColor()
            hitCorner = hitCorner || x <= tolerance || x + boxWidth >= sr.scaledWidth - tolerance
        } else if (y + boxHeight >= sr.scaledHeight) {
            y = (sr.scaledHeight - boxHeight).toDouble()
            dy = -dy
            color = randomColor()
            hitCorner = hitCorner || x <= tolerance || x + boxWidth >= sr.scaledWidth - tolerance
        }

        if (hitCorner && !justInitialized) {
            MinecraftCompat.getLocalPlayer()?.playSound("mob.enderdragon.growl", 1.0f, 1.0f)
            ChatUtils.sendMessage("§d§lâœ¦ §5§lLEGENDARY CORNER HIT! §d§lâœ¦")
        }

        justInitialized = false
    }

    private fun randomColor(): Int {
        val javaColor = Color.getHSBColor((Math.random() * 360).toFloat(), 1.0f, 0.8f)
        return (javaColor.red shl 16) or (javaColor.green shl 8) or javaColor.blue
    }
}
