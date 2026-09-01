package io.hamlook.aetheria.features.chat.chatfilters.listener;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.chat.chatfilters.ChatFilterManager;
import io.hamlook.aetheria.init.RegisterEvents;
import net.minecraft.util.IChatComponent;
import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.events.ASMChatEvent;

@RegisterEvents
public class ChatFilterListener {

    @HandleEvent
    public void onChatRecieved(ASMChatEvent event) {
        if(!ATHRConfig.feature.chat.chatFilterConfig.chatFilters) return;
        IChatComponent result = ChatFilterManager.applyFilters(event.message);
        if (result == null || result.getUnformattedText().isEmpty()) {
            event.cancel();
        } else {
            event.message = result;
        }
    }

}
