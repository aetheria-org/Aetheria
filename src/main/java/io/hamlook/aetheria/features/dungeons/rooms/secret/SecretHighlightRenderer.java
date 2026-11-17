package io.hamlook.aetheria.features.dungeons.rooms.secret;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.core.features.dungeons.SecretHighlightsConfig;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import io.hamlook.aetheria.events.ASMTickEvent;
import io.hamlook.aetheria.events.RenderEntityOutlineEvent;
import io.hamlook.aetheria.events.TickPhase;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.compat.EntityCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.item.ItemUtils;
import io.hamlook.aetheria.utils.render.EntityRenderUtils;
import io.hamlook.aetheria.utils.render.SolidRender;
import io.hamlook.aetheria.utils.render.WorldRenderUtils;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.util.AxisAlignedBB;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RegisterEvents
public final class SecretHighlightRenderer {

    // Both style dropdowns start at 0; 1 is a solid entity shell on legacy and a glow outline on modern.
    private static final int TYPE_BOX = 0;
    private static final int TYPE_SECONDARY = 1;
    private static final int DISABLED_TYPE = -1;

    private static final float SHELL_SCALE = 1.1f;
    private static final double BAT_EXPAND_XZ = 0.35;
    private static final double BAT_EXPAND_Y = 0.25;
    private static final double ITEM_EXPAND_XZ = 0.15;
    private static final double ITEM_EXPAND_Y = 0.1;
    private static final double ESSENCE_CUBE_WIDTH = 0.65;
    private static final double ESSENCE_CUBE_HEIGHT = 0.6;
    private static final double ESSENCE_HEAD_HEIGHT = 1.75;
    private static final int ITEM_SCAN_INTERVAL = 3;

    private static final class Snapshot {
        final Set<Entity> bats;
        final Set<Entity> items;
        final int batArgb;
        final int itemArgb;
        final int batsType;
        final int itemsType;
        final boolean batsThrough;
        final boolean itemsThrough;

        Snapshot(Set<Entity> bats, Set<Entity> items, int batArgb, int itemArgb,
                 int batsType, int itemsType, boolean batsThrough, boolean itemsThrough) {
            this.bats = bats;
            this.items = items;
            this.batArgb = batArgb;
            this.itemArgb = itemArgb;
            this.batsType = batsType;
            this.itemsType = itemsType;
            this.batsThrough = batsThrough;
            this.itemsThrough = itemsThrough;
        }
    }

    private static final Snapshot EMPTY = new Snapshot(Collections.emptySet(), Collections.emptySet(),
            0, 0, DISABLED_TYPE, DISABLED_TYPE, false, false);

    private static volatile Snapshot snapshot = EMPTY;
    private static final List<AxisAlignedBB> boxBuffer = new ArrayList<>();
    private static int itemScanTick;

    private SecretHighlightRenderer() {
    }

    public static void draw(float partialTicks) {
        SecretHighlightsConfig config = SecretHighlightsConfig.get();
        if (config == null) return;

        Snapshot current = snapshot;
        drawLeversAndEssence(config);
        drawBoxes(current, partialTicks);
        drawSolids(current, partialTicks);
    }

    private static void drawLeversAndEssence(SecretHighlightsConfig config) {
        Color leverColor = config.lever.enabled ? color(config.lever.color) : null;

        for (SecretWaypoint secret : SecretTracker.secrets()) {
            if (secret.isHidden()) continue;

            SecretCategory category = secret.category();
            if (leverColor != null && category == SecretCategory.LEVER) {
                SolidRender.block(secret.pos(), leverColor, config.lever.throughWalls);
            } else if (config.essence.enabled && category == SecretCategory.WITHER) {
                drawEssence(config.essence, secret);
            }
        }
    }

