package io.hamlook.aetheria.utils.compat

import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.EnumFacing

fun C08PacketPlayerBlockPlacement.getFacing(): EnumFacing =
    EnumFacing.getFront(placedBlockDirection)

fun C08PacketPlayerBlockPlacement.getUsedItem(): ItemStack? =
    stack
