package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.RenderingAetheriaEvent;
import io.hamlook.aetheria.utils.compat.DrawContext;

public class ASMRenderOverlayEvent extends RenderingAetheriaEvent {

    public final int scaledWidth;
    public final int scaledHeight;
    public final float partialTicks;
    public final int type;

    public ASMRenderOverlayEvent(DrawContext context, int scaledWidth, int scaledHeight, float partialTicks, int type) {
        super(context);
        this.scaledWidth = scaledWidth;
        this.scaledHeight = scaledHeight;
        this.partialTicks = partialTicks;
        this.type = type;
    }
}
