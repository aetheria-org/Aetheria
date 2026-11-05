package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.AetheriaEvent;
import io.hamlook.aetheria.features.dungeons.rooms.secret.SecretWaypoint;

public class SecretGainedEvent extends AetheriaEvent {
    public final SecretWaypoint secret;

    public SecretGainedEvent(SecretWaypoint secret) {
        this.secret = secret;
    }
}
