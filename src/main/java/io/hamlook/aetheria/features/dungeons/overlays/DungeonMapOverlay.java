package io.hamlook.aetheria.features.dungeons.overlays.map;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.Position;
import io.hamlook.aetheria.utils.data.SkyblockData;
import io.hamlook.aetheria.utils.overlay.Overlay;
import net.minecraft.block.material.MapColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import lombok.Getter;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec4b;
import net.minecraft.world.storage.MapData;
import io.hamlook.aetheria.features.dungeons.utils.dung.DungeonRoomDetector;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.util.*;
import java.util.regex.Pattern;

import io.hamlook.aetheria.features.dungeons.utils.dung.DungeonRoom;

@RegisterEvents
public class DungeonMapOverlay extends Overlay {

    @Getter
    private static DungeonMapOverlay instance;
    private static DynamicTexture map = new DynamicTexture(128,128);
    private static ResourceLocation mapTexture;
    private static byte[] lastMapColors = null;
    private static int ticks = 0;

    public static boolean dungeonRunEnded = false;

    public static final List<EntityPlayer> players = new ArrayList<>();
    public static Pattern PLAYER_REGEX = Pattern.compile("^\\[\\d+]\\s(?:\\[\\S+]\\s)?\\S+$");
    public DungeonMapOverlay() {
        super(128,128);
        instance = this;
        mapTexture = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("dungeon_map",map);
    }

    @SubscribeEvent
    public void onUnload(WorldEvent.Unload e){

        if(map != null) map = new DynamicTexture(128,128);
        mapTexture = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("dungeon_map",map);
        lastMapColors = null;
        ticks = 0;
        players.clear();
        DungeonRoomDetector.getVisitedRooms().clear();
        dungeonRunEnded = false;
    }

    @Override
    public void render(boolean preview) {
        if (!preview && !SkyblockData.isInDungeon() || dungeonRunEnded) return;
        MapData info = getDungeonMap(Minecraft.getMinecraft().thePlayer);
        if (info == null){
            if(players.isEmpty()) populatePlayers();
            return;
        }

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
            ticks++;
            drawDungeonMap(0, 0, baseSize, baseSize, info);
            if (players.isEmpty() && ticks % 20 == 0) {
                populatePlayers();
            }
            if (!players.isEmpty()) {
                for (EntityPlayer player : players) {
                    int worldX = -1 * (player.getPosition().getX() + 6);
                    int worldZ = -1 * (player.getPosition().getZ() + 6);

                    float pixelX = baseSize - ((worldX / 186f) * baseSize);
                    float pixelZ = baseSize - ((worldZ / 186f) * baseSize);

                    if (ATHRConfig.feature.dungeons.dungeonMapConfig.showPlayerHead) {
                        renderPlayerHead(pixelX, pixelZ, -1, (getScale() * getHeadScale()), new NetworkPlayerInfo(player.getGameProfile()), player.rotationYaw);
                    }
                    if (ATHRConfig.feature.dungeons.dungeonMapConfig.showPlayerUsername) {
                        String name = player.getDisplayName().getFormattedText();
                        if (!ATHRConfig.feature.dungeons.dungeonMapConfig.showPlayerRank) {
                            name = name.substring(name.indexOf("]") + 1).trim();
                        }
                        renderName(pixelX, pixelZ + ((getScale() * getHeadScale()) * 12), -1, (getScale() * getHeadScale()), (getScale() * ATHRConfig.feature.dungeons.dungeonMapConfig.nameSize * 0.75f), name,false);
                    }
                }
            }
            if (!ATHRConfig.feature.dungeons.dungeonMapConfig.showPlayerHead) {
                drawMarkers(info.mapDecorations);
            }
            if (ATHRConfig.feature.dungeons.dungeonMapConfig.showVisitedRoomNames) {
                Collection<DungeonRoom> rooms = DungeonRoomDetector.getVisitedRooms();
                for (DungeonRoom dr : rooms) {
                    int worldX = -1 * (dr.center.getX() + 6);
                    int worldZ = -1 * (dr.center.getZ() + 6);
                    float pixelX = baseSize - ((worldX / 186f) * baseSize);
                    float pixelZ = baseSize - ((worldZ / 186f) * baseSize);
                    renderName(pixelX, pixelZ, -1, 0f,
                            getScale() * ATHRConfig.feature.dungeons.dungeonMapConfig.roomnameSize* 0.75f,
                            dr.name,true);
                }
            }
        }

        GL11.glPopMatrix();
    }

    public void populatePlayers() {
        players.clear();
        Minecraft mc = Minecraft.getMinecraft();
        GuiPlayerTabOverlay tab = mc.ingameGUI.getTabList();
        List<NetworkPlayerInfo> infos = mc.thePlayer.sendQueue.getPlayerInfoMap().stream().sorted((a, b) -> {
            String ta = a.getPlayerTeam() != null ? a.getPlayerTeam().getRegisteredName() : "";
            String tb = b.getPlayerTeam() != null ? b.getPlayerTeam().getRegisteredName() : "";
            int cmp = ta.compareTo(tb);
            return cmp != 0 ? cmp : a.getGameProfile().getName().compareTo(b.getGameProfile().getName());
        }).collect(java.util.stream.Collectors.toList());
        if(infos.isEmpty()){
            Aetheria.logger.info("Empty Tab List??");
        }
        for (NetworkPlayerInfo info : infos) {
            String raw = tab.getPlayerName(info);
            String stripped = ColorUtils.stripColor(raw != null ? raw : "").trim();
            if(stripped.isEmpty()){
                continue;
            }
            if(PLAYER_REGEX.matcher(stripped).lookingAt()) {
                String[] words = stripped.split("\\s+");
                String username;
                if (words.length >= 3) {
                    username = words[2];
                } else {
                    username = words[1];
                }

                if(username.isEmpty()){
                    Aetheria.logger.info("Empty Username: " + stripped);
                    continue;
                }
                EntityPlayer player = Minecraft.getMinecraft().theWorld.getPlayerEntityByName(username);
                if(player == null){
                    Aetheria.logger.info("Player Null for: " + username + " | " + stripped);
                    continue;
                }
                players.add(player);
            }
            if(stripped.contains("nth_to_smth")){
                Aetheria.logger.info("Regex is wrong: " + stripped);
            }
        }
    }

    private List<String> getScoreboardLines() {
        Minecraft mc = Minecraft.getMinecraft();
        if(mc.theWorld == null || mc.theWorld.getScoreboard() == null) return new ArrayList<>();

        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
        if(objective == null) return new ArrayList<>();
        List<String> lines = new ArrayList<>();
        for(Score score : scoreboard.getSortedScores(objective)){
            String playerName = score.getPlayerName();
            lines.add(EnumChatFormatting.getTextWithoutFormattingCodes(playerName));
        }
        return lines;
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

    public static void drawDungeonMap(int x, int y, int w, int h, MapData info) {
        if (info == null) return;
        if (!Arrays.equals(info.colors, lastMapColors) || ticks % 20 == 0) {
            lastMapColors = Arrays.copyOf(info.colors, info.colors.length);
            byte[] colors = info.colors;
            int[] pixels = map.getTextureData();
            for (int i = 0; i < 16384; i++) {
                int colByte = colors[i] & 0xFF;
                if (colByte / 4 == 0) continue;
                int colour = MapColor.mapColorArray[colByte / 4].getMapColor(colByte & 3);
                pixels[i] = colour;
            }
            map.updateDynamicTexture();
        }
        GlStateManager.pushMatrix();
        Minecraft.getMinecraft().getTextureManager().bindTexture(mapTexture);
        GlStateManager.color(1f,1f,1f,1f);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0f, 0f, w, h, 128, 128);
        GlStateManager.popMatrix();
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

