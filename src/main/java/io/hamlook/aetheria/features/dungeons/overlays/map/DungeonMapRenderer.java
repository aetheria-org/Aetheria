package io.hamlook.aetheria.features.dungeons.overlays.map;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.dungeons.DungeonMapConfig;
import io.hamlook.aetheria.features.dungeons.rooms.DungeonRoom;
import io.hamlook.aetheria.utils.compat.DrawContextUtils;
import io.hamlook.aetheria.utils.compat.EntityCompat;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.TextCompat;
import io.hamlook.aetheria.utils.render.RenderUtils;
import io.hamlook.aetheria.utils.render.RectBatch;
import net.minecraft.client.Minecraft;
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

    private static long lastRegionLogMs = 0;

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

        drawRoomGeometry(grid, roomSize, connSize);

        //#if MC < 1.16
        //#else
        //$$ DrawContextUtils.flushIfPresent();
        //#endif

        int checkmarkStyle = cfg.rooms.mapCheckmark;
        int checkmarkSize = checkmarkStyle == 1 ? 10 : 8;

        if (ATHRConfig.feature.debug.dungeonRoomDebug && System.currentTimeMillis() - lastRegionLogMs > 5000) {
            lastRegionLogMs = System.currentTimeMillis();
            Map<String, Integer> stateCounts = new HashMap<>();
            int mismatched = 0;
            for (DungeonMapGrid.RoomRegion region : grid.getRegions()) {
                String key = region.state == null ? "null" : region.state.name();
                stateCounts.merge(key, 1, Integer::sum);
            }
            if (visitedRooms != null) {
                for (DungeonRoom room : visitedRooms) {
                    DungeonMapGrid.RoomRegion region = findRoomRegion(grid, room);
                    DungeonMapGrid.RoomState viaRegion = region != null ? region.state : null;
                    DungeonMapGrid.RoomState viaWorld = grid.stateAtWorld(room.center.getX(), room.center.getZ());
                    if (!Objects.equals(viaRegion, viaWorld)) mismatched++;
                }
            }
            Aetheria.logger.info("[ATHR] Map regions: total=" + grid.getRegions().size()
                + " states=" + stateCounts
                + " mismatched=" + mismatched
                + " split=" + cfg.rooms.splitRoomMarkers + " checkmark=" + cfg.rooms.mapCheckmark);
        }

        drawCheckmarks(grid, roomSize, checkmarkSize, checkmarkStyle, cfg.rooms.splitRoomMarkers);

        if (showVisitedRoomNames && visitedRooms != null) {
            float invScale = 1f / Math.max(scale, 0.01f);
            boolean split = cfg.rooms.splitRoomMarkers;
            float roomHalf = roomSize / 2f;
            Set<DungeonMapGrid.RoomRegion> renderedRegions = new HashSet<>();
            for (DungeonRoom room : visitedRooms) {
                float px = grid.worldToPixelX(room.center.getX());
                float py = grid.worldToPixelZ(room.center.getZ());
                DungeonMapGrid.RoomRegion region = split ? findRoomRegion(grid, room) : null;
                if (region != null && !renderedRegions.add(region)) continue;
                DungeonMapGrid.RoomState state = grid.stateForRoom(region, room.center.getX(), room.center.getZ());
                if (region != null) {
                    if (region.hasRowAnchor) {
                        px = grid.gridToPixelX((region.nameRowMinX + region.nameRowMaxX) / 2f) + roomHalf;
                        py = grid.gridToPixelZ(region.nameRowY) + roomHalf;
                    } else {
                        px = grid.gridToPixelX((region.minX + region.maxX) / 2f) + roomHalf;
                        py = grid.gridToPixelZ((region.minY + region.maxY) / 2f) + roomHalf;
                    }
                }
                int color = colorText ? labelColor(state) : 0xFFFFFFFF;
                RenderUtils.renderRoomName(px, py, cfg.rooms.roomnameSize * NAME_TEXT_SCALE * invScale, room.alias, color);
            }
        }

        if (!cfg.players.showPlayerHead || tracker == null) {
            GlStateManagerCompat.popMatrix();
            return;
        }

        // Player Markers & Names (positions resolved by tracker.getMarker).
        float invScale = 1f / Math.max(scale, 0.01f);
        DungeonMapConfig.Self selfCfg = cfg.players.self;
        DungeonMapConfig.Teammates teammatesCfg = cfg.players.teammates;
        String selfName = MinecraftCompat.getLocalPlayer() != null ? EntityCompat.getName(MinecraftCompat.getLocalPlayer()) : null;
        int teammateOrdinal = 0;

        for (String name : playerNames) {
            DungeonPlayerTracker.MarkerState marker = tracker.getMarker(name, selfName, grid, mapCalibrated, cfg.players.accurateSelfPosition);
            if (marker == null) {
                continue;
            }
            EntityPlayer entity = marker.entity;
            float px = marker.px;
            float pz = marker.pz;
            float yaw = marker.yaw;
            boolean isSelf = marker.isSelf;

            NetworkPlayerInfo info = tracker.getNetworkPlayerInfo(name, mc);

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
        for (int i = idx - 1; i >= 0; i--) {
            if (displayName.charAt(i) == '§' && i + 1 < displayName.length()) {
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
            return TextCompat.codedString(info.getDisplayName());
        }
        if (entity != null && entity.getDisplayName() != null) {
            return TextCompat.codedString(entity.getDisplayName());
        }
        return name;
    }

    private static DungeonMapGrid.RoomRegion findRoomRegion(DungeonMapGrid grid, DungeonRoom room) {
        return grid.regionForRoom(room.center.getX(), room.center.getZ());
    }

    private static void drawRoomGeometry(DungeonMapGrid grid, int roomSize, int connSize) {
        RectBatch.begin();
        try {
            for (Map.Entry<DungeonMapGrid.RoomOffset, DungeonMapGrid.RoomCell> entry : grid.getRooms().entrySet()) {
                int rx = (int) grid.gridToPixelX(entry.getKey().x);
                int ry = (int) grid.gridToPixelZ(entry.getKey().y);
                DungeonMapGrid.RoomCell cell = entry.getValue();

                RectBatch.rect(rx, ry, rx + roomSize, ry + roomSize, cell.color | 0xFF000000);
                drawConnector(cell.down, true, rx, ry + roomSize, roomSize, connSize);
                drawConnector(cell.right, false, rx + roomSize, ry, roomSize, connSize);
            }
            for (DungeonMapGrid.Junction junction : grid.getJunctions()) {
                RectBatch.rect(junction.px, junction.py, junction.px + connSize, junction.py + connSize, junction.color | 0xFF000000);
            }
        } finally {
            RectBatch.end();
        }
    }

    private static void drawConnector(DungeonMapGrid.RoomConnection connection, boolean vertical,
            int x, int y, int roomSize, int connSize) {
        int argb = connection.color | 0xFF000000;

        if (connection.type == DungeonMapGrid.ConnectionType.ROOM_DIVIDER) {
            if (vertical) RectBatch.rect(x, y, x + roomSize, y + connSize, argb);
            else RectBatch.rect(x, y, x + connSize, y + roomSize, argb);
            return;
        }
        if (connection.type != DungeonMapGrid.ConnectionType.CORRIDOR) return;

        int doorOffset = Math.max(0, (roomSize - 6) / 2);
        if (vertical) RectBatch.rect(x + doorOffset, y, x + doorOffset + 6, y + connSize, argb);
        else RectBatch.rect(x, y + doorOffset, x + connSize, y + doorOffset + 6, argb);
    }

    private static void drawCheckmarks(DungeonMapGrid grid, int roomSize, int checkmarkSize, int style, boolean split) {
        if (split) {
            for (DungeonMapGrid.RoomRegion region : grid.getRegions()) {
                drawCheckmark(grid, region.tickCell.x, region.tickCell.y, region.state, roomSize, checkmarkSize, style);
            }
            return;
        }
        for (Map.Entry<DungeonMapGrid.RoomOffset, DungeonMapGrid.RoomCell> entry : grid.getRooms().entrySet()) {
            drawCheckmark(grid, entry.getKey().x, entry.getKey().y, entry.getValue().state, roomSize, checkmarkSize, style);
        }
    }

    private static void drawCheckmark(DungeonMapGrid grid, int gridX, int gridZ, DungeonMapGrid.RoomState state,
            int roomSize, int checkmarkSize, int style) {
        ResourceLocation icon = getCheckmark(state, style);
        if (icon == null) return;

        float offset = (roomSize - checkmarkSize) / 2f;
        RenderUtils.renderMapCheckmark(icon, grid.gridToPixelX(gridX) + offset, grid.gridToPixelZ(gridZ) + offset, checkmarkSize);
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