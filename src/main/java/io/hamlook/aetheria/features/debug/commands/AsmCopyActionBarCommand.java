package io.hamlook.aetheria.features.debug.commands;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.command.brigadier.CommandCategory;
import io.hamlook.aetheria.command.brigadier.CommandRegistrationEvent;
import io.hamlook.aetheria.features.misc.SkyblockExp.ActionBarDispatcher;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.compat.ClipboardCompat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.EnumChatFormatting;

import java.util.Collections;

@RegisterEvents
public class AsmCopyActionBarCommand {

    @HandleEvent
    public void onCommandRegistration(CommandRegistrationEvent event) {
        event.registerBrigadier("asmcopyactionbar", builder -> {
            builder.setAliases(Collections.singletonList("asmcopyab"));
            builder.description = "Copies the last received action bar text";
            builder.setCategory(CommandCategory.DEVELOPER_DEBUG);
            builder.simpleCallback(() -> {
                boolean noColor = false;

                String actionBar = noColor ? ActionBarDispatcher.lastActionBarStripped : ActionBarDispatcher.lastActionBarFormatted;
                if (actionBar == null || actionBar.isEmpty()) {
                    ChatUtils.sendMessage(EnumChatFormatting.YELLOW + "No action bar text received yet.");
                    return;
                }

                ClipboardCompat.setClipboard(actionBar);
                ChatUtils.sendMessage(EnumChatFormatting.YELLOW + "Action bar copied to clipboard " + (noColor ? "without" : "with") + " formatting codes!");
            });
        });
    }
}
