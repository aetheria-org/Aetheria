package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.AetheriaEvent;
import net.minecraft.entity.Entity;

public class ASMLivingDeathEvent extends AetheriaEvent {

    public final Entity entity;

    public ASMLivingDeathEvent(Entity entity) {
        this.entity = entity;
    }
}
