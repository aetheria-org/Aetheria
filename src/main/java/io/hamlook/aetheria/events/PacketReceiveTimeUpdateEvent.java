package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.AetheriaEvent;
import lombok.Getter;
import net.minecraft.network.play.server.S03PacketTimeUpdate;

@Getter
public class PacketReceiveTimeUpdateEvent extends AetheriaEvent {

    private final S03PacketTimeUpdate packet;

    public PacketReceiveTimeUpdateEvent(S03PacketTimeUpdate packet) {
        this.packet = packet;
    }

}