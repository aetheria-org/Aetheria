package io.hamlook.aetheria.events;

import net.minecraft.network.Packet;
import net.minecraftforge.fml.common.eventhandler.Event;

public class PacketEvent extends Event {

    public final Packet<?> packet;

    public PacketEvent(Packet<?> packet) {
        this.packet = packet;
    }

    public static class Receive extends PacketEvent {
        public Receive(Packet<?> packet) { super(packet); }
    }

    public static class Send extends PacketEvent {
        public Send(Packet<?> packet) { super(packet); }
    }
}
