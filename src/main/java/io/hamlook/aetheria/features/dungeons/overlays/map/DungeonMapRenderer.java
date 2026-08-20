package io.hamlook.aetheria.features.dungeons.overlays.map;

import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.dungeons.DungeonMapConfig;
import io.hamlook.aetheria.features.dungeons.overlays.DungeonMapOverlay;
import io.hamlook.aetheria.features.dungeons.rooms.DungeonRoom;
import io.hamlook.aetheria.utils.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DungeonMapRenderer {

    private static final float NAME_TEXT_SCALE = 0.75f;
    private static final DungeonMapGrid.RoomState[] ICON_STATES = {
            DungeonMapGrid.RoomState.GREEN,
            DungeonMapGrid.RoomState.CLEARED,
            DungeonMapGrid.RoomState.FAILED,
            DungeonMapGrid.RoomState.UNOPENED
    };

    public static void render(DungeonMapGrid grid, float centerX, float centerY, float scale, List<String> playerNames, DungeonPlayerTracker tracker, Collection<DungeonRoom> visitedRooms, boolean showVisitedRoomNames, boolean colorText) {
        if (!grid.isValid()) return;

        DungeonMapConfig cfg = ATHRConfig.feature.dungeons.dungeonMapConfig;
        int roomSize = grid.getRoomPixelSize();
        int connSize = grid.getConnectorPixelSize();
        int gridW = grid.getGridPixelWidth();
        int gridH = grid.getGridPixelHeight();

        Minecraft mc = Minecraft.getMinecraft();

        GlStateManager.pushMatrix();
        GlStateManager.translate(centerX - gridW * scale / 2f, centerY - gridH * scale / 2f, 0f);
        GlStateManager.scale(scale, scale, 1f);

        // 1. Render Rooms and Connectors
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

        // 2. Render State Checkmarks (texture-batched by state, ≤4 binds/frame)
        int checkmarkStyle = cfg.rooms.mapCheckmark;
        int checkmarkSize = checkmarkStyle == 1 ? 8 : 10;
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        if (checkmarkStyle != 0) {
            boolean split = cfg.rooms.splitRoomMarkers;
            for (DungeonMapGrid.RoomState state : ICON_STATES) {
                ResourceLocation icon = getCheckmark(state, checkmarkStyle);
                if (icon == null) continue;
                mc.getTextureManager().bindTexture(icon);
                GlStateManager.color(1f, 1f, 1f, 1f);
                if (split) {
                    for (DungeonMapGrid.RoomRegion region : grid.getRegions()) {
                        if (region.state != state) continue;
                        int rx = (int) grid.gridToPixelX(region.tickCell.x);
                        int ry = (int) grid.gridToPixelZ(region.tickCell.y);
                        Utils.drawTexturedRect(rx + (roomSize - checkmarkSize) / 2f, ry + (roomSize - checkmarkSize) / 2f, checkmarkSize, checkmarkSize, GL11.GL_NEAREST);
                    }
                } else {
                    for (Map.Entry<DungeonMapGrid.RoomOffset, DungeonMapGrid.RoomCell> entry : grid.getRooms().entrySet()) {
                        DungeonMapGrid.RoomCell cell = entry.getValue();
                        if (cell.state != state) continue;
                        int rx = (int) grid.gridToPixelX(entry.getKey().x);
                        int ry = (int) grid.gridToPixelZ(entry.getKey().y);
                        Utils.drawTexturedRect(rx + (roomSize - checkmarkSize) / 2f, ry + (roomSize - checkmarkSize) / 2f, checkmarkSize, checkmarkSize, GL11.GL_NEAREST);
                    }
                }
            }
        }

        // 3. Visited Room Names (centered, fixed screen scale)
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
                DungeonMapOverlay.renderRoomName(px, py, cfg.rooms.roomnameSize * NAME_TEXT_SCALE * invScale, room.alias, color);
            }
        }

        if (!cfg.players.showPlayerHead) {
            GlStateManager.popMatrix();
            return;
        }

        // 4. Player Heads & Names (constant screen sizes — decoupled from map scale)
        float invScale = 1f / Math.max(scale, 0.01f);
        float headScale = cfg.players.headScale * 1.25f * invScale;
        float headPixelSize = 8f * headScale;

        for (String name : playerNames) {
            if (tracker == null) continue;
            EntityPlayer entity = tracker.getEntity(name);
            float[] pos = tracker.getPosition(name);
            if (pos == null) {
                continue;
            }
            float px = pos[0];
            float pz = pos[1];
            float yaw = pos[2];

            if (entity != null && !entity.isDead) {
                yaw = entity.rotationYaw;
            }

            NetworkPlayerInfo info = null;
            if (entity != null) {
                info = mc.getNetHandler().getPlayerInfo(entity.getUniqueID());
            }
            if (info == null) {
                info = tracker.getNetworkPlayerInfo(name, mc);
            }
            ResourceLocation skin = (info != null && info.getLocationSkin() != null)
                    ? info.getLocationSkin()
                    : DefaultPlayerSkin.getDefaultSkinLegacy();

            DungeonMapOverlay.renderPlayerHead(px - headPixelSize / 2f, pz - headPixelSize / 2f, -1, headScale, skin, yaw);

            if (cfg.players.showPlayerUsername) {
                String displayName = getDisplayName(name, info, entity);
                if (!cfg.players.showPlayerRank) {
                    int idx = displayName.lastIndexOf("]");
                    if (idx >= 0) displayName = displayName.substring(idx + 1).trim();
                }

                DungeonMapOverlay.renderName(px - headPixelSize / 2f, pz - headPixelSize / 2f, -1, headScale, cfg.players.nameSize * NAME_TEXT_SCALE * invScale, displayName, false);
            }
        }

        GlStateManager.popMatrix();
    }

    private static String getDisplayName(String name, NetworkPlayerInfo info, EntityPlayer entity) {
        if (info != null && info.getDisplayName() != null) {
            return info.getDisplayName().getFormattedText();
        }
        if (entity != null && entity.getDisplayName() != null) {
            return entity.getDisplayName().getFormattedText();
        }
        return name;
    }

    private static DungeonMapGrid.RoomRegion findRoomRegion(DungeonMapGrid grid, DungeonRoom room) {
        return grid.regionAtPixel(grid.worldToPixelX(room.center.getX()), grid.worldToPixelZ(room.center.getZ()));
    }

    private static ResourceLocation getCheckmark(DungeonMapGrid.RoomState state, int style) {
        switch (state) {
            case GREEN:
                return style == 2 ? Resources.DUNGEON_MAP_CHECK_NEU_GREEN : Resources.DUNGEON_MAP_CHECK_GREEN;
            case CLEARED:
                return style == 2 ? Resources.DUNGEON_MAP_CHECK_NEU_WHITE : Resources.DUNGEON_MAP_CHECK_WHITE;
            case FAILED:
                return style == 2 ? Resources.DUNGEON_MAP_CHECK_NEU_CROSS : Resources.DUNGEON_MAP_CHECK_CROSS;
            case UNOPENED:
                return style == 2 ? Resources.DUNGEON_MAP_CHECK_NEU_QUESTION : Resources.DUNGEON_MAP_CHECK_QUESTION;
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