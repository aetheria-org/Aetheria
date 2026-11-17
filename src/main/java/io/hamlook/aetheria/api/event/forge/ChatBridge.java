package io.hamlook.aetheria.api.event.forge;

import io.hamlook.aetheria.events.ASMChatEvent;
import io.hamlook.aetheria.features.chat.ChatUtilsState;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.compat.TextCompat;
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
        ASMChatEvent asmEvent = new ASMChatEvent(event.message, ASMChatEvent.Type.fromId(event.type));
        asmEvent.post();
        ChatUtilsState.currentLineMessage =
                (asmEvent.isCancelled() || asmEvent.type == ASMChatEvent.Type.ACTION_BAR)
                        ? null
                        : asmEvent.parsed() == null ? null : asmEvent.parsed().type;
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
    //$$         athr$intake(message, overlay ? ASMChatEvent.Type.ACTION_BAR : ASMChatEvent.Type.SYSTEM));
    //$$     ClientReceiveMessageEvents.MODIFY_GAME.register((message, overlay) ->
    //$$         athr$apply(message));
    //$$     ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, profile, params, timestamp) ->
    //$$         athr$intake(message, ASMChatEvent.Type.CHAT));
    //$$     ClientReceiveMessageEvents.CHAT.register((message, signedMessage, profile, params, timestamp) -> {
    //$$     });
    //$$ }
    //$$
    //$$ private static boolean athr$intake(Text message, ASMChatEvent.Type type) {
//$$     ASMChatEvent asmEvent = new ASMChatEvent(message, type);
//$$     asmEvent.post();
//$$     ChatUtilsState.currentLineMessage =
//$$             (asmEvent.isCancelled() || asmEvent.type == ASMChatEvent.Type.ACTION_BAR)
//$$                     ? null
//$$                     : asmEvent.parsed() == null ? null : asmEvent.parsed().type;
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
    //$$     if (pending != null) {
    //$$         return TextCompat.parseLegacyLiterals(pending.message);
    //$$     }
    //$$     return TextCompat.parseLegacyLiterals(message);
    //$$ }
    //#endif
}
