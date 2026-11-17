package io.hamlook.aetheria.features.dungeons.rooms.secret;

import lombok.Getter;
import io.hamlook.aetheria.events.SecretGainedEvent;
import io.hamlook.aetheria.utils.compat.EntityCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;

public final class SecretWaypoint {

    public static final int AIR_GRACE_TICKS = 20;
    public static final int NO_ENTITY = -1;

    private final SecretCategory category;
    private final String name;
    private final BlockPos pos;
    private final AxisAlignedBB box;

    @Getter
    private volatile boolean collected;
    private volatile boolean dismissed;
    @Getter
    private boolean seen;
    private int ticksExisted;
    private volatile int boundEntityId = NO_ENTITY;
    private int presenceMisses;

    public SecretWaypoint(SecretCategory category, String name, BlockPos pos) {
        this.category = category;
        this.name = name;
        this.pos = pos;
        this.box = new AxisAlignedBB(
                pos.getX() + 0.05, pos.getY() + 0.05, pos.getZ() + 0.05,
                pos.getX() + 0.95, pos.getY() + 0.95, pos.getZ() + 0.95);
    }

    public SecretCategory category() {
        return category;
    }

    public String name() {
        return name;
    }

    public BlockPos pos() {
        return pos;
    }

    public AxisAlignedBB box() {
        return box;
    }

    // collected credits a secret, dismissed only hides one that was already looted
    public boolean isHidden() {
        return collected || dismissed;
    }

    public void collect() {
        if (collected) return;
        collected = true;
        new SecretGainedEvent(this).post();
    }

    public void dismiss() {
        dismissed = true;
    }

    public void restore() {
        collected = false;
        dismissed = false;
    }

    public void markSeen() {
        seen = true;
    }

    public int ticksExisted() {
        return ticksExisted;
    }

    public void advanceTicks(int intervalTicks) {
        ticksExisted += intervalTicks;
    }

    public boolean airGraceExpired() {
        return seen && ticksExisted >= AIR_GRACE_TICKS;
    }

    public boolean isBound() {
        return boundEntityId != NO_ENTITY;
    }

    public int boundEntityId() {
        return boundEntityId;
    }

    public void bind(int entityId) {
        boundEntityId = entityId;
        presenceMisses = 0;
    }

    public void notePresent() {
        presenceMisses = 0;
    }

    public boolean noteAbsent(int missesToCollect) {
        presenceMisses++;
        return presenceMisses >= missesToCollect;
    }

    public double distanceSq(double x, double y, double z) {
        double dx = x - (pos.getX() + 0.5);
        double dy = y - (pos.getY() + 0.5);
        double dz = z - (pos.getZ() + 0.5);
        return dx * dx + dy * dy + dz * dz;
    }

    public double distanceSqToPlayer() {
        Entity player = MinecraftCompat.getLocalPlayer();
        if (player == null) return Double.MAX_VALUE;
        return distanceSq(EntityCompat.getPosX(player), EntityCompat.getPosY(player), EntityCompat.getPosZ(player));
    }
}
