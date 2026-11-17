package io.hamlook.aetheria.utils.item;

import io.hamlook.aetheria.events.ItemPickupEvent;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;

//#if MC < 1.21
import net.minecraft.network.play.server.S0DPacketCollectItem;
import net.minecraft.entity.item.EntityItem;
//#else
//$$ import net.minecraft.entity.ItemEntity;
//$$ import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
//#endif

public final class ItemPickupCapture {

    private ItemPickupCapture() {
    }

    public static ItemPickupEvent capture(Object packet, WorldClient world) {
        if (world == null) return null;

        //#if MC < 1.21
        if (!(packet instanceof S0DPacketCollectItem)) return null;
        S0DPacketCollectItem collect = (S0DPacketCollectItem) packet;
        Entity entity = world.getEntityByID(collect.getCollectedItemEntityID());
        if (!(entity instanceof EntityItem)) return null;
        ItemStack stack = ((EntityItem) entity).getEntityItem();
        if (stack == null) return null;
        return new ItemPickupEvent(stack.copy(), entity.posX, entity.posY, entity.posZ, collect.getEntityID());
        //#else
        //$$ if (!(packet instanceof ItemPickupAnimationS2CPacket)) return null;
        //$$ ItemPickupAnimationS2CPacket collect = (ItemPickupAnimationS2CPacket) packet;
        //$$ Entity entity = world.getEntityById(collect.getEntityId());
        //$$ if (!(entity instanceof ItemEntity)) return null;
        //$$ ItemStack stack = ((ItemEntity) entity).getStack();
        //$$ if (stack == null || stack.isEmpty()) return null;
        //$$ return new ItemPickupEvent(stack.copy(), entity.getX(), entity.getY(), entity.getZ(), collect.getCollectorEntityId());
        //#endif
    }
}
