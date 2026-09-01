package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.AetheriaEvent;
import net.minecraft.network.play.server.S37PacketStatistics;

public class PacketReceiveStatsEvent extends AetheriaEvent {
    public final S37PacketStatistics packet;

    public PacketReceiveStatsEvent(S37PacketStatistics packet) {
        this.packet = packet;
    }
}