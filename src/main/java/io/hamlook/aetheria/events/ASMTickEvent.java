package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.AetheriaEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class ASMTickEvent extends AetheriaEvent {

    public final TickEvent.Phase phase;

    public ASMTickEvent(TickEvent.Phase phase) {
        this.phase = phase;
    }
}
