package io.hamlook.aetheria.utils.hooks;

import io.hamlook.aetheria.events.PacketEvent;
import io.hamlook.aetheria.events.PacketReceiveStatsEvent;
import io.hamlook.aetheria.events.PacketReceiveTimeUpdateEvent;
import io.hamlook.aetheria.features.storage.utils.SPacketHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.server.S03PacketTimeUpdate;
import net.minecraft.network.play.server.S22PacketMultiBlockChange;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraft.network.play.server.S2DPacketOpenWindow;
import net.minecraft.network.play.server.S2EPacketCloseWindow;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import net.minecraft.network.play.server.S30PacketWindowItems;
import net.minecraft.network.play.server.S37PacketStatistics;

public class NetHandlerPlayClientHook {

    private static final SPacketHandler STORAGE_HANDLER = new SPacketHandler();

    /**
     * Posts PacketEvent.Send for every outgoing packet and delegates
     * C0EPacketClickWindow to the storage handler.
     */
    public static void onAddToSendQueue(Packet<?> packet) {
        new PacketEvent.Send(packet).post();
        if (packet instanceof C0EPacketClickWindow) {
            STORAGE_HANDLER.handleClickWindow((C0EPacketClickWindow) packet);
        }
    }

    public static void onHandleSetSlot(S2FPacketSetSlot packet) {
        STORAGE_HANDLER.handleSetSlot(packet);
    }

    public static void onHandleOpenWindow(S2DPacketOpenWindow packet) {
        STORAGE_HANDLER.handleOpenWindow(packet);
    }

    public static void onHandleCloseWindow(S2EPacketCloseWindow packet) {
        STORAGE_HANDLER.handleCloseWindow(packet);
    }

    public static void onHandleWindowItems(S30PacketWindowItems packet) {
        STORAGE_HANDLER.handleWindowItems(packet);
    }

    public static void onTimeUpdate(S03PacketTimeUpdate packet) {
        new PacketReceiveTimeUpdateEvent(packet).post();
    }

    public static void onStatistics(S37PacketStatistics packet) {
        new PacketReceiveStatsEvent(packet).post();
    }

    public static void onBlockChange(S23PacketBlockChange packet) {
        new PacketEvent.Receive(packet).post();
    }

    public static void onMultiBlockChange(S22PacketMultiBlockChange packet) {
        new PacketEvent.Receive(packet).post();
    }

    public static void onSoundEffect(S29PacketSoundEffect packet) {
        new PacketEvent.Receive(packet).post();
    }
}
