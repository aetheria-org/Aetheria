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

/**
 * Single home for entity outlines. Features queue (entity, color) pairs;
 * backends consume them. No GL lives here and no GL lives at call sites.
 *
 * Two overloads, different visibility contracts:
 * - {@link #xray} — visible through walls (Blood/Boss model mode).
 * - {@link #boxed} — occluded normally, same depth rules as the model
 *   itself (PartyMember; deprecated).
 *
 * Backends: legacy x-ray re-renders the model shell (see
 * {@link EntityHighlight}); modern x-ray uses vanilla glow via the
 * {@code hasOutline}/{@code getTeamColorValue} twins; boxes on both
 * versions go through the depth-tested world-box path. Callers never
 * branch on version — the types below remap cleanly on both.
 */
@RegisterEvents
public final class EntityOutlines {

    private static final Map<Entity, Color> xray = new ConcurrentHashMap<>();
    private static final Map<Entity, Color> occluded = new ConcurrentHashMap<>();

    private EntityOutlines() {
    }

    /** Queue an outline visible through walls. */
    public static void xray(Entity entity, Color color) {
        if (entity == null || color == null) return;
        xray.put(entity, color);
    }

    /** Queue an outline hidden by terrain like the model itself. */
    public static void boxed(Entity entity, Color color) {
        if (entity == null || color == null) return;
        occluded.put(entity, color);
    }

    /** Glow color for the modern vanilla-glow twin; null means no override. */
    public static Integer glowColor(Entity entity) {
        Color color = xray.get(entity);
        if (color != null) return color.getRGB();
        return null;
    }

    public static boolean hasQueued() {
        return !xray.isEmpty();
    }

    @HandleEvent
    public void onTick(ASMTickEvent event) {
        if (event.phase != TickPhase.END) return;
        xray.keySet().removeIf(EntityCompat::isDead);
        occluded.keySet().removeIf(EntityCompat::isDead);
    }

    @HandleEvent
    public void onWorldUnload(ASMWorldUnloadEvent event) {
        xray.clear();
        occluded.clear();
    }
}
