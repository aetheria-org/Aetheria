package io.hamlook.aetheria.features.dungeons.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import io.hamlook.aetheria.events.ActionBarUpdateEvent;
import io.hamlook.aetheria.events.BlockBreakEvent;
import io.hamlook.aetheria.features.waypoints.WaypointRenderer;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.data.SkyblockData;
import io.hamlook.aetheria.utils.render.WorldRenderUtils;
import io.netty.util.internal.ConcurrentSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;
import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RegisterEvents
public class SecretRenderUtils {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final ConcurrentSet<SecretWaypoint> currentSecrets = new ConcurrentSet<>();
    private static int lastActionBarSecrets = -1;
    private static int periodicTickCounter = 0;
    private static final int WAYPOINT_REMOVAL_GRACE_TICKS = 5; // ticks to wait before auto‑removing waypoints that become air or invalid
    private static final Pattern SECRETS_FOUND_PATTERN = Pattern.compile("Secrets Found:\\s*(\\d+)/(\\d+)");

    private static class SecretWaypoint {
        final AxisAlignedBB aabb;
        final String category;
        final String secretName;
        final BlockPos pos;
        boolean collected;
        boolean everSeen;
        int ticksExisted;

        SecretWaypoint(AxisAlignedBB aabb, String category, String secretName, BlockPos pos) {
            this.aabb = aabb;
            this.category = category;
            this.secretName = secretName;
            this.pos = pos;
            this.collected = false;
            this.everSeen = false;
            this.ticksExisted = 0;
        }
    }

    public static void clearSecrets() {
        currentSecrets.clear();
        lastActionBarSecrets = -1;
    }

    public static void loadSecrets(String roomName, JsonObject secretLocationsJson) {
        currentSecrets.clear();
        lastActionBarSecrets = -1;

        if (!secretLocationsJson.has(roomName)) return;

        JsonArray secrets = secretLocationsJson.get(roomName).getAsJsonArray();
        for (int i = 0; i < secrets.size(); i++) {
            JsonObject secret = secrets.get(i).getAsJsonObject();
            String category = secret.get("category").getAsString();
            String secretName = secret.get("secretName").getAsString();
            int rx = secret.get("x").getAsInt();
            int ay = secret.get("y").getAsInt();
            int rz = secret.get("z").getAsInt();

            BlockPos relPos = new BlockPos(rx, ay, rz);
            BlockPos absPos = DungeonRoomDetector.relativeToActual(relPos);
            if (absPos != null) {
                AxisAlignedBB aabb = new AxisAlignedBB(
                    absPos.getX() + 0.05, absPos.getY() + 0.05, absPos.getZ() + 0.05,
                    absPos.getX() + 0.95, absPos.getY() + 0.95, absPos.getZ() + 0.95
                );
                currentSecrets.add(new SecretWaypoint(aabb, category, secretName, absPos));
            }
        }
    }

    public static int getActiveSecretCount() {
        int count = 0;
        for (SecretWaypoint sw : currentSecrets) {
            if (!sw.collected) count++;
        }
        return count;
    }

    private static String formatSecretName(String secretName) {
        boolean compact = ATHRConfig.feature != null
            && ATHRConfig.feature.dungeons.dungeonSecretFinder.toggles.compactLabels;
        if (!compact) return secretName;
        int idx = secretName.indexOf(" - ");
        return idx >= 0 ? secretName.substring(0, idx) : secretName;
    }

    public static void renderSecrets(float partialTicks) {
        if (currentSecrets.isEmpty() || getActiveSecretCount() == 0) return;

        boolean sfOn = ATHRConfig.feature != null && ATHRConfig.feature.dungeons.dungeonSecretFinder.enabled;
        if (!sfOn) return;

        Color labelColor = parseConfigColor(ATHRConfig.feature.dungeons.dungeonSecretFinder.colors.labelColor);
        Color boxColor = parseConfigColor(ATHRConfig.feature.dungeons.dungeonSecretFinder.colors.boxColor);
        Color waypointColor = parseConfigColor(ATHRConfig.feature.dungeons.dungeonSecretFinder.colors.waypointColor);
        Color tracerColor = parseConfigColor(ATHRConfig.feature.dungeons.dungeonSecretFinder.colors.tracerColor);
        boolean showLabels = ATHRConfig.feature.dungeons.dungeonSecretFinder.toggles.showLabels;
        boolean showWaypoints = ATHRConfig.feature.dungeons.dungeonSecretFinder.toggles.showWaypoints;
        boolean showTracer = ATHRConfig.feature.dungeons.dungeonSecretFinder.toggles.showTracer;
        boolean showBoundingBox = ATHRConfig.feature.dungeons.dungeonSecretFinder.toggles.showBoundingBox;
        boolean showBorderBox = ATHRConfig.feature.dungeons.dungeonSecretFinder.toggles.showBorderBox;
        boolean throughWalls = ATHRConfig.feature.dungeons.dungeonSecretFinder.toggles.showThroughWalls;
        double labelScale = ATHRConfig.feature.dungeons.dungeonSecretFinder.other.labelScale;

        double vx = mc.getRenderManager().viewerPosX;
        double vy = mc.getRenderManager().viewerPosY;
        double vz = mc.getRenderManager().viewerPosZ;

        SecretWaypoint nearest = findNearestSecret();

        if (showLabels) {
            resetGLState();
            GL11.glPushMatrix();
            GL11.glTranslated(-vx, -vy, -vz);
            for (SecretWaypoint sw : currentSecrets) {
                if (sw.collected) continue;
                int bx = (int) Math.floor(sw.aabb.minX);
                int by = (int) Math.floor(sw.aabb.minY);
                int bz = (int) Math.floor(sw.aabb.minZ);
                WaypointRenderer.drawLabel(bx + 0.5, by + 2.0, bz + 0.5, formatSecretName(sw.secretName), labelColor.getRGB(), tracerColor.getRGB(), labelScale);
            }
            GL11.glPopMatrix();
        }

        if (showBorderBox) {
            resetGLState();
            GL11.glPushMatrix();
            GL11.glTranslated(-vx, -vy, -vz);
            setDepth(!throughWalls);
            GlStateManager.disableTexture2D();
            GL11.glLineWidth(3f);
            for (SecretWaypoint sw : currentSecrets) {
                if (sw.collected) continue;
                WorldRenderUtils.drawEspBox((int) Math.floor(sw.aabb.minX), (int) Math.floor(sw.aabb.minY), (int) Math.floor(sw.aabb.minZ), boxColor);
            }
            GL11.glLineWidth(1f);
            GL11.glPopMatrix();
        }

        if (showWaypoints) {
            resetGLState();
            setDepth(!throughWalls);
            for (SecretWaypoint sw : currentSecrets) {
                if (sw.collected) continue;
                int bx = (int) Math.floor(sw.aabb.minX);
                int by = (int) Math.floor(sw.aabb.minY);
                int bz = (int) Math.floor(sw.aabb.minZ);
                AxisAlignedBB beam = new AxisAlignedBB(bx + 0.25, by, bz + 0.25, bx + 0.75, 300, bz + 0.75);
                WorldRenderUtils.drawFilledBlock(beam, waypointColor, true);
            }
        }

        if (showBoundingBox) {
            resetGLState();
            setDepth(!throughWalls);
            for (SecretWaypoint sw : currentSecrets) {
                if (sw.collected) continue;
                WorldRenderUtils.drawFilledBlock(sw.aabb, boxColor, true);
            }
        }

        if (showTracer && nearest != null) {
            int bx = (int) Math.floor(nearest.aabb.minX);
            int by = (int) Math.floor(nearest.aabb.minY);
            int bz = (int) Math.floor(nearest.aabb.minZ);
            float tracerWidth = ATHRConfig.feature.dungeons.dungeonSecretFinder.other.tracerWidth;
            WorldRenderUtils.drawTracer(new Vec3(bx, by, bz), partialTicks, tracerColor, tracerWidth);
        }

        setDepth(true);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glLineWidth(1f);
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.disableTexture2D();
        GlStateManager.enableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableLighting();
        GlStateManager.enableBlend();
        GlStateManager.disableBlend();
        GlStateManager.disableCull();
        GlStateManager.enableCull();
    }

