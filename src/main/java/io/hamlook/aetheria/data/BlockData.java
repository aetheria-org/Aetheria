package io.hamlook.aetheria.data;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.events.PacketEvent;
import io.hamlook.aetheria.events.PlaySoundEvent;
import io.hamlook.aetheria.events.ServerBlockChangeEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.compat.PacketCompat;
import net.minecraft.network.play.server.S22PacketMultiBlockChange;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.network.play.server.S29PacketSoundEffect;

@RegisterEvents
public class BlockData {

    @HandleEvent
    public void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof S23PacketBlockChange) {
            S23PacketBlockChange pkt = (S23PacketBlockChange) event.packet;
            net.minecraft.util.BlockPos pos = pkt.getBlockPosition();
            net.minecraft.block.state.IBlockState state = pkt.getBlockState();
            if (pos != null && state != null) {
                new ServerBlockChangeEvent(pos, state).post();
            }
        } else if (event.packet instanceof S22PacketMultiBlockChange) {
            S22PacketMultiBlockChange pkt = (S22PacketMultiBlockChange) event.packet;
            //#if MC < 1.21
            for (S22PacketMultiBlockChange.BlockUpdateData entry : pkt.getChangedBlocks()) {
                new ServerBlockChangeEvent(entry.getPos(), entry.getBlockState()).post();
            }
            //#else
            //$$ pkt.visitUpdates((pos, state) -> {
            //$$     new ServerBlockChangeEvent(pos, state).post();
            //$$ });
            //#endif
        } else if (event.packet instanceof S29PacketSoundEffect) {
            S29PacketSoundEffect pkt = (S29PacketSoundEffect) event.packet;
            new PlaySoundEvent(PacketCompat.soundName(pkt), pkt.getX(), pkt.getY(), pkt.getZ(), pkt.getPitch(), pkt.getVolume()).post();
        }
    }
}
