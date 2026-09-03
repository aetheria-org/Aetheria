package io.hamlook.aetheria.utils.item;

import io.hamlook.aetheria.utils.compat.InventoryCompatKt;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;


public class ItemStackFinder {

    public static ItemStack findItemStack(String itemId) {
        Minecraft mc = MinecraftCompat.getMinecraft();
        if (mc.thePlayer == null) return null;

        for (ItemStack stack : mc.thePlayer.inventory.mainInventory) {
            if (InventoryCompatKt.isStackNotEmpty(stack) && itemId.equals(ItemUtils.getInternalName(stack))) {
                return stack;
            }
        }

        for (ItemStack stack : mc.thePlayer.inventory.armorInventory) {
            if (InventoryCompatKt.isStackNotEmpty(stack) && itemId.equals(ItemUtils.getInternalName(stack))) {
                return stack;
            }
        }

        return null;
    }
}
