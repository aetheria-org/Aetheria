package io.hamlook.aetheria.features.misc

import io.hamlook.aetheria.core.ATHRConfig
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour
import io.hamlook.aetheria.init.RegisterEvents
import io.hamlook.aetheria.utils.ContainerUtils
import io.hamlook.aetheria.utils.data.SkyblockData
import io.hamlook.aetheria.utils.item.ItemUtils
import io.hamlook.aetheria.utils.render.HighlightUtils

@RegisterEvents
object BazaarOrderHighlight {

    private const val CONTAINER_NAME = "Bazaar Orders"
    private const val CLAIM_LINE = "§eClick to claim!"
    private const val SELL_PREFIX = "§6§lSELL"
    private const val BUY_PREFIX = "§a§lBUY"

    init {
        HighlightUtils.registerHighlighter { gui, slot ->
            if (!SkyblockData.isOnSkyblock()) return@registerHighlighter null
            val config = ATHRConfig.feature?.misc?.bazaarOrders ?: return@registerHighlighter null
            if (!config.highlightSellOrders && !config.highlightBuyOrders) return@registerHighlighter null

            val container = ContainerUtils.getOpenChest(gui) ?: return@registerHighlighter null
            if (ContainerUtils.getTitle(container)?.contains(CONTAINER_NAME) != true) {
                return@registerHighlighter null
            }

            val stack = slot.stack ?: return@registerHighlighter null
            if (ItemUtils.getLoreLines(stack).none { it == CLAIM_LINE }) return@registerHighlighter null

            val name = stack.displayName
            when {
                config.highlightSellOrders && name.startsWith(SELL_PREFIX) -> ChromaColour.specialToChromaRGB(config.sellOrderColor)
                config.highlightBuyOrders && name.startsWith(BUY_PREFIX) -> ChromaColour.specialToChromaRGB(config.buyOrderColor)
                else -> null
            }
        }
    }
}