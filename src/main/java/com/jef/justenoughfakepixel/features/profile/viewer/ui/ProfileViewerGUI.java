package com.jef.justenoughfakepixel.features.profile.viewer.ui;

import com.jef.justenoughfakepixel.core.JefConfig;
import com.jef.justenoughfakepixel.core.config.gui.GuiTextures;
import com.jef.justenoughfakepixel.features.capes.Cape;
import com.jef.justenoughfakepixel.features.capes.CapeManager;
import com.jef.justenoughfakepixel.features.profile.data.ProfileData;
import com.jef.justenoughfakepixel.features.profile.viewer.PlayerProfile;
import com.jef.justenoughfakepixel.features.profile.viewer.ProfileViewerAPI;
import com.jef.justenoughfakepixel.features.profile.viewer.SkinManager;
import com.jef.justenoughfakepixel.features.profile.viewer.ui.modules.BaseDataModule;
import com.jef.justenoughfakepixel.utils.render.NineSliceUtils;
import com.jef.justenoughfakepixel.utils.render.ResolutionUtils;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class ProfileViewerGUI extends GuiScreen {

    // UI Data
    private static final ResourceLocation CONTAINER_BG = GuiTextures.CAPES_UI;
    private static float uiScale = 1f;
    private static int page = 0;
    private int boxW;
    private int boxH;
    private int boxX;
    private int boxY;
    private int pad10,pad25,pad2;

    // Player Data
    public String username;
    public PlayerProfile playerProfile;
    public ProfileData activeProfileData;
    public AbstractClientPlayer playerModel;

    // State Trackers
    public boolean isFetching = true;
    public boolean hasError = false;

    public ProfileViewerGUI(String username) {
        this.username = username;

        new Thread(() -> {
            try {
                if (ProfileViewerAPI.profileHashMap.containsKey(username)) {
                    this.playerProfile = ProfileViewerAPI.profileHashMap.get(username);
                } else {
                    this.playerProfile = ProfileViewerAPI.fetchUser(username);
                    if (this.playerProfile != null) {
                        ProfileViewerAPI.profileHashMap.put(username, this.playerProfile);
                    }
                }

                if (this.playerProfile != null && this.playerProfile.profiles != null && !this.playerProfile.profiles.isEmpty()) {
                    this.activeProfileData = this.playerProfile.profiles.get(0);
                }
            } catch (Exception e) {
                e.printStackTrace();
                this.hasError = true;
            } finally {
                this.isFetching = false;
            }
        }, "JEF-GUI-FetchThread").start();
    }

    @Override
    public void initGui() {
        super.initGui();
        uiScale = JefConfig.feature.overlays.profileViewer.pvScale;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {

        boxW = (int) Math.min((Minecraft.getMinecraft().displayWidth * 95f)/100f,
                ResolutionUtils.getXStatic((int)(1200 * uiScale)));
        boxH = (int)(boxW * 9/16.0);
        boxX = (this.width / 2) - (boxW / 2);
        boxY = (this.height / 2) - (boxH / 2);

        NineSliceUtils.draw(CONTAINER_BG, boxX, boxY, boxW, boxH, 6, 18);

        int centerX = boxX + (boxW / 2);
        int centerY = boxY + (boxH / 2);

        if (isFetching) {
            String text = "Fetching data...";
            int textWidth = fontRendererObj.getStringWidth(text);
            drawString(fontRendererObj, text, centerX - (textWidth / 2), centerY - (fontRendererObj.FONT_HEIGHT / 2), 0xFFFFAA00); // Yellow/Orange

        } else if (hasError) {
            String text = "An error occurred while fetching!";
            int textWidth = fontRendererObj.getStringWidth(text);
            drawString(fontRendererObj, text, centerX - (textWidth / 2), centerY - (fontRendererObj.FONT_HEIGHT / 2), 0xFFFF5555); // Light Red

        } else if (this.playerProfile == null) {
            String text = this.username + " (Not In Database)";
            int textWidth = fontRendererObj.getStringWidth(text);
            drawString(fontRendererObj, text, centerX - (textWidth / 2), centerY - (fontRendererObj.FONT_HEIGHT / 2), 0xFFAAAAAA); // Gray

        } else {
            pad10 = (int) (10 * uiScale);
            pad25 = (int) (25 * uiScale);
            pad2 = (int) (2 * uiScale);
            drawString(fontRendererObj, this.playerProfile.player_name + " §8(Fetched)", boxX + pad10, boxY + pad10, 0xFF55FF55); // Green
            if (page == 0) {
                drawPageZero(mouseX,mouseY);
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    public void drawPageZero(int mouseX,int mouseY){
        int holeX = boxX + pad10;
        int holeY = boxY + pad25;
        int holeW = boxW / 5;
        int holeH = (int) (holeW * 1.77777777778);
        int baseDataH = (boxY + boxH) - (holeY + holeH + pad2) - pad10;
        drawRect(holeX, holeY, holeX + holeW, holeY + holeH, 0xFF1A1A1A);

        int scale = (int) (65 * uiScale);
        int playerX = holeX + (holeW / 2);
        int playerY = (int) (holeY + (holeH / 2.0) + (scale * 0.9));

        if (playerModel == null) {
            playerModel = new AbstractClientPlayer(mc.theWorld,
                    new GameProfile(UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8)), username)) {

                @Override
                public ResourceLocation getLocationSkin() {
                    return SkinManager.getSkin(username);
                }

                @Override
                public ResourceLocation getLocationCape() {
                    Cape cape = CapeManager.getCapeForPlayer(username);
                    return cape == null ? super.getLocationCape() : cape.resourceLocation;
                }
            };

            playerModel.posX = 9999999.0D;
            playerModel.posY = 9999999.0D;
            playerModel.posZ = 9999999.0D;
        }

        drawEntityOnScreenSmooth(playerX, playerY, scale, mouseX, mouseY, this.playerModel);

        // TODO: Draw the rest of fetched profile UI
        BaseDataModule.draw(this.activeProfileData.baseData, holeX + holeW + 2,
                holeY, (holeW *2), holeH, uiScale);
    }

    public static void drawEntityOnScreenSmooth(int posX, int posY, int scale, float mouseX, float mouseY, EntityLivingBase ent) {
        GlStateManager.enableColorMaterial();
        GlStateManager.pushMatrix();
        GlStateManager.translate((float)posX, (float)posY, 50.0F);
        GlStateManager.scale((float)(-scale), (float)scale, (float)scale);
        GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);

        float f = (float)posX - mouseX;
        float f1 = (float)posY - 50.0F - mouseY;
        GlStateManager.rotate(135.0F, 0.0F, 1.0F, 0.0F);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.rotate(-135.0F, 0.0F, 1.0F, 0.0F);

        GlStateManager.rotate(-((float)Math.atan((f / 40.0F))) * 20.0F, 0.0F, 1.0F, 0.0F);
        ent.renderYawOffset = (float)Math.atan((f / 40.0F)) * 20.0F;
        ent.rotationYaw = (float)Math.atan((f / 40.0F)) * 40.0F;
        ent.rotationPitch = -((float)Math.atan((f1 / 40.0F))) * 20.0F;
        ent.rotationYawHead = ent.rotationYaw;
        ent.prevRotationYawHead = ent.rotationYaw;

        GlStateManager.translate(0.0F, 0.0F, 0.0F);
        RenderManager rendermanager = Minecraft.getMinecraft().getRenderManager();
        rendermanager.setPlayerViewY(180.0F);
        rendermanager.setRenderShadow(false);

        GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        rendermanager.renderEntityWithPosYaw(ent, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F);
        rendermanager.setRenderShadow(true);
        ent.renderYawOffset = 0;
        ent.rotationYaw = 0;
        ent.rotationPitch = 0;
        ent.prevRotationYawHead = 0;
        ent.rotationYawHead = 0;
        GlStateManager.popMatrix();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }
}