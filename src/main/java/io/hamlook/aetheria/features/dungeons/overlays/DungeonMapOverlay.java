package io.hamlook.aetheria.features.dungeons.overlays;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.dungeons.DungeonMapConfig;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaStyle;
import io.hamlook.aetheria.events.ASMTickEvent;
import io.hamlook.aetheria.events.ASMWorldUnloadEvent;
import io.hamlook.aetheria.events.DungeonRoomEvent;
import io.hamlook.aetheria.events.TickPhase;
import io.hamlook.aetheria.features.dungeons.DungeonStats;
import io.hamlook.aetheria.features.dungeons.overlays.map.DungeonMapGrid;
import io.hamlook.aetheria.features.dungeons.overlays.map.DungeonMapRenderer;
import io.hamlook.aetheria.features.dungeons.overlays.map.DungeonPlayerTracker;
import io.hamlook.aetheria.features.dungeons.rooms.DungeonRoomDetector;
import io.hamlook.aetheria.features.dungeons.rooms.DungeonRoomOverlay;
import io.hamlook.aetheria.features.dungeons.rooms.secret.SecretRoom;
import io.hamlook.aetheria.features.dungeons.rooms.secret.SecretTracker;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.Position;
import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.compat.InventoryCompat;
import io.hamlook.aetheria.utils.compat.EntityCompat;
import io.hamlook.aetheria.utils.compat.DrawContextUtils;
import io.hamlook.aetheria.utils.compat.GuiScreenUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.data.SkyblockData;
import io.hamlook.aetheria.utils.overlay.Overlay;
import lombok.Getter;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
//#if MC > 1.21
//$$ import net.minecraft.item.FilledMapItem;
//#endif
import net.minecraft.world.storage.MapData;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RegisterEvents
public class DungeonMapOverlay extends Overlay {

    public static boolean dungeonRunEnded = false;
    @Getter
    private static DungeonMapOverlay instance;
    public static DungeonPlayerTracker playerTracker = new DungeonPlayerTracker();
    private DungeonMapGrid cachedGrid = null;
    private byte[] lastAttemptColors = null;
    private static final String ENTRANCE_ROOM = "Entrance Room";
    /**
     * Frozen map<->world calibration, captured once the entrance room's world
     * position is known (detector latch while standing in the Entrance Room)
     * and never re-derived afterwards: the
     * server's dungeon map keeps a fixed pixel<->world mapping for the whole
     * run, so recalculating from freshly parsed geometry (whose raster-first
     * anchor can relocate as rooms are drawn) would offset entity-based self
     * tracking. Kept separate from {@link DungeonMapGrid} fields so live parse
     * values remain untouched for other consumers.
     */
    private boolean mapCalibrated = false;
    private float calibOriginX = 0f;
    private float calibOriginZ = 0f;
    private float calibBtp = 0f;
    /** Entrance-corner pixel anchor from this run's first valid parse; -1 until captured. */
    private int anchorStartX = -1;
    private int anchorStartY = -1;
    private double entranceCenterX = 0;
    private double entranceCenterZ = 0;
    private int lastPopulateTick = -40;

    public DungeonMapOverlay() {
        super(128, 128);
        instance = this;
    }

    public static MapData getDungeonMap(EntityPlayerSP player) {
        if (player == null || InventoryCompat.getInventory(player) == null) return null;
        ItemStack[] inv = InventoryCompat.getMainInventory(player);
        if (inv == null || inv.length < 9) return null;
        ItemStack stack = inv[8];
        if (stack == null) return null;
        //#if MC < 1.21
        return Items.filled_map.getMapData(stack, MinecraftCompat.getLocalWorld());
        //#else
        //$$ return FilledMapItem.getMapState(stack, MinecraftCompat.getLocalWorld());
        //#endif
    }

    public void resetRunState() {
        cachedGrid = null;
        lastAttemptColors = null;
        playerTracker.clear();
        DungeonRoomDetector.getVisitedRooms().clear();
        dungeonRunEnded = false;
        mapCalibrated = false;
        calibOriginX = 0f;
        calibOriginZ = 0f;
        calibBtp = 0f;
        anchorStartX = -1;
        anchorStartY = -1;
        entranceCenterX = 0;
        entranceCenterZ = 0;
        lastPopulateTick = -40;
    }

    public static void resetRun() {
        if (instance != null) instance.resetRunState();
    }

    @HandleEvent
    public void onUnload(ASMWorldUnloadEvent e) {
        resetRunState();
    }

    // keeps the grid alive when the map overlay is off, so secrets and future events can read it
    @HandleEvent
    public void onTick(ASMTickEvent event) {
        if (event.phase != TickPhase.START) return;
        if (ATHRConfig.feature == null) return;
        if (!SkyblockData.isInDungeon() || dungeonRunEnded) return;
        if (DungeonStats.isInBossFight()) return;
        EntityPlayerSP player = MinecraftCompat.getLocalPlayer();
        if (player == null || player.ticksExisted % 20 != 0) return;
        updateCachedGrid(player);
        postRoomState();
    }

    // green tick means the room is done, white means mobs are done but secrets can remain
    private void postRoomState() {
        // before calibration the world origin tracks the player, so a room centre maps nowhere useful
        if (!mapCalibrated) return;

        SecretRoom room = SecretTracker.current();
        if (room == null || !room.hasBounds()) return;

        DungeonMapGrid grid = getCachedGrid();
        if (grid == null) return;

        DungeonMapConfig cfg = ATHRConfig.feature.dungeons.dungeonMapConfig;
        DungeonMapGrid.RoomState state = grid.stateForRoom(
                room.centerX(), room.centerZ(), cfg.rooms.splitRoomMarkers);
        if (state == null) return;

        boolean allSecretsCleared = state == DungeonMapGrid.RoomState.GREEN;
        boolean cleared = allSecretsCleared || state == DungeonMapGrid.RoomState.CLEARED;

        boolean roomClearedFired = SecretTracker.updateCleared(room, cleared);
        boolean secretsFired = SecretTracker.updateSecretsCleared(room, allSecretsCleared);

        if (roomClearedFired) {
            new DungeonRoomEvent(room, DungeonRoomEvent.Type.ROOM_CLEARED).post();
        }
        if (secretsFired) {
            new DungeonRoomEvent(room, DungeonRoomEvent.Type.SECRETS_CLEARED).post();
        }
    }

    @Override
    public void render(boolean preview) {
        if (!preview && (!SkyblockData.isInDungeon() || dungeonRunEnded)) return;
        if (!preview && DungeonStats.isInBossFight()) return;

        EntityPlayerSP player = MinecraftCompat.getLocalPlayer();
        if (player == null) return;

        DungeonMapConfig cfg = ATHRConfig.feature.dungeons.dungeonMapConfig;

        if ((playerTracker.getPlayerNames().isEmpty() || player.ticksExisted < lastPopulateTick || player.ticksExisted - lastPopulateTick >= 40) && player.ticksExisted % 20 == 0) {
            playerTracker.populate();
            lastPopulateTick = player.ticksExisted;
        }

        MapData info = getDungeonMap(player);
        if (info != null) {
            updateCachedGrid(player);
        }

        if (info == null && !preview) return;

        float scale = getScale();
        int screenW = Overlay.cachedWidth != 0 ? Overlay.cachedWidth : GuiScreenUtils.getScaledWindowWidth();
        int screenH = Overlay.cachedHeight != 0 ? Overlay.cachedHeight : GuiScreenUtils.getScaledWindowHeight();
        Position pos = getPosition();

        int gridW = lastW;
        int gridH = lastH;
        float scaledW = gridW * scale;
        float scaledH = gridH * scale;

        int bx = pos.getAbsX(screenW, (int) scaledW);
        int by = pos.getAbsY(screenH, (int) scaledH);
        if (pos.isCenterX()) bx -= (int) scaledW / 2;
        if (pos.isCenterY()) by -= (int) scaledH / 2;

        int cx = bx + (int) scaledW / 2;
        int cy = by + (int) scaledH / 2;
        int bw = bx + (int) scaledW + 4;
        int bh = by + (int) scaledH + 4;
        int radius = getCornerRadius();

        int bgColor = getBgColor();
        if ((bgColor >>> 24) != 0) {
            if (cfg.appearance.bgFlowChroma) {
                Overlay.drawRoundedRectFlow(bx, by, bw, bh, radius, ChromaStyle.of(cfg.appearance.bgColor, 1, cfg.appearance.border.flowChromaSize));
            } else {
                Overlay.drawRoundedRect(bx, by, bw, bh, radius, bgColor);
            }
        }

        if (cfg.appearance.border.borderEnabled) {
            int borderColor = ChromaColour.specialToChromaRGB(cfg.appearance.border.borderColor);
            if ((borderColor >>> 24) != 0) {
                if (cfg.appearance.border.borderFlowChroma) {
                    Overlay.drawRoundedRectBorderFlow(bx, by, bw, bh, radius, cfg.appearance.border.borderThickness, ChromaStyle.of(cfg.appearance.border.borderColor, 1, cfg.appearance.border.flowChromaSize));
                } else {
                    Overlay.drawRoundedRectBorder(bx, by, bw, bh, radius, cfg.appearance.border.borderThickness, borderColor);
                }
            }
        }

        if (preview) {
            String txt = "Preview Map";
            int tw = MinecraftCompat.getFontRenderer().getStringWidth(txt);
            DrawContextUtils.drawTextWithShadow(MinecraftCompat.getFontRenderer(), txt, cx - tw / 2f, cy - 4f, 0xFFFFFFFF);
        } else if (cachedGrid != null && cachedGrid.isValid()) {
            renderDungeonMap(cx, cy, scale, cfg.rooms.showVisitedRoomNames, cfg.rooms.mapColorText);
        }
    }

    public void renderDungeonMap(float centerX, float centerY, float scale, boolean showRoomNames, boolean colorRoomNames) {
        if (cachedGrid == null || !cachedGrid.isValid()) return;
        EntityPlayerSP player = MinecraftCompat.getLocalPlayer();
        MapData info = player != null ? getDungeonMap(player) : null;
        //#if MC < 1.21
        if (info != null) playerTracker.matchDecorations(info.mapDecorations);
        //#else
        //$$ if (info != null) playerTracker.matchDecorations(info.getDecorations());
        //#endif
        DungeonMapRenderer.render(cachedGrid, centerX, centerY, scale, playerTracker.getPlayerNames(), playerTracker, DungeonRoomDetector.getVisitedRooms(), showRoomNames, colorRoomNames, mapCalibrated);
    }

    /**
     * Exposes the currently-cached dungeon map grid so other UI (e.g. the Leap
     * menu overlay) can compute marker positions in the same local pixel space
     * that renderDungeonMap() actually draws into. Returns null if no valid map
     * has been parsed yet.
     */
    public DungeonMapGrid getCachedGrid() {
        return (cachedGrid != null && cachedGrid.isValid()) ? cachedGrid : null;
    }

    public void updateCachedGrid(EntityPlayerSP player) {
        MapData info = getDungeonMap(player);
        if (info == null) return;
        if (!Arrays.equals(info.colors, lastAttemptColors)) {
            lastAttemptColors = Arrays.copyOf(info.colors, info.colors.length);
            // Reparses seed this run's captured entrance-corner anchor so the grid
            // coordinate frame is identical every parse; only the very first valid
            // parse (entrance-only map) locates the anchor by raster scan.
            DungeonMapGrid parsed = anchorStartX >= 0
                    ? DungeonMapGrid.parse(info, ATHRConfig.feature.dungeons.dungeonMapConfig.appearance.cellSizeBlocks, anchorStartX, anchorStartY)
                    : DungeonMapGrid.parse(info, ATHRConfig.feature.dungeons.dungeonMapConfig.appearance.cellSizeBlocks);
            // partial map can parse invalid, keep the last good grid instead of blanking
            if (parsed.isValid()) {
                cachedGrid = parsed;
                if (anchorStartX < 0) {
                    anchorStartX = parsed.getStartPixelX();
                    anchorStartY = parsed.getStartPixelY();
                }
                lastW = parsed.getGridPixelWidth();
                lastH = parsed.getGridPixelHeight();
            }
        }
        // Calibration runs on every call with a valid grid, not just on reparse:
        // the block scanner can become ready while the map bytes are stable, and
        // a reparse-only latch would never fire. One-shot per run.
        if (cachedGrid != null && cachedGrid.isValid()) {
            if (!mapCalibrated && inEntranceRoom()
                    && DungeonRoomDetector.roomBoundsValid && DungeonRoomDetector.originBlock != null) {
                entranceCenterX = (DungeonRoomDetector.roomMinX + DungeonRoomDetector.roomMaxX) / 2.0 + 0.5;
                entranceCenterZ = (DungeonRoomDetector.roomMinZ + DungeonRoomDetector.roomMaxZ) / 2.0 + 0.5;
                calibOriginX = cachedGrid.entrancePixelCenterX / cachedGrid.blockToPixel - (float) entranceCenterX;
                calibOriginZ = cachedGrid.entrancePixelCenterZ / cachedGrid.blockToPixel - (float) entranceCenterZ;
                calibBtp = cachedGrid.blockToPixel;
                mapCalibrated = true;
            }
            if (mapCalibrated) {
                cachedGrid.worldOriginX = calibOriginX;
                cachedGrid.worldOriginZ = calibOriginZ;
                cachedGrid.blockToPixel = calibBtp;
            } else {
                cachedGrid.worldOriginX = cachedGrid.entrancePixelCenterX / cachedGrid.blockToPixel - (float) EntityCompat.getPosX(player);
                cachedGrid.worldOriginZ = cachedGrid.entrancePixelCenterZ / cachedGrid.blockToPixel - (float) EntityCompat.getPosZ(player);
            }
        }
    }

    // the detector's bounds are whichever room you stand in, so calibrating anywhere else ties the
    // entrance pixel to that room's world centre and shifts every name and self-marker by a cell
    private static boolean inEntranceRoom() {
        String name = DungeonRoomOverlay.currentRoomName;
        return name != null && ENTRANCE_ROOM.equals(ColorUtils.stripColor(name).trim());
    }

    @Override
    public List<String> getLines(boolean preview) {
        return Collections.emptyList();
    }

    @Override
    public Position getPosition() {
        return ATHRConfig.feature.dungeons.dungeonMapConfig.dungeonMapPos;
    }

    @Override
    public float getScale() {
        return ATHRConfig.feature.dungeons.dungeonMapConfig.appearance.scale;
    }

    @Override
    public int getBgColor() {
        return ChromaColour.specialToChromaRGB(ATHRConfig.feature.dungeons.dungeonMapConfig.appearance.bgColor);
    }

    @Override
    public int getCornerRadius() {
        return ATHRConfig.feature.dungeons.dungeonMapConfig.appearance.cornerRadius;
    }

    @Override
    public boolean isEnabled() {
        return ATHRConfig.feature.dungeons.dungeonMapConfig.enabled;
    }
}
