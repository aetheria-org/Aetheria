package io.hamlook.aetheria.features.mining

import io.hamlook.aetheria.core.ATHRConfig
import io.hamlook.aetheria.init.RegisterEvents
import io.hamlook.aetheria.utils.ColorUtils
import io.hamlook.aetheria.utils.ContainerUtils
import io.hamlook.aetheria.utils.item.ItemUtils
import io.hamlook.aetheria.utils.render.HighlightUtils
import net.minecraft.item.ItemStack

@RegisterEvents
object CommissionHighlight {

    private const val COMPLETED_HIGHLIGHT_COLOR = 0x8000FF00.toInt()

    init {
        HighlightUtils.registerHighlighter { gui, slot ->
            if (ATHRConfig.feature?.mining?.commissionHighlight != true) return@registerHighlighter null

            val container = ContainerUtils.getOpenChest(gui) ?: return@registerHighlighter null
            if (ContainerUtils.getTitle(container)?.contains("Commissions") != true) return@registerHighlighter null

            val stack = slot.stack ?: return@registerHighlighter null
            if (isCommissionCompleted(stack)) COMPLETED_HIGHLIGHT_COLOR else null
        }
    }

    private fun isCommissionCompleted(stack: ItemStack): Boolean {
        val loreLines = ItemUtils.getLoreLines(stack)
        return loreLines.any { line ->
            ColorUtils.stripColor(line) == "COMPLETED"
        }
    }
}