package io.hamlook.aetheria.features.debug.commands;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.command.brigadier.CommandCategory;
import io.hamlook.aetheria.command.brigadier.CommandRegistrationEvent;
import io.hamlook.aetheria.init.EventRegistrar;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import net.minecraft.util.EnumChatFormatting;

@RegisterEvents
public class AsmReloadListenersCommand {

    @HandleEvent
    public void onCommandRegistration(CommandRegistrationEvent event) {
        event.registerBrigadier("asmreloadlisteners", builder -> {
            builder.description = "Re-registers every tracked Aetheria event listener onto the Forge event bus";
            builder.setCategory(CommandCategory.DEVELOPER_DEBUG);
            builder.simpleCallback(() -> {
                int count = EventRegistrar.getRegisteredEventInstances().size();
                EventRegistrar.reloadAllListeners();
                ChatUtils.sendMessage(EnumChatFormatting.YELLOW + "Reloaded " + count + " Aetheria event listeners.");
            });
        });
    }
}
