package io.hamlook.aetheria.features.debug.commands;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.command.brigadier.CommandCategory;
import io.hamlook.aetheria.command.brigadier.CommandRegistrationEvent;
import io.hamlook.aetheria.init.EventRegistrar;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import net.minecraft.util.EnumChatFormatting;

@RegisterEvents
public class AsmStopListenersCommand {

    @HandleEvent
    public void onCommandRegistration(CommandRegistrationEvent event) {
        event.registerBrigadier("asmstoplisteners", builder -> {
            builder.description = "Unregisters every tracked Aetheria event listener from the Forge event bus";
            builder.setCategory(CommandCategory.DEVELOPER_DEBUG);
            builder.simpleCallback(() -> {
                int count = EventRegistrar.getRegisteredEventInstances().size();
                EventRegistrar.stopAllListeners();
                ChatUtils.sendMessage(EnumChatFormatting.YELLOW + "Unregistered " + count + " Aetheria event listeners. Run /asmreloadlisteners to restore them.");
            });
        });
    }
}
