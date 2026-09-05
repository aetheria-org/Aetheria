package io.hamlook.aetheria.features.dungeons.overlays.map;

import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.dungeons.DungeonMapConfig;
import io.hamlook.aetheria.features.dungeons.rooms.DungeonRoom;
import io.hamlook.aetheria.utils.compat.EntityCompat;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.TextCompat;
import io.hamlook.aetheria.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import java.util.*;

public class DungeonMapRenderer {

    private static final float NAME_TEXT_SCALE = 0.75f;
    private static final int SELF_ARROW_COLOR = 0xFFFFFFFF;
    private static final int[] TEAMMATE_ARROW_COLORS = {0xFF5AA5FF, // blue
            0xFFFFE45A, // yellow
            0xFFFFA135, // orange
            0xFFFF5A5A  // red
    };

    private static final DungeonMapGrid.RoomState[] ICON_STATES = {DungeonMapGrid.RoomState.GREEN, DungeonMapGrid.RoomState.CLEARED, DungeonMapGrid.RoomState.FAILED, DungeonMapGrid.RoomState.UNOPENED};

    public static void render(DungeonMapGrid grid, float centerX, float centerY, float scale, List<String> playerNames, DungeonPlayerTracker tracker, Collection<DungeonRoom> visitedRooms, boolean showVisitedRoomNames, boolean colorText, boolean mapCalibrated) {
        if (!grid.isValid()) return;

        DungeonMapConfig cfg = ATHRConfig.feature.dungeons.dungeonMapConfig;
        int roomSize = grid.getRoomPixelSize();
        int connSize = grid.getConnectorPixelSize();
        int gridW = grid.getGridPixelWidth();
        int gridH = grid.getGridPixelHeight();

        Minecraft mc = MinecraftCompat.getMinecraft();

        GlStateManagerCompat.pushMatrix();
        GlStateManagerCompat.translate(centerX - gridW * scale / 2f, centerY - gridH * scale / 2f, 0f);
        GlStateManagerCompat.scale(scale, scale, 1f);

        for (Map.Entry<DungeonMapGrid.RoomOffset, DungeonMapGrid.RoomCell> entry : grid.getRooms().entrySet()) {
            DungeonMapGrid.RoomOffset off = entry.getKey();
            DungeonMapGrid.RoomCell cell = entry.getValue();
            int rx = (int) grid.gridToPixelX(off.x);
            int ry = (int) grid.gridToPixelZ(off.y);

            Gui.drawRect(rx, ry, rx + roomSize, ry + roomSize, cell.color | 0xFF000000);

            int doorOffset = Math.max(0, (roomSize - 6) / 2);

            if (cell.down.type == DungeonMapGrid.ConnectionType.ROOM_DIVIDER) {
                Gui.drawRect(rx, ry + roomSize, rx + roomSize, ry + roomSize + connSize, cell.down.color | 0xFF000000);
            } else if (cell.down.type == DungeonMapGrid.ConnectionType.CORRIDOR) {
                Gui.drawRect(rx + doorOffset, ry + roomSize, rx + doorOffset + 6, ry + roomSize + connSize, cell.down.color | 0xFF000000);
            }

            if (cell.right.type == DungeonMapGrid.ConnectionType.ROOM_DIVIDER) {
                Gui.drawRect(rx + roomSize, ry, rx + roomSize + connSize, ry + roomSize, cell.right.color | 0xFF000000);
            } else if (cell.right.type == DungeonMapGrid.ConnectionType.CORRIDOR) {
                Gui.drawRect(rx + roomSize, ry + doorOffset, rx + roomSize + connSize, ry + doorOffset + 6, cell.right.color | 0xFF000000);
            }
        }

        for (DungeonMapGrid.Junction j : grid.getJunctions()) {
            Gui.drawRect(j.px, j.py, j.px + connSize, j.py + connSize, j.color | 0xFF000000);
        }

        int checkmarkStyle = cfg.rooms.mapCheckmark;
        int checkmarkSize = checkmarkStyle == 0 ? 8 : 10;

        for (DungeonMapGrid.RoomState state : ICON_STATES) {
            ResourceLocation icon = getCheckmark(state, checkmarkStyle);
            if (icon == null) continue;
            if (cfg.rooms.splitRoomMarkers) {
                for (DungeonMapGrid.RoomRegion region : grid.getRegions()) {
                    if (region.state != state) continue;
                    int rx = (int) grid.gridToPixelX(region.tickCell.x);
                    int ry = (int) grid.gridToPixelZ(region.tickCell.y);
                    RenderUtils.renderMapCheckmark(icon, rx + (roomSize - checkmarkSize) / 2f, ry + (roomSize - checkmarkSize) / 2f, checkmarkSize);
                }
            } else {
                for (Map.Entry<DungeonMapGrid.RoomOffset, DungeonMapGrid.RoomCell> entry : grid.getRooms().entrySet()) {
                    DungeonMapGrid.RoomCell cell = entry.getValue();
                    if (cell.state != state) continue;
                    int rx = (int) grid.gridToPixelX(entry.getKey().x);
                    int ry = (int) grid.gridToPixelZ(entry.getKey().y);
                    RenderUtils.renderMapCheckmark(icon, rx + (roomSize - checkmarkSize) / 2f, ry + (roomSize - checkmarkSize) / 2f, checkmarkSize);
                }
            }
        }

        if (showVisitedRoomNames && visitedRooms != null) {
            float invScale = 1f / Math.max(scale, 0.01f);
            boolean split = cfg.rooms.splitRoomMarkers;
            float roomHalf = roomSize / 2f;
            Set<DungeonMapGrid.RoomRegion> renderedRegions = new HashSet<>();
            for (DungeonRoom room : visitedRooms) {
                float px = grid.worldToPixelX(room.center.getX());
                float py = grid.worldToPixelZ(room.center.getZ());
                DungeonMapGrid.RoomState state;
                if (split) {
                    DungeonMapGrid.RoomRegion region = findRoomRegion(grid, room);
                    if (region != null) {
                        if (!renderedRegions.add(region)) continue;
                        state = region.state;
                        if (region.hasRowAnchor) {
                            px = grid.gridToPixelX((region.nameRowMinX + region.nameRowMaxX) / 2f) + roomHalf;
                            py = grid.gridToPixelZ(region.nameRowY) + roomHalf;
                        } else {
                            px = grid.gridToPixelX((region.minX + region.maxX) / 2f) + roomHalf;
                            py = grid.gridToPixelZ((region.minY + region.maxY) / 2f) + roomHalf;
                        }
                    } else {
                        state = grid.stateAtWorld(room.center.getX(), room.center.getZ());
                    }
                } else {
                    state = grid.stateAtWorld(room.center.getX(), room.center.getZ());
                }
                int color = colorText ? labelColor(state) : 0xFFFFFFFF;
                RenderUtils.renderRoomName(px, py, cfg.rooms.roomnameSize * NAME_TEXT_SCALE * invScale, room.alias, color);
            }
        }

        if (!cfg.players.showPlayerHead || tracker == null) {
            GlStateManagerCompat.popMatrix();
            return;
        }

        // Player Markers & Names
        // Positions come from each player's map decoration (tracker.getPosition) so
        // out-of-render-distancestill show. Self can optionally be
        // overridden with the real entity position (accurateSelfPosition) for smoother
        // movement; self stays anchored to its own decoration index.
        float invScale = 1f / Math.max(scale, 0.01f);
        DungeonMapConfig.Self selfCfg = cfg.players.self;
        DungeonMapConfig.Teammates teammatesCfg = cfg.players.teammates;
        String selfName = MinecraftCompat.getLocalPlayer() != null ? MinecraftCompat.getLocalPlayer().getName() : null;
        int teammateOrdinal = 0;

        for (String name : playerNames) {
            EntityPlayer entity = tracker.getEntity(name);
            float[] pos = tracker.getPosition(name);
            if (pos == null) {
                continue;
            }
            float px = pos[0];
            float pz = pos[1];
            float yaw = pos[2];

            NetworkPlayerInfo info = tracker.getNetworkPlayerInfo(name, mc);

            boolean isSelf = name.equalsIgnoreCase(selfName);
            // Entity-based self tracking requires a calibrated map<->world frame:
            // before the entrance latch the fallback origin would pin the marker
            // to the entrance pixel, so uncalibrated runs draw self from its
            // decoration (same as teammates) until calibration completes.
            if (isSelf && mapCalibrated && cfg.players.accurateSelfPosition && entity != null && !EntityCompat.isDead(entity)) {
                px = grid.worldToPixelX(entity.posX);
                pz = grid.worldToPixelZ(entity.posZ);
                // Same +180 convention as the decoration decode (vanilla parity).
                yaw = entity.rotationYaw + 180f;
            }

            int style = isSelf ? selfCfg.iconStyle : teammatesCfg.iconStyle;
            float groupScale = isSelf ? selfCfg.markerScale : teammatesCfg.markerScale;
            float headScale = groupScale * 1.25f * invScale;
            float markerSize = 8f * headScale;

            if (style == 1) {
                int color = isSelf ? SELF_ARROW_COLOR : TEAMMATE_ARROW_COLORS[teammateOrdinal % TEAMMATE_ARROW_COLORS.length];
                if (!isSelf) teammateOrdinal++;
                RenderUtils.renderPlayerArrow(px - markerSize / 2f, pz - markerSize / 2f, headScale, yaw, color, isSelf);
            } else if (style == 2) {
                boolean flowChroma = isSelf ? selfCfg.frameFlowChroma : teammatesCfg.frameFlowChroma;
                String frameColorStr = isSelf ? selfCfg.frameColor : teammatesCfg.frameColor;
                RenderUtils.renderFramedHead(px, pz, yaw, markerSize, frameColorStr, tracker.resolveSkin(name, mc), flowChroma);
            } else {
                RenderUtils.renderPlayerHead(px - markerSize / 2f, pz - markerSize / 2f, -1, headScale, tracker.resolveSkin(name, mc), yaw);
            }

            if (cfg.players.showPlayerUsername) {
                String displayName = getDisplayName(name, info, entity);
                if (!cfg.players.showPlayerRank) {
                    displayName = stripRankKeepColor(displayName);
                }

                RenderUtils.renderPlayerName(px - markerSize / 2f, pz - markerSize / 2f, -1, headScale, cfg.players.nameSize * NAME_TEXT_SCALE * invScale, displayName, false);
            }
        }

        GlStateManagerCompat.popMatrix();
    }

