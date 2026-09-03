package io.hamlook.aetheria.command;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.events.ASMChatEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import net.minecraft.client.Minecraft;

@RegisterEvents
public class CommandIntercept {

    private static void redirectToCommand(String msg) {
        Minecraft mc = MinecraftCompat.getMinecraft();
        if (mc.thePlayer != null) {
            ChatUtils.sendChatCommand("/" + msg);
        } else {
            ChatUtils.sendMessage("§c[ATHR] §7You must be in a world to use commands.");
        }
    }

    @HandleEvent(priority = HandleEvent.HIGHEST)
    public void onChat(ASMChatEvent event) {
        String msg = String.valueOf(event.message);
        String firstWord = CommandRegistry.firstWordOf(msg);
        if (CommandRegistry.isRegistered(firstWord)) {
            event.cancel();
            redirectToCommand(msg);
        }
    }
}
