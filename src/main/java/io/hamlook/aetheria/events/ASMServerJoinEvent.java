package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.AetheriaEvent;
import net.minecraft.network.NetworkManager;

public class ASMServerJoinEvent extends AetheriaEvent {

    public final NetworkManager manager;

    public ASMServerJoinEvent(NetworkManager manager) {
        this.manager = manager;
    }
}
