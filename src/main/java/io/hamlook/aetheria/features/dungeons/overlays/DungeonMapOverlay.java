package io.hamlook.aetheria.features.dungeons.overlays;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.dungeons.DungeonMapConfig;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaStyle;
import io.hamlook.aetheria.features.dungeons.DungeonStats;
import io.hamlook.aetheria.features.dungeons.overlays.map.DungeonMapGrid;
import io.hamlook.aetheria.features.dungeons.overlays.map.DungeonMapRenderer;
import io.hamlook.aetheria.features.dungeons.overlays.map.DungeonPlayerTracker;
import io.hamlook.aetheria.features.dungeons.rooms.DungeonRoomDetector;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.Position;
import io.hamlook.aetheria.utils.data.SkyblockData;
import io.hamlook.aetheria.utils.overlay.Overlay;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.storage.MapData;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.*;

@RegisterEvents
public class DungeonMapOverlay extends Overlay {

    public static boolean dungeonRunEnded = false;
    @Getter
    private static DungeonMapOverlay instance;
    public static DungeonPlayerTracker playerTracker = new DungeonPlayerTracker();
    private DungeonMapGrid cachedGrid = null;
    private byte[] lastMapColors = null;
    private boolean spawnRecorded = false;
    private double entranceCenterX = 0;
    private double entranceCenterZ = 0;
    private int lastPopulateTick = -40;

    public DungeonMapOverlay() {
        super(128, 128);
        instance = this;
    }

    public static void clearPlayers() {
        if (instance != null) playerTracker.clear();
    }

    public static MapData getDungeonMap(EntityPlayerSP player) {
        if (player == null || player.inventory == null) return null;
        ItemStack[] inv = player.inventory.mainInventory;
        if (inv == null || inv.length < 9) return null;
        ItemStack stack = inv[8];
        if (stack == null) return null;
        return Items.filled_map.getMapData(stack, Minecraft.getMinecraft().theWorld);
    }

    @SubscribeEvent
    public void onUnload(WorldEvent.Unload e) {
        cachedGrid = null;
        lastMapColors = null;
        playerTracker.clear();
        DungeonRoomDetector.getVisitedRooms().clear();
        dungeonRunEnded = false;
        spawnRecorded = false;
        lastPopulateTick = -40;
    }

    @Override
    public void render(boolean preview) {
        if (!preview && (!SkyblockData.isInDungeon() || dungeonRunEnded)) return;
        if (!preview && DungeonStats.isInBossFight()) return;

        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        if (player == null) return;

        DungeonMapConfig cfg = ATHRConfig.feature.dungeons.dungeonMapConfig;

        if ((playerTracker.getPlayerNames().isEmpty() || player.ticksExisted - lastPopulateTick >= 40) && player.ticksExisted % 20 == 0) {
            playerTracker.populate();
            lastPopulateTick = player.ticksExisted;
        }

        MapData info = getDungeonMap(player);
        if (info != null) {
            updateCachedGrid(player);
        }

        if (info == null && !preview) return;

        float scale = getScale();
        ScaledResolution sr = Overlay.sr != null ? Overlay.sr : new ScaledResolution(Minecraft.getMinecraft());
        Position pos = getPosition();

        int gridW = lastW;
        int gridH = lastH;
        float scaledW = gridW * scale;
        float scaledH = gridH * scale;

        int bx = pos.getAbsX(sr, (int) scaledW);
        int by = pos.getAbsY(sr, (int) scaledH);
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
            int tw = Minecraft.getMinecraft().fontRendererObj.getStringWidth(txt);
            Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(txt, cx - tw / 2f, cy - 4f, 0xFFFFFFFF);
        } else if (cachedGrid != null && cachedGrid.isValid()) {
            renderDungeonMap(cx, cy, scale, cfg.rooms.showVisitedRoomNames, cfg.rooms.mapColorText);
        }
    }

    public void renderDungeonMap(float centerX, float centerY, float scale, boolean showRoomNames, boolean colorRoomNames) {
        if (cachedGrid == null || !cachedGrid.isValid()) return;
        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        MapData info = player != null ? getDungeonMap(player) : null;
        if (info != null) playerTracker.matchDecorations(info.mapDecorations);
        DungeonMapRenderer.render(cachedGrid, centerX, centerY, scale, playerTracker.getPlayerNames(), playerTracker, DungeonRoomDetector.getVisitedRooms(), showRoomNames, colorRoomNames);
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

    private void updateCachedGrid(EntityPlayerSP player) {
        MapData info = getDungeonMap(player);
        if (info == null) return;
        if (!Arrays.equals(info.colors, lastMapColors)) {
            lastMapColors = Arrays.copyOf(info.colors, info.colors.length);
            cachedGrid = DungeonMapGrid.parse(info, ATHRConfig.feature.dungeons.dungeonMapConfig.appearance.cellSizeBlocks);
            if (cachedGrid.isValid()) {
                if (!spawnRecorded && DungeonRoomDetector.roomBoundsValid && DungeonRoomDetector.originBlock != null) {
                    entranceCenterX = (DungeonRoomDetector.roomMinX + DungeonRoomDetector.roomMaxX) / 2.0 + 0.5;
                    entranceCenterZ = (DungeonRoomDetector.roomMinZ + DungeonRoomDetector.roomMaxZ) / 2.0 + 0.5;
                    cachedGrid.worldOriginX = cachedGrid.entrancePixelCenterX / cachedGrid.blockToPixel - (float) entranceCenterX;
                    cachedGrid.worldOriginZ = cachedGrid.entrancePixelCenterZ / cachedGrid.blockToPixel - (float) entranceCenterZ;
                    spawnRecorded = true;
                }
                if (spawnRecorded) {
                    cachedGrid.worldOriginX = cachedGrid.entrancePixelCenterX / cachedGrid.blockToPixel - (float) entranceCenterX;
                    cachedGrid.worldOriginZ = cachedGrid.entrancePixelCenterZ / cachedGrid.blockToPixel - (float) entranceCenterZ;
                } else {
                    cachedGrid.worldOriginX = cachedGrid.entrancePixelCenterX / cachedGrid.blockToPixel - (float) player.posX;
                    cachedGrid.worldOriginZ = cachedGrid.entrancePixelCenterZ / cachedGrid.blockToPixel - (float) player.posZ;
                }
                int maxPixelX = 128;
                int maxPixelY = 128;
                for (Map.Entry<DungeonMapGrid.RoomOffset, DungeonMapGrid.RoomCell> entry : cachedGrid.getRooms().entrySet()) {
                    int rx = (int) cachedGrid.gridToPixelX(entry.getKey().x) + cachedGrid.getRoomPixelSize() + cachedGrid.getConnectorPixelSize();
                    int ry = (int) cachedGrid.gridToPixelZ(entry.getKey().y) + cachedGrid.getRoomPixelSize() + cachedGrid.getConnectorPixelSize();
                    if (rx > maxPixelX) maxPixelX = rx;
                    if (ry > maxPixelY) maxPixelY = ry;
                }
                lastW = maxPixelX;
                lastH = maxPixelY;
            }
        }
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