    /**
     * Strips the rank prefix (up to the last ']') while preserving the rank's
     * color: if the name segment carries no color code of its own, the last
     * color code found inside the prefix is applied to the name.
     */
    private static String stripRankKeepColor(String displayName) {
        int idx = displayName.lastIndexOf(']');
        if (idx < 0) return displayName;
        String namePart = displayName.substring(idx + 1).trim();
        if (namePart.startsWith("§")) return namePart;
        String color = "";
        for (int i = idx - 1; i >= 1; i--) {
            if (displayName.charAt(i) == '§') {
                char c = Character.toLowerCase(displayName.charAt(i + 1));
                if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')) {
                    color = "§" + c;
                    break;
                }
            }
        }
        return color.isEmpty() ? namePart : color + namePart;
    }

    private static String getDisplayName(String name, NetworkPlayerInfo info, EntityPlayer entity) {
        if (info != null && info.getDisplayName() != null) {
            return TextCompat.getFormattedText(info.getDisplayName());
        }
        if (entity != null && entity.getDisplayName() != null) {
            return TextCompat.getFormattedText(entity.getDisplayName());
        }
        return name;
    }

    private static DungeonMapGrid.RoomRegion findRoomRegion(DungeonMapGrid grid, DungeonRoom room) {
        return grid.regionAtPixel(grid.worldToPixelX(room.center.getX()), grid.worldToPixelZ(room.center.getZ()));
    }

    private static ResourceLocation getCheckmark(DungeonMapGrid.RoomState state, int style) {
        switch (state) {
            case GREEN:
                return style == 1 ? Resources.DUNGEON_MAP_CHECK_NEU_GREEN : Resources.DUNGEON_MAP_CHECK_GREEN;
            case CLEARED:
                return style == 1 ? Resources.DUNGEON_MAP_CHECK_NEU_WHITE : Resources.DUNGEON_MAP_CHECK_WHITE;
            case FAILED:
                return style == 1 ? Resources.DUNGEON_MAP_CHECK_NEU_CROSS : Resources.DUNGEON_MAP_CHECK_CROSS;
            case UNOPENED:
                return style == 1 ? Resources.DUNGEON_MAP_CHECK_NEU_QUESTION : Resources.DUNGEON_MAP_CHECK_QUESTION;
            default:
                return null;
        }
    }

    private static int labelColor(DungeonMapGrid.RoomState state) {
        switch (state) {
            case GREEN:
                return 0xFF55FF55;
            case CLEARED:
                return 0xFFFFFFFF;
            case FAILED:
                return 0xFFFF0000;
            case UNOPENED:
                return 0xFF555555;
            default:
                return 0xFFAAAAAA;
        }
    }
}