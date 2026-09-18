package io.hamlook.aetheria.utils.hooks;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.events.PacketEvent;
import io.hamlook.aetheria.features.storage.utils.SPacketHandler;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.compat.PacketCompat;
import io.hamlook.aetheria.utils.compat.TextCompat;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.server.S03PacketTimeUpdate;
import net.minecraft.network.play.server.S2DPacketOpenWindow;
import net.minecraft.network.play.server.S2EPacketCloseWindow;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import net.minecraft.network.play.server.S30PacketWindowItems;
import net.minecraft.network.play.server.S37PacketStatistics;

@RegisterEvents
public class NetHandlerPlayClientHook {

    private static final SPacketHandler STORAGE_HANDLER = new SPacketHandler();

    @HandleEvent(priority = HandleEvent.HIGHEST)
    public void onReceive(PacketEvent.Receive event) {
        if (event.packet instanceof S2DPacketOpenWindow) {
            S2DPacketOpenWindow packet = (S2DPacketOpenWindow) event.packet;
            STORAGE_HANDLER.handleOpenWindow(TextCompat.getUnformattedText(packet.getWindowTitle()));
        } else if (event.packet instanceof S2EPacketCloseWindow) {
            STORAGE_HANDLER.handleCloseWindow();
        } else if (event.packet instanceof S2FPacketSetSlot) {
            S2FPacketSetSlot packet = (S2FPacketSetSlot) event.packet;
            STORAGE_HANDLER.handleSetSlot(packet.func_149175_c(), packet.func_149173_d(), packet.func_149174_e());
        } else if (event.packet instanceof S30PacketWindowItems) {
            S30PacketWindowItems packet = (S30PacketWindowItems) event.packet;
            STORAGE_HANDLER.handleWindowItems(PacketCompat.windowItemsId(packet), PacketCompat.windowItemsStacks(packet));
        }
    }

    @HandleEvent(priority = HandleEvent.HIGHEST)
    public void onSend(PacketEvent.Send event) {
        if (event.packet instanceof C0EPacketClickWindow) {
            STORAGE_HANDLER.handleClickWindow(PacketCompat.clickData((C0EPacketClickWindow) event.packet));
        }
    }
}
