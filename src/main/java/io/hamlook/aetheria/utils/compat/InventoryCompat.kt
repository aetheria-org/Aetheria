package io.hamlook.aetheria.utils.compat

import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Container
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack

/**
 * Version-agnostic wrappers for inventory/container APIs. On 1.8.9 [mainInventory] is
 * `ItemStack[]` (null = empty); on 1.21+ it is `NonNullList<ItemStack>` (ItemStack.EMPTY = empty).
 * Use [isStackNotEmpty] and [convertEmptyToNull] to bridge the two conventions.
 *
 * Feature code should call [ItemUtils] for convenience methods; this object provides the
 * low-level bridge only.
 */
object InventoryCompat {

    @JvmStatic
    fun getContainer(gui: GuiContainer): Container = gui.inventorySlots

    @JvmStatic
    fun getWindowId(container: Container): Int = container.windowId

    @JvmStatic
    fun getSlot(container: Container, index: Int): Slot = container.getSlot(index)

    @JvmStatic
    fun getContainerSlots(container: Container): List<*> = container.inventorySlots

    @JvmStatic
    fun getContainerSize(container: Container): Int = container.inventorySlots.size

    @JvmStatic
    fun getSlotUnderMouse(gui: GuiContainer): Slot? = gui.slotUnderMouse

    @JvmStatic
    fun getCursorStack(player: EntityPlayer): ItemStack? = player.inventory.itemStack

    @JvmStatic
    fun windowClick(
        windowId: Int, slotId: Int, mouseButton: Int, mode: Int, player: EntityPlayer
    ) {
        val mc = MinecraftCompat.getMinecraft()
        mc.playerController.windowClick(windowId, slotId, mouseButton, mode, player)
    }

    @JvmStatic
    fun getOpenContainer(player: EntityPlayer): Container? =
        player.openContainer
}

/**
 * Null-safe emptiness check. On 1.8.9 null means empty; on 1.21+ ItemStack.EMPTY means empty.
 * Use this instead of `!= null` when checking if an inventory slot has an item.
 */
fun ItemStack?.isStackNotEmpty(): Boolean {
    this ?: return false
    // on 1.21+: return !this.isEmpty
    return true
}

fun ItemStack?.orNull(): ItemStack? {
    // on 1.21+: return this?.takeUnless { it.isEmpty }
    return this
}

fun Array<ItemStack?>?.filterNotNullOrEmpty(): List<ItemStack>? {
    return this?.filterNotNull()?.filter { it.isStackNotEmpty() }
}

/**
 * Converts an inventory array from 1.21+ NonNullList convention (EMPTY sentinels) to 1.8.9
 * convention (null sentinels). Useful when consuming mainInventory for diff comparison or display.
 */
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
