package io.hamlook.aetheria.mixins.network;

import io.hamlook.aetheria.events.PacketEvent;
import io.hamlook.aetheria.events.ItemPickupEvent;
import io.hamlook.aetheria.api.event.AetheriaEventBus;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.item.ItemPickupCapture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S0DPacketCollectItem;
import net.minecraft.network.play.server.S03PacketTimeUpdate;
import net.minecraft.network.play.server.S22PacketMultiBlockChange;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraft.network.play.server.S2DPacketOpenWindow;
import net.minecraft.network.play.server.S2EPacketCloseWindow;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import net.minecraft.network.play.server.S30PacketWindowItems;
import net.minecraft.network.play.server.S37PacketStatistics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public class MixinNetHandlerPlayClient {

    @Inject(method = "addToSendQueue", at = @At("HEAD"))
    public void ATHR$addToSendQueue(Packet<?> packet, CallbackInfo ci) {
        new PacketEvent.Send(packet).post();
    }

    @Inject(method = "handleSetSlot", at = @At("RETURN"))
    public void ATHR$handleSetSlot(S2FPacketSetSlot packetIn, CallbackInfo ci) {
        new PacketEvent.Receive(packetIn).post();
    }

    @Inject(method = "handleOpenWindow", at = @At("RETURN"))
    public void ATHR$handleOpenWindow(S2DPacketOpenWindow packetIn, CallbackInfo ci) {
        new PacketEvent.Receive(packetIn).post();
    }

    @Inject(method = "handleCloseWindow", at = @At("RETURN"))
    public void ATHR$handleCloseWindow(S2EPacketCloseWindow packetIn, CallbackInfo ci) {
        new PacketEvent.Receive(packetIn).post();
    }

    @Inject(method = "handleWindowItems", at = @At("RETURN"))
    public void ATHR$handleWindowItems(S30PacketWindowItems packetIn, CallbackInfo ci) {
        new PacketEvent.Receive(packetIn).post();
    }

    @Inject(method = "handleTimeUpdate", at = @At("HEAD"))
    private void ATHR$onTimeUpdate(S03PacketTimeUpdate packet, CallbackInfo ci) {
        new PacketEvent.Receive(packet).post();
    }

    @Inject(method = "handleStatistics", at = @At("HEAD"))
    private void ATHR$onStatistics(S37PacketStatistics packet, CallbackInfo ci) {
        new PacketEvent.Receive(packet).post();
    }

    @Inject(method = "handleBlockChange", at = @At("HEAD"))
    private void ATHR$onBlockChange(S23PacketBlockChange packet, CallbackInfo ci) {
        new PacketEvent.Receive(packet).post();
    }

    @Inject(method = "handleMultiBlockChange", at = @At("HEAD"))
    private void ATHR$onMultiBlockChange(S22PacketMultiBlockChange packet, CallbackInfo ci) {
        new PacketEvent.Receive(packet).post();
    }

    @Inject(method = "handleCollectItem", at = @At("HEAD"))
    public void ATHR$handleCollectItem(S0DPacketCollectItem packetIn, CallbackInfo ci) {
        if (!Minecraft.getMinecraft().isCallingFromMinecraftThread()) return;
        new PacketEvent.Receive(packetIn).post();
        if (!AetheriaEventBus.INSTANCE.hasListeners(ItemPickupEvent.class)) return;
        ItemPickupEvent event = ItemPickupCapture.capture(packetIn, MinecraftCompat.getLocalWorld());
        if (event != null) event.post();
    }

    @Inject(method = "handleSoundEffect", at = @At("HEAD"))
    private void ATHR$onSoundEffect(S29PacketSoundEffect packet, CallbackInfo ci) {
        new PacketEvent.Receive(packet).post();
    }
}
