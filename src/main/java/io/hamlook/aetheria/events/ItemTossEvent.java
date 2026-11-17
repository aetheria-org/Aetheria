package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.AetheriaEvent;
import net.minecraft.item.ItemStack;

public class ItemTossEvent extends AetheriaEvent implements AetheriaEvent.Cancellable {
    public final ItemStack item;
    public final boolean dropAll;

    public ItemTossEvent(ItemStack item, boolean dropAll) {
        this.item = item;
        this.dropAll = dropAll;
    }
}