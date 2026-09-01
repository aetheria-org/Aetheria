package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.AetheriaEvent;
import net.minecraft.entity.Entity;

public class ASMEntityJoinWorldEvent extends AetheriaEvent {

    public final Entity entity;

    public ASMEntityJoinWorldEvent(Entity entity) {
        this.entity = entity;
    }
}
