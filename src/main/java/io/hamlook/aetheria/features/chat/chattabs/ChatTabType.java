package io.hamlook.aetheria.features.chat.chattabs;

import io.hamlook.aetheria.utils.chat.ChatUtils.MessageType;

public enum ChatTabType {
    ALL("ALL"),
    PARTY("PARTY"),
    PLAYER("PLAYER"),
    MSG("MSG"),
    GUILD("GUILD"),
    COOP("COOP");

    public final String label;

    ChatTabType(String label) {
        this.label = label;
    }

    public static ChatTabType owner(MessageType type) {
        if (type == null) return ALL;
        switch (type) {
            case PARTY:        return PARTY;
            case PLAYER:       return PLAYER;
            case MSG_RECEIVED: return MSG;
            case MSG_SENT:     return MSG;
            case GUILD:        return GUILD;
            case COOP:         return COOP;
            case DONATE:
            default:           return ALL;
        }
    }
}
