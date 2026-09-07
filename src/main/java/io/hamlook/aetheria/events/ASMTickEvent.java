package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.AetheriaEvent;

public class ASMTickEvent extends AetheriaEvent {

    public final TickPhase phase;

    public ASMTickEvent(TickPhase phase) {
        this.phase = phase;
    }
}
