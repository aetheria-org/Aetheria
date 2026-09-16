package io.hamlook.aetheria.utils.compat
//#if MC < 1.16
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Container
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
//#else
//$$ import net.minecraft.client.gui.screen.ingame.HandledScreen
//$$ import net.minecraft.world.entity.player.Player
//$$ import net.minecraft.world.inventory.AbstractContainerMenu
//$$ import net.minecraft.world.inventory.Slot
//$$ import net.minecraft.world.item.ItemStack
//#endif


object InventoryCompat {

    //#if MC < 1.16
    @JvmStatic
    fun getContainer(gui: GuiContainer): Container = gui.inventorySlots
    //#else
    //$$ @JvmStatic
    //$$ fun getContainer(gui: HandledScreen<*>): ScreenHandler = gui.screenHandler
    //#endif

    @JvmStatic
    fun getWindowId(container: Container): Int =
        //#if MC < 1.16
        container.windowId
        //#else
        //$$ container.containerId
        //#endif

    @JvmStatic
    fun getSlot(container: Container, index: Int): Slot = container.getSlot(index)

    @JvmStatic
    fun ogetContainerSlots(container: Container): List<*> =
        //#if MC < 1.16
        container.inventorySlots
        //#else
        //$$ container.slots
        //#endif

    @JvmStatic
    fun getContainerSize(container: Container): Int =
        //#if MC < 1.16
        container.inventorySlots.size
        //#else
        //$$ container.slots.size
        //#endif

    //#if MC < 1.16
    @JvmStatic
    fun getSlotUnderMouse(gui: GuiContainer): Slot? = gui.slotUnderMouse
    //#elseif FORGE
    //$$ @JvmStatic
    //$$ fun getSlotUnderMouse(gui: HandledScreen<*>): Slot? = gui.slotUnderMouse
    //#else
    //$$ @JvmStatic
    //$$ fun getSlotUnderMouse(gui: HandledScreen<*>): Slot? = gui.focusedSlot
    //#endif

    @JvmStatic
    fun getCursorStack(player: EntityPlayer): ItemStack? =
        //#if MC < 1.21
        player.inventory.itemStack
        //#else
        //$$ player.currentScreenHandler?.cursorStack?.takeIf { !it.isEmpty }
        //#endif

    @JvmStatic
    fun windowClick(
        windowId: Int, slotId: Int, mouseButton: Int, mode: Int, player: EntityPlayer
    ) {
        val mc = MinecraftCompat.getMinecraft()
        //#if FORGE
        mc.playerController.windowClick(windowId, slotId, mouseButton, mode, player)
        //#else
        //$$ mc.interactionManager?.clickSlot(windowId, slotId, mouseButton, net.minecraft.screen.slot.SlotActionType.entries[mode], player)
        //#endif
    }

    @JvmStatic
    fun getOpenContainer(player: EntityPlayer): Container? =
        //#if MC < 1.16
        player.openContainer
        //#else
        //$$ player.currentScreenHandler
        //#endif

    //#if MC < 1.16
    @JvmStatic
    fun getGuiLeft(gui: GuiContainer): Int = gui.guiLeft
    //#else
    //$$ @JvmStatic
    //$$ fun getGuiLeft(gui: HandledScreen<*>): Int = gui.x
    //#endif

    //#if MC < 1.16
    @JvmStatic
    fun getGuiTop(gui: GuiContainer): Int = gui.guiTop
    //#else
    //$$ @JvmStatic
    //$$ fun getGuiTop(gui: HandledScreen<*>): Int = gui.y
    //#endif
}

/**
 * Null-safe emptiness check. On 1.8.9 null means empty; on 1.21+ ItemStack.EMPTY means empty.
 * Use this instead of `!= null` when checking if an inventory slot has an item.
 */
fun ItemStack?.isStackNotEmpty(): Boolean {
    this ?: return false
    //#if MC > 1.21
    //$$ return !this.isEmpty
    //#else
    return true
    //#endif
}

fun ItemStack?.orNull(): ItemStack? {
    //#if MC > 1.21
    //$$ return this?.takeUnless { it.isEmpty }
    //#endif
    return this
}

fun Array<ItemStack?>?.filterNotNullOrEmpty(): List<ItemStack>? {
    return this?.filterNotNull()?.filter { it.isStackNotEmpty() }
}

fun Array<ItemStack?>?.convertEmptyToNull(): Array<ItemStack?>? {
    if (this == null) return null
    if (this.isEmpty()) return this
    val new: MutableList<ItemStack?> = mutableListOf()
    for (stack in this) {
        if (!stack.isStackNotEmpty()) new.add(null)
        else new.add(stack)
    }
    return new.normalizeAsArray()
}
