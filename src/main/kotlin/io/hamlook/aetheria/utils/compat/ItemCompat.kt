package io.hamlook.aetheria.utils.compat

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack

fun ItemStack.getTooltipCompat(player: EntityPlayer?, advanced: Boolean): MutableList<String> {
    return this.getTooltip(player, advanced)
}

fun ItemStack.getTooltipCompat(advanced: Boolean): MutableList<String> {
    return this.getTooltip(MinecraftCompat.getLocalPlayer(), advanced)
}

fun Item.getIdentifierString(): String {
    return this.registryName
}

fun ItemStack.setCustomItemName(name: String): ItemStack {
    this.setStackDisplayName(name)
    return this
}
