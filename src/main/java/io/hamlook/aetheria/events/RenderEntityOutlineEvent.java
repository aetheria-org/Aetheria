package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.AetheriaEvent;
import net.minecraft.entity.Entity;

import java.util.HashMap;
import java.util.Map;

public class RenderEntityOutlineEvent extends AetheriaEvent {

    private final Map<Entity, Integer> outlines = new HashMap<>();

    public void queue(Entity entity, int argb) {
        if (entity != null) outlines.put(entity, argb);
    }

    public Map<Entity, Integer> getOutlines() {
        return outlines;
    }
}
