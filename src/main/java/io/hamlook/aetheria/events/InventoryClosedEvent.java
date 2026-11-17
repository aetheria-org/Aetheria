package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.AetheriaEvent;

public class InventoryClosedEvent extends AetheriaEvent {

    private final int inventoryId;
    private final String inventoryName;

    public InventoryClosedEvent(int inventoryId, String inventoryName) {
        this.inventoryId = inventoryId;
        this.inventoryName = inventoryName;
    }

    public int getInventoryId() {
        return inventoryId;
    }

    public String getInventoryName() {
        return inventoryName;
    }
}