    private static void drawEssence(SecretHighlightsConfig.Essence config, SecretWaypoint secret) {
        EntityArmorStand stand = SecretCollector.essenceStand(secret);
        if (stand == null || EntityCompat.isDead(stand)) return;

        double scale = stand.isSmall() ? 0.5 : 1.0;
        double width = ESSENCE_CUBE_WIDTH * scale;
        double height = ESSENCE_CUBE_HEIGHT * scale;
        double x = EntityCompat.getPosX(stand);
        double y = EntityCompat.getPosY(stand) + ESSENCE_HEAD_HEIGHT * scale;
        double z = EntityCompat.getPosZ(stand);

        AxisAlignedBB box = new AxisAlignedBB(
                x - width * 0.5, y - height * 0.5, z - width * 0.5,
                x + width * 0.5, y + height * 0.5, z + width * 0.5);
        SolidRender.cube(box, color(config.color), config.throughWalls);
    }

    private static void drawBoxes(Snapshot current, float partialTicks) {
        if (current.batsType == TYPE_BOX) {
            drawBox(current.bats, current.batArgb, current.batsThrough, partialTicks, BAT_EXPAND_XZ, BAT_EXPAND_Y);
        }
        if (current.itemsType == TYPE_BOX) {
            drawBox(current.items, current.itemArgb, current.itemsThrough, partialTicks, ITEM_EXPAND_XZ, ITEM_EXPAND_Y);
        }
    }

    private static void drawBox(Set<Entity> targets, int argb, boolean throughWalls, float partialTicks,
            double expandXZ, double expandY) {
        if (targets.isEmpty()) return;

        EntityRenderUtils.buildInterpolatedBoxes(boxBuffer, targets, partialTicks, expandXZ, expandY);
        if (!boxBuffer.isEmpty()) {
            WorldRenderUtils.drawEspBoxes(boxBuffer, new Color(argb, true), throughWalls, 1f);
        }
    }

    private static void drawSolids(Snapshot current, float partialTicks) {
        //#if MC < 1.21
        if (current.batsType == TYPE_SECONDARY) {
            for (Entity bat : current.bats) {
                if (EntityCompat.isDead(bat)) continue;
                EntityRenderUtils.renderEntityShell(bat, partialTicks, current.batArgb, current.batsThrough, SHELL_SCALE);
            }
        }
        if (current.itemsType == TYPE_SECONDARY) {
            for (Entity item : current.items) {
                if (EntityCompat.isDead(item)) continue;
                EntityRenderUtils.renderEntityShell(item, partialTicks, current.itemArgb, current.itemsThrough, SHELL_SCALE);
            }
        }
        //#endif
    }

    @HandleEvent
    public void onTick(ASMTickEvent event) {
        if (event.phase != TickPhase.START) return;
        snapshot = collect();
    }

    @HandleEvent
    public void onOutline(RenderEntityOutlineEvent event) {
        Snapshot current = snapshot;
        if (current.batsType == TYPE_SECONDARY) queueAll(event, current.bats, current.batArgb);
        if (current.itemsType == TYPE_SECONDARY) queueAll(event, current.items, current.itemArgb);
    }

    private static void queueAll(RenderEntityOutlineEvent event, Set<Entity> entities, int argb) {
        for (Entity entity : entities) {
            event.queue(entity, argb);
        }
    }

