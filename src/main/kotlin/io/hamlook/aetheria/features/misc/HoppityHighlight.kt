package io.hamlook.aetheria.features.misc

import io.hamlook.aetheria.core.ATHRConfig
import io.hamlook.aetheria.init.RegisterEvents
import io.hamlook.aetheria.utils.ColorUtils
import io.hamlook.aetheria.utils.ContainerUtils
import io.hamlook.aetheria.utils.render.HighlightUtils

@RegisterEvents
object HoppityHighlight {

    private const val HIGHLIGHT_COLOR = 0x8000FF00.toInt()

    init {
        HighlightUtils.registerHighlighter { gui, slot ->
            if (ATHRConfig.feature?.misc?.hoppityHighlight != true) return@registerHighlighter null

            val container = ContainerUtils.getOpenChest(gui) ?: return@registerHighlighter null
            if (ContainerUtils.getTitle(container)?.contains("Hoppity") != true) return@registerHighlighter null

            val stack = slot.stack ?: return@registerHighlighter null
            if (ColorUtils.stripColor(stack.displayName).contains("NEW RABBIT!")) HIGHLIGHT_COLOR else null
        }
    }
}