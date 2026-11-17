package io.hamlook.aetheria.features.misc.invbuttons;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.command.brigadier.CommandCategory;
import io.hamlook.aetheria.command.brigadier.CommandRegistrationEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.init.RegisterEvents;

import java.util.Arrays;

@RegisterEvents
public class InvButtonsCommand {

    @HandleEvent
    public void onCommandRegistration(CommandRegistrationEvent event) {
        event.registerBrigadier("athrbuttons", builder -> {
            builder.setAliases(Arrays.asList("aetheriabuttons", "jefbuttons", "asmbuttons"));
            builder.description = "Open the inventory button editor";
            builder.setCategory(CommandCategory.USERS_ACTIVE);
            builder.simpleCallback(() -> {
                ATHRConfig.openInvButtonEditor();
            });
        });
    }
}