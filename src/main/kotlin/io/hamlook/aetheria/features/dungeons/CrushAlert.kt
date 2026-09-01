package io.hamlook.aetheria.features.dungeons

import io.hamlook.aetheria.core.ATHRConfig
import io.hamlook.aetheria.utils.overlay.SimpleOverlay
import io.hamlook.aetheria.init.RegisterEvents
import io.hamlook.aetheria.utils.SoundUtils
import io.hamlook.aetheria.utils.Utils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.entity.boss.EntityWither
import net.minecraft.util.EnumChatFormatting
import io.hamlook.aetheria.api.event.HandleEvent
import org.lwjgl.opengl.GL11
import kotlin.math.sqrt
import io.hamlook.aetheria.events.ASMTickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

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

        val mc = Minecraft.getMinecraft()
        val stats = DungeonStats.getInstance()

        if (mc.theWorld == null || stats == null || !stats.currentFloor.isF7orM7 || !stats.isInStormPhase) {
            alertActive = false
            return
        }

        val storm = mc.theWorld.loadedEntityList.filterIsInstance<EntityWither>()
            .firstOrNull { EnumChatFormatting.getTextWithoutFormattingCodes(it.name) == "Storm" }

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
        val mc = Minecraft.getMinecraft()
        val fr = mc.fontRendererObj

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
