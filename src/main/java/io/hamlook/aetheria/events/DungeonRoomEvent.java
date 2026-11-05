package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.AetheriaEvent;
import lombok.Getter;

@Getter
public class DungeonRoomEvent extends AetheriaEvent {

    private final boolean cleared;
    private final boolean allSecretsCleared;

    public DungeonRoomEvent(boolean cleared, boolean allSecretsCleared) {
        this.cleared = cleared;
        this.allSecretsCleared = allSecretsCleared;
    }

}
