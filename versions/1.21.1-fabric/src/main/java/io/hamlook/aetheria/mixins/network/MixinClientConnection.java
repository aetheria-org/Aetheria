package io.hamlook.aetheria.mixins.network;

import io.hamlook.aetheria.events.ItemPickupEvent;
import io.hamlook.aetheria.events.PacketEvent;
import io.hamlook.aetheria.api.event.AetheriaEventBus;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.item.ItemPickupCapture;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class MixinClientConnection {

    @Inject(method = "channelRead0", at = @At("HEAD"), cancellable = true)
    private void athr$onReceive(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        if (new PacketEvent.Receive(packet).post()) {
            ci.cancel();
            return;
        }
        if (!(packet instanceof ItemPickupAnimationS2CPacket)) return;
        if (!AetheriaEventBus.INSTANCE.hasListeners(ItemPickupEvent.class)) return;
        MinecraftCompat.getMinecraft().submit(() -> {
            ItemPickupEvent event = ItemPickupCapture.capture(packet, MinecraftCompat.getLocalWorld());
            if (event != null) event.post();
        });
    }

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/PacketCallbacks;Z)V",
            at = @At("HEAD"), cancellable = true)
    private void athr$onSend(Packet<?> packet, PacketCallbacks callbacks, boolean flush, CallbackInfo ci) {
        if (new PacketEvent.Send(packet).post()) {
            ci.cancel();
        }
    }
}
