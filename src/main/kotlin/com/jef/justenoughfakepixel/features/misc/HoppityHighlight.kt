package com.jef.justenoughfakepixel.features.misc

import com.jef.justenoughfakepixel.core.JefConfig
import com.jef.justenoughfakepixel.init.RegisterEvents
import com.jef.justenoughfakepixel.utils.ColorUtils
import com.jef.justenoughfakepixel.utils.render.HighlightUtils
import net.minecraft.inventory.ContainerChest

@RegisterEvents
object HoppityHighlight {

    private const val HIGHLIGHT_COLOR = 0x8000FF00.toInt()

    init {
        HighlightUtils.registerHighlighter { gui, slot ->
            if (JefConfig.feature?.misc?.hoppityHighlight != true) return@registerHighlighter null

            val container = gui.inventorySlots as? ContainerChest ?: return@registerHighlighter null
            if (!container.lowerChestInventory.displayName.unformattedText.contains("Hoppity")) return@registerHighlighter null

            val stack = slot.stack ?: return@registerHighlighter null
            if (ColorUtils.stripColor(stack.displayName).contains("NEW RABBIT!")) HIGHLIGHT_COLOR else null
        }
    }
}