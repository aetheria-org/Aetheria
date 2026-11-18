package io.hamlook.aetheria.utils.render;

import com.mojang.authlib.GameProfile;
import io.hamlook.aetheria.features.capes.Cape;
import io.hamlook.aetheria.features.capes.CapeManager;
import io.hamlook.aetheria.utils.SkinManager;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.RenderHelperCompat;
import net.minecraft.client.entity.AbstractClientPlayer;
//#if MC < 1.21
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.OpenGlHelper;
//#else
//$$ import net.minecraft.client.gui.DrawContext;
//$$ import net.minecraft.client.gui.screen.ingame.InventoryScreen;
//$$ import net.minecraft.client.network.AbstractClientPlayerEntity;
//$$ import net.minecraft.client.util.DefaultSkinHelper;
//$$ import net.minecraft.client.util.SkinTextures;
//$$ import net.minecraft.client.world.ClientWorld;
//$$ import net.minecraft.client.render.LightmapTextureManager;
//$$ import net.minecraft.util.math.RotationAxis;
//$$ import net.minecraft.client.util.math.MatrixStack;
//$$ import io.hamlook.aetheria.utils.compat.DrawContextUtils;
//$$ import java.util.Map;
//$$ import java.util.concurrent.ConcurrentHashMap;
//#endif
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLivingBase;
//#if MC < 1.16
import net.minecraft.entity.player.EnumPlayerModelParts;
//#endif
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.UUID;

public class PlayerRenderer {

    public static HashMap<String,AbstractClientPlayer> cachedModels = new HashMap<>();
    private static final ThreadLocal<Cape> capePreview = new ThreadLocal<>();
    private static final ThreadLocal<Float> rotationPreview = new ThreadLocal<>();
    private static final ThreadLocal<Float> partialTickPreview = new ThreadLocal<>();

    /** Render a player model with a temporary cape and an explicit yaw, without equipping it. */
    public static void renderCapePreview(String username, Cape cape, int posX, int posY, int scale,
                                         float yaw, float partialTicks) {
        if (username == null || username.isEmpty() || cape == null) return;
        Cape previousCape = capePreview.get();
        Float previousYaw = rotationPreview.get();
        Float previousPartialTicks = partialTickPreview.get();
        capePreview.set(cape);
        rotationPreview.set(yaw);
        partialTickPreview.set(partialTicks);
        try {
            renderPlayer(username, posX, posY, scale, 0f, 0f, true);
        } finally {
            if (previousCape == null) capePreview.remove();
            else capePreview.set(previousCape);
            if (previousYaw == null) rotationPreview.remove();
            else rotationPreview.set(previousYaw);
            if (previousPartialTicks == null) partialTickPreview.remove();
            else partialTickPreview.set(previousPartialTicks);
        }
    }

    private static Cape getCapeForRender(String username) {
        Cape preview = capePreview.get();
        return preview != null ? preview : CapeManager.getCapeForPlayer(username);
    }

    //#if MC < 1.21
    //#else
    //$$ private static final Map<String, SkinTextures> skinCache = new ConcurrentHashMap<>();
    //$$
    //$$ private static void fetchSkin(String username, GameProfile profile) {
    //$$     if (skinCache.containsKey(username)) return;
    //$$     MinecraftCompat.getMinecraft().getSkinProvider().fetchSkinTextures(profile)
    //$$             .thenAccept(textures -> skinCache.put(username, textures));
    //$$ }
    //#endif

    public static void renderPlayer(String username,int posX,int posY,int scale,float mX,float mY){
        renderPlayer(username,posX,posY,scale,mX,mY,true);
    }

