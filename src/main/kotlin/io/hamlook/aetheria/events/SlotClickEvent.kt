package io.hamlook.aetheria.events

import io.hamlook.aetheria.api.event.AetheriaEvent
//#if MC < 1.16
import net.minecraft.client.gui.inventory.GuiContainer
//#else
//$$ import net.minecraft.client.gui.screen.ingame.HandledScreen
//#endif
import net.minecraft.inventory.Slot

class SlotClickEvent(
    //#if MC < 1.16
    val gui: GuiContainer,
    //#else
    //$$ val gui: HandledScreen<*>,
    //#endif
    val slot: Slot?,
    val slotId: Int,
    val clickedButton: Int,
    val clickType: ClickType
) : AetheriaEvent(), AetheriaEvent.Cancellable {

    enum class ClickType(val id: Int) {
        NORMAL(0),
        SHIFT(1),
        HOTBAR(2),
        MIDDLE(3),
        DROP(4),
        DRAW(5),
        DOUBLE_CLICK(6);

        companion object {
            @JvmStatic
            fun fromId(id: Int): ClickType = values().firstOrNull { it.id == id } ?: NORMAL
        }
    }
}
