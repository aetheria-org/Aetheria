package io.hamlook.aetheria.features.dungeons.rooms.secret;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.events.TickPhase;
import io.hamlook.aetheria.core.features.dungeons.DungeonSecretFinderConfig;
import io.hamlook.aetheria.core.features.dungeons.SecretHighlightsConfig;
import io.hamlook.aetheria.events.*;
import io.hamlook.aetheria.features.dungeons.DungeonStats;
import io.hamlook.aetheria.features.dungeons.HideBlessing;
import io.hamlook.aetheria.features.dungeons.rooms.DungeonRoomDetector;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.compat.BlockCompat;
import io.hamlook.aetheria.utils.compat.EntityCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.item.ItemUtils;
import io.hamlook.aetheria.utils.data.SkyblockData;
import net.minecraft.block.Block;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RegisterEvents
public class SecretCollector {

    private static final Pattern SECRETS_FOUND = Pattern.compile("(\\d+)/(\\d+)\\s*Secrets?");
    private static final Pattern CHEST_LOCKED = Pattern.compile("this secret is locked!", Pattern.CASE_INSENSITIVE);
    private static final Pattern SECRET_CHEST = Pattern.compile("secret chest", Pattern.CASE_INSENSITIVE);
    private static final Pattern WITHER_ESSENCE = Pattern.compile("found a wither essence", Pattern.CASE_INSENSITIVE);

    private static final double CHEST_PENDING_RADIUS = 3.0;
    private static final int CHEST_PENDING_TICKS = 40;

    private static final double ESSENCE_WAYPOINT_RADIUS = 2.0;
    private static final int ESSENCE_PENDING_TICKS = 40;

    // an essence is an armor stand, so its waypoint block never changes, watch the stand instead
    private static final int ESSENCE_SAMPLE_TICKS = 10;
    private static final int ESSENCE_MISSES = 2;
    private static final double ESSENCE_SCAN_RADIUS = 32.0;
    private static final double ESSENCE_BIND_RADIUS = 1.5;
    private static final double ESSENCE_BIND_VERTICAL = 2.0;
    private static final double ESSENCE_PRESENT_RADIUS = 2.0;
    private static final double ESSENCE_COLLECT_RADIUS = 12.0;

