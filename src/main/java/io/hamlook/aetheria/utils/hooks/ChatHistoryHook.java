package io.hamlook.aetheria.utils.hooks;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.chat.ChatConfig;

import java.util.List;

public final class ChatHistoryHook {

    private ChatHistoryHook() {
    }

    public static int limit() {
        ChatConfig chat = ATHRConfig.feature == null ? null : ATHRConfig.feature.chat;
        return chat != null && chat.extendedChatHistory ? 16384 : 100;
    }

    public static void trimToLimit(List<?> list) {
        int limit = limit();
        while (list.size() > limit) {
            list.remove(list.size() - 1);
        }
    }
}
