package com.jef.justenoughfakepixel.features.farming

import com.jef.justenoughfakepixel.core.JefConfig
import com.jef.justenoughfakepixel.core.config.utils.Position
import com.jef.justenoughfakepixel.events.BlockBreakEvent
import com.jef.justenoughfakepixel.init.RegisterEvents
import com.jef.justenoughfakepixel.utils.data.SkyblockData
import com.jef.justenoughfakepixel.utils.overlay.Overlay
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

    private val config get() = JefConfig.feature.farming.bps

    private fun isInFarmingLocation(): Boolean {
        val location = SkyblockData.getCurrentLocation()
        return location == SkyblockData.Location.BARN || location == SkyblockData.Location.PRIVATE_ISLAND
    }

    @SubscribeEvent
    fun onBlockBreak(event: BlockBreakEvent) {
        if (!config.bpsCalculator) return
        if (config.bpsRequireFarmingIsland && !isInFarmingLocation()) return

        if (blocksBroken == 0) startTime = System.currentTimeMillis()
        blocksBroken++
        lastBreakTime = System.currentTimeMillis()
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START || !config.bpsCalculator) return

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
        config.bpsCalculator && (!config.bpsRequireFarmingIsland || isInFarmingLocation())
}