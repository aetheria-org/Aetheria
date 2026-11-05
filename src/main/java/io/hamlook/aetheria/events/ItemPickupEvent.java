package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.AetheriaEvent;
import net.minecraft.item.ItemStack;

public class ItemPickupEvent extends AetheriaEvent {
    public final ItemStack stack;
    public final double x;
    public final double y;
    public final double z;
    public final int collectorEntityId;

    public ItemPickupEvent(ItemStack stack, double x, double y, double z, int collectorEntityId) {
        this.stack = stack;
        this.x = x;
        this.y = y;
        this.z = z;
        this.collectorEntityId = collectorEntityId;
    }
}
