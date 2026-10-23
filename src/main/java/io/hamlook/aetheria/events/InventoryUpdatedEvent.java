package io.hamlook.aetheria.events;

import net.minecraft.item.ItemStack;

import java.util.Map;

public class InventoryUpdatedEvent extends InventoryOpenEvent {

    public InventoryUpdatedEvent(int inventoryId, String inventoryName, int inventorySize, Map<Integer, ItemStack> inventoryItems) {
        super(inventoryId, inventoryName, inventorySize, inventoryItems);
    }
}