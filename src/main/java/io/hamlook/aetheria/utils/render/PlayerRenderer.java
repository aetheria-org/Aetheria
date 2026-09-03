package io.hamlook.aetheria.utils.render;

import com.mojang.authlib.GameProfile;
import io.hamlook.aetheria.features.capes.Cape;
import io.hamlook.aetheria.features.capes.CapeManager;
import io.hamlook.aetheria.features.profile.viewer.SkinManager;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.OpenGlHelper;
import io.hamlook.aetheria.utils.compat.RenderHelperCompat;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.UUID;

public class PlayerRenderer {

    public static HashMap<String,AbstractClientPlayer> cachedModels = new HashMap<>();

    public static void renderPlayer(String username,int posX,int posY,int scale,float mX,float mY){
        renderPlayer(username,posX,posY,scale,mX,mY,true);
    }

    public static void renderPlayer(String username,int posX,int posY,int scale,float mX,float mY,boolean nametag){
        AbstractClientPlayer player;
        if(cachedModels.containsKey(username)){
            player = cachedModels.get(username);
        }else {
            player = new AbstractClientPlayer(MinecraftCompat.getMinecraft().theWorld,
                    new GameProfile(UUID.nameUUIDFromBytes((username).getBytes()), username)) {
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
            cachedModels.put(username,player);
        }
        if(!nametag){
            player.posX = 9999999.0D;
            player.posY = 9999999.0D;
            player.posZ = 9999999.0D;
        }
        drawEntityOnScreenSmooth(posX,posY,scale,mX,mY,player);
    }
    public static void renderPlayer(AbstractClientPlayer player, int posX, int posY, int scale, float mX, float mY){
        drawEntityOnScreenSmooth(posX,posY,scale,mX,mY,player);
    }

    public static void drawEntityOnScreenSmooth(int posX, int posY, int scale, float mouseX, float mouseY, EntityLivingBase ent) {
        GlStateManagerCompat.enableColorMaterial();
        GlStateManagerCompat.pushMatrix();
        GlStateManagerCompat.translate((float)posX, (float)posY, 50.0F);
        GlStateManagerCompat.scale((float)(-scale), (float)scale, (float)scale);
        GlStateManagerCompat.rotate(180.0F, 0.0F, 0.0F, 1.0F);

        float f = (float)posX - mouseX;
        float f1 = (float)posY - 50.0F - mouseY;

        GlStateManagerCompat.rotate(135.0F, 0.0F, 1.0F, 0.0F);
        RenderHelperCompat.enableStandardItemLighting();
        GlStateManagerCompat.rotate(-135.0F, 0.0F, 1.0F, 0.0F);

        GlStateManagerCompat.rotate(-((float)Math.atan((f / 40.0F))) * 20.0F, 0.0F, 1.0F, 0.0F);
        ent.renderYawOffset = ((float)Math.atan((f / 40.0F)) * 20.0F);
        ent.rotationYaw = ((float)Math.atan((f / 40.0F)) * 40.0F);
        ent.rotationPitch = -((float)Math.atan((f1 / 40.0F))) * 20.0F;
        ent.rotationYawHead = ent.rotationYaw;
        ent.prevRotationYawHead = ent.rotationYaw;

        GlStateManagerCompat.translate(0.0F, 0.0F, 0.0F);
        RenderManager rendermanager = MinecraftCompat.getMinecraft().getRenderManager();
        rendermanager.setPlayerViewY(180.0F);
        rendermanager.setRenderShadow(false);

        GlStateManagerCompat.clear(GL11.GL_DEPTH_BUFFER_BIT);
        GlStateManagerCompat.color(1.0F, 1.0F, 1.0F, 1.0F);

        rendermanager.renderEntityWithPosYaw(ent, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F);
        rendermanager.setRenderShadow(true);
        ent.renderYawOffset = 0;
        ent.rotationYaw = 0;
        ent.rotationPitch = 0;
        ent.prevRotationYawHead = 0;
        ent.rotationYawHead = 0;
        GlStateManagerCompat.popMatrix();
        RenderHelperCompat.disableStandardItemLighting();
        GlStateManagerCompat.disableRescaleNormal();
        GlStateManagerCompat.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManagerCompat.disableTexture2D();
        GlStateManagerCompat.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

}
