package io.hamlook.aetheria.features.dungeons

import io.hamlook.aetheria.api.event.HandleEvent
import io.hamlook.aetheria.core.ATHRConfig
import io.hamlook.aetheria.events.ASMTickEvent
import io.hamlook.aetheria.init.RegisterEvents
import io.hamlook.aetheria.utils.SoundUtils
import io.hamlook.aetheria.utils.Utils
import io.hamlook.aetheria.utils.compat.MinecraftCompat
import io.hamlook.aetheria.utils.compat.WorldCompat
import io.hamlook.aetheria.utils.overlay.SimpleOverlay
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.entity.boss.EntityWither
import net.minecraft.util.EnumChatFormatting
import net.minecraftforge.fml.common.gameevent.TickEvent
import org.lwjgl.opengl.GL11
import kotlin.math.sqrt

@RegisterEvents
class CrushAlert : SimpleOverlay() {

    private data class Pillar(val x: Double, val z: Double, val name: String, val color: String)

    private val pillars = listOf(
        Pillar(54.0, 48.0, "Green", "§a"),
        Pillar(54.0, 73.0, "Yellow", "§e"),
        Pillar(108.0, 73.0, "Purple", "§5")
    )

    private var alertActive = false
    private var activePillar: Pillar? = null
    private val config get() = ATHRConfig.feature?.dungeons?.crushAlert

    @HandleEvent
    fun onTick(event: ASMTickEvent) {
        if (event.phase != TickEvent.Phase.END) return
        val cfg = config ?: return
        if (!cfg.enabled) {
            alertActive = false; return
        }

        val mc = MinecraftCompat.getMinecraft()
        val stats = DungeonStats.getInstance()

        if (MinecraftCompat.getLocalWorld() == null || stats == null || !stats.currentFloor.isF7orM7 || !stats.isInStormPhase) {
            alertActive = false
            return
        }

        val storm = MinecraftCompat.getLocalWorld()?.let { WorldCompat.getAllEntities(it) }?.filterIsInstance<EntityWither>()
            ?.firstOrNull { EnumChatFormatting.getTextWithoutFormattingCodes(it.name) == "Storm" }

        if (storm == null) {
            alertActive = false; return
        }

        val range = cfg.alertRange.toDouble()
        val wasActive = alertActive

        activePillar = pillars.firstOrNull { pillar ->
            val dx = storm.posX - pillar.x
            val dz = storm.posZ - pillar.z
            sqrt(dx * dx + dz * dz) <= range
        }

        alertActive = activePillar != null

        if (alertActive && !wasActive) SoundUtils.playSound("note.pling", 1.0f, 1.5f)
    }

    override fun shouldRender() = alertActive && config?.enabled == true

    override fun render(sr: ScaledResolution) {
        val mc = MinecraftCompat.getMinecraft()
        val fr = MinecraftCompat.getFontRenderer()

        val pillar = activePillar
        val pillarText = if (pillar != null) " ${pillar.color}${pillar.name}" else ""
        val text = "${EnumChatFormatting.RED}${EnumChatFormatting.BOLD}CRUSH STORM$pillarText"

        GL11.glPushMatrix()
        GL11.glScalef(2f, 2f, 1f)
        Utils.drawStringCentered(
            text, fr, sr.scaledWidth / 4f, (sr.scaledHeight / 2f - 40f) / 2f, true, 0xFFFFFF
        )
        GL11.glPopMatrix()
    }
}
