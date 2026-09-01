package io.hamlook.aetheria.mixins.core;

import io.hamlook.aetheria.features.capes.Cape;
import io.hamlook.aetheria.features.capes.CapeManager;
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
    private void ATHR$getLocationCape(CallbackInfoReturnable<ResourceLocation> cir) {
        if (this.playerInfo == null) return;
        String user = this.playerInfo.getGameProfile().getName();
        EntityPlayer player = Minecraft.getMinecraft().theWorld.getPlayerEntityByName(user);
        if (player == null) return;
        if (Minecraft.getMinecraft().thePlayer.getPosition().distanceSq(
                player.getPosition()
        ) > 65536) return;
        Cape cape = CapeManager.getCapeForPlayer(user);
        if (cape == null) return;
        cir.setReturnValue(cape.resourceLocation);
    }
}