    private static Snapshot collect() {
        if (!SecretCollector.isActive()) return EMPTY;

        SecretHighlightsConfig config = SecretHighlightsConfig.get();
        boolean batsOn = config != null && config.bats.enabled;
        boolean itemsOn = config != null && config.items.enabled;
        if (!batsOn && !itemsOn) return EMPTY;

        WorldClient world = MinecraftCompat.getLocalWorld();
        Entity player = MinecraftCompat.getLocalPlayer();
        if (world == null || player == null) return EMPTY;

        int batsType = batsOn ? config.bats.type : DISABLED_TYPE;
        int itemsType = itemsOn ? config.items.type : DISABLED_TYPE;

        boolean batKnown = false;
        List<SecretWaypoint> itemSecrets = null;
        for (SecretWaypoint secret : SecretTracker.secrets()) {
            if (secret.isHidden()) continue;

            SecretCategory category = secret.category();
            if (batsType != DISABLED_TYPE && category == SecretCategory.BAT) {
                batKnown = true;
            } else if (itemsType != DISABLED_TYPE && category == SecretCategory.ITEM) {
                if (itemSecrets == null) itemSecrets = new ArrayList<>(2);
                itemSecrets.add(secret);
            }
        }

        //#if MC < 1.21
        boolean batsThrough = batsOn && config.bats.throughWalls;
        boolean itemsThrough = itemsOn && config.items.throughWalls;
        //#else
        //$$ boolean batsThrough = true, itemsThrough = true;
        //#endif

        return new Snapshot(
                batKnown ? batsInBox(world, player) : Collections.emptySet(),
                itemSecrets == null ? Collections.emptySet() : scanItems(world, itemSecrets),
                batsOn ? argb(config.bats.color) : 0,
                itemsOn ? argb(config.items.color) : 0,
                batsType, itemsType, batsThrough, itemsThrough);
    }

    private static Set<Entity> scanItems(WorldClient world, List<SecretWaypoint> secrets) {
        boolean rescan = (++itemScanTick % ITEM_SCAN_INTERVAL) == 0;
        return rescan ? itemsInBoxes(world, secrets) : snapshot.items;
    }

    private static Set<Entity> batsInBox(WorldClient world, Entity player) {
        Set<Entity> bats = new HashSet<>();
        for (Entity entity : EntityCompat.getEntitiesInBox(world, batSearchBox(player))) {
            if (entity instanceof EntityBat && !EntityCompat.isDead(entity)) bats.add(entity);
        }
        return bats;
    }

    private static AxisAlignedBB batSearchBox(Entity player) {
        SecretRoom room = SecretTracker.current();
        double x = EntityCompat.getPosX(player);
        double y = EntityCompat.getPosY(player);
        double z = EntityCompat.getPosZ(player);

        if (room != null && room.hasBounds()) {
            return new AxisAlignedBB(room.minX() - 1, y - 32, room.minZ() - 1,
                    room.maxX() + 2, y + 32, room.maxZ() + 2);
        }
        return new AxisAlignedBB(x - 32, y - 32, z - 32, x + 32, y + 32, z + 32);
    }

    private static Set<Entity> itemsInBoxes(WorldClient world, List<SecretWaypoint> secrets) {
        Set<Entity> found = null;
        for (SecretWaypoint secret : secrets) {
            AxisAlignedBB box = SecretCollector.itemSearchBox(
                    secret.pos().getX() + 0.5, secret.pos().getY() + 0.5, secret.pos().getZ() + 0.5);
            for (Entity entity : EntityCompat.getEntitiesInBox(world, box)) {
                if (!(entity instanceof EntityItem) || EntityCompat.isDead(entity)) continue;
                if (!isSecretDrop((EntityItem) entity)) continue;

                double x = EntityCompat.getPosX(entity);
                double y = EntityCompat.getPosY(entity);
                double z = EntityCompat.getPosZ(entity);
                if (SecretCollector.nearestItemSecret(x, y, z) == null) continue;

                if (found == null) found = new HashSet<>();
                found.add(entity);
            }
        }
        return found != null ? found : Collections.emptySet();
    }

    // internal ids only, same source as pickup so a drop highlights exactly when it would count
    private static boolean isSecretDrop(EntityItem item) {
        return SecretCollector.isSecretItemId(ItemUtils.getInternalName(item.getEntityItem()));
    }

    private static Color color(String configColor) {
        return ChromaColour.specialToColor(configColor);
    }

    private static int argb(String configColor) {
        return ChromaColour.specialToChromaRGB(configColor);
    }
}
