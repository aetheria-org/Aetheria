package com.jef.justenoughfakepixel.features.profile.viewer.ui.modules;

import com.jef.justenoughfakepixel.features.capes.Cape;
import com.jef.justenoughfakepixel.features.capes.CapeManager;
import com.jef.justenoughfakepixel.features.profile.viewer.SkinManager;
import com.jef.justenoughfakepixel.utils.render.ResolutionUtils;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class PlayerModule {

    public static AbstractClientPlayer playerModel;

    public static void draw(int boxX,int boxY,int boxW,int boxH,float uiScale,Minecraft mc,String username,int mouseX,int mouseY,boolean showCape, boolean showArmor){

        int pad10 = (int) (10 * uiScale);
        int pad25 = (int) (25 * uiScale);
        int pad2 = (int) (2 * uiScale);

        int holeX = boxX + pad10;
        int holeY = boxY + pad25;
        int holeW = boxW / 5;
        int holeH = (int) (holeW * 1.77777777778);
        int baseDataH = (boxY + boxH) - (holeY + holeH + pad2) - pad10;
        Gui.drawRect(holeX, holeY, holeX + holeW, holeY + holeH, 0xFF1A1A1A);

        float displayScale = ResolutionUtils.getXStatic(1);
        int scale = (int) (100 * (uiScale * displayScale));
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

        drawEntityOnScreenSmooth(playerX, playerY, scale, mouseX, mouseY, playerModel,showCape);
    }

    public static void drawEntityOnScreenSmooth(int posX, int posY, int scale, float mouseX, float mouseY, EntityLivingBase ent, boolean showCape) {
        GlStateManager.enableColorMaterial();
        GlStateManager.pushMatrix();
        GlStateManager.translate((float)posX, (float)posY, 50.0F);
        GlStateManager.scale((float)(-scale), (float)scale, (float)scale);
        GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);

        float f = (float)posX - mouseX;
        float f1 = (float)posY - 50.0F - mouseY;
        float capeYawOffset = showCape ? 180.0F : 0.0F;

        GlStateManager.rotate(135.0F, 0.0F, 1.0F, 0.0F);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.rotate(-135.0F, 0.0F, 1.0F, 0.0F);

        GlStateManager.rotate(-((float)Math.atan((f / 40.0F))) * 20.0F, 0.0F, 1.0F, 0.0F);
        ent.renderYawOffset = ((float)Math.atan((f / 40.0F)) * 20.0F) + capeYawOffset;
        ent.rotationYaw = ((float)Math.atan((f / 40.0F)) * 40.0F) + capeYawOffset;
        ent.rotationPitch = showCape ? 0 : -((float)Math.atan((f1 / 40.0F))) * 20.0F;
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
