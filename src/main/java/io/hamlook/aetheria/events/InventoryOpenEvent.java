package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.AetheriaEvent;
import net.minecraft.item.ItemStack;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class InventoryOpenEvent extends AetheriaEvent {

    private final int inventoryId;
    private final String inventoryName;
    private final int inventorySize;
    private final Map<Integer, ItemStack> inventoryItems;

    public InventoryOpenEvent(int inventoryId, String inventoryName, int inventorySize, Map<Integer, ItemStack> inventoryItems) {
        this.inventoryId = inventoryId;
        this.inventoryName = inventoryName;
        this.inventorySize = inventorySize;
        this.inventoryItems = Collections.unmodifiableMap(new LinkedHashMap<>(inventoryItems));
    }

    public int getInventoryId() {
        return inventoryId;
    }

    public String getInventoryName() {
        return inventoryName;
    }

    // container slot count; -1 until the contents packet reveals it on modern
    public int getInventorySize() {
        return inventorySize;
    }

    public Map<Integer, ItemStack> getInventoryItems() {
        return inventoryItems;
    }
}