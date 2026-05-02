package com.jef.justenoughfakepixel.mixins;

import com.jef.justenoughfakepixel.features.capes.Cape;
import com.jef.justenoughfakepixel.features.capes.CapeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public class MixinAbstractClientPlayer {

    @Shadow
    private NetworkPlayerInfo playerInfo;

    @Inject(method = "getLocationCape", at = @At("HEAD"), cancellable = true)
    private void getLocationCape(CallbackInfoReturnable<ResourceLocation> cir) {
        if (this.playerInfo == null) return;

        String user = this.playerInfo.getGameProfile().getName();
        EntityPlayer player = Minecraft.getMinecraft().theWorld.getPlayerEntityByName(user);
        if(player == null) return;
        if(Minecraft.getMinecraft().thePlayer.getPosition().distanceSq(
                player.getPosition()
        ) > 65536) return;

        if(!CapeManager.hasCape(user)) {
            CapeManager.fetchCapeAsync(user);
        }
        if (!CapeManager.hasCape(user)) {  return; }

        Cape cape = CapeManager.getCapeForPlayer(user);
        if (cape == null) {  return; }

        cir.setReturnValue(cape.resourceLocation);
    }

}
