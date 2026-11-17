package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.AetheriaEvent;
import net.minecraft.util.MovingObjectPosition;

public class ASMBlockHighlightEvent extends AetheriaEvent implements AetheriaEvent.Cancellable {

    public final float partialTicks;
    public final MovingObjectPosition target;

    public ASMBlockHighlightEvent(float partialTicks, MovingObjectPosition target) {
        this.partialTicks = partialTicks;
        this.target = target;
    }
}
