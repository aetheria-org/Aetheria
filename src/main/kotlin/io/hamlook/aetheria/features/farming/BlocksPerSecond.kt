package io.hamlook.aetheria.features.farming

import io.hamlook.aetheria.core.ATHRConfig
import io.hamlook.aetheria.utils.Position
import io.hamlook.aetheria.events.BlockBreakEvent
import io.hamlook.aetheria.init.RegisterEvents
import io.hamlook.aetheria.utils.data.SkyblockData
import io.hamlook.aetheria.utils.overlay.Overlay
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

@RegisterEvents
class BPSOverlay : Overlay(50, 20) {

    companion object {
        @JvmStatic
        private var instance: BPSOverlay? = null

        @JvmStatic
        fun getInstance(): BPSOverlay? = instance
    }

    init {
        instance = this
    }

    private var blocksBroken = 0
    private var startTime = 0L
    private var lastBreakTime = 0L
    private var currentBPS = 0.0

    private val config get() = ATHRConfig.feature.farming.bps

    private fun isInFarmingLocation(): Boolean {
        val location = SkyblockData.getCurrentLocation()
        return location == SkyblockData.Location.BARN || location == SkyblockData.Location.PRIVATE_ISLAND || location == SkyblockData.Location.GARDEN
    }

    @SubscribeEvent
    fun onBlockBreak(event: BlockBreakEvent) {
        if (config.bpsRequireFarmingIsland && !isInFarmingLocation()) return

        if (blocksBroken == 0) startTime = System.currentTimeMillis()
        blocksBroken++
        lastBreakTime = System.currentTimeMillis()
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return

        if (blocksBroken > 0) {
            val timeInSeconds = (System.currentTimeMillis() - startTime) / 1000.0
            if (timeInSeconds > 0) currentBPS = blocksBroken / timeInSeconds

            val timeout = config.bpsResetTimeout * 1000L
            if (System.currentTimeMillis() - lastBreakTime > timeout) {
                blocksBroken = 0
                startTime = 0L
                lastBreakTime = 0L
                currentBPS = 0.0
            }
        }
    }

    override fun getLines(preview: Boolean): List<String> {
        if (!preview && config.bpsRequireFarmingIsland && !isInFarmingLocation()) {
            return emptyList()
        }

        val bpsText = if (preview) {
            "20.00"
        } else {
            String.format("%.2f", currentBPS)
        }

        return listOf("§f$bpsText §bBlocks/s")
    }

    override fun getPosition(): Position = config.bpsPosition
    override fun getScale(): Float = config.bpsScale
    override fun getBgColor(): Int = config.bpsBgColor
    override fun getCornerRadius(): Int = config.bpsCornerRadius
    override fun isEnabled(): Boolean =
        config.bpsCalculator
                && (!config.bpsRequireFarmingIsland || isInFarmingLocation())
                && (!config.showOnlyWhileFarming || FarmingApi.isCurrentlyFarming())

    override fun hideOnChat() = config.hideOnChat
    override fun hideOnTab() = config.hideOnTab
    override fun hideOnDebug() = config.hideOnDebug
}