    private static final int ITEM_PENDING_TICKS = 20;
    private static final double ITEM_EJECT_HEIGHT = 4.0;
    private static final double AIR_CHECK_RANGE_SQ = 36.0;
    private static final Set<String> SECRET_ITEM_IDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "DUNGEON_DECOY", "DEFUSE_KIT", "SPIRIT_LEAP", "DUNGEON_CHEST_KEY",
            "INFLATABLE_JERRY", "TREASURE_TALISMAN", "TRAINING_WEIGHTS", "SECRET_DYE")));

    // one place so pickup and highlight stay in sync
    static boolean isSecretItemId(String id) {
        return SECRET_ITEM_IDS.contains(id);
    }

    private static final List<Pending> pending = new ArrayList<>();
    private static final Map<SecretWaypoint, EntityArmorStand> essenceStands = new HashMap<>();
    private static int periodicTickCounter;
    private static int essenceTickCounter;

    private static Pattern selfPattern;
    private static String selfName;

    private static final class Pending {
        final SecretCategory category;
        final SecretWaypoint secret;
        final boolean hiddenOnArm;
        int ticksLeft;

        Pending(SecretCategory category, SecretWaypoint secret, boolean hiddenOnArm, int ticksLeft) {
            this.category = category;
            this.secret = secret;
            this.hiddenOnArm = hiddenOnArm;
            this.ticksLeft = ticksLeft;
        }
    }

    public static void reset() {
        pending.clear();
        essenceStands.clear();
        periodicTickCounter = 0;
        essenceTickCounter = 0;
    }

    @HandleEvent
    public void onPlayerTick(ASMTickEvent event) {
        if (event.phase != TickPhase.START) return;
        if (MinecraftCompat.getLocalPlayer() == null) return;

        SecretTracker.activateAt(
                EntityCompat.getPosX(MinecraftCompat.getLocalPlayer()),
                EntityCompat.getPosZ(MinecraftCompat.getLocalPlayer()));

        if (!isActive()) return;

        tickPending();
        collectEnteredEntrance();
        tickPeriodic();
        tickEssences();
    }

    @HandleEvent
    public void onActionBar(ActionBarUpdateEvent event) {
        if (MinecraftCompat.getLocalPlayer() == null) return;

        Matcher matcher = SECRETS_FOUND.matcher(ColorUtils.stripColor(event.getText()));
        if (!matcher.find()) return;

        int found = Integer.parseInt(matcher.group(1));
        int total = Integer.parseInt(matcher.group(2));
        if (SecretTracker.acceptCounters(found, total) != SecretTracker.CounterResult.INCREMENT) return;

        if (hasPendingItem()) resolvePending(SecretCategory.ITEM, true);
    }

    @HandleEvent
    public void onChat(ASMChatEvent event) {
        if (event.type == ASMChatEvent.Type.ACTION_BAR) return;
        // parsed only comes back non-null when a player wrote the line
        if (event.parsed() != null) return;

        String text = event.plain();
        if (CHEST_LOCKED.matcher(text).find()) {
            removePending(SecretCategory.CHEST);
            return;
        }
        // only the finder gets credited, a teammate's line says nothing about our clicks
        if (!mentionsSelf(text)) return;

        if (WITHER_ESSENCE.matcher(text).find()) {
            resolvePending(SecretCategory.WITHER, true);
            return;
        }
        if (HideBlessing.BLESSING.matcher(event.formatted()).find()) {
            resolvePending(SecretCategory.CHEST, true);
        }
    }

    @HandleEvent
    public void onInventoryOpen(InventoryOpenEvent event) {
        // a secret chest always names itself, any other window means the click hit a looted one
        if (SECRET_CHEST.matcher(event.getInventoryName()).find()) {
            resolvePending(SecretCategory.CHEST, true);
            return;
        }
        resolvePending(SecretCategory.CHEST, false);
    }

    @HandleEvent
    public void onItemPickup(ItemPickupEvent event) {
        if (!isActive()) return;
        Entity player = MinecraftCompat.getLocalPlayer();
        if (player == null || event.collectorEntityId != EntityCompat.getEntityId(player)) return;
        String id = ItemUtils.getInternalName(event.stack);
        if (id.isEmpty()) return;
        if (!SECRET_ITEM_IDS.contains(id)) return;
        SecretWaypoint hit = nearestItemSecret(event.x, event.y, event.z);
        if (hit != null) arm(SecretCategory.ITEM, hit, ITEM_PENDING_TICKS, true);
    }

    static AxisAlignedBB itemSearchBox(double x, double y, double z) {
        double h = config().secrets.itemHorizontalRange;
        double v = config().secrets.itemVerticalRange;
        return new AxisAlignedBB(x - h, y - v, z - h, x + h, y + ITEM_EJECT_HEIGHT, z + h);
    }

    static SecretWaypoint nearestItemSecret(double x, double y, double z) {
        double h = config().secrets.itemHorizontalRange;
        double v = config().secrets.itemVerticalRange;
        double hSq = h * h;

        SecretWaypoint best = null;
        double bestMetric = Double.MAX_VALUE;
        for (SecretWaypoint secret : SecretTracker.secrets()) {
            if (isUnavailable(secret, SecretCategory.ITEM)) continue;
            double fly = y - (secret.pos().getY() + 0.5);
            if (fly > ITEM_EJECT_HEIGHT || fly < -v) continue;
            double dx = secret.pos().getX() + 0.5 - x;
            double dz = secret.pos().getZ() + 0.5 - z;
            double dxzSq = dx * dx + dz * dz;
            if (dxzSq > hSq) continue;
            double metric = dxzSq / hSq + (fly * fly) / (v * v);
            if (metric < bestMetric) {
                bestMetric = metric;
                best = secret;
            }
        }
        return best;
    }

    private static boolean hasPendingItem() {
        for (Pending entry : pending) {
            if (entry.category == SecretCategory.ITEM) return true;
        }
        return false;
    }

    @HandleEvent
    public void onPlayerInteract(ASMPlayerInteractEvent event) {
        if (event.action == ASMPlayerInteractEvent.INTERACT_ENTITY) {
            armEssence(event.entity);
            return;
        }
        if (event.action != ASMPlayerInteractEvent.RIGHT_CLICK_BLOCK) return;
        if (event.pos == null || SecretTracker.secrets().isEmpty()) return;

        World world = MinecraftCompat.getLocalWorld();
        if (world == null) return;

        Block clicked = world.getBlockState(event.pos).getBlock();
        if (BlockCompat.isLever(clicked)) {
            hideLever(event.pos);
            return;
        }
        if (!isChest(clicked)) return;

        SecretWaypoint chest = nearestSecret(event.pos.getX() + 0.5, event.pos.getY() + 0.5,
                event.pos.getZ() + 0.5, SecretCategory.CHEST, CHEST_PENDING_RADIUS);
        if (chest != null) armChest(chest);
    }

    private static boolean isChest(Block block) {
        return block == Blocks.chest || block == Blocks.trapped_chest || block == Blocks.ender_chest;
    }

    private static void hideLever(BlockPos pos) {
        SecretWaypoint lever = at(SecretCategory.LEVER, pos);
        if (lever != null) lever.dismiss();
    }

    private static SecretWaypoint at(SecretCategory category, BlockPos pos) {
        for (SecretWaypoint secret : SecretTracker.secrets()) {
            if (isUnavailable(secret, category)) continue;
            if (secret.pos().equals(pos)) return secret;
        }
        return null;
    }

    // either button arms an essence, the chat line is the only thing that credits it
    private void armEssence(Entity entity) {
        if (!(entity instanceof EntityArmorStand)) return;
        if (!EssenceSkull.matches(EntityCompat.getHelmet((EntityLivingBase) entity))) return;

        // the waypoint only decides what to hide, an essence with none still arms
        SecretWaypoint essence = nearestSecret(
                EntityCompat.getPosX(entity), EntityCompat.getPosY(entity), EntityCompat.getPosZ(entity),
                SecretCategory.WITHER, ESSENCE_WAYPOINT_RADIUS);
        arm(SecretCategory.WITHER, essence, ESSENCE_PENDING_TICKS, true);
    }

    private static SecretWaypoint nearestSecret(double x, double y, double z, SecretCategory category, double range) {
        double bestSq = range * range;
        SecretWaypoint best = null;

        for (SecretWaypoint secret : SecretTracker.secrets()) {
            if (isUnavailable(secret, category)) continue;

            double distanceSq = secret.distanceSq(x, y, z);
            if (distanceSq > bestSq) continue;
            bestSq = distanceSq;
            best = secret;
        }
        return best;
    }

    private static boolean isUnavailable(SecretWaypoint secret, SecretCategory category) {
        return secret.isHidden() || secret.category() != category;
    }

    // server lines name the finder, so a teammate's buff cannot credit our armed chest
    private static boolean mentionsSelf(String plain) {
        Entity player = MinecraftCompat.getLocalPlayer();
        if (player == null) return false;

        String name = EntityCompat.getName(player);
        if (selfPattern == null || !name.equals(selfName)) {
            selfName = name;
            selfPattern = Pattern.compile("\\b" + Pattern.quote(name) + "\\b");
        }
        return selfPattern.matcher(plain).find();
    }

    private void tickEssences() {
        if (++essenceTickCounter < ESSENCE_SAMPLE_TICKS) return;
        essenceTickCounter = 0;

        List<SecretWaypoint> essences = nearbyEssences();
        if (essences.isEmpty()) return;

        List<EntityArmorStand> stands = nearbyStands();
        trackEssenceStands(essences, stands);

        for (SecretWaypoint essence : essences) {
            if (essence.isBound()) sampleBoundEssence(essence, stands);
            else bindNearestStand(essence, stands);
        }
    }

    static EntityArmorStand essenceStand(SecretWaypoint essence) {
        return essenceStands.get(essence);
    }

    private static void trackEssenceStands(List<SecretWaypoint> essences, List<EntityArmorStand> stands) {
        SecretHighlightsConfig highlights = SecretHighlightsConfig.get();
        if (highlights == null || !highlights.essence.enabled) {
            essenceStands.clear();
            return;
        }

        for (SecretWaypoint essence : essences) {
            EntityArmorStand known = essenceStands.get(essence);
            if (known != null && !EntityCompat.isDead(known)) continue;

            EntityArmorStand match = matchingStand(essence, stands);
            if (match == null) essenceStands.remove(essence);
            else essenceStands.put(essence, match);
        }
    }

    private static EntityArmorStand matchingStand(SecretWaypoint essence, List<EntityArmorStand> stands) {
        for (EntityArmorStand stand : stands) {
            if (EntityCompat.isDead(stand)) continue;
            if (distanceTo(stand, essence) > ESSENCE_WAYPOINT_RADIUS) continue;
            if (EssenceSkull.matches(EntityCompat.getHelmet(stand))) return stand;
        }
        return null;
    }

    private static List<SecretWaypoint> nearbyEssences() {
        List<SecretWaypoint> nearby = new ArrayList<>();
        double rangeSq = ESSENCE_SCAN_RADIUS * ESSENCE_SCAN_RADIUS;

        for (SecretWaypoint secret : SecretTracker.secrets()) {
            if (isUnavailable(secret, SecretCategory.WITHER)) continue;
            if (secret.distanceSqToPlayer() > rangeSq) continue;
            nearby.add(secret);
        }
        return nearby;
    }

    // box query, vanilla only walks the chunks the box touches
    private static List<EntityArmorStand> nearbyStands() {
        List<EntityArmorStand> stands = new ArrayList<>();
        WorldClient world = MinecraftCompat.getLocalWorld();
        Entity player = MinecraftCompat.getLocalPlayer();
        if (world == null || player == null) return stands;

        AxisAlignedBB box = EntityCompat.getEntityBoundingBox(player)
                .expand(ESSENCE_SCAN_RADIUS, ESSENCE_SCAN_RADIUS, ESSENCE_SCAN_RADIUS);

        for (EntityLivingBase entity : EntityCompat.getLivingInBox(world, box)) {
            if (!(entity instanceof EntityArmorStand)) continue;
            // any helmet counts, the head item is not the same on every version
            if (EntityCompat.getHelmet(entity) == null) continue;
            stands.add((EntityArmorStand) entity);
        }
        return stands;
    }

    private static void bindNearestStand(SecretWaypoint essence, List<EntityArmorStand> stands) {
        EntityArmorStand best = null;
        double bestSq = ESSENCE_BIND_RADIUS * ESSENCE_BIND_RADIUS;

        for (EntityArmorStand stand : stands) {
            if (Math.abs(EntityCompat.getPosY(stand) - (essence.pos().getY() + 0.5)) > ESSENCE_BIND_VERTICAL) continue;

            double dx = EntityCompat.getPosX(stand) - (essence.pos().getX() + 0.5);
            double dz = EntityCompat.getPosZ(stand) - (essence.pos().getZ() + 0.5);
            double distanceSq = dx * dx + dz * dz;
            if (distanceSq > bestSq) continue;
            bestSq = distanceSq;
            best = stand;
        }
        if (best == null) return;

        essence.bind(best.getEntityId());
    }

    // ids get reused, so every sample rechecks the entity instead of trusting the id alone
    private static void sampleBoundEssence(SecretWaypoint essence, List<EntityArmorStand> stands) {
        EntityArmorStand bound = null;
        for (EntityArmorStand stand : stands) {
            if (stand.getEntityId() != essence.boundEntityId()) continue;
            bound = stand;
            break;
        }

        boolean present = bound != null
                && essence.distanceSq(EntityCompat.getPosX(bound), EntityCompat.getPosY(bound),
                EntityCompat.getPosZ(bound)) <= ESSENCE_PRESENT_RADIUS * ESSENCE_PRESENT_RADIUS;

        if (present) {
            essence.notePresent();
            return;
        }

        // entity tracking drops stands too, so only judge it while we are standing close
        if (essence.distanceSqToPlayer() > ESSENCE_COLLECT_RADIUS * ESSENCE_COLLECT_RADIUS) return;

        if (!essence.noteAbsent(ESSENCE_MISSES)) return;
        // a teammate can take it, so only hide the waypoint, never credit it
        essence.dismiss();
    }

    @HandleEvent
    public void onBlockBreak(BlockBreakEvent event) {
        if (MinecraftCompat.getLocalWorld() == null || SecretTracker.secrets().isEmpty()) return;

        SecretWaypoint stonk = at(SecretCategory.STONK, event.pos);
        mark(stonk);
    }

    @HandleEvent
    public void onEntityJoinWorld(ASMEntityJoinWorldEvent event) {
        if (MinecraftCompat.getLocalWorld() == null || SecretTracker.secrets().isEmpty()) return;
        if (!(event.entity instanceof EntityTNTPrimed)) return;

        double range = config().secrets.superboomRemovalRange;
        for (SecretWaypoint secret : SecretTracker.secrets()) {
            if (secret.isHidden() || secret.category() != SecretCategory.SUPERBOOM) continue;
            if (distanceTo(event.entity, secret) <= range) mark(secret);
        }
    }

    @HandleEvent
    public void onLivingDeath(ASMLivingDeathEvent event) {
        if (MinecraftCompat.getLocalWorld() == null || SecretTracker.secrets().isEmpty()) return;
        if (!(event.entity instanceof EntityBat)) return;
        if (!DungeonRoomDetector.roomBoundsValid) return;

        double x = EntityCompat.getPosX(event.entity);
        double z = EntityCompat.getPosZ(event.entity);
        if (x < DungeonRoomDetector.roomMinX || x > DungeonRoomDetector.roomMaxX) return;
        if (z < DungeonRoomDetector.roomMinZ || z > DungeonRoomDetector.roomMaxZ) return;

        for (SecretWaypoint secret : SecretTracker.secrets()) {
            if (secret.category() == SecretCategory.BAT) secret.collect();
        }
    }

    private static DungeonSecretFinderConfig config() {
        return DungeonSecretFinderConfig.get();
    }

    static boolean isActive() {
        return SkyblockData.getCurrentLocation() == SkyblockData.Location.DUNGEON
                && !DungeonStats.isInBossFight()
                && !SecretTracker.secrets().isEmpty();
    }

    private static void mark(SecretWaypoint secret) {
        if (secret == null) return;
        if (secret.category().isSecret()) secret.collect();
        else secret.dismiss();
    }

    private static double distanceTo(Entity entity, SecretWaypoint secret) {
        return EntityCompat.getDistanceToPos(entity,
                secret.pos().getX() + 0.5, secret.pos().getY() + 0.5, secret.pos().getZ() + 0.5);
    }

    private void arm(SecretCategory category, SecretWaypoint secret, int ticks, boolean hiddenOnArm) {
        if (secret != null && hiddenOnArm) secret.dismiss();
        pending.add(new Pending(category, secret, hiddenOnArm, ticks));
    }

    // only one chest menu can be open, so a new click replaces the previous guess
    private void armChest(SecretWaypoint chest) {
        removePending(SecretCategory.CHEST);
        arm(SecretCategory.CHEST, chest, CHEST_PENDING_TICKS, false);
    }

    // credit only when the server says a secret was gained, otherwise it was already looted
    private void resolvePending(SecretCategory category, boolean credited) {
        for (Pending entry : removePending(category)) {
            if (entry.secret == null) continue;
            if (credited) entry.secret.collect();
            else entry.secret.dismiss();
        }
    }

    private static List<Pending> removePending(SecretCategory category) {
        List<Pending> removed = new ArrayList<>();
        for (Iterator<Pending> iterator = pending.iterator(); iterator.hasNext(); ) {
            Pending entry = iterator.next();
            if (entry.category != category) continue;
            iterator.remove();
            removed.add(entry);
        }
        return removed;
    }

    private void tickPending() {
        for (Iterator<Pending> iterator = pending.iterator(); iterator.hasNext(); ) {
            Pending entry = iterator.next();
            if (--entry.ticksLeft > 0) continue;
            iterator.remove();
            // an essence waypoint already went on the click, a chest one goes once the guess failed
            if (entry.secret != null && !entry.hiddenOnArm) entry.secret.dismiss();
        }
    }

    private void collectEnteredEntrance() {
        Entity player = MinecraftCompat.getLocalPlayer();
        if (player == null) return;

        double range = config().secrets.entranceRemovalRange;
        double rangeSq = range * range;
        double px = EntityCompat.getPosX(player);
        double pz = EntityCompat.getPosZ(player);

        for (SecretWaypoint secret : SecretTracker.secrets()) {
            if (secret.isHidden() || secret.category() != SecretCategory.ENTRANCE) continue;
            double dx = px - (secret.pos().getX() + 0.5);
            double dz = pz - (secret.pos().getZ() + 0.5);
            if (dx * dx + dz * dz <= rangeSq) mark(secret);
        }
    }

    private void tickPeriodic() {
        int intervalTicks = (int) (config().secrets.updateInterval * 20);
        if (++periodicTickCounter < intervalTicks) return;
        periodicTickCounter = 0;

        World world = MinecraftCompat.getLocalWorld();
        if (world == null) return;

        for (SecretWaypoint secret : SecretTracker.secrets()) {
            if (secret.isHidden()) continue;

            secret.advanceTicks(intervalTicks);
            if (secret.distanceSqToPlayer() > AIR_CHECK_RANGE_SQ) continue;

            boolean air = BlockCompat.isAir(world.getBlockState(secret.pos()).getBlock());
            if (!secret.isSeen() && !air) secret.markSeen();

            switch (secret.category()) {
                case SUPERBOOM:
                    if (air && secret.airGraceExpired()) mark(secret);
                    break;
                case WITHER:
                    // a bound essence is owned by presence tracking, the block form still goes by air
                    if (!secret.isBound() && air && secret.airGraceExpired()) secret.collect();
                    break;
            }
        }
    }
}
