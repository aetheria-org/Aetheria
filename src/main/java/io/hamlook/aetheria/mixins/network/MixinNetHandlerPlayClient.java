package io.hamlook.aetheria.mixins.network;

import io.hamlook.aetheria.utils.hooks.NetHandlerPlayClientHook;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public class MixinNetHandlerPlayClient {

    @Inject(method = "addToSendQueue", at = @At("HEAD"))
    public void ATHR$addToSendQueue(Packet<?> packet, CallbackInfo ci) {
        NetHandlerPlayClientHook.onAddToSendQueue(packet);
    }

    @Inject(method = "handleSetSlot", at = @At("RETURN"))
    public void ATHR$handleSetSlot(S2FPacketSetSlot packetIn, CallbackInfo ci) {
        NetHandlerPlayClientHook.onHandleSetSlot(packetIn);
    }

    @Inject(method = "handleOpenWindow", at = @At("RETURN"))
    public void ATHR$handleOpenWindow(S2DPacketOpenWindow packetIn, CallbackInfo ci) {
        NetHandlerPlayClientHook.onHandleOpenWindow(packetIn);
    }

    @Inject(method = "handleCloseWindow", at = @At("RETURN"))
    public void ATHR$handleCloseWindow(S2EPacketCloseWindow packetIn, CallbackInfo ci) {
        NetHandlerPlayClientHook.onHandleCloseWindow(packetIn);
    }

    @Inject(method = "handleWindowItems", at = @At("RETURN"))
    public void ATHR$handleWindowItems(S30PacketWindowItems packetIn, CallbackInfo ci) {
        NetHandlerPlayClientHook.onHandleWindowItems(packetIn);
    }

    @Inject(method = "handleTimeUpdate", at = @At("HEAD"))
    private void ATHR$onTimeUpdate(S03PacketTimeUpdate packet, CallbackInfo ci) {
        NetHandlerPlayClientHook.onTimeUpdate(packet);
    }

    @Inject(method = "handleStatistics", at = @At("HEAD"))
    private void ATHR$onStatistics(S37PacketStatistics packet, CallbackInfo ci) {
        NetHandlerPlayClientHook.onStatistics(packet);
    }

    @Inject(method = "handleBlockChange", at = @At("HEAD"))
    private void ATHR$onBlockChange(S23PacketBlockChange packet, CallbackInfo ci) {
        NetHandlerPlayClientHook.onBlockChange(packet);
    }

    @Inject(method = "handleMultiBlockChange", at = @At("HEAD"))
    private void ATHR$onMultiBlockChange(S22PacketMultiBlockChange packet, CallbackInfo ci) {
        NetHandlerPlayClientHook.onMultiBlockChange(packet);
    }

    @Inject(method = "handleSoundEffect", at = @At("HEAD"))
    private void ATHR$onSoundEffect(S29PacketSoundEffect packet, CallbackInfo ci) {
        NetHandlerPlayClientHook.onSoundEffect(packet);
    }
}
