package io.hamlook.aetheria.utils.item;

import io.hamlook.aetheria.utils.compat.InventoryCompatKt;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;


public class ItemStackFinder {

    public static ItemStack findItemStack(String itemId) {
        Minecraft mc = MinecraftCompat.getMinecraft();
        if (MinecraftCompat.getLocalPlayer() == null) return null;

        for (ItemStack stack : MinecraftCompat.getLocalPlayer().inventory.mainInventory) {
            if (InventoryCompatKt.isStackNotEmpty(stack) && itemId.equals(ItemUtils.getInternalName(stack))) {
                return stack;
            }
        }

        for (ItemStack stack : MinecraftCompat.getLocalPlayer().inventory.armorInventory) {
            if (InventoryCompatKt.isStackNotEmpty(stack) && itemId.equals(ItemUtils.getInternalName(stack))) {
                return stack;
            }
        }

        return null;
    }
}
