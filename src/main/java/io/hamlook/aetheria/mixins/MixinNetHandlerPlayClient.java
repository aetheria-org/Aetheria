package io.hamlook.aetheria.mixins;

import io.hamlook.aetheria.features.storage.utils.SPacketHandler;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.server.S2DPacketOpenWindow;
import net.minecraft.network.play.server.S2EPacketCloseWindow;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import net.minecraft.network.play.server.S30PacketWindowItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public class MixinNetHandlerPlayClient {

    private static final SPacketHandler storageHandler = new SPacketHandler();

    @Inject(method = "addToSendQueue", at = @At("HEAD"))
    public void addToSendQueue(Packet packet, CallbackInfo ci) {
        if (packet instanceof C0EPacketClickWindow) {
            storageHandler.handleClickWindow((C0EPacketClickWindow) packet);
        }
    }

    @Inject(method = "handleSetSlot", at = @At("RETURN"))
    public void handleSetSlot(S2FPacketSetSlot packetIn, CallbackInfo ci) {
        storageHandler.handleSetSlot(packetIn);
    }

    @Inject(method = "handleOpenWindow", at = @At("RETURN"))
    public void handleOpenWindow(S2DPacketOpenWindow packetIn, CallbackInfo ci) {
        storageHandler.handleOpenWindow(packetIn);
    }

    @Inject(method = "handleCloseWindow", at = @At("RETURN"))
    public void handleCloseWindow(S2EPacketCloseWindow packetIn, CallbackInfo ci) {
        storageHandler.handleCloseWindow(packetIn);
    }

    @Inject(method = "handleWindowItems", at = @At("RETURN"))
    public void handleWindowItems(S30PacketWindowItems packetIn, CallbackInfo ci) {
        storageHandler.handleWindowItems(packetIn);
    }
}
