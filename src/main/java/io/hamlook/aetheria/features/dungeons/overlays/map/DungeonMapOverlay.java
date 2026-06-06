package io.hamlook.aetheria.features.dungeons.overlays.map;

import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.Position;
import io.hamlook.aetheria.utils.data.SkyblockData;
import io.hamlook.aetheria.utils.overlay.Overlay;
import net.minecraft.block.material.MapColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import lombok.Getter;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Vec4b;
import net.minecraft.world.storage.MapData;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RegisterEvents
public class DungeonMapOverlay extends Overlay {

    @Getter
    private static DungeonMapOverlay instance;

    public DungeonMapOverlay() {
        super(128,128);
        instance = this;
    }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (ATHRConfig.feature == null || !isEnabled()) return;
        render(false);
    }

    @Override
    public void render(boolean preview) {
        if (!preview && !SkyblockData.isInDungeon()) return;
        MapData info = getDungeonMap(Minecraft.getMinecraft().thePlayer);
        if (info == null) return;

        final int baseSize = 128;

        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        Position pos = getPosition();
        int x = pos.getAbsX(sr, (int) (baseSize * getScale()));
        int y = pos.getAbsY(sr, (int) (baseSize * getScale()));
        if (pos.isCenterX()) x -= (int) (baseSize * getScale() / 2);
        if (pos.isCenterY()) y -= (int) (baseSize * getScale() / 2);

        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0f);
        GL11.glScalef(getScale(), getScale(), 1f);

        int bgColor = getBgColor();
        if ((bgColor >>> 24) != 0) {
            Overlay.drawRoundedRect(-3, -3, baseSize, baseSize - 3, getCornerRadius(), bgColor);
        }

        if (preview) {
            Minecraft mc = Minecraft.getMinecraft();
            String txt = "Preview Map";
            int tw = mc.fontRendererObj.getStringWidth(txt);
            int th = mc.fontRendererObj.FONT_HEIGHT;
            mc.fontRendererObj.drawStringWithShadow(txt, (baseSize - tw) / 2f, (baseSize - th) / 2f, 0xFFFFFFFF);
        } else {
            drawDungeonMap(0, 0, baseSize, baseSize, info);
            List<EntityPlayerSP> players = Minecraft.getMinecraft().theWorld.getPlayers(
                    EntityPlayerSP.class, Objects::nonNull
            );
            if (players.isEmpty()) return;
            for (EntityPlayerSP playerSP : players) {
                int worldX = -1 * (playerSP.getPosition().getX() + 6);
                int worldZ = -1 * (playerSP.getPosition().getZ() + 6);

                float pixelX = baseSize - ((worldX / 186f) * baseSize);
                float pixelZ = baseSize - ((worldZ / 186f) * baseSize);

                if (ATHRConfig.feature.dungeons.dungeonMapConfig.showPlayerHead) {
                    renderPlayerHead(pixelX, pixelZ, -1, (getScale() * getHeadScale()), new NetworkPlayerInfo(playerSP.getGameProfile()), playerSP.rotationYaw);
                }
                if (ATHRConfig.feature.dungeons.dungeonMapConfig.showPlayerUsername) {
                    String name = playerSP.getDisplayName().getFormattedText();
                    if(!ATHRConfig.feature.dungeons.dungeonMapConfig.showPlayerRank){
                        name = name.substring(name.indexOf("]")+1).trim();
                    }
                    renderPlayerName(pixelX, pixelZ + ((getScale() * getHeadScale()) * 12), -1, (getScale() * getHeadScale()),(getScale() * ATHRConfig.feature.dungeons.dungeonMapConfig.nameSize), name);
                }
            }
            if(!ATHRConfig.feature.dungeons.dungeonMapConfig.showPlayerHead){
                drawMarkers(info.mapDecorations);
            }
        }

        GL11.glPopMatrix();
    }


    /*
    <p>Taken from Minecraft's ItemMapRenderer.java & Modified to work for Overlay
     */
    private void drawMarkers(Map<String, Vec4b> decorations) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        Minecraft.getMinecraft().getTextureManager().bindTexture(Resources.DEFAULT_MAP_ICONS);
        int layer = 0;
        for (Vec4b decoration : decorations.values()) {
                            if (decoration.func_176110_a() == 1) {
                GlStateManager.pushMatrix();
                GlStateManager.translate((float)decoration.func_176112_b() / 2.0F + 64.0F, decoration.func_176113_c() / 2.0F + 64.0F, 0f);
                GlStateManager.rotate((float)(decoration.func_176111_d() * 360) / 16.0F, 0.0F, 0.0F, 1.0F);
                GlStateManager.scale(6.0F, 6.0F, 4.5F);
                GlStateManager.translate(-0.125F, 0.125F, 0.0F);
                byte iconId = decoration.func_176110_a();
                float uStart = (float)(iconId % 4) / 4.0F;
                float vStart = (float)(iconId / 4) / 4.0F;
                float uEnd = (float)(iconId % 4 + 1) / 4.0F;
                float vEnd = (float)(iconId / 4 + 1) / 4.0F;
                worldRenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
                worldRenderer.pos(-1.0F, 1.0F, (float)layer * -0.001F).tex(uStart, vStart).endVertex();
                worldRenderer.pos(1.0F, 1.0F, (float)layer * -0.001F).tex(uEnd, vStart).endVertex();
                worldRenderer.pos(1.0F, -1.0F, (float)layer * -0.001F).tex(uEnd, vEnd).endVertex();
                worldRenderer.pos(-1.0F, -1.0F, (float)layer * -0.001F).tex(uStart, vEnd).endVertex();
                tessellator.draw();
                GlStateManager.popMatrix();
                ++layer;
            }
        }
    }

    public static void renderPlayerName(float pixelX, float pixelZ, int color, float headScale, float scale, String name) {
        if (name == null || name.isEmpty()) return;
        float headSize = headScale * 8f;
        float half = headSize / 2f;

        float cx = pixelX + half;
        float cy = (pixelZ - headSize) + ATHRConfig.feature.dungeons.dungeonMapConfig.nameOffset;


        Minecraft mc = Minecraft.getMinecraft();
        float nameWidth = mc.fontRendererObj.getStringWidth(name) * scale;
        float nameX = cx - nameWidth / 2f;

        int alpha = (color >> 24) & 0xFF;
        float nameAlpha = (alpha == 0) ? 1.0f : alpha / 255f;
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1.0f, 1.0f, 1.0f, nameAlpha);
        GlStateManager.translate(nameX,cy,0f);
        GlStateManager.scale(scale,scale,scale);
        mc.fontRendererObj.drawString(name, 0,0, 0xFFFFFFFF);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.popMatrix();
    }

    public static void drawDungeonMap(int x, int y, int w, int h, MapData info) {
        if (info == null) return;
        byte[] colors = info.colors;
        for (int ix = 0; ix < w; ix++) {
            for (int iy = 0; iy < h; iy++) {
                int idx = ix + iy * w;
                int colByte = colors[idx] & 0xFF;
                int colour;
                if (colByte / 4 == 0) {
                    colour = 0x00000000;
                } else {
                    colour = MapColor.mapColorArray[colByte / 4].getMapColor(colByte & 3);
                }
                Gui.drawRect(x + ix, y + iy, x + ix + 1, y + iy + 1, colour);
            }
        }
    }

    public static MapData getDungeonMap(EntityPlayerSP player) {
        if (player == null || player.inventory == null) return null;
        ItemStack[] inv = player.inventory.mainInventory;
        if (inv == null || inv.length < 9) return null;
        ItemStack stack = inv[8];
        if (stack == null) return null;
        return Items.filled_map.getMapData(stack, Minecraft.getMinecraft().theWorld);
    }

    public void renderPlayerHead(float x, float y, int color, float scale, NetworkPlayerInfo info, float rotation) {
        int alpha = (color >> 24) & 0xFF;
        float headAlpha = (alpha == 0) ? 1.0f : alpha / 255f;
        Minecraft mc = Minecraft.getMinecraft();
        GlStateManager.enableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        // Apply rotation around the head center
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
        Gui.drawScaledCustomSizeModalRect((int) x, (int) (y - 1f), 8f, 8f, 8, 8, (int)(scale * 8), (int)(scale * 8), 64f, 64f);
        Gui.drawScaledCustomSizeModalRect((int) x, (int) (y - 1f), 40f, 8f, 8, 8, (int)(scale * 8), (int)(scale * 8), 64f, 64f);
        GlStateManager.popMatrix();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
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

    public float getHeadScale() {
        return ATHRConfig.feature.dungeons.dungeonMapConfig.headScale * 1.25f;
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

