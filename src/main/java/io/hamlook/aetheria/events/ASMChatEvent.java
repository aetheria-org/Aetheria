package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.AetheriaEvent;
import net.minecraft.util.IChatComponent;

public class ASMChatEvent extends AetheriaEvent implements AetheriaEvent.Cancellable {

    public enum Type {
        CHAT,
        SYSTEM,
        ACTION_BAR;

        public static Type fromId(byte id) {
            for (Type type : values()) {
                if (type.ordinal() == id) return type;
            }
            return CHAT;
        }
    }

    public IChatComponent message;
    public final Type type;

    public ASMChatEvent(IChatComponent message, Type type) {
        this.message = message;
        this.type = type;
    }
}
