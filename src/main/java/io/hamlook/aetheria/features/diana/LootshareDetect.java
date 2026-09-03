package io.hamlook.aetheria.features.diana;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.events.ASMEntityJoinWorldEvent;
import io.hamlook.aetheria.events.ASMServerDisconnectEvent;
import io.hamlook.aetheria.events.ASMTickEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;

@RegisterEvents
public class LootshareDetect {

    private static final long NAME_CHECK_TIMEOUT_MS = 5_000L;
    private static final long LOOTSHARE_WINDOW_MS = 2_000L;
    private static final String DIANA_MARKER = "✿"; // ✿
    private static volatile long lastInqDisappearMs = -1L;
    private static volatile boolean nonInqMobActive = false;
    private static LootshareDetect INSTANCE;
    private final Map<Integer, Long> unconfirmed = new HashMap<>();
    private final Map<Integer, EntityArmorStand> tracked = new HashMap<>();
    private final Set<Integer> trackedInqs = new HashSet<>();
    private final Set<Integer> trackedNonInqs = new HashSet<>();
    private final Minecraft mc = MinecraftCompat.getMinecraft();

    public LootshareDetect() {
        INSTANCE = this;
    }

    // Called by DianaTracker when "Uh oh! You dug out X" fires for a non-inq mob
    public static void onNonInqMobDug() {
        nonInqMobActive = true;
    }

    public static boolean wasInqKilledByOther() {
        return lastInqDisappearMs > 0 && System.currentTimeMillis() - lastInqDisappearMs <= LOOTSHARE_WINDOW_MS;
    }

    public static void clearInqDisappear() {
        lastInqDisappearMs = -1L;
    }

    // Returns the closest tracked inq stand name, or null
    public static String getClosestInqName() {
        if (INSTANCE == null) return null;
        Minecraft mc = MinecraftCompat.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) return null;
        EntityArmorStand closest = null;
        double minDist = Double.MAX_VALUE;
        for (Map.Entry<Integer, EntityArmorStand> e : INSTANCE.tracked.entrySet()) {
            if (!INSTANCE.trackedInqs.contains(e.getKey())) continue;
            EntityArmorStand stand = e.getValue();
            if (stand.isDead) continue;
            double d = mc.thePlayer.getDistanceSqToEntity(stand);
            if (d < minDist) {
                minDist = d;
                closest = stand;
            }
        }
        return closest != null ? closest.getCustomNameTag() : null;
    }

    // Returns the closest tracked non-inq diana mob stand name, only after a dig message fired
    public static String getClosestNonInqMobName() {
        if (INSTANCE == null || !nonInqMobActive) return null;
        Minecraft mc = MinecraftCompat.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) return null;
        EntityArmorStand closest = null;
        double minDist = Double.MAX_VALUE;
        for (Map.Entry<Integer, EntityArmorStand> e : INSTANCE.tracked.entrySet()) {
            if (!INSTANCE.trackedNonInqs.contains(e.getKey())) continue;
            EntityArmorStand stand = e.getValue();
            if (stand.isDead) continue;
            double d = mc.thePlayer.getDistanceSqToEntity(stand);
            if (d < minDist) {
                minDist = d;
                closest = stand;
            }
        }
        return closest != null ? closest.getCustomNameTag() : null;
    }

    @HandleEvent
    public void onEntityJoin(ASMEntityJoinWorldEvent event) {
        if (mc.theWorld == null) return;
        if (!(event.entity instanceof EntityArmorStand)) return;
        // Only bother tracking if spade in hotbar (diana active) or a mob was just dug
        if ((DianaStats.hasSpadeInHotbar() || nonInqMobActive) && DianaStats.getInstance().isDianaMayor())
            unconfirmed.put(event.entity.getEntityId(), System.currentTimeMillis());
    }

    @HandleEvent
    public void onTick(ASMTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (mc.theWorld == null || mc.thePlayer == null) return;
        long now = System.currentTimeMillis();
        promoteUnconfirmed(now);
        checkTracked();
    }

    @HandleEvent
    public void onDisconnect(ASMServerDisconnectEvent event) {
        unconfirmed.clear();
        tracked.clear();
        trackedInqs.clear();
        trackedNonInqs.clear();
        nonInqMobActive = false;
    }

    private void promoteUnconfirmed(long now) {
        Iterator<Map.Entry<Integer, Long>> it = unconfirmed.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Long> entry = it.next();
            int id = entry.getKey();
            Entity entity = mc.theWorld.getEntityByID(id);

            if (!(entity instanceof EntityArmorStand) || entity.isDead) {
                it.remove();
                continue;
            }

            String name = entity.getCustomNameTag();
            if (name.contains(DIANA_MARKER)) {
                tracked.put(id, (EntityArmorStand) entity);
                String cleanName = net.minecraft.util.StringUtils.stripControlCodes(name);
                if (cleanName.contains("Minos Inquisitor")) {
                    trackedInqs.add(id);
                } else {
                    trackedNonInqs.add(id);
                }
                it.remove();
            } else if (now - entry.getValue() > NAME_CHECK_TIMEOUT_MS) {
                it.remove();
            }
        }
    }

    private void checkTracked() {
        Iterator<Map.Entry<Integer, EntityArmorStand>> it = tracked.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, EntityArmorStand> entry = it.next();
            int id = entry.getKey();
            EntityArmorStand stand = entry.getValue();

            if (!stand.isDead) {
                if (!trackedInqs.contains(id) && !trackedNonInqs.contains(id)) {
                    String clean = net.minecraft.util.StringUtils.stripControlCodes(stand.getCustomNameTag());
                    if (clean.contains("Minos Inquisitor")) trackedInqs.add(id);
                }
                continue;
            }

            if (trackedInqs.contains(id)) {
                DianaStats.getInstance().onInqDeath();
                lastInqDisappearMs = System.currentTimeMillis();
            }
            if (trackedNonInqs.contains(id)) {
                nonInqMobActive = false;
            }

            it.remove();
            trackedInqs.remove(id);
            trackedNonInqs.remove(id);
        }
    }
}