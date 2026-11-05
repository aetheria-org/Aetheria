package io.hamlook.aetheria.features.dungeons.rooms.secret;

import io.hamlook.aetheria.core.features.dungeons.DungeonSecretFinderConfig;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import io.hamlook.aetheria.features.dungeons.DungeonStats;
import io.hamlook.aetheria.features.dungeons.rooms.DungeonRoomDetector;
import io.hamlook.aetheria.features.waypoints.WaypointRenderer;
import io.hamlook.aetheria.utils.compat.EntityCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.render.FrustumUtils;
import io.hamlook.aetheria.utils.render.WorldRenderUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SecretRenderer {

    private static final double BEAM_FALLBACK_HEIGHT = 100.0;
    private static final double MIN_RENDER_DISTANCE_SQ = 25.0;

    private SecretRenderer() {
    }

    public static void render(float partialTicks) {
        if (DungeonStats.isInBossFight()) return;

        DungeonSecretFinderConfig config = DungeonSecretFinderConfig.get();
        if (config == null) return;

        SecretHighlightRenderer.draw(partialTicks);

        if (!config.enabled) return;
        if (SecretTracker.allSecretsCleared()) return;

        Frame frame = Frame.capture(config);
        if (frame == null) return;

        if (config.waypoints.showLabels) drawLabels(frame);
        if (config.waypoints.showBoxOutline) drawOutlines(frame);
        if (config.waypoints.showBoxFill) drawFills(frame);
        if (config.waypoints.showWaypoints) drawBeams(frame, partialTicks);
        if (config.waypoints.showTracer || config.waypoints.target.targetEsp) {
            SecretWaypoint target = frame.target();
            if (config.waypoints.showTracer) drawTracer(frame, target, partialTicks);
            if (config.waypoints.target.targetEsp) drawTargetEsp(frame, target);
        }
    }

    private static void drawLabels(Frame frame) {
        DungeonSecretFinderConfig config = frame.config();
        int nameColor = ChromaColour.specialToChromaRGB(config.colors.labelColor);
        int distanceColor = ChromaColour.specialToChromaRGB(config.colors.tracerColor);
        for (SecretWaypoint secret : frame.visible()) {
            WaypointRenderer.drawLabel(
                    secret.pos().getX() + 0.5, secret.pos().getY() + 2.0, secret.pos().getZ() + 0.5,
                    displayName(secret, config), nameColor, distanceColor, config.waypoints.labelScale);
        }
    }

    private static void drawOutlines(Frame frame) {
        if (frame.outlineBoxes().isEmpty()) return;
        DungeonSecretFinderConfig config = frame.config();
        WorldRenderUtils.drawEspBoxes(frame.outlineBoxes(), color(config.colors.outlineColor),
                config.waypoints.showThroughWalls, config.waypoints.tracerWidth);
    }

    private static void drawFills(Frame frame) {
        if (frame.boxes().isEmpty()) return;
        DungeonSecretFinderConfig config = frame.config();
        WorldRenderUtils.drawFilledBlocks(frame.boxes(), color(config.colors.fillColor), false,
                config.waypoints.showThroughWalls);
    }

    private static void drawBeams(Frame frame, float partialTicks) {
        WorldRenderUtils.drawBeaconBeams(frame.beams(), color(frame.config().colors.waypointColor), partialTicks);
    }

    private static void drawTracer(Frame frame, SecretWaypoint target, float partialTicks) {
        if (target == null) return;

        DungeonSecretFinderConfig config = frame.config();
        WorldRenderUtils.drawTracer(
                new Vec3(target.pos().getX() + 0.5, target.pos().getY() + 0.5, target.pos().getZ() + 0.5),
                partialTicks, color(config.colors.tracerColor), config.waypoints.tracerWidth);
    }

    private static void drawTargetEsp(Frame frame, SecretWaypoint target) {
        if (target == null) return;

        DungeonSecretFinderConfig config = frame.config();
        WorldRenderUtils.drawEspBoxes(Collections.singletonList(target.box()),
                color(config.waypoints.target.targetColor),
                config.waypoints.target.throughWalls, config.waypoints.target.targetWidth);
    }

    private static String displayName(SecretWaypoint secret, DungeonSecretFinderConfig config) {
        if (!config.waypoints.compactLabels) return secret.name();
        int separator = secret.name().indexOf(" - ");
        return separator < 0 ? secret.name() : secret.name().substring(0, separator);
    }

    // beams stop at the room ceiling so they read as one room's worth of height
    private static double beamTop(SecretWaypoint secret) {
        int ceiling = DungeonRoomDetector.roomCeilingY;
        if (ceiling <= 0 || ceiling <= secret.pos().getY()) return BEAM_FALLBACK_HEIGHT;
        return ceiling - secret.pos().getY() + 1;
    }

    private static Color color(String configColor) {
        return ChromaColour.specialToColor(configColor);
    }

    private static final class Frame {

        private final DungeonSecretFinderConfig config;
        private final List<SecretWaypoint> visible;
        private final List<AxisAlignedBB> boxes;
        private final List<AxisAlignedBB> outlineBoxes;
        private final SecretWaypoint target;
        private final double playerX;
        private final double playerY;
        private final double playerZ;

        private Frame(DungeonSecretFinderConfig config, List<SecretWaypoint> visible,
                      List<AxisAlignedBB> boxes, List<AxisAlignedBB> outlineBoxes, SecretWaypoint target,
                      double playerX, double playerY, double playerZ) {
            this.config = config;
            this.visible = visible;
            this.boxes = boxes;
            this.outlineBoxes = outlineBoxes;
            this.target = target;
            this.playerX = playerX;
            this.playerY = playerY;
            this.playerZ = playerZ;
        }

        private static Frame capture(DungeonSecretFinderConfig config) {
            EntityPlayer player = MinecraftCompat.getLocalPlayer();
            if (player == null) return null;

            double playerX = EntityCompat.getPosX(player);
            double playerY = EntityCompat.getPosY(player);
            double playerZ = EntityCompat.getPosZ(player);

            List<SecretWaypoint> secrets = new ArrayList<>();
            for (SecretWaypoint secret : SecretTracker.secrets()) {
                if (!secret.isHidden()) secrets.add(secret);
            }
            if (secrets.isEmpty()) return null;

            SecretWaypoint target = selectTarget(config, secrets, playerX, playerY, playerZ);
            boolean targetBoxSeparate = config.waypoints.target.targetEsp;

            List<SecretWaypoint> visible = new ArrayList<>();
            List<AxisAlignedBB> boxes = new ArrayList<>();
            List<AxisAlignedBB> outlineBoxes = new ArrayList<>();
            for (SecretWaypoint secret : secrets) {
                BlockPos pos = secret.pos();
                if (!FrustumUtils.isVisible(pos.getX(), pos.getY(), pos.getZ(),
                        pos.getX() + 1, pos.getY() + beamTop(secret), pos.getZ() + 1)) {
                    continue;
                }
                AxisAlignedBB box = secret.box();
                visible.add(secret);
                boxes.add(box);
                if (!targetBoxSeparate || secret != target) outlineBoxes.add(box);
            }

            return new Frame(config, visible, boxes, outlineBoxes, target, playerX, playerY, playerZ);
        }

        private static SecretWaypoint selectTarget(DungeonSecretFinderConfig config, List<SecretWaypoint> secrets,
                                                   double playerX, double playerY, double playerZ) {
            if (config.waypoints.tracerTarget == 1) return secrets.get(0);

            SecretWaypoint nearest = null;
            double nearestSq = Double.MAX_VALUE;
            for (SecretWaypoint secret : secrets) {
                double distance = distanceSq(secret, playerX, playerY, playerZ);
                if (distance >= nearestSq) continue;
                nearest = secret;
                nearestSq = distance;
            }
            return nearest;
        }

        private static double distanceSq(SecretWaypoint secret, double playerX, double playerY, double playerZ) {
            BlockPos pos = secret.pos();
            double dx = pos.getX() + 0.5 - playerX;
            double dy = pos.getY() + 0.5 - playerY;
            double dz = pos.getZ() + 0.5 - playerZ;
            return dx * dx + dy * dy + dz * dz;
        }

        private DungeonSecretFinderConfig config() {
            return config;
        }

        private List<SecretWaypoint> visible() {
            return visible;
        }

        private List<AxisAlignedBB> boxes() {
            return boxes;
        }

        private List<AxisAlignedBB> outlineBoxes() {
            return outlineBoxes;
        }

        private List<WorldRenderUtils.Beam> beams() {
            List<WorldRenderUtils.Beam> beams = new ArrayList<>();
            for (SecretWaypoint secret : visible) {
                if (distanceSq(secret) <= MIN_RENDER_DISTANCE_SQ) continue;
                BlockPos pos = secret.pos();
                beams.add(new WorldRenderUtils.Beam(
                        pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, beamTop(secret)));
            }
            return beams;
        }

        private double distanceSq(SecretWaypoint secret) {
            return distanceSq(secret, playerX, playerY, playerZ);
        }

        private SecretWaypoint target() {
            return target;
        }
    }
}
