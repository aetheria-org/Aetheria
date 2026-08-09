package io.hamlook.aetheria.features.farming.trevor;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.farming.TrevorConfig;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import io.hamlook.aetheria.features.waypoints.WaypointRenderer;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.data.SkyblockData;
import io.hamlook.aetheria.utils.render.WorldRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Trevor the Trapper quest helper for the Mushroom Desert: highlights the fixed
 * spawn spots of the area Trevor announces and marks the spawned animal with a
 * depth-tested beacon beam once its armor stand is loaded.
 */
@RegisterEvents
public class TrevorSolver {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final Pattern QUEST_START = Pattern.compile(
            "^\\[NPC\\] Trevor: You can find your (Trackable|Untrackable|Undetected|Endangered|Elusive) (Cow|Pig|Sheep|Rabbit|Chicken|Horse) near the (.+?)\\.?$");
    private static final Pattern QUEST_REWARD = Pattern.compile("^Killing the animal rewarded you (\\d+) Pelts?\\.?$");
    private static final long WARP_WINDOW_MS = 5000;

    private static final String[] DESERT_AREAS = {"Desert Settlement", "Oasis", "Mushroom Gorge",
            "Overgrown Mushroom Cave", "Glowing Mushroom Cave", "Desert Mountain", "Jake's House",
            "Treasure Hunter Camp", "Trapper's Den", "Mushroom Desert"};

    private String questAnimal = null;
    private String questRarityLower = null;
    private String questAnimalLower = null;
    private List<BlockPos> activeSpots = Collections.emptyList();
    private EntityArmorStand trackedAnimal = null;
    private final Set<Integer> alertedIds = new HashSet<>();
    private static boolean onDesertIsland = false;
    private int tickCounter = 0;
    private long lastRewardMs = 0;

    public static boolean isOnDesertIsland() {
        return onDesertIsland;
    }

    private TrevorConfig config() {
        return ATHRConfig.feature == null ? null : ATHRConfig.feature.farming.trevor;
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        TrevorConfig config = config();
        if (config == null || !config.enabled) return;
        if (!ChatUtils.isFromServer(event)) return;
        String msg = ChatUtils.clean(event);

        Matcher start = QUEST_START.matcher(msg);
        if (start.matches()) {
            questAnimal = start.group(1) + " " + start.group(2);
            questRarityLower = start.group(1).toLowerCase(Locale.ROOT);
            questAnimalLower = start.group(2).toLowerCase(Locale.ROOT);
            activeSpots = TrevorSpots.forLocation(start.group(3));
            PeltOverlay.startCooldown();
            return;
        }
        Matcher reward = QUEST_REWARD.matcher(msg);
        if (reward.matches()) {
            PeltOverlay.addPelts(Integer.parseInt(reward.group(1)));
            lastRewardMs = System.currentTimeMillis();
            reset();
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        TrevorConfig config = config();
        if (config == null || !config.enabled || !config.warpHelper) return;
        if (mc.thePlayer == null || mc.currentScreen != null) return;
        int key = config.warpKey;
        if (key == Keyboard.KEY_NONE || !Keyboard.getEventKeyState() || Keyboard.getEventKey() != key) return;
        if (lastRewardMs == 0 || System.currentTimeMillis() - lastRewardMs > WARP_WINDOW_MS) return;
        lastRewardMs = 0;
        mc.thePlayer.sendChatMessage("/warp trapper");
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        TrevorConfig config = config();
        if (config == null || !config.enabled || mc.theWorld == null || mc.thePlayer == null) return;
        if (++tickCounter < 5) return;
        tickCounter = 0;

        onDesertIsland = detectDesertIsland();
        // Only scan while a quest is active; matching the quest's exact rarity +
        // animal keeps other players' trapper animals (and the just-killed one
        // after the reward message resets state) from triggering alerts.
        if (!onDesertIsland || questAnimal == null) {
            trackedAnimal = null;
            return;
        }

        EntityArmorStand nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityArmorStand) || entity.isDead) continue;
            String raw = entity.getName();
            if (raw == null) continue;
            String name = StringUtils.stripControlCodes(raw).toLowerCase(Locale.ROOT);
            if (!name.contains(questRarityLower) || !name.contains(questAnimalLower)) continue;
            double dist = mc.thePlayer.getDistanceSqToEntity(entity);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = (EntityArmorStand) entity;
            }
        }
        trackedAnimal = nearest;

        if (nearest != null && config.detectAlert && alertedIds.add(nearest.getEntityId())) {
            if (mc.ingameGUI != null) mc.ingameGUI.displayTitle("§6§lANIMAL DETECTED", "§e" + questAnimal, 5, 40, 10);
            ChatUtils.sendMessage("§6[Aetheria] §eAnimal detected: §6" + questAnimal);
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        TrevorConfig config = config();
        if (config == null || !config.enabled || !onDesertIsland || mc.thePlayer == null) return;

        if (!activeSpots.isEmpty()) {
            Color spotColor = argbToColor(config.spotColor, 0x7800FFFF);
            List<AxisAlignedBB> boxes = new ArrayList<>();
            for (BlockPos pos : activeSpots) {
                boxes.add(new AxisAlignedBB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1));
            }
            WorldRenderUtils.drawFilledBlocks(boxes, spotColor);

            if (config.spotLabels) {
                double vx = mc.getRenderManager().viewerPosX;
                double vy = mc.getRenderManager().viewerPosY;
                double vz = mc.getRenderManager().viewerPosZ;
                String label = questAnimal != null ? questAnimal : "Trevor Spot";
                GL11.glPushMatrix();
                GL11.glTranslated(-vx, -vy, -vz);
                for (BlockPos pos : activeSpots) {
                    WaypointRenderer.drawLabel(pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5, label, 0xFF55FFFF, 0xFF55FFFF);
                }
                GL11.glPopMatrix();
            }
        }

        EntityArmorStand animal = trackedAnimal;
        if (animal != null && config.animalBeacon && !animal.isDead) {
            double x = animal.lastTickPosX + (animal.posX - animal.lastTickPosX) * event.partialTicks;
            double y = animal.lastTickPosY + (animal.posY - animal.lastTickPosY) * event.partialTicks;
            double z = animal.lastTickPosZ + (animal.posZ - animal.lastTickPosZ) * event.partialTicks;
            Color beaconColor = argbToColor(config.beaconColor, 0xFFFFAA00);
            WorldRenderUtils.drawBeaconBeam(x - 0.5, y - 2.0, z - 0.5, beaconColor, event.partialTicks, 64);
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        reset();
        onDesertIsland = false;
    }

    private void reset() {
        questAnimal = null;
        questRarityLower = null;
        questAnimalLower = null;
        activeSpots = Collections.emptyList();
        trackedAnimal = null;
        alertedIds.clear();
    }

    private boolean detectDesertIsland() {
        for (String line : SkyblockData.getCleanScoreboardLines()) {
            for (String area : DESERT_AREAS) {
                if (line.contains(area)) return true;
            }
        }
        return false;
    }

    private static Color argbToColor(String special, int fallback) {
        int argb = fallback;
        try {
            argb = ChromaColour.specialToChromaRGB(special);
        } catch (Exception ignored) {
        }
        return new Color((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >> 24) & 0xFF);
    }
}
