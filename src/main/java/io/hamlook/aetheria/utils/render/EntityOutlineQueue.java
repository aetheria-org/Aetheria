package io.hamlook.aetheria.utils.render;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.events.ASMTickEvent;
import io.hamlook.aetheria.events.ASMWorldUnloadEvent;
import io.hamlook.aetheria.events.TickPhase;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.compat.EntityCompat;
import net.minecraft.entity.Entity;

import java.awt.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RegisterEvents
public final class EntityOutlineQueue {

    private static final Map<Entity, Color> occluded = new ConcurrentHashMap<>();

    private EntityOutlineQueue() {
    }

    /** Queue an outline hidden by terrain like the model itself. */
    public static void boxed(Entity entity, Color color) {
        if (entity == null || color == null) return;
        occluded.put(entity, color);
    }

    @HandleEvent
    public void onTick(ASMTickEvent event) {
        if (event.phase != TickPhase.END) return;
        occluded.keySet().removeIf(EntityCompat::isDead);
    }

    @HandleEvent
    public void onWorldUnload(ASMWorldUnloadEvent event) {
        occluded.clear();
    }
}
