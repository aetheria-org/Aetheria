package io.hamlook.aetheria.events;

import net.minecraft.item.ItemStack;

import java.util.Map;

public class InventoryUpdatedEvent extends InventoryOpenEvent {

    private final int changedSlot;

    public InventoryUpdatedEvent(int inventoryId, String inventoryName, int inventorySize,
                                 Map<Integer, ItemStack> inventoryItems, int changedSlot) {
        super(inventoryId, inventoryName, inventorySize, inventoryItems);
        this.changedSlot = changedSlot;
    }

    // -1 = full snapshot (bulk contents), otherwise the container slot the server changed
    public int getChangedSlot() {
        return changedSlot;
    }
}
