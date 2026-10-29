package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.AetheriaEvent;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.compat.TextCompat;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StringUtils;

public class ASMChatEvent extends AetheriaEvent implements AetheriaEvent.Cancellable {

    public enum Type {
        CHAT, SYSTEM, ACTION_BAR;
        public static Type fromId(byte id) {
            for (Type type : values()) { if (type.ordinal() == id) return type; }
            return CHAT;
        }
    }

    public IChatComponent message;
    public final Type type;

    private final String arrivedPlain;
    private ChatUtils.Message parsed;
    private boolean parsedDone;
    private String blockReason;

    public ASMChatEvent(IChatComponent message, Type type) {
        this.message = message;
        this.type = type;
        this.arrivedPlain = strip(message);
    }

    // text as it arrived, before any listener edits it
    public String plain() {
        return arrivedPlain;
    }

    // text as it stands now, after any listener edits
    public String currentPlain() {
        return strip(message);
    }

    public String formatted() {
        return message == null ? "" : TextCompat.getFormattedTextWithResets(message);
    }

    // null for action bar lines, which fire far too often to classify
    public ChatUtils.Message parsed() {
        if (type == Type.ACTION_BAR) return null;
        if (!parsedDone) {
            parsed = ChatUtils.findMessage(arrivedPlain);
            parsedDone = true;
        }
        return parsed;
    }

    // hide the message and record why
    public void block(String reason) {
        blockReason = reason;
        cancel();
    }

    public String blockReason() {
        return blockReason;
    }

    private static String strip(IChatComponent component) {
        if (component == null) return "";
        return StringUtils.stripControlCodes(TextCompat.getFormattedTextWithResets(component));
    }
}
