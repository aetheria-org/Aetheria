package io.hamlook.aetheria.features.dungeons.overlays.map;

import net.minecraft.block.material.MapColor;
import net.minecraft.world.storage.MapData;

import lombok.Getter;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DungeonMapGrid {

    public static float worldOriginX = 200f;
    public static float worldOriginZ = 200f;
    public static int cellSizeBlocks = 32;

    @Getter
    private final Map<RoomOffset, RoomCell> rooms = new HashMap<>();
    public String debugInfo = "";
    private int startPixelX = -1;
    private int startPixelY = -1;
    @Getter
    private int roomPixelSize = 0;
    @Getter
    private int connectorPixelSize = 5;
    public static float entrancePixelCenterX = 0f;
    public static float entrancePixelCenterZ = 0f;
    public static float blockToPixel = 0f;

    public static DungeonMapGrid parse(MapData data) {
        if (data == null || data.colors == null) {
            DungeonMapGrid empty = new DungeonMapGrid();
            empty.debugInfo = "data or data.colors is null";
            return empty;
        }

        Color[][] colors = new Color[128][128];
        int alphaPixels = 0;

        for (int i = 0; i < 16384; i++) {
            int x = i % 128;
            int y = i / 128;
            int b = data.colors[i] & 0xFF;
            if (b / 4 == 0) {
                int checkerAlpha = (i + i / 128 & 1) * 8 + 16;
                colors[x][y] = new Color(0, 0, 0, checkerAlpha);
            } else {
                int rgb = MapColor.mapColorArray[b / 4].getMapColor(b & 3);
                colors[x][y] = new Color(rgb, true);
            }
            if (colors[x][y].getAlpha() < 50) alphaPixels++;
        }

        DungeonMapGrid grid = new DungeonMapGrid();
        grid.debugInfo = "alphaPixels=" + alphaPixels + "/16384";

        for (int x = 0; x < 128; x++) {
            for (int y = 0; y < 128; y++) {
                Color c = colors[x][y];
                int rawByte = data.colors[y * 128 + x] & 0xFF;
                if (c.getAlpha() > 80 && rawByte / 4 == 7) {
                    grid.startPixelX = x;
                    grid.startPixelY = y;
                    int foundRoomSize = 0;
                    for (int d = 0; d <= 31; d++) {
                        if (x + d < 128 && y + d < 128) {
                            Color c2 = colors[x + d][y + d];
                            if (c2.getAlpha() > 80 && (data.colors[(y + d) * 128 + (x + d)] & 0xFF) / 4 == 7) {
                                foundRoomSize = Math.max(foundRoomSize, d + 1);
                            }
                        }
                    }
                    grid.roomPixelSize = Math.max(foundRoomSize, 4);
                    break;
                }
            }
            if (grid.startPixelX >= 0) break;
        }

        if (grid.startPixelX < 0 || grid.roomPixelSize <= 0) {
            grid.debugInfo = "no starting room (foliageColor type 7) found.";
            return grid;
        }

        grid.connectorPixelSize = findConnectorSize(colors, grid.startPixelX, grid.startPixelY, grid.roomPixelSize);

        grid.loadNeighbors(colors, new RoomOffset(0, 0));

        if (grid.rooms.isEmpty()) {
            Color startColor = colors[grid.startPixelX][grid.startPixelY];
            grid.debugInfo = "startPixel=(" + grid.startPixelX + "," + grid.startPixelY + ") roomSize=" + grid.roomPixelSize + " connSize=" + grid.connectorPixelSize + " but flood fill found 0 rooms. Start color: R" + startColor.getRed() + "G" + startColor.getGreen() + "B" + startColor.getBlue() + "A" + startColor.getAlpha();
            return grid;
        }

        grid.updateRoomColors(colors);
        for (RoomOffset off : grid.rooms.keySet()) {
            grid.updateRoomConnections(colors, off);
        }

        blockToPixel = (float) (grid.roomPixelSize + grid.connectorPixelSize) / cellSizeBlocks;
        entrancePixelCenterX = grid.startPixelX + grid.roomPixelSize / 2f;
        entrancePixelCenterZ = grid.startPixelY + grid.roomPixelSize / 2f;
        grid.debugInfo = "valid: " + grid.rooms.size() + " rooms, roomSize=" + grid.roomPixelSize + " connSize=" + grid.connectorPixelSize + " blockToPixel=" + blockToPixel;

        return grid;
    }

    public static float worldToPixelX(double worldX) {
        return (float) ((worldX + worldOriginX) * blockToPixel);
    }

    public static float worldToPixelZ(double worldZ) {
        return (float) ((worldZ + worldOriginZ) * blockToPixel);
    }

    private void loadNeighbors(Color[][] colors, RoomOffset pos) {
        if (rooms.containsKey(pos)) return;
        int px = startPixelX + pos.x * (roomPixelSize + connectorPixelSize);
        int py = startPixelY + pos.y * (roomPixelSize + connectorPixelSize);
        if (px < 0 || py < 0 || px + roomPixelSize >= 128 || py + roomPixelSize >= 128) return;
        if (colors[px][py].getAlpha() <= 100) return;
        rooms.put(pos, new RoomCell());
        for (RoomOffset neighbor : pos.getNeighbors()) {
            loadNeighbors(colors, neighbor);
        }
    }

    private void updateRoomColors(Color[][] colors) {
        for (Map.Entry<RoomOffset, RoomCell> entry : rooms.entrySet()) {
            int px = startPixelX + entry.getKey().x * (roomPixelSize + connectorPixelSize);
            int py = startPixelY + entry.getKey().y * (roomPixelSize + connectorPixelSize);
            if (px >= 0 && py >= 0 && px < 128 && py < 128) {
                entry.getValue().color = colors[px][py].getRGB();
                entry.getValue().tickColor = 0;
            }
        }
    }

    private void updateRoomConnections(Color[][] colors, RoomOffset pos) {
        RoomCell room = rooms.get(pos);
        if (room == null) return;

        int baseX = startPixelX + pos.x * (roomPixelSize + connectorPixelSize);
        int baseY = startPixelY + pos.y * (roomPixelSize + connectorPixelSize);

        room.up = sampleConnection(colors, baseX, baseY, 0);
        room.right = sampleConnection(colors, baseX, baseY, 1);
        room.down = sampleConnection(colors, baseX, baseY, 2);
        room.left = sampleConnection(colors, baseX, baseY, 3);
    }

    private RoomConnection sampleConnection(Color[][] colors, int baseX, int baseY, int dir) {
        int totalFilled = 0;
        Integer dominantColor = null;

        for (int i = 0; i < roomPixelSize; i++) {
            for (int j = 1; j <= connectorPixelSize; j++) {
                int sx, sy;
                switch (dir) {
                    case 0:
                        sx = baseX + i;
                        sy = baseY - j;
                        break;
                    case 1:
                        sx = baseX + roomPixelSize + j - 1;
                        sy = baseY + i;
                        break;
                    case 2:
                        sx = baseX + i;
                        sy = baseY + roomPixelSize + j - 1;
                        break;
                    default:
                        sx = baseX - j;
                        sy = baseY + i;
                        break;
                }

                if (sx >= 0 && sy >= 0 && sx < 128 && sy < 128) {
                    Color pixel = colors[sx][sy];
                    if (pixel.getAlpha() > 40) {
                        totalFilled++;
                        if (dominantColor == null) {
                            dominantColor = pixel.getRGB();
                        }
                    }
                }
            }
        }

        float proportion = (float) totalFilled / (roomPixelSize * connectorPixelSize);
        RoomConnection conn = new RoomConnection();
        if (proportion > 0.8f) {
            conn.type = ConnectionType.ROOM_DIVIDER;
        } else if (proportion > 0.1f) {
            conn.type = ConnectionType.CORRIDOR;
        } else {
            conn.type = ConnectionType.WALL;
        }
        conn.color = dominantColor != null ? dominantColor : 0;
        return conn;
    }

    public float gridToPixelX(float gridX) {
        return startPixelX + gridX * (roomPixelSize + connectorPixelSize);
    }

    public float gridToPixelZ(float gridZ) {
        return startPixelY + gridZ * (roomPixelSize + connectorPixelSize);
    }

    public int getGridPixelWidth() {
        return 128;
    }

    public int getGridPixelHeight() {
        return 128;
    }

    public boolean isValid() {
        return startPixelX >= 0 && !rooms.isEmpty();
    }

    private static int findConnectorSize(Color[][] colors, int startX, int startY, int roomPixelSize) {
        int foundConn = 8;
        for (int i = 0; i < roomPixelSize; i++) {
            for (int dir = 0; dir < 4; dir++) {
                for (int j = 1; j < 8; j++) {
                    int cx, cy;
                    switch (dir) {
                        case 0:
                            cx = startX + i;
                            cy = startY - j;
                            break;
                        case 1:
                            cx = startX + roomPixelSize + j - 1;
                            cy = startY + i;
                            break;
                        case 2:
                            cx = startX + i;
                            cy = startY + roomPixelSize + j - 1;
                            break;
                        default:
                            cx = startX - j;
                            cy = startY + i;
                            break;
                    }
                    if (cx >= 0 && cy >= 0 && cx < 128 && cy < 128 && colors[cx][cy].getAlpha() > 80) {
                        if (j == 1) break;
                        foundConn = Math.min(foundConn, j - 1);
                    }
                }
            }
        }
        return foundConn > 0 && foundConn < 8 ? foundConn : 4;
    }

    public enum ConnectionType {
        NONE, WALL, CORRIDOR, ROOM_DIVIDER
    }

    public static class RoomOffset {
        public final int x;
        public final int y;

        public RoomOffset(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public RoomOffset left() {
            return new RoomOffset(x - 1, y);
        }

        public RoomOffset right() {
            return new RoomOffset(x + 1, y);
        }

        public RoomOffset up() {
            return new RoomOffset(x, y - 1);
        }

        public RoomOffset down() {
            return new RoomOffset(x, y + 1);
        }

        public RoomOffset[] getNeighbors() {
            return new RoomOffset[]{left(), right(), up(), down()};
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RoomOffset that = (RoomOffset) o;
            return x == that.x && y == that.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }

    public static class RoomConnection {
        public ConnectionType type = ConnectionType.NONE;
        public int color = 0;
    }

    public static class RoomCell {
        public int color = 0;
        public int tickColor = 0;
        public RoomConnection up = new RoomConnection();
        public RoomConnection down = new RoomConnection();
        public RoomConnection left = new RoomConnection();
        public RoomConnection right = new RoomConnection();
    }
}
