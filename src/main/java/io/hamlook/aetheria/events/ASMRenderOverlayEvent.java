package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.AetheriaEvent;
import net.minecraft.client.gui.ScaledResolution;

public class ASMRenderOverlayEvent extends AetheriaEvent {

    public final ScaledResolution resolution;
    public final float partialTicks;
    public final int type;

    public ASMRenderOverlayEvent(ScaledResolution resolution, float partialTicks, int type) {
        this.resolution = resolution;
        this.partialTicks = partialTicks;
        this.type = type;
    }
}
