package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.RenderingAetheriaEvent;
import io.hamlook.aetheria.utils.compat.DrawContext;
import net.minecraft.item.ItemStack;

public class RenderItemOverlayEvent extends RenderingAetheriaEvent {
    public final ItemStack stack;
    public final int x;
    public final int y;

    public RenderItemOverlayEvent(DrawContext context, ItemStack stack, int x, int y) {
        super(context);
        this.stack = stack;
        this.x = x;
        this.y = y;
    }
}