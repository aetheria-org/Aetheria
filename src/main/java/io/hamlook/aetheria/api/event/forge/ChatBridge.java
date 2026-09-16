package io.hamlook.aetheria.api.event.forge;

import io.hamlook.aetheria.events.ASMChatEvent;
import io.hamlook.aetheria.init.RegisterEvents;
//#if FORGE
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
//#else
//$$ import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
//$$ import net.minecraft.text.Text;
//#endif

@RegisterEvents
public class ChatBridge {

    //#if FORGE
    @SubscribeEvent(receiveCanceled = true)
    public void onChatReceived(ClientChatReceivedEvent event) {
        ASMChatEvent asmEvent = new ASMChatEvent(event.message, event.type);
        asmEvent.post();
        if (asmEvent.isCancelled()) {
            event.setCanceled(true);
        }
        event.message = asmEvent.message;
    }
    //#else
    //$$ private static ASMChatEvent athr$pending;
    //$$
    //$$ static {
    //$$     ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) ->
    //$$         athr$intake(message, overlay ? (byte) 2 : (byte) 1));
    //$$     ClientReceiveMessageEvents.MODIFY_GAME.register((message, overlay) ->
    //$$         athr$apply(message));
    //$$     ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, profile, params, timestamp) ->
    //$$         athr$intake(message, (byte) 0));
    //$$     ClientReceiveMessageEvents.CHAT.register((message, signedMessage, profile, params, timestamp) -> {
    //$$     });
    //$$ }
    //$$
    //$$ private static boolean athr$intake(Text message, byte type) {
    //$$     ASMChatEvent asmEvent = new ASMChatEvent(message, type);
    //$$     asmEvent.post();
    //$$     if (asmEvent.isCancelled()) {
    //$$         athr$pending = null;
    //$$         return false;
    //$$     }
    //$$     athr$pending = asmEvent;
    //$$     return true;
    //$$ }
    //$$
    //$$ private static Text athr$apply(Text message) {
    //$$     ASMChatEvent pending = athr$pending;
    //$$     athr$pending = null;
    //$$     if (pending != null) return pending.message;
    //$$     return message;
    //$$ }
    //#endif
}
