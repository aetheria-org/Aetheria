package io.hamlook.aetheria.features.chat.chatfilters.listener;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.events.ASMChatEvent;
import io.hamlook.aetheria.features.chat.chatfilters.ChatFilterManager;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.compat.TextCompat;
import net.minecraft.util.IChatComponent;

@RegisterEvents
public class ChatFilterListener {

    @HandleEvent
    public void onChatRecieved(ASMChatEvent event) {
        if (!ATHRConfig.feature.chat.chatFilterConfig.chatFilters) return;
        IChatComponent result = ChatFilterManager.applyFilters(event.message);
        if (result == null || TextCompat.getUnformattedText(result).isEmpty()) {
            event.cancel();
        } else {
            event.message = result;
        }
    }

}
