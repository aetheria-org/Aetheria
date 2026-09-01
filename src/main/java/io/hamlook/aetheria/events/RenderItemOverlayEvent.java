package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.AetheriaEvent;
import net.minecraft.item.ItemStack;

public class RenderItemOverlayEvent extends AetheriaEvent {
    public final ItemStack stack;
    public final int x;
    public final int y;

    public RenderItemOverlayEvent(ItemStack stack, int x, int y) {
        this.stack = stack;
        this.x = x;
        this.y = y;
    }
}