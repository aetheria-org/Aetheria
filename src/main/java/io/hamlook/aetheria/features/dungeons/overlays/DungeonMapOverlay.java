package io.hamlook.aetheria.features.dungeons.overlays;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import io.hamlook.aetheria.features.dungeons.DungeonStats;
import io.hamlook.aetheria.features.dungeons.overlays.map.DungeonMapGrid;
import io.hamlook.aetheria.features.dungeons.overlays.map.DungeonMapRenderer;
import io.hamlook.aetheria.features.dungeons.overlays.map.DungeonPlayerTracker;
import io.hamlook.aetheria.features.dungeons.rooms.DungeonRoomDetector;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.Position;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.data.SkyblockData;
import io.hamlook.aetheria.utils.overlay.Overlay;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.storage.MapData;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RegisterEvents
public class DungeonMapOverlay extends Overlay {

    public static boolean dungeonRunEnded = false;
    @Getter
    private static DungeonMapOverlay instance;
    private final DungeonPlayerTracker playerTracker = new DungeonPlayerTracker();
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
        if (instance != null) instance.playerTracker.clear();
    }

    public static MapData getDungeonMap(EntityPlayerSP player) {
        if (player == null || player.inventory == null) return null;
        ItemStack[] inv = player.inventory.mainInventory;
        if (inv == null || inv.length < 9) return null;
        ItemStack stack = inv[8];
        if (stack == null) return null;
        return Items.filled_map.getMapData(stack, Minecraft.getMinecraft().theWorld);
    }

    public static void renderName(float pixelX, float pixelZ, int color, float headScale, float scale, String name, boolean centered) {
        if (name == null || name.isEmpty()) return;

        Minecraft mc = Minecraft.getMinecraft();
        float stringWidth = mc.fontRendererObj.getStringWidth(name);

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        int alpha = (color >> 24) & 0xFF;
        float nameAlpha = (alpha == 0) ? 1.0f : alpha / 255f;
        GlStateManager.color(1.0f, 1.0f, 1.0f, nameAlpha);

        if (centered) {
            GlStateManager.translate(pixelX, pixelZ, 0f);
            GlStateManager.scale(scale, scale, 1.0f);

            float paddingX = 3f;
            float paddingY = 2f;
            float x1 = -stringWidth / 2f - paddingX;
            float y1 = -mc.fontRendererObj.FONT_HEIGHT / 2f - paddingY;
            float x2 = stringWidth / 2f + paddingX;
            float y2 = mc.fontRendererObj.FONT_HEIGHT / 2f + paddingY;

            Gui.drawRect((int) x1, (int) y1, (int) x2, (int) y2, 0x60000000);
            GlStateManager.enableTexture2D();
            mc.fontRendererObj.drawString(name, (int) (-stringWidth / 2f), (int) (-mc.fontRendererObj.FONT_HEIGHT / 2f), 0xFFFFFFFF);
        } else {
            float headSize = headScale * 8f;
            float half = headSize / 2f;
            float cx = pixelX + half;
            float cy = (pixelZ - headSize) + ATHRConfig.feature.dungeons.dungeonMapConfig.nameOffset;

            float nameWidth = stringWidth * scale;
            float nameX = cx - nameWidth / 2f;

            GlStateManager.translate(nameX, cy, 0f);
            GlStateManager.scale(scale, scale, scale);
            mc.fontRendererObj.drawString(name, 0, 0, 0xFFFFFFFF);
        }

        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.popMatrix();
    }

    public static void renderPlayerHead(float x, float y, int color, float scale, NetworkPlayerInfo info, float rotation) {
        int alpha = (color >> 24) & 0xFF;
        float headAlpha = (alpha == 0) ? 1.0f : alpha / 255f;
        Minecraft mc = Minecraft.getMinecraft();
        GlStateManager.enableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.pushMatrix();
        float half = (scale * 8f) / 2f;
        float cx = x + half;
        float cy = (y - 1f) + half;
        GlStateManager.translate(cx, cy, 0f);
        GlStateManager.rotate(rotation, 0f, 0f, 1f);
        GlStateManager.translate(-cx, -cy, 0f);
        mc.getTextureManager().bindTexture(info.getLocationSkin());
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1.0f, 1.0f, 1.0f, headAlpha);
        Gui.drawScaledCustomSizeModalRect((int) x, (int) (y - 1f), 8f, 8f, 8, 8, (int) (scale * 8), (int) (scale * 8), 64f, 64f);
        Gui.drawScaledCustomSizeModalRect((int) x, (int) (y - 1f), 40f, 8f, 8, 8, (int) (scale * 8), (int) (scale * 8), 64f, 64f);
        GlStateManager.popMatrix();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
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

        DungeonMapGrid.cellSizeBlocks = ATHRConfig.feature.dungeons.dungeonMapConfig.cellSizeBlocks;

        if ((playerTracker.getPlayerNames().isEmpty() || player.ticksExisted - lastPopulateTick >= 40) && player.ticksExisted % 20 == 0) {
            playerTracker.populate();
            lastPopulateTick = player.ticksExisted;
            if (ATHRConfig.feature.debug.dungeonMapDebug) {
                ChatUtils.sendMessage("§7[§6MapDebug§7] Populated " + playerTracker.getPlayerNames().size() + " players: " +
                    String.join(", ", playerTracker.getPlayerNames()));
            }
        }

        MapData info = getDungeonMap(player);
        if (info != null) {
            if (!Arrays.equals(info.colors, lastMapColors)) {
                lastMapColors = Arrays.copyOf(info.colors, info.colors.length);
                cachedGrid = DungeonMapGrid.parse(info);
                if (cachedGrid.isValid()) {
                    if (!spawnRecorded && DungeonRoomDetector.roomBoundsValid && DungeonRoomDetector.originBlock != null) {
                        entranceCenterX = (DungeonRoomDetector.roomMinX + DungeonRoomDetector.roomMaxX) / 2.0 + 0.5;
                        entranceCenterZ = (DungeonRoomDetector.roomMinZ + DungeonRoomDetector.roomMaxZ) / 2.0 + 0.5;
                        DungeonMapGrid.worldOriginX = DungeonMapGrid.entrancePixelCenterX / DungeonMapGrid.blockToPixel - (float) entranceCenterX;
                        DungeonMapGrid.worldOriginZ = DungeonMapGrid.entrancePixelCenterZ / DungeonMapGrid.blockToPixel - (float) entranceCenterZ;
                        spawnRecorded = true;
                    }
                    if (spawnRecorded) {
                        DungeonMapGrid.worldOriginX = DungeonMapGrid.entrancePixelCenterX / DungeonMapGrid.blockToPixel - (float) entranceCenterX;
                        DungeonMapGrid.worldOriginZ = DungeonMapGrid.entrancePixelCenterZ / DungeonMapGrid.blockToPixel - (float) entranceCenterZ;
                    } else {
                        DungeonMapGrid.worldOriginX = DungeonMapGrid.entrancePixelCenterX / DungeonMapGrid.blockToPixel - (float) player.posX;
                        DungeonMapGrid.worldOriginZ = DungeonMapGrid.entrancePixelCenterZ / DungeonMapGrid.blockToPixel - (float) player.posZ;
                    }
                    int maxPixelX = 128;
                    int maxPixelY = 128;
                    for (Map.Entry<DungeonMapGrid.RoomOffset, DungeonMapGrid.RoomCell> entry : cachedGrid.getRooms().entrySet()) {
                        int rx = (int) cachedGrid.gridToPixelX(entry.getKey().x) + cachedGrid.getRoomPixelSize();
                        int ry = (int) cachedGrid.gridToPixelZ(entry.getKey().y) + cachedGrid.getRoomPixelSize();
                        if (rx > maxPixelX) maxPixelX = rx;
                        if (ry > maxPixelY) maxPixelY = ry;
                    }
                    lastW = maxPixelX;
                    lastH = maxPixelY;
                } else if (ATHRConfig.feature.debug.dungeonMapDebug) {
                    ChatUtils.sendMessage("§7[§6MapDebug§7] " + cachedGrid.debugInfo);
                }
            }
        } else if (ATHRConfig.feature.debug.dungeonMapDebug) {
            ChatUtils.sendMessage("§7[§6MapDebug§7] getDungeonMap() returned null");
        }

        if (ATHRConfig.feature.debug.dungeonMapDebug && player.ticksExisted % 20 == 0) {
            for (String name : playerTracker.getPlayerNames()) {
                float[] pos = playerTracker.getPosition(name);
                String pstr = (pos != null) ? String.format("%.1f,%.1f", pos[0], pos[1]) : "no pos";
                EntityPlayer ep = playerTracker.getEntity(name);
                String estate = (ep != null && !ep.isDead) ? "alive" : "decor";
                ChatUtils.sendMessage("§7[§6MapDebug§7] " + name +
                    " | pixel: " + pstr +
                    " | src: " + estate +
                    " | origin: " + String.format("%.1f", DungeonMapGrid.worldOriginX) + "," + String.format("%.1f", DungeonMapGrid.worldOriginZ));
            }
        }

        float scale = getScale();
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        Position pos = getPosition();

        int gridW = lastW;
        int gridH = lastH;

        int cx = pos.getAbsX(sr, gridW);
        int cy = pos.getAbsY(sr, gridH);
        if (pos.isCenterX()) cx += gridW / 2;
        if (pos.isCenterY()) cy += gridH / 2;

        int bgColor = getBgColor();
        if ((bgColor >>> 24) != 0) {
            Overlay.drawRoundedRect(cx - gridW / 2 - 4, cy - gridH / 2 - 4, cx + gridW / 2 + 4, cy + gridH / 2 + 4, getCornerRadius(), bgColor);
        }

        if (preview) {
            String txt = "Preview Map";
            int tw = Minecraft.getMinecraft().fontRendererObj.getStringWidth(txt);
            Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(txt, cx - tw / 2f, cy - 4f, 0xFFFFFFFF);
        } else if (cachedGrid != null && cachedGrid.isValid()) {
            playerTracker.matchDecorations(info != null ? info.mapDecorations : null);
            DungeonMapRenderer.render(cachedGrid, cx, cy, scale, player, playerTracker.getPlayerNames(), playerTracker, DungeonRoomDetector.getVisitedRooms());
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
        return ATHRConfig.feature.dungeons.dungeonMapConfig.scale;
    }

    @Override
    public int getBgColor() {
        return ChromaColour.specialToChromaRGB(ATHRConfig.feature.dungeons.dungeonMapConfig.bgColor);
    }

    @Override
    public int getCornerRadius() {
        return ATHRConfig.feature.dungeons.dungeonMapConfig.cornerRadius;
    }

    @Override
    public boolean isEnabled() {
        return ATHRConfig.feature.dungeons.dungeonMapConfig.enabled;
    }
}
