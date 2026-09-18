package io.hamlook.aetheria.utils.compat

//#if MC < 1.16
//#endif
//#if MC < 1.21
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C0EPacketClickWindow
import net.minecraft.network.play.client.C16PacketClientStatus
import net.minecraft.network.play.server.S29PacketSoundEffect
import net.minecraft.network.play.server.S30PacketWindowItems
import net.minecraft.util.EnumFacing

//#else
//$$ import net.minecraft.item.ItemStack
//$$ import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket
//$$ import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket
//$$ import net.minecraft.network.packet.s2c.play.InventoryS2CPacket
//$$ import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket
//$$ import net.minecraft.screen.slot.SlotActionType
//#endif

//#if MC < 1.16
fun C08PacketPlayerBlockPlacement.getFacing(): EnumFacing = EnumFacing.getFront(placedBlockDirection)

fun C08PacketPlayerBlockPlacement.getUsedItem(): ItemStack? = stack
//#endif

data class ClickData(val windowId: Int, val slotId: Int, val button: Int, val mode: Int)

object PacketCompat {
    //#if MC < 1.21
    @JvmStatic
    fun isTimeUpdate(packet: net.minecraft.network.Packet<*>): Boolean =
        packet is net.minecraft.network.play.server.S03PacketTimeUpdate

    @JvmStatic
    fun isStats(packet: net.minecraft.network.Packet<*>): Boolean =
        packet is net.minecraft.network.play.server.S37PacketStatistics

    @JvmStatic
    fun clickData(packet: C0EPacketClickWindow): ClickData =
        ClickData(packet.windowId, packet.slotId, packet.usedButton, packet.mode)

    @JvmStatic
    fun soundName(packet: S29PacketSoundEffect): String = packet.soundName
    //#else
    //$$ @JvmStatic
    //$$ fun isTimeUpdate(packet: net.minecraft.network.packet.Packet<*>): Boolean =
    //$$     packet is net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket
    //$$
    //$$ @JvmStatic
    //$$ fun isStats(packet: net.minecraft.network.packet.Packet<*>): Boolean =
    //$$     packet is net.minecraft.network.packet.s2c.play.StatisticsS2CPacket
    //$$
    //$$ @JvmStatic
    //$$ fun clickData(packet: ClickSlotC2SPacket): ClickData =
    //$$     ClickData(packet.syncId, packet.slot, packet.button, slotActionToMode(packet.actionType))
    //$$
    //$$ private fun slotActionToMode(action: SlotActionType): Int = when (action) {
    //$$     SlotActionType.PICKUP -> 0
    //$$     SlotActionType.QUICK_MOVE -> 1
    //$$     SlotActionType.SWAP -> 2
    //$$     SlotActionType.CLONE -> 3
    //$$     SlotActionType.THROW -> 4
    //$$     SlotActionType.QUICK_CRAFT -> 5
    //$$     SlotActionType.PICKUP_ALL -> 6
    //$$ }
    //$$
    //$$ @JvmStatic
    //$$ fun soundName(packet: PlaySoundS2CPacket): String =
    //$$     packet.sound.value().id.toString()
    //#endif

    //#if MC < 1.21
    @JvmStatic
    fun windowItemsId(packet: S30PacketWindowItems): Int = packet.func_148911_c()

    @JvmStatic
    fun windowItemsStacks(packet: S30PacketWindowItems): Array<ItemStack?> = packet.itemStacks
    //#else
    //$$ @JvmStatic
    //$$ fun windowItemsId(packet: InventoryS2CPacket): Int = packet.syncId
    //$$
    //$$ @JvmStatic
    //$$ fun windowItemsStacks(packet: InventoryS2CPacket): Array<ItemStack?> =
    //$$     packet.contents.normalizeItemStackArray()
    //#endif

    @JvmStatic
    fun requestStats() {
        //#if MC < 1.21
        val queue = MinecraftCompat.getLocalPlayer()?.sendQueue ?: return
        queue.networkManager.sendPacket(C16PacketClientStatus(C16PacketClientStatus.EnumState.REQUEST_STATS))
        //#else
        //$$ val connection = MinecraftCompat.getMinecraft().networkHandler?.connection ?: return
        //$$ connection.send(ClientStatusC2SPacket(ClientStatusC2SPacket.Mode.REQUEST_STATS))
        //#endif
    }
}

