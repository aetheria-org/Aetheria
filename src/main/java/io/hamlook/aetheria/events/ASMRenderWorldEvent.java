package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.AetheriaEvent;

public class ASMRenderWorldEvent extends AetheriaEvent {

    public final float partialTicks;

    public ASMRenderWorldEvent(float partialTicks) {
        this.partialTicks = partialTicks;
    }
}
