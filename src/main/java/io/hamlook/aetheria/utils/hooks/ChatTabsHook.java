package io.hamlook.aetheria.utils.hooks;

import io.hamlook.aetheria.features.chat.ChatLineHook;
import io.hamlook.aetheria.features.chat.ChatUtilsState;
import io.hamlook.aetheria.features.chat.chattabs.ChatTabs;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import net.minecraft.util.IChatComponent;
//#if MC < 1.21
import net.minecraft.client.gui.ChatLine;
//#endif

import java.util.List;
//#if MC < 1.21
import java.util.IdentityHashMap;
import java.util.Map;
//#endif

public final class ChatTabsHook {

    private ChatTabsHook() {
    }

    public static void beginRefresh(List<?> chatLines) {
        //#if MC < 1.21
        Map<IChatComponent, ChatUtils.MessageType> map = new IdentityHashMap<>();
        for (Object line : chatLines) {
            if (line instanceof ChatLine) {
                ChatUtils.MessageType type = ((ChatLineHook) line).athr$getMessageType();
                if (type != null) {
                    map.put(((ChatLine) line).getChatComponent(), type);
                }
            }
        }
        ChatUtilsState.refreshTags = map;
        //#endif
    }

    public static void endRefresh() {
        //#if MC < 1.21
        ChatUtilsState.refreshTags = null;
        ChatUtilsState.pendingLineMessage = null;
        //#endif
    }

    public static ChatUtils.MessageType tagForRefresh(IChatComponent component) {
        //#if MC < 1.21
        Map<IChatComponent, ChatUtils.MessageType> map = ChatUtilsState.refreshTags;
        return map == null ? null : map.get(component);
        //#else
        //$$ return null;
        //#endif
    }

    public static boolean shouldInsertIntoDrawn(List<?> drawnChatLines, List<?> target, Object line) {
        if (drawnChatLines != target) return true;
        if (!(line instanceof ChatLineHook)) return true;
        return ChatTabs.lineMatchesTab(((ChatLineHook) line).athr$getMessageType(), ChatTabs.effectiveTab());
    }

    public static boolean shouldInsertVisible(Object line) {
        ChatUtils.MessageType type = ChatUtilsState.pendingLineMessage;
        if (type == null && line instanceof ChatLineHook) {
            type = ((ChatLineHook) line).athr$getMessageType();
        }
        return ChatTabs.lineMatchesTab(type, ChatTabs.effectiveTab());
    }
}
