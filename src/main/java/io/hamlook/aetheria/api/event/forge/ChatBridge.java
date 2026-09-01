package io.hamlook.aetheria.api.event.forge;

import io.hamlook.aetheria.events.ASMChatEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@RegisterEvents
public class ChatBridge {

    @SubscribeEvent(receiveCanceled = true)
    public void onChatReceived(ClientChatReceivedEvent event) {
        ASMChatEvent asmEvent = new ASMChatEvent(event.message, event.type);
        asmEvent.post();
        if (asmEvent.isCancelled()) {
            event.setCanceled(true);
        }
        event.message = asmEvent.message;
    }
}
