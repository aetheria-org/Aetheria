package io.hamlook.aetheria.features.misc

import io.hamlook.aetheria.api.event.HandleEvent
import io.hamlook.aetheria.core.ATHRConfig
import io.hamlook.aetheria.events.ASMGuiDrawEvent
import io.hamlook.aetheria.events.SignSubmitEvent
import io.hamlook.aetheria.init.RegisterEvents
import io.hamlook.aetheria.mixins.accessors.GuiEditSignAccessor
import io.hamlook.aetheria.utils.CalculatorUtils
import io.hamlook.aetheria.utils.Utils
import io.hamlook.aetheria.utils.compat.MinecraftCompat
import net.minecraft.client.gui.inventory.GuiEditSign
import net.minecraft.util.EnumChatFormatting.*
import java.math.BigDecimal

@RegisterEvents
class SignCalculator {

    private var lastSource: String? = null
    private var lastResult: BigDecimal? = null
    private var lastError: String? = null

    @HandleEvent
    fun onSignDrawn(event: ASMGuiDrawEvent) {
        if (!ATHRConfig.feature.misc.signCalculator) return

        val gui = event.gui as? GuiEditSign ?: return
        val sign = (gui as GuiEditSignAccessor).`ATHR$getTileSign`()

        val trigger = sign.signText[1].unformattedText
        if (trigger != "^^^^^^^^^^^^^^^" && trigger != "^^^^^^") return

        val source = sign.signText[0].unformattedText
        refresh(source)

        MinecraftCompat.getMinecraft()
        val result = lastResult
        val rendered = when {
            result != null -> {
                val formatted = CalculatorUtils.FORMAT.format(result)
                if (MinecraftCompat.getFontRenderer().getStringWidth(formatted) > 90) {
                    "$WHITE$lastSource $YELLOW= ${RED}Result too long"
                } else {
                    "$WHITE$lastSource $YELLOW= $GREEN$formatted"
                }
            }

            lastError != null -> "$RED$lastError"
            else -> "${RED}No calculation"
        }

        Utils.drawStringCentered(
            rendered,
            MinecraftCompat.getFontRenderer(),
            gui.width / 2f,
            58f,
            false,
            0x808080FF.toInt()
        )
    }

    @HandleEvent
    fun onSignSubmit(event: SignSubmitEvent) {
        if (!ATHRConfig.feature.misc.signCalculator) return

        val trigger = event.lines[1]
        if (trigger != "^^^^^^^^^^^^^^^" && trigger != "^^^^^^") return

        refresh(event.lines[0])

        val result = lastResult
        if (result != null) {
            event.lines[0] = result.toPlainString()
        }
    }

    private fun refresh(source: String) {
        if (source == lastSource) return
        lastSource = source

        if (source.isEmpty() || CalculatorUtils.isPlainNumber(source)) {
            lastResult = null
            lastError = null
            return
        }

        try {
            lastResult = CalculatorUtils.calculate(source)
            lastError = null
        } catch (e: CalculatorUtils.CalculatorException) {
            lastError = e.message
            lastResult = null
        }
    }
}
