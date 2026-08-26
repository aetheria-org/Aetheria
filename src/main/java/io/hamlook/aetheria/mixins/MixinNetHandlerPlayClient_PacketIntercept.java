package io.hamlook.aetheria.mixins;

import io.hamlook.aetheria.events.PacketEvent;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.S22PacketMultiBlockChange;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public class MixinNetHandlerPlayClient_PacketIntercept {

    @Inject(method = "handleBlockChange", at = @At("HEAD"))
    private void ATHR$onBlockChange(S23PacketBlockChange packet, CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new PacketEvent.Receive(packet));
    }

    @Inject(method = "handleMultiBlockChange", at = @At("HEAD"))
    private void ATHR$onMultiBlockChange(S22PacketMultiBlockChange packet, CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new PacketEvent.Receive(packet));
    }

    @Inject(method = "handleSoundEffect", at = @At("HEAD"))
    private void ATHR$onSoundEffect(S29PacketSoundEffect packet, CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new PacketEvent.Receive(packet));
    }
}
