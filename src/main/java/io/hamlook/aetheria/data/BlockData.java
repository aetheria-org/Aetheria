package io.hamlook.aetheria.data;

import io.hamlook.aetheria.events.PacketEvent;
import io.hamlook.aetheria.events.PlaySoundEvent;
import io.hamlook.aetheria.events.ServerBlockChangeEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import net.minecraft.network.play.server.S22PacketMultiBlockChange;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@RegisterEvents
public class BlockData {

    @SubscribeEvent
    public void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof S23PacketBlockChange) {
            S23PacketBlockChange pkt = (S23PacketBlockChange) event.packet;
            net.minecraft.util.BlockPos pos = pkt.getBlockPosition();
            net.minecraft.block.state.IBlockState state = pkt.getBlockState();
            if (pos != null && state != null) {
                MinecraftForge.EVENT_BUS.post(new ServerBlockChangeEvent(pos, state));
            }
        } else if (event.packet instanceof S22PacketMultiBlockChange) {
            S22PacketMultiBlockChange pkt = (S22PacketMultiBlockChange) event.packet;
            for (S22PacketMultiBlockChange.BlockUpdateData entry : pkt.getChangedBlocks()) {
                MinecraftForge.EVENT_BUS.post(new ServerBlockChangeEvent(entry.getPos(), entry.getBlockState()));
            }
        } else if (event.packet instanceof S29PacketSoundEffect) {
            S29PacketSoundEffect pkt = (S29PacketSoundEffect) event.packet;
            MinecraftForge.EVENT_BUS.post(new PlaySoundEvent(pkt.getSoundName(), pkt.getX(), pkt.getY(), pkt.getZ(), pkt.getPitch(), pkt.getVolume()));
        }
    }
}
