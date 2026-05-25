package io.hamlook.aetheria.features.chatfilters.listener;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.chatfilters.ChatFilterManager;
import io.hamlook.aetheria.init.RegisterEvents;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@RegisterEvents
public class ChatFilterListener {

    @SubscribeEvent
    public void onChatRecieved(ClientChatReceivedEvent event) {
        if(!ATHRConfig.feature.chat.chatFilterConfig.chatFilters) return;
        IChatComponent result = ChatFilterManager.applyFilters(event.message);
        if (result == null || result.getUnformattedText().isEmpty()) {
            event.setCanceled(true);
        } else {
            event.message = result;
        }
    }

}
