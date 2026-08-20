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
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.MapData;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.util.*;

@RegisterEvents
public class DungeonMapOverlay extends Overlay {

    // Vanilla's own map icon atlas — same texture the vanilla map item and NEF's dungeon map use.
    // Icon 0 = small colorable arrow (teammates), icon 1 = larger arrow (self position).
    private static final ResourceLocation MAP_ICONS_TEXTURE = new ResourceLocation("textures/map/map_icons.png");

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
            float mapScale = Math.max(ATHRConfig.feature.dungeons.dungeonMapConfig.appearance.scale, 0.01f);
            float cy = (pixelZ - headSize) + ATHRConfig.feature.dungeons.dungeonMapConfig.players.nameOffset / mapScale;

            float nameWidth = stringWidth * scale;
            float nameX = cx - nameWidth / 2f;

            GlStateManager.translate(nameX, cy, 0f);
            GlStateManager.scale(scale, scale, scale);
            mc.fontRendererObj.drawString(name, 0, 0, 0xFFFFFFFF);
        }

        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.popMatrix();
    }

    public static void renderRoomName(float pixelX, float pixelZ, float scale, String name, int color) {
        if (name == null || name.isEmpty()) return;
        Minecraft mc = Minecraft.getMinecraft();
        String[] words = name.split(" ");
        if (words.length == 0) return;
        int fontHeight = mc.fontRendererObj.FONT_HEIGHT + 1;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.translate(pixelX, pixelZ, 0f);
        GlStateManager.scale(scale, scale, 1.0f);
        float yTextOffset = words.length * fontHeight / -2f;
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            mc.fontRendererObj.drawString(word, (int) (-mc.fontRendererObj.getStringWidth(word) / 2f), (int) (yTextOffset + i * fontHeight), color, true);
        }
        GlStateManager.popMatrix();
    }

    public static void renderPlayerHead(float x, float y, int color, float scale, ResourceLocation skin, float rotation) {
        if (skin == null) {
            skin = DefaultPlayerSkin.getDefaultSkinLegacy();
        }
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
        mc.getTextureManager().bindTexture(skin);
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1.0f, 1.0f, 1.0f, headAlpha);
        Gui.drawScaledCustomSizeModalRect((int) x, (int) (y - 1f), 8f, 8f, 8, 8, (int) (scale * 8), (int) (scale * 8), 64f, 64f);
        Gui.drawScaledCustomSizeModalRect((int) x, (int) (y - 1f), 40f, 8f, 8, 8, (int) (scale * 8), (int) (scale * 8), 64f, 64f);
        GlStateManager.popMatrix();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public static void renderPlayerArrow(float x, float y, float scale, float rotation, int rgbColor, boolean isSelf) {
        Minecraft mc = Minecraft.getMinecraft();
        float size = scale * 8f;
        float half = size / 2f;
        float cx = x + half;
        float cy = (y - 1f) + half;

        GlStateManager.pushMatrix();
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        GlStateManager.translate(cx, cy, 0f);
        GlStateManager.rotate(rotation + 180f, 0f, 0f, 1f);

        mc.getTextureManager().bindTexture(MAP_ICONS_TEXTURE);

        int iconType = isSelf ? 1 : 0;
        float u0 = (iconType % 4) / 4f;
        float v0 = (iconType / 4) / 4f;
        float u1 = (iconType % 4 + 1) / 4f;
        float v1 = (iconType / 4 + 1) / 4f;

        int alphaByte = (rgbColor >>> 24) & 0xFF;
        float a = (alphaByte == 0) ? 1.0f : alphaByte / 255f;
        float r = ((rgbColor >> 16) & 0xFF) / 255f;
        float g = ((rgbColor >> 8) & 0xFF) / 255f;
        float b = (rgbColor & 0xFF) / 255f;
        GlStateManager.color(r, g, b, a);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        worldrenderer.pos(-half, half, 0.0D).tex(u0, v1).endVertex();
        worldrenderer.pos(half, half, 0.0D).tex(u1, v1).endVertex();
        worldrenderer.pos(half, -half, 0.0D).tex(u1, v0).endVertex();
        worldrenderer.pos(-half, -half, 0.0D).tex(u0, v0).endVertex();
        tessellator.draw();

        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.popMatrix();
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
        // Player marker positions are now sourced from each tracked EntityPlayer's
        // real world coordinates (see DungeonPlayerTracker.getPixelPosition), so the
        // map's own decoration bytes (which carry no player identity) no longer need
        // to be matched against playerTracker at all.
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