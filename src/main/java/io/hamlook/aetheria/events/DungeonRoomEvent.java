package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.AetheriaEvent;
import io.hamlook.aetheria.features.dungeons.rooms.secret.SecretRoom;

public class DungeonRoomEvent extends AetheriaEvent {

    public enum Type {
        ROOM_CLEARED,
        SECRETS_CLEARED
    }

    public final SecretRoom room;
    public final Type type;

    public DungeonRoomEvent(SecretRoom room, Type type) {
        this.room = room;
        this.type = type;
    }
}
