package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.AetheriaEvent;
import net.minecraft.network.Packet;

public class PacketEvent extends AetheriaEvent {

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
