package io.hamlook.aetheria.features.dungeons.overlays.map;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.dungeons.overlays.DungeonMapOverlay;
import io.hamlook.aetheria.features.dungeons.rooms.DungeonRoom;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.IChatComponent;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class DungeonMapRenderer {

    public static void render(DungeonMapGrid grid, float centerX, float centerY, float scale, EntityPlayerSP self, List<String> playerNames, DungeonPlayerTracker tracker, Collection<DungeonRoom> visitedRooms) {
        if (!grid.isValid()) return;

        int roomSize = grid.getRoomPixelSize();
        int connSize = grid.getConnectorPixelSize();
        int gridW = grid.getGridPixelWidth();
        int gridH = grid.getGridPixelHeight();

        GlStateManager.pushMatrix();
        GlStateManager.translate(centerX - gridW * scale / 2f, centerY - gridH * scale / 2f, 0f);
        GlStateManager.scale(scale, scale, 1f);

        Gui.drawRect(-2, -2, gridW + 2, gridH + 2, 0x80000000);

        for (Map.Entry<DungeonMapGrid.RoomOffset, DungeonMapGrid.RoomCell> entry : grid.getRooms().entrySet()) {
            DungeonMapGrid.RoomOffset off = entry.getKey();
            DungeonMapGrid.RoomCell cell = entry.getValue();
            int rx = (int) grid.gridToPixelX(off.x);
            int ry = (int) grid.gridToPixelZ(off.y);

            Gui.drawRect(rx, ry, rx + roomSize, ry + roomSize, cell.color | 0xFF000000);

            if (cell.down.type != DungeonMapGrid.ConnectionType.NONE && cell.down.type != DungeonMapGrid.ConnectionType.WALL) {
                int cy = ry + roomSize;
                int cw = cell.down.type == DungeonMapGrid.ConnectionType.CORRIDOR ? Math.min(8, roomSize) : roomSize;
                int cxo = cell.down.type == DungeonMapGrid.ConnectionType.CORRIDOR ? (roomSize - cw) / 2 : 0;
                Gui.drawRect(rx + cxo, cy, rx + cxo + cw, cy + connSize, cell.down.color | 0xFF000000);
            }

            if (cell.right.type != DungeonMapGrid.ConnectionType.NONE && cell.right.type != DungeonMapGrid.ConnectionType.WALL) {
                int cx = rx + roomSize;
                int ch = cell.right.type == DungeonMapGrid.ConnectionType.CORRIDOR ? Math.min(8, roomSize) : roomSize;
                int cyo = cell.right.type == DungeonMapGrid.ConnectionType.CORRIDOR ? (roomSize - ch) / 2 : 0;
                Gui.drawRect(cx, ry + cyo, cx + connSize, ry + cyo + ch, cell.right.color | 0xFF000000);
            }
        }

        if (ATHRConfig.feature.dungeons.dungeonMapConfig.showVisitedRoomNames && visitedRooms != null) {
            for (DungeonRoom room : visitedRooms) {
                float px = DungeonMapGrid.worldToPixelX(room.center.getX()) + roomSize / 2f;
                float py = DungeonMapGrid.worldToPixelZ(room.center.getZ()) + roomSize / 2f;

                DungeonMapOverlay.renderName(px, py, -1, 0f, ATHRConfig.feature.dungeons.dungeonMapConfig.roomnameSize * 0.75f, room.name, true);
            }
        }

        if (!ATHRConfig.feature.dungeons.dungeonMapConfig.showPlayerHead) {
            GlStateManager.popMatrix();
            return;
        }

        float headScale = ATHRConfig.feature.dungeons.dungeonMapConfig.headScale * 1.25f;
        float headPixelSize = 8f * headScale;
        Minecraft mc = Minecraft.getMinecraft();
        String selfName = self != null ? self.getName() : null;

        for (String name : playerNames) {
            float px, pz, yaw;

            EntityPlayer entity = tracker != null ? tracker.getEntity(name) : null;

            // Determine position: self always uses entity, others use decoration + entity
            if (name.equals(selfName)) {
                px = DungeonMapGrid.worldToPixelX(self.posX);
                pz = DungeonMapGrid.worldToPixelZ(self.posZ);
                yaw = self.rotationYaw;
            } else if (tracker != null) {
                float[] pos = tracker.getPosition(name);
                if (pos == null) {
                    if (ATHRConfig.feature.debug.dungeonMapDebug) {
                        ChatUtils.sendMessage("§7[§6MapDebug§7] " + name + " | no position — skipping");
                    }
                    continue;
                }
                px = pos[0];
                pz = pos[1];
                yaw = pos[2];

                if (entity != null && !entity.isDead) {
                    float entityPx = DungeonMapGrid.worldToPixelX(entity.posX);
                    float entityPz = DungeonMapGrid.worldToPixelZ(entity.posZ);
                    px += entityPx - px;
                    pz += entityPz - pz;
                    yaw = entity.rotationYaw;
                }
            } else {
                continue;
            }

            NetworkPlayerInfo info = null;
            if (entity != null) {
                info = mc.getNetHandler().getPlayerInfo(entity.getUniqueID());
            }
            if (info == null && tracker != null) {
                info = tracker.getNetworkPlayerInfo(name, mc);
            }
            if (info == null) continue;

            DungeonMapOverlay.renderPlayerHead(px - headPixelSize / 2f, pz - headPixelSize / 2f, -1, headScale, info, yaw);

            if (ATHRConfig.feature.dungeons.dungeonMapConfig.showPlayerUsername) {
                String displayName = getDisplayName(name, info, entity);
                if (!ATHRConfig.feature.dungeons.dungeonMapConfig.showPlayerRank) {
                    int idx = displayName.lastIndexOf("]");
                    if (idx >= 0) displayName = displayName.substring(idx + 1).trim();
                }

                DungeonMapOverlay.renderName(px - headPixelSize / 2f, pz - headPixelSize / 2f, -1, headScale, ATHRConfig.feature.dungeons.dungeonMapConfig.nameSize * 0.75f, displayName, false);
            }
        }

        GlStateManager.popMatrix();
    }

    private static String getDisplayName(String name, NetworkPlayerInfo info, EntityPlayer entity) {
        IChatComponent display = info.getDisplayName();
        if (display != null) {
            return display.getFormattedText();
        }
        if (entity != null) {
            return entity.getDisplayName().getFormattedText();
        }
        return name;
    }
}
