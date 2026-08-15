package io.hamlook.aetheria.features.dungeons.overlays.map;

import net.minecraft.block.material.MapColor;
import net.minecraft.world.storage.MapData;

import lombok.Getter;
import java.awt.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class DungeonMapGrid {

    private static final int SAMPLE_ALPHA_THRESHOLD = 40;
    public float worldOriginX = 200f;
    public float worldOriginZ = 200f;
    public int cellSizeBlocks = 32;

    @Getter
    private final Map<RoomOffset, RoomCell> rooms = new HashMap<>();
    @Getter
    private final List<Junction> junctions = new ArrayList<>();
    @Getter
    private final List<RoomRegion> regions = new ArrayList<>();
    @Getter
    private final Map<Long, RoomRegion> regionCellMap = new HashMap<>();
    private final Map<Long, RoomState> stateCache = new HashMap<>();
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
            return new DungeonMapGrid();
        }

        Color[][] colors = new Color[128][128];

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
        }

        DungeonMapGrid grid = new DungeonMapGrid();
        grid.cellSizeBlocks = cellSizeBlocks;

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
            return grid;
        }

        grid.connectorPixelSize = findConnectorSize(colors, grid.startPixelX, grid.startPixelY, grid.roomPixelSize);

        grid.loadNeighbors(colors, new RoomOffset(0, 0));

        if (grid.rooms.isEmpty()) {
            return grid;
        }

        grid.updateRoomColors(colors);
        for (RoomOffset off : grid.rooms.keySet()) {
            grid.updateRoomConnections(colors, off);
        }
        grid.computeRegions();
        grid.findJunctions(colors);

        grid.blockToPixel = (float) (grid.roomPixelSize + grid.connectorPixelSize) / grid.cellSizeBlocks;
        grid.entrancePixelCenterX = grid.startPixelX + grid.roomPixelSize / 2f;
        grid.entrancePixelCenterZ = grid.startPixelY + grid.roomPixelSize / 2f;

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

        SampleResult result = sampleRect(colors, x0, y0, w, h);
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

            SampleResult result = sampleRect(colors, px, py, connectorPixelSize, connectorPixelSize);
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

    private void computeRegions() {
        regions.clear();
        Set<RoomOffset> visited = new HashSet<>();
        for (RoomOffset start : rooms.keySet()) {
            if (visited.contains(start)) continue;
            RoomRegion region = new RoomRegion();
            Deque<RoomOffset> stack = new ArrayDeque<>();
            stack.push(start);
            visited.add(start);
            while (!stack.isEmpty()) {
                RoomOffset off = stack.pop();
                region.cells.add(off);
                if (off.x < region.minX) region.minX = off.x;
                if (off.y < region.minY) region.minY = off.y;
                if (off.x > region.maxX) region.maxX = off.x;
                if (off.y > region.maxY) region.maxY = off.y;
                RoomCell cell = rooms.get(off);
                if (cell == null) continue;
                expandRegion(visited, stack, off.left(), cell.left);
                expandRegion(visited, stack, off.right(), cell.right);
                expandRegion(visited, stack, off.up(), cell.up);
                expandRegion(visited, stack, off.down(), cell.down);
            }
            region.cellCount = region.cells.size();
            layoutRegion(region);
            region.state = aggregateState(region);
            for (RoomOffset off : region.cells) {
                regionCellMap.put(pack(off.x, off.y), region);
            }
            regions.add(region);
        }
    }

    private void expandRegion(Set<RoomOffset> visited, Deque<RoomOffset> stack, RoomOffset neighbor, RoomConnection conn) {
        if (conn.type == ConnectionType.ROOM_DIVIDER && rooms.containsKey(neighbor) && !visited.contains(neighbor)) {
            visited.add(neighbor);
            stack.push(neighbor);
        }
    }

    private static void layoutRegion(RoomRegion region) {
        if (region.cellCount == 1) {
            RoomOffset cell = region.cells.get(0);
            region.tickCell = cell;
            region.hasRowAnchor = true;
            region.nameRowY = cell.y;
            region.nameRowMinX = region.nameRowMaxX = cell.x;
            return;
        }
        region.cells.sort((a, b) -> a.x != b.x ? Integer.compare(a.x, b.x) : Integer.compare(a.y, b.y));
        int tallestColumn = region.cells.get(0).x;
        int maxCount = 0;
        int count = 0;
        int currentX = region.cells.get(0).x;
        for (RoomOffset cell : region.cells) {
            if (cell.x == currentX) {
                count++;
            } else {
                if (count > maxCount) {
                    maxCount = count;
                    tallestColumn = currentX;
                }
                currentX = cell.x;
                count = 1;
            }
        }
        if (count > maxCount) {
            tallestColumn = currentX;
        }
        region.tickCell = null;
        for (RoomOffset cell : region.cells) {
            if (cell.x == tallestColumn) {
                region.tickCell = cell;
                break;
            }
        }
        Map<Integer, Integer> rowCounts = new HashMap<>();
        for (RoomOffset cell : region.cells) {
            rowCounts.merge(cell.y, 1, Integer::sum);
        }
        int widestRow = 0;
        int maxRowCount = 0;
        int tiedRows = 0;
        for (Map.Entry<Integer, Integer> e : rowCounts.entrySet()) {
            int c = e.getValue();
            if (c > maxRowCount) {
                maxRowCount = c;
                widestRow = e.getKey();
                tiedRows = 1;
            } else if (c == maxRowCount) {
                tiedRows++;
            }
        }
        region.hasRowAnchor = false;
        if (tiedRows == 1) {
            region.hasRowAnchor = true;
            region.nameRowY = widestRow;
            region.nameRowMinX = Integer.MAX_VALUE;
            region.nameRowMaxX = Integer.MIN_VALUE;
            for (RoomOffset cell : region.cells) {
                if (cell.y == widestRow) {
                    region.nameRowMinX = Math.min(region.nameRowMinX, cell.x);
                    region.nameRowMaxX = Math.max(region.nameRowMaxX, cell.x);
                }
            }
        }
    }

    private RoomState aggregateState(RoomRegion region) {
        RoomState best = RoomState.DISCOVERED;
        for (RoomOffset off : region.cells) {
            RoomCell cell = rooms.get(off);
            if (cell != null && statePriority(cell.state) > statePriority(best)) {
                best = cell.state;
            }
        }
        return best;
    }

    private static int statePriority(RoomState state) {
        switch (state) {
            case FAILED:
                return 5;
            case GREEN:
                return 4;
            case CLEARED:
                return 3;
            case UNOPENED:
                return 2;
            default:
                return 1;
        }
    }

    public RoomRegion regionAtPixel(float pixelX, float pixelZ) {
        if (roomPixelSize <= 0 || regions.isEmpty()) return null;
        int gx = Math.round((pixelX - startPixelX) / (float) (roomPixelSize + connectorPixelSize));
        int gz = Math.round((pixelZ - startPixelY) / (float) (roomPixelSize + connectorPixelSize));
        RoomRegion region = regionCellMap.get(pack(gx, gz));
        if (region != null) return region;
        for (RoomRegion r : regions) {
            if (gx >= r.minX && gx <= r.maxX && gz >= r.minY && gz <= r.maxY) return r;
        }
        return null;
    }

    private static SampleResult sampleRect(Color[][] colors, int x0, int y0, int w, int h) {
        int filled = 0;
        Integer dominant = null;
        for (int dx = 0; dx < w; dx++) {
            for (int dy = 0; dy < h; dy++) {
                int sx = x0 + dx;
                int sy = y0 + dy;
                if (sx >= 0 && sy >= 0 && sx < 128 && sy < 128) {
                    Color pixel = colors[sx][sy];
                    if (pixel.getAlpha() > SAMPLE_ALPHA_THRESHOLD) {
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

    public static class RoomRegion {
        public final List<RoomOffset> cells = new ArrayList<>();
        public RoomOffset tickCell;
        public boolean hasRowAnchor; // widest-row span anchor; false -> bbox-center anchor
        public int nameRowY;
        public int nameRowMinX;
        public int nameRowMaxX;
        public int cellCount = 0;
        public RoomState state = RoomState.DISCOVERED;
        public int minX = Integer.MAX_VALUE;
        public int minY = Integer.MAX_VALUE;
        public int maxX = Integer.MIN_VALUE;
        public int maxY = Integer.MIN_VALUE;
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