package io.hamlook.aetheria.command;

import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@RegisterEvents
public class CommandIntercept {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onChat(ClientChatReceivedEvent event) {
        String msg = String.valueOf(event.message);
        String firstWord = CommandRegistry.firstWordOf(msg);
        if (CommandRegistry.isRegistered(firstWord)) {
            event.setCanceled(true);
            redirectToCommand(msg);
        }
    }

    private static void redirectToCommand(String msg) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            ChatUtils.sendChatCommand("/" + msg);
        } else {
            ChatUtils.sendMessage("§c[ATHR] §7You must be in a world to use commands.");
        }
    }
}
