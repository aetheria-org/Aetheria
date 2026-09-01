package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.AetheriaEvent;
import net.minecraft.util.IChatComponent;

public class ASMChatEvent extends AetheriaEvent implements AetheriaEvent.Cancellable {

    public IChatComponent message;
    public final byte type;

    public ASMChatEvent(IChatComponent message, byte type) {
        this.message = message;
        this.type = type;
    }
}
