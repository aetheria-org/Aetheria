package io.hamlook.aetheria.features.farming

import io.hamlook.aetheria.core.ATHRConfig
import io.hamlook.aetheria.init.RegisterEvents
import io.hamlook.aetheria.utils.Position
import io.hamlook.aetheria.utils.Utils
import io.hamlook.aetheria.utils.overlay.Overlay

@RegisterEvents
class FarmingTrackerOverlay : Overlay(160, 70) {

    companion object {
        @JvmStatic
        private var instance: FarmingTrackerOverlay? = null

        @JvmStatic
        fun getInstance(): FarmingTrackerOverlay? = instance
    }

    init {
        instance = this
    }

    private val config get() = ATHRConfig.feature.farming.farmingTracker

    // Ordinal 0 = title, 1 = value/rate, 2+i = crop family line for FarmingTracker.getCrops()[i].
    // Must stay in sync with FarmingTrackerConfig's exampleText ordering.
    private fun lineForOrdinal(ordinal: Int, preview: Boolean): String? {
        if (ordinal == 0) {
            val pausedTag = if (!preview && FarmingTracker.isPaused()) " §7[Paused]" else ""
            return "§a§lFarming Tracker$pausedTag"
        }
        if (ordinal == 1) {
            if (preview) return "§76,144,000 coins §7(3.2M/h)"
            return "§7${Utils.shortNumberFormat(FarmingTracker.currentValue(), 0)} coins §7(${Utils.shortNumberFormat(FarmingTracker.coinsPerHour(), 0)}/h)"
        }

        val crops = FarmingTracker.getCrops()
        val index = ordinal - 2
        if (index < 0 || index >= crops.size) return null
        val crop = crops[index]

        if (preview) {
            val enchLabel = "E.${crop.displayName}"
            return if (crop.blockId != null) {
                "§a${crop.displayName}: §f12 §7$enchLabel: §f3"
            } else {
                "§a${crop.displayName}: §f12 §7$enchLabel: §f3"
            }
        }

        val raw = FarmingTracker.getCount(crop.rawId)
        val ench = FarmingTracker.getCount(crop.enchantedId)
        val block = if (crop.blockId != null) FarmingTracker.getCount(crop.blockId) else 0L

        if (raw == 0L && ench == 0L && block == 0L) return null

        val parts = mutableListOf<String>()
        if (raw > 0) parts.add("§a${crop.displayName}: §f${Utils.shortNumberFormat(raw.toDouble(), 0)}")
        if (ench > 0) parts.add("§7E.${crop.displayName}: §f${Utils.shortNumberFormat(ench.toDouble(), 0)}")
        if (block > 0 && crop.blockChatName != null) parts.add("§7${crop.blockChatName}: §f${Utils.shortNumberFormat(block.toDouble(), 0)}")

        return parts.joinToString(" ")
    }

    override fun getLines(preview: Boolean): List<String> {
        val lines = mutableListOf<String>()
        for (ordinal in config.farmingDisplayLines) {
            val line = lineForOrdinal(ordinal, preview)
            if (line != null) lines.add(line)
        }
        return lines
    }

    override fun getPosition(): Position = config.farmingTrackerPosition
    override fun getScale(): Float = config.farmingTrackerScale
    override fun getBgColor(): Int = config.farmingTrackerBgColor
    override fun getCornerRadius(): Int = config.farmingTrackerCornerRadius
    override fun isEnabled(): Boolean = config.enabled

    override fun hideOnChat() = config.hideOnChat
    override fun hideOnTab() = config.hideOnTab
    override fun hideOnDebug() = config.hideOnDebug
}
