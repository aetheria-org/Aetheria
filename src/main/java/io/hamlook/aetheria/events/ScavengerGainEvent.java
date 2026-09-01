package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.AetheriaEvent;
import lombok.Getter;

@Getter
public class ScavengerGainEvent extends AetheriaEvent {

    private final int amount;

    public ScavengerGainEvent(int amount) {
        this.amount = amount;
    }
}
