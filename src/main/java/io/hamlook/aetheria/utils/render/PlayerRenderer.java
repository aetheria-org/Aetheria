package io.hamlook.aetheria.utils.render;

import com.mojang.authlib.GameProfile;
import io.hamlook.aetheria.features.capes.Cape;
import io.hamlook.aetheria.features.capes.CapeManager;
import io.hamlook.aetheria.utils.SkinManager;
import io.hamlook.aetheria.utils.compat.DrawContextUtils;
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
//#endif
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerRenderer {

    public static HashMap<String,AbstractClientPlayer> cachedModels = new HashMap<>();

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
        drawEntityOnScreenSmooth(posX,posY,scale,mX,mY,player);
        //#else
        //$$ ClientWorld world = MinecraftCompat.getLocalWorld();
        //$$ if (world == null) return;
        //$$ GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(username.getBytes()), username);
        //$$ AbstractClientPlayerEntity player = cachedModels.get(username);
        //$$ if (player == null) {
        //$$     fetchSkin(username, profile);
        //$$     player = new AbstractClientPlayerEntity(world, profile) {
        //$$         @Override
        //$$         public SkinTextures getSkinTextures() {
        //$$             SkinTextures cached = skinCache.get(username);
        //$$             if (cached == null) cached = DefaultSkinHelper.getSkinTextures(profile);
        //$$             Cape cape = CapeManager.getCapeForPlayer(username);
        //$$             if (cape == null || cape.resourceLocation == null) return cached;
        //$$             if (cape.resourceLocation.equals(cached.capeTexture())) return cached;
        //$$             return new SkinTextures(cached.texture(), cached.textureUrl(), cape.resourceLocation, cached.elytraTexture(), cached.model(), cached.secure());
        //$$         }
        //$$     };
        //$$     cachedModels.put(username, player);
        //$$ }
        //$$ drawEntityOnScreenSmooth(posX, posY, scale, mX, mY, player);
        //#endif
    }
    public static void renderPlayer(AbstractClientPlayer player, int posX, int posY, int scale, float mX, float mY){
        drawEntityOnScreenSmooth(posX,posY,scale,mX,mY,player);
    }

    public static void drawEntityOnScreenSmooth(int posX, int posY, int scale, float mouseX, float mouseY, EntityLivingBase ent) {
        //#if MC < 1.21
        float f = (float) posX - mouseX;
        float f1 = (float) posY - 50.0F - mouseY;
        GuiInventory.drawEntityOnScreen(posX, posY, scale, f, f1, ent);
        //#else
        //$$ DrawContext ctx;
        //$$ try {
        //$$     ctx = DrawContextUtils.getDrawContext();
        //$$ } catch (IllegalStateException unbound) {
        //$$     return;
        //$$ }
        //$$ int x1 = posX - scale / 2, y1 = posY - scale * 2 - 12, x2 = posX + scale / 2, y2 = posY;
        //$$ InventoryScreen.drawEntity(ctx, x1, y1, x2, y2, scale, 0.0f, mouseX, mouseY, ent);
        //#endif
    }

}
