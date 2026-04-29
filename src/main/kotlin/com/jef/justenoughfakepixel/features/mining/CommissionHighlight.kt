package com.jef.justenoughfakepixel.features.mining

import com.jef.justenoughfakepixel.core.JefConfig
import com.jef.justenoughfakepixel.init.RegisterEvents
import com.jef.justenoughfakepixel.utils.ColorUtils
import com.jef.justenoughfakepixel.utils.item.ItemUtils
import com.jef.justenoughfakepixel.utils.render.HighlightUtils
import net.minecraft.inventory.ContainerChest
import net.minecraft.item.ItemStack

@RegisterEvents
object CommissionHighlight {

    private const val COMPLETED_HIGHLIGHT_COLOR = 0x8000FF00.toInt()

    init {
        HighlightUtils.registerHighlighter { gui, slot ->
            if (JefConfig.feature?.mining?.commissionHighlight != true) return@registerHighlighter null

            val container = gui.inventorySlots as? ContainerChest ?: return@registerHighlighter null
            if (!container.lowerChestInventory.displayName.unformattedText.contains("Commissions")) return@registerHighlighter null

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