    public static void renderPlayer(String username,int posX,int posY,int scale,float mX,float mY,boolean nametag){
        //#if MC < 1.21
        AbstractClientPlayer player;
        if(cachedModels.containsKey(username)){
            player = cachedModels.get(username);
        }else {
            player = new AbstractClientPlayer(MinecraftCompat.getLocalWorld(),
                    new GameProfile(UUID.nameUUIDFromBytes((username).getBytes()), username)) {
                @Override
                public ResourceLocation getLocationSkin() {
                    if (MinecraftCompat.getLocalPlayer() != null) {
                        return MinecraftCompat.getLocalPlayer().getLocationSkin();
                    }
                    return SkinManager.getSkin(username);
                }

                //#if MC < 1.16
                @Override
                public String getSkinType() {
                    return MinecraftCompat.getLocalPlayer() == null
                            ? "default" : MinecraftCompat.getLocalPlayer().getSkinType();
                }
                //#endif

                @Override
                public ResourceLocation getLocationCape() {
                    Cape cape = getCapeForRender(username);
                    return cape == null ? super.getLocationCape() : cape.resourceLocation;
                }

                @Override
                public boolean hasPlayerInfo() {
                    return true;
                }

                //#if MC < 1.16
                @Override
                public int getBrightnessForRender(float partialTicks) {
                    return 0xF000F0;
                }
                //#endif

                //#if MC < 1.16
                @Override
                public boolean isWearing(EnumPlayerModelParts part) {
                    return part == EnumPlayerModelParts.CAPE || super.isWearing(part);
                }
                //#endif
            };
            cachedModels.put(username,player);
        }

        RenderHelperCompat.enableGUIStandardItemLighting();
        if (rotationPreview.get() != null) drawEntityAtYaw(posX, posY, scale, rotationPreview.get(), partialTickPreview.get(), player);
        else drawEntityOnScreenSmooth(posX,posY,scale,mX,mY,player);
        RenderHelperCompat.disableStandardItemLighting();
        //#else
        //$$ ClientWorld world = MinecraftCompat.getLocalWorld();
        //$$ if (world == null) return;
        //$$ GameProfile profile = MinecraftCompat.getLocalPlayer() != null
        //$$         && username.equalsIgnoreCase(MinecraftCompat.getLocalPlayer().getGameProfile().getName())
        //$$         ? MinecraftCompat.getLocalPlayer().getGameProfile()
        //$$         : new GameProfile(UUID.nameUUIDFromBytes(username.getBytes()), username);
        //$$ AbstractClientPlayerEntity player = cachedModels.get(username);
        //$$ if (player == null) {
        //$$     fetchSkin(username, profile);
        //$$     player = new AbstractClientPlayerEntity(world, profile) {
        //$$         @Override
        //$$         public SkinTextures getSkinTextures() {
        //$$             SkinTextures cached = MinecraftCompat.getLocalPlayer() != null
        //$$                     && username.equalsIgnoreCase(MinecraftCompat.getLocalPlayer().getGameProfile().getName())
        //$$                     ? MinecraftCompat.getLocalPlayer().getSkinTextures() : skinCache.get(username);
        //$$             if (cached == null) cached = DefaultSkinHelper.getSkinTextures(profile);
        //$$             Cape cape = getCapeForRender(username);
        //$$             if (cape == null || cape.resourceLocation == null) return cached;
        //$$             if (cape.resourceLocation.equals(cached.capeTexture())) return cached;
        //$$             return new SkinTextures(cached.texture(), cached.textureUrl(), cape.resourceLocation, cached.elytraTexture(), cached.model(), cached.secure());
        //$$         }
        //$$     };
        //$$     cachedModels.put(username, player);
        //$$ }
        //$$ RenderHelperCompat.enableGUIStandardItemLighting();
        //$$ if (rotationPreview.get() != null) drawEntityAtYaw(posX, posY, scale, rotationPreview.get(), partialTickPreview.get(), player);
        //$$ else drawEntityOnScreenSmooth(posX, posY, scale, mX, mY, player);
        //$$ RenderHelperCompat.disableStandardItemLighting();
        //#endif
    }
    public static void renderPlayer(AbstractClientPlayer player, int posX, int posY, int scale, float mX, float mY){
        drawEntityOnScreenSmooth(posX,posY,scale,mX,mY,player);
    }

    public static void drawEntityOnScreenSmooth(int posX, int posY, int scale, float mouseX, float mouseY, EntityLivingBase ent) {
        //#if MC < 1.21
        float f = (float) posX - mouseX;
        float f1 = (float) posY - 50.0F - mouseY;
        GlStateManagerCompat.color(1f,1f,1f,1f);
        try {
            GuiInventory.drawEntityOnScreen(posX, posY, scale, f, f1, ent);
        } finally {
            GlStateManagerCompat.resetColor();
        }
        //#else
        //$$ DrawContext ctx;
        //$$ try {
        //$$     ctx = DrawContextUtils.getDrawContext();
        //$$ } catch (IllegalStateException unbound) {
        //$$     return;
        //$$ }
        //$$ int x1 = posX - scale / 2, y1 = posY - scale * 2 - 12, x2 = posX + scale / 2, y2 = posY;
        //$$ GlStateManagerCompat.color(1f,1f,1f,1f);
        //$$ try {
        //$$     InventoryScreen.drawEntity(ctx, x1, y1, x2, y2, scale, 0.0f, mouseX, mouseY, ent);
        //$$ } finally {
        //$$     GlStateManagerCompat.resetColor();
        //$$ }
        //#endif
    }

    private static void drawEntityAtYaw(int posX, int posY, int scale, float yaw, float partialTicks, EntityLivingBase entity) {
        //#if MC < 1.21
        RenderManager manager = MinecraftCompat.getMinecraft().getRenderManager();
        float oldPlayerViewY = manager.playerViewY;
        boolean oldShadow = manager.isRenderShadow();
        float oldLightmapX = OpenGlHelper.lastBrightnessX;
        float oldLightmapY = OpenGlHelper.lastBrightnessY;
        float oldBodyYaw = entity.renderYawOffset;
        float oldPrevBodyYaw = entity.prevRenderYawOffset;
        float oldYaw = entity.rotationYaw;
        float oldPrevYaw = entity.prevRotationYaw;
        float oldPitch = entity.rotationPitch;
        float oldPrevPitch = entity.prevRotationPitch;
        float oldHeadYaw = entity.rotationYawHead;
        float oldPrevHeadYaw = entity.prevRotationYawHead;
        //#if MC < 1.16
        AbstractClientPlayer previewPlayer = (AbstractClientPlayer) entity;
        double oldChasingX = previewPlayer.chasingPosX;
        double oldChasingY = previewPlayer.chasingPosY;
        double oldChasingZ = previewPlayer.chasingPosZ;
        double oldPrevChasingX = previewPlayer.prevChasingPosX;
        double oldPrevChasingY = previewPlayer.prevChasingPosY;
        double oldPrevChasingZ = previewPlayer.prevChasingPosZ;
        float oldCameraYaw = previewPlayer.cameraYaw;
        float oldPrevCameraYaw = previewPlayer.prevCameraYaw;
        float oldWalkDistance = previewPlayer.distanceWalkedModified;
        float oldPrevWalkDistance = previewPlayer.prevDistanceWalkedModified;
        //#endif
        GlStateManagerCompat.enableColorMaterial();
        GlStateManagerCompat.pushMatrix();
        try {
            GlStateManagerCompat.color(1f,1f,1f,1f);
            GlStateManagerCompat.translate(posX, posY, 50f);
            GlStateManagerCompat.scale(-scale, scale, scale);
            GlStateManagerCompat.rotate(180f, 0f, 0f, 1f);
            GlStateManagerCompat.rotate(135f, 0f, 1f, 0f);
            RenderHelperCompat.enableStandardItemLighting();
            GlStateManagerCompat.rotate(-135f, 0f, 1f, 0f);
            GlStateManagerCompat.enableRescaleNormal();

            entity.renderYawOffset = yaw;
            entity.prevRenderYawOffset = yaw;
            entity.rotationYaw = yaw;
            entity.prevRotationYaw = yaw;
            entity.rotationPitch = 0f;
            entity.prevRotationPitch = 0f;
            entity.rotationYawHead = yaw;
            entity.prevRotationYawHead = yaw;
            //#if MC < 1.16
            // A gallery preview has no movement updates. Keep the cape layer's
            // interpolation inputs aligned so turning the model doesn't fling the cape.
            previewPlayer.chasingPosX = previewPlayer.prevChasingPosX = previewPlayer.posX;
            previewPlayer.chasingPosY = previewPlayer.prevChasingPosY = previewPlayer.posY;
            previewPlayer.chasingPosZ = previewPlayer.prevChasingPosZ = previewPlayer.posZ;
            previewPlayer.cameraYaw = previewPlayer.prevCameraYaw = 0f;
            previewPlayer.distanceWalkedModified = previewPlayer.prevDistanceWalkedModified = 0f;
            //#endif
            manager.setPlayerViewY(180f);
            manager.setRenderShadow(false);
            OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GlStateManagerCompat.enableTexture2D();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            manager.renderEntityWithPosYaw(entity, 0, 0, 0, 0f, 1f);
        } finally {
            manager.setPlayerViewY(oldPlayerViewY);
            manager.setRenderShadow(oldShadow);
            entity.renderYawOffset = oldBodyYaw;
            entity.prevRenderYawOffset = oldPrevBodyYaw;
            entity.rotationYaw = oldYaw;
            entity.prevRotationYaw = oldPrevYaw;
            entity.rotationPitch = oldPitch;
            entity.prevRotationPitch = oldPrevPitch;
            entity.rotationYawHead = oldHeadYaw;
            entity.prevRotationYawHead = oldPrevHeadYaw;
            //#if MC < 1.16
            previewPlayer.chasingPosX = oldChasingX;
            previewPlayer.chasingPosY = oldChasingY;
            previewPlayer.chasingPosZ = oldChasingZ;
            previewPlayer.prevChasingPosX = oldPrevChasingX;
            previewPlayer.prevChasingPosY = oldPrevChasingY;
            previewPlayer.prevChasingPosZ = oldPrevChasingZ;
            previewPlayer.cameraYaw = oldCameraYaw;
            previewPlayer.prevCameraYaw = oldPrevCameraYaw;
            previewPlayer.distanceWalkedModified = oldWalkDistance;
            previewPlayer.prevDistanceWalkedModified = oldPrevWalkDistance;
            //#endif
            GlStateManagerCompat.popMatrix();
            RenderHelperCompat.disableStandardItemLighting();
            GlStateManagerCompat.disableRescaleNormal();
            OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GlStateManagerCompat.enableTexture2D();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, oldLightmapX, oldLightmapY);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GlStateManagerCompat.enableTexture2D();
            GlStateManagerCompat.disableLighting();
            GlStateManagerCompat.resetColor();
        }
        //#else
        //$$ DrawContext ctx;
        //$$ try {
        //$$     ctx = DrawContextUtils.getDrawContext();
        //$$ } catch (IllegalStateException unbound) {
        //$$     return;
        //$$ }
        //$$ float oldBodyYaw = entity.bodyYaw;
        //$$ float oldPrevBodyYaw = entity.prevBodyYaw;
        //$$ float oldYaw = entity.getYaw();
        //$$ float oldPitch = entity.getPitch();
        //$$ float oldHeadYaw = entity.headYaw;
        //$$ float oldPrevHeadYaw = entity.prevHeadYaw;
        //$$ float modelYaw = yaw + 180f;
        //$$ entity.bodyYaw = entity.prevBodyYaw = modelYaw;
        //$$ entity.setYaw(modelYaw);
        //$$ entity.setPitch(0f);
        //$$ entity.headYaw = entity.prevHeadYaw = modelYaw;
        //$$ EntityRenderDispatcher dispatcher = MinecraftCompat.getMinecraft().getEntityRenderDispatcher();
        //$$ org.joml.Quaternionf oldRotation = dispatcher.getRotation() == null ? null : new org.joml.Quaternionf(dispatcher.getRotation());
        //$$ float oldPrevYaw = entity.prevYaw;
        //$$ dispatcher.setRotation(new org.joml.Quaternionf().rotateZ((float) Math.PI).rotateY((float) Math.PI));
        //$$ MatrixStack matrices = ctx.getMatrices();
        //$$ matrices.push();
        //$$ try {
        //$$     matrices.translate(posX, posY, 50.0);
        //$$     matrices.scale(scale, scale, -scale);
        //$$     matrices.translate(0.0f, entity.getHeight() * 0.5f + scale, 0.0f);
        //$$     matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180f));
        //$$     RenderHelperCompat.enableGUIStandardItemLighting();
        //$$     GlStateManagerCompat.color(1f,1f,1f,1f);
        //$$     dispatcher.render(entity, 0.0, 0.0, 0.0, modelYaw, partialTicks, matrices,
        //$$             ctx.getVertexConsumers(), LightmapTextureManager.MAX_LIGHT_COORDINATE);
        //$$     ctx.draw();
        //$$ } finally {
        //$$     matrices.pop();
        //$$     dispatcher.setRotation(oldRotation);
        //$$     entity.prevYaw = oldPrevYaw;
        //$$     entity.bodyYaw = oldBodyYaw;
        //$$     entity.prevBodyYaw = oldPrevBodyYaw;
        //$$     entity.setYaw(oldYaw);
        //$$     entity.setPitch(oldPitch);
        //$$     entity.headYaw = oldHeadYaw;
        //$$     entity.prevHeadYaw = oldPrevHeadYaw;
        //$$     GlStateManagerCompat.resetColor();
        //$$ }
        //#endif
    }

}
