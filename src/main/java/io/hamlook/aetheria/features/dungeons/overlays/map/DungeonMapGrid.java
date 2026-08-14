package io.hamlook.aetheria.features.dungeons.overlays.map;

import net.minecraft.block.material.MapColor;
import net.minecraft.world.storage.MapData;

import lombok.Getter;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DungeonMapGrid {

    public float worldOriginX = 200f;
    public float worldOriginZ = 200f;
    public int cellSizeBlocks = 32;

    @Getter
    private final Map<RoomOffset, RoomCell> rooms = new HashMap<>();
    @Getter
    private final List<Junction> junctions = new ArrayList<>();
    private final Map<Long, RoomState> stateCache = new HashMap<>();
    public String debugInfo = "";
    private int startPixelX = -1;
    private int startPixelY = -1;
    @Getter
    private int roomPixelSize = 0;
    @Getter
    private int connectorPixelSize = 5;
    public float entrancePixelCenterX = 0f;
    public float entrancePixelCenterZ = 0f;
    public float blockToPixel = 0f;

    public static DungeonMapGrid parse(MapData data, int cellSizeBlocks) {
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
        grid.cellSizeBlocks = cellSizeBlocks;
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
        grid.findJunctions(colors);

        grid.blockToPixel = (float) (grid.roomPixelSize + grid.connectorPixelSize) / grid.cellSizeBlocks;
        grid.entrancePixelCenterX = grid.startPixelX + grid.roomPixelSize / 2f;
        grid.entrancePixelCenterZ = grid.startPixelY + grid.roomPixelSize / 2f;
        grid.debugInfo = "valid: " + grid.rooms.size() + " rooms, roomSize=" + grid.roomPixelSize + " connSize=" + grid.connectorPixelSize + " stride=" + (grid.roomPixelSize + grid.connectorPixelSize) + " junctions=" + grid.junctions.size() + " blockToPixel=" + grid.blockToPixel;

        return grid;
    }

    public float worldToPixelX(double worldX) {
        return (float) ((worldX + worldOriginX) * blockToPixel);
    }

    public float worldToPixelZ(double worldZ) {
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
        stateCache.clear();
        for (Map.Entry<RoomOffset, RoomCell> entry : rooms.entrySet()) {
            int px = startPixelX + entry.getKey().x * (roomPixelSize + connectorPixelSize);
            int py = startPixelY + entry.getKey().y * (roomPixelSize + connectorPixelSize);
            if (px >= 0 && py >= 0 && px < 128 && py < 128) {
                RoomCell cell = entry.getValue();
                Color bg = colors[px][py];
                cell.color = bg.getRGB();

                boolean isDarkGreyRoom = (bg.getRed() < 80 && bg.getGreen() < 80 && bg.getBlue() < 80);

                int greenCount = 0;
                int whiteCount = 0;
                int redCount = 0;
                int darkMarkCount = 0;

                for (int dx = 0; dx < roomPixelSize; dx++) {
                    for (int dy = 0; dy < roomPixelSize; dy++) {
                        int rx = px + dx;
                        int ry = py + dy;
                        if (rx < 128 && ry < 128) {
                            Color c = colors[rx][ry];
                            if (c.getAlpha() > 80 && c.getRGB() != cell.color) {
                                int red = c.getRed();
                                int green = c.getGreen();
                                int blue = c.getBlue();

                                if (green > 130 && red < 120 && blue < 120) {
                                    greenCount++;
                                }
                                else if (red > 180 && green > 180 && blue > 180) {
                                    whiteCount++;
                                }
                                else if (red > 180 && green < 100 && blue < 100) {
                                    redCount++;
                                }
                                else if (isDarkGreyRoom && red < 35 && green < 35 && blue < 35) {
                                    darkMarkCount++;
                                }
                            }
                        }
                    }
                }

                if (redCount >= 2) {
                    cell.state = RoomState.FAILED;
                }
                else if (greenCount >= 2) {
                    cell.state = RoomState.GREEN;
                }
                else if (whiteCount >= 2) {
                    cell.state = RoomState.CLEARED;
                }
                else if (isDarkGreyRoom || darkMarkCount >= 2) {
                    cell.state = RoomState.UNOPENED;
                }
                else {
                    cell.state = RoomState.DISCOVERED;
                }
                stateCache.put(pack(entry.getKey().x, entry.getKey().y), cell.state);
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
        int x0, y0, w, h;
        switch (dir) {
            case 0:
                x0 = baseX;
                y0 = baseY - connectorPixelSize;
                w = roomPixelSize;
                h = connectorPixelSize;
                break;
            case 1:
                x0 = baseX + roomPixelSize;
                y0 = baseY;
                w = connectorPixelSize;
                h = roomPixelSize;
                break;
            case 2:
                x0 = baseX;
                y0 = baseY + roomPixelSize;
                w = roomPixelSize;
                h = connectorPixelSize;
                break;
            default:
                x0 = baseX - connectorPixelSize;
                y0 = baseY;
                w = connectorPixelSize;
                h = roomPixelSize;
                break;
        }

        SampleResult result = sampleRect(colors, x0, y0, w, h, 40);
        float proportion = (float) result.filled / (roomPixelSize * connectorPixelSize);
        RoomConnection conn = new RoomConnection();
        if (proportion > 0.8f) {
            conn.type = ConnectionType.ROOM_DIVIDER;
        } else if (proportion > 0.1f) {
            conn.type = ConnectionType.CORRIDOR;
        } else {
            conn.type = ConnectionType.WALL;
        }
        conn.color = result.dominant != null ? result.dominant : 0;
        return conn;
    }

    private void findJunctions(Color[][] colors) {
        junctions.clear();
        for (RoomOffset off : rooms.keySet()) {
            int px = startPixelX + off.x * (roomPixelSize + connectorPixelSize) + roomPixelSize;
            int py = startPixelY + off.y * (roomPixelSize + connectorPixelSize) + roomPixelSize;
            if (px < 0 || py < 0 || px >= 128 || py >= 128) continue;

            SampleResult result = sampleRect(colors, px, py, connectorPixelSize, connectorPixelSize, 40);
            float proportion = (float) result.filled / (connectorPixelSize * connectorPixelSize);
            if (proportion > 0.3f) {
                Junction j = new Junction();
                j.px = px;
                j.py = py;
                j.color = result.dominant != null ? result.dominant : 0;
                junctions.add(j);
            }
        }
    }

    private static SampleResult sampleRect(Color[][] colors, int x0, int y0, int w, int h, int alphaThreshold) {
        int filled = 0;
        Integer dominant = null;
        for (int dx = 0; dx < w; dx++) {
            for (int dy = 0; dy < h; dy++) {
                int sx = x0 + dx;
                int sy = y0 + dy;
                if (sx >= 0 && sy >= 0 && sx < 128 && sy < 128) {
                    Color pixel = colors[sx][sy];
                    if (pixel.getAlpha() > alphaThreshold) {
                        filled++;
                        if (dominant == null) {
                            dominant = pixel.getRGB();
                        }
                    }
                }
            }
        }
        return new SampleResult(filled, dominant);
    }

    public float gridToPixelX(float gridX) {
        return startPixelX + gridX * (roomPixelSize + connectorPixelSize);
    }

    public float gridToPixelZ(float gridZ) {
        return startPixelY + gridZ * (roomPixelSize + connectorPixelSize);
    }

    public RoomState stateAtWorld(double worldX, double worldZ) {
        return stateAtPixel(worldToPixelX(worldX), worldToPixelZ(worldZ));
    }

    public RoomState stateAtPixel(float pixelX, float pixelZ) {
        if (roomPixelSize <= 0) return RoomState.DISCOVERED;
        int gx = Math.round((pixelX - startPixelX) / (float) (roomPixelSize + connectorPixelSize));
        int gz = Math.round((pixelZ - startPixelY) / (float) (roomPixelSize + connectorPixelSize));
        RoomState state = stateCache.get(pack(gx, gz));
        return state != null ? state : RoomState.DISCOVERED;
    }

    private static long pack(int x, int y) {
        return ((long) x << 32) | (y & 0xffffffffL);
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
                    int[] c = connectorCoords(startX, startY, roomPixelSize, dir, i, j);
                    int cx = c[0];
                    int cy = c[1];
                    if (cx >= 0 && cy >= 0 && cx < 128 && cy < 128 && colors[cx][cy].getAlpha() > 80) {
                        if (j == 1) break;
                        foundConn = Math.min(foundConn, j - 1);
                    }
                }
            }
        }
        return foundConn > 0 && foundConn < 8 ? foundConn : 4;
    }

    private static int[] connectorCoords(int baseX, int baseY, int roomPixelSize, int dir, int i, int j) {
        switch (dir) {
            case 0:
                return new int[]{baseX + i, baseY - j};
            case 1:
                return new int[]{baseX + roomPixelSize + j - 1, baseY + i};
            case 2:
                return new int[]{baseX + i, baseY + roomPixelSize + j - 1};
            default:
                return new int[]{baseX - j, baseY + i};
        }
    }

    public enum ConnectionType {
        NONE, WALL, CORRIDOR, ROOM_DIVIDER
    }

    public enum RoomState {
        GREEN, CLEARED, FAILED, UNOPENED, DISCOVERED
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

    public static class Junction {
        public int px;
        public int py;
        public int color;
    }

    private static class SampleResult {
        final int filled;
        final Integer dominant;

        SampleResult(int filled, Integer dominant) {
            this.filled = filled;
            this.dominant = dominant;
        }
    }

    public static class RoomCell {
        public int color = 0;
        public RoomState state = RoomState.DISCOVERED;
        public RoomConnection up = new RoomConnection();
        public RoomConnection down = new RoomConnection();
        public RoomConnection left = new RoomConnection();
        public RoomConnection right = new RoomConnection();
    }
}