package io.hamlook.aetheria.features.chat.chatfilters;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.command.brigadier.CommandCategory;
import io.hamlook.aetheria.command.brigadier.CommandRegistrationEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.chat.chatfilters.ui.ChatFilterGUI;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;

import java.util.Arrays;

@RegisterEvents
public class ChatFilterCommand {

    @HandleEvent
    public void onCommandRegistration(CommandRegistrationEvent event) {
        event.registerBrigadier("chatfilters", builder -> {
            builder.setAliases(Arrays.asList("athrChatFilters",
                    "athrchatfilters", "acf", "asmChatFilters", "asmchatfilters",
                    "aetheriaChatFilters", "aetheriachatfilters"));
            builder.description = "Open the chat filter configuration GUI";
            builder.setCategory(CommandCategory.USERS_ACTIVE);

            builder.simpleCallback(() -> {
                if (MinecraftCompat.getLocalPlayer() == null) return;
                ATHRConfig.screenToOpen = new ChatFilterGUI();
            });
        });
    }
}