    private static void resetGLState() {
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.enableDepth();
        GL11.glDepthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableCull();
    }

    private static void setDepth(boolean enable) {
        if (enable) {
            GL11.glDepthMask(true);
            GlStateManager.enableDepth();
        } else {
            GL11.glDepthMask(false);
            GlStateManager.disableDepth();
        }
    }

    private static SecretWaypoint findNearestSecret() {
        SecretWaypoint nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (SecretWaypoint sw : currentSecrets) {
            if (sw.collected) continue;
            double dist = mc.thePlayer.getDistance(sw.pos.getX() + 0.5, sw.pos.getY() + 0.5, sw.pos.getZ() + 0.5);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = sw;
            }
        }
        return nearest;
    }

    private static Color parseConfigColor(String colourStr) {
        int argb = ChromaColour.specialToChromaRGB(colourStr);
        return new Color((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >> 24) & 0xFF);
    }

    private static boolean isInDungeonContext() {
        return SkyblockData.getCurrentLocation() == SkyblockData.Location.DUNGEON
            && !currentSecrets.isEmpty();
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (mc.thePlayer == null || !isInDungeonContext()) return;

        for (SecretWaypoint sw : currentSecrets) {
            if (sw.collected) continue;

            if (!"entrance".equals(sw.category)) continue;

            double dx = mc.thePlayer.posX - (sw.pos.getX() + 0.5);
            double dz = mc.thePlayer.posZ - (sw.pos.getZ() + 0.5);
            double horizDist = Math.sqrt(dx * dx + dz * dz);

            double range = ATHRConfig.feature.dungeons.dungeonSecretFinder.range.entranceRemovalRange;
            if (horizDist <= range) {
                sw.collected = true;
            }
        }

        int intervalTicks = (int) (ATHRConfig.feature.dungeons.dungeonSecretFinder.other.updateInterval * 20);
        periodicTickCounter++;
        if (periodicTickCounter >= intervalTicks) {
            periodicTickCounter = 0;
            double itemRange = ATHRConfig.feature.dungeons.dungeonSecretFinder.range.itemRemovalRange;
for (SecretWaypoint sw : currentSecrets) {
                    if (sw.collected) continue;
                    sw.ticksExisted++;
                    if (!sw.everSeen) {
                        if (mc.theWorld.getBlockState(sw.pos).getBlock() != net.minecraft.init.Blocks.air) {
                            sw.everSeen = true;
                        }
                    }
                    if ("wither".equals(sw.category) || sw.secretName.contains("Essence")) {
                        if (sw.everSeen && sw.ticksExisted >= WAYPOINT_REMOVAL_GRACE_TICKS && mc.theWorld.getBlockState(sw.pos).getBlock() == net.minecraft.init.Blocks.air) {
                            sw.collected = true;
                        }
                    } else if ("item".equals(sw.category)) {
                        double dist = mc.thePlayer.getDistance(sw.pos.getX() + 0.5, sw.pos.getY() + 0.5, sw.pos.getZ() + 0.5);
                        if (dist <= itemRange) {
                            sw.collected = true;
                        }
                    } else if ("superboom".equals(sw.category) || "chest".equals(sw.category)) {
                        if (sw.everSeen && sw.ticksExisted >= WAYPOINT_REMOVAL_GRACE_TICKS && mc.theWorld.getBlockState(sw.pos).getBlock() == net.minecraft.init.Blocks.air) {
                            sw.collected = true;
                        }
                    }
                }
        }
    }

    @SubscribeEvent
    public void onActionBar(ActionBarUpdateEvent event) {
        if (mc.thePlayer == null || currentSecrets.isEmpty()) return;

        String cleanText = net.minecraft.util.StringUtils.stripControlCodes(event.getText());
        Matcher m = SECRETS_FOUND_PATTERN.matcher(cleanText);
        if (m.find()) {
            int found = Integer.parseInt(m.group(1));
            int total = Integer.parseInt(m.group(2));
            if (found == total) {
                for (SecretWaypoint sw : currentSecrets) {
                    sw.collected = true;
                }
            } else {
                double range = ATHRConfig.feature.dungeons.dungeonSecretFinder.range.itemRemovalRange;
                for (SecretWaypoint sw : currentSecrets) {
                    if (sw.collected || !"item".equals(sw.category)) continue;
                    double dist = mc.thePlayer.getDistance(sw.pos.getX() + 0.5, sw.pos.getY() + 0.5, sw.pos.getZ() + 0.5);
                    if (dist <= range) {
                        sw.collected = true;
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.world == null || event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) return;
        if (event.pos == null || currentSecrets.isEmpty()) return;

        BlockPos clickedPos = event.pos;

        double range = ATHRConfig.feature.dungeons.dungeonSecretFinder.range.interactRemovalRange;

        for (SecretWaypoint sw : currentSecrets) {
            if (sw.collected) continue;

            double dist = Math.sqrt(clickedPos.distanceSq(sw.pos));
            if (dist > range) continue;

            if ("chest".equals(sw.category)) {
                net.minecraft.block.Block b = event.world.getBlockState(clickedPos).getBlock();
                if (b == net.minecraft.init.Blocks.chest || b == net.minecraft.init.Blocks.trapped_chest || b == net.minecraft.init.Blocks.ender_chest) {
                    sw.collected = true;
                }
            } else if ("lever".equals(sw.category)) {
                if (event.world.getBlockState(clickedPos).getBlock() == net.minecraft.init.Blocks.lever) {
                    sw.collected = true;
                }
            }
        }
    }

    @SubscribeEvent
    public void onBlockBreak(BlockBreakEvent event) {
        if (mc.theWorld == null || currentSecrets.isEmpty()) return;

        for (SecretWaypoint sw : currentSecrets) {
            if (sw.collected || !"stonk".equals(sw.category)) continue;
            if (event.pos.equals(sw.pos)) {
                sw.collected = true;
            }
        }
    }

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (mc.theWorld == null || currentSecrets.isEmpty()) return;
        if (!(event.entity instanceof EntityTNTPrimed)) return;

        EntityTNTPrimed tnt = (EntityTNTPrimed) event.entity;

        for (SecretWaypoint sw : currentSecrets) {
            if (sw.collected || !"superboom".equals(sw.category)) continue;

            double range = ATHRConfig.feature.dungeons.dungeonSecretFinder.range.superboomRemovalRange;
            double dist = tnt.getDistance(sw.pos.getX() + 0.5, sw.pos.getY() + 0.5, sw.pos.getZ() + 0.5);
            if (dist <= range) {
                sw.collected = true;
            }
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (mc.theWorld == null || currentSecrets.isEmpty()) return;
        if (!(event.entity instanceof EntityBat)) return;
        if (!DungeonRoomDetector.roomBoundsValid) return;

        double ex = event.entity.posX;
        double ez = event.entity.posZ;

        if (ex >= DungeonRoomDetector.roomMinX && ex <= DungeonRoomDetector.roomMaxX
            && ez >= DungeonRoomDetector.roomMinZ && ez <= DungeonRoomDetector.roomMaxZ) {
            for (SecretWaypoint sw : currentSecrets) {
                if ("bat".equals(sw.category)) {
                    sw.collected = true;
                }
            }
        }
    }

}
