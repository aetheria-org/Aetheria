package io.hamlook.aetheria.features.capes;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.command.brigadier.CommandCategory;
import io.hamlook.aetheria.command.brigadier.CommandRegistrationEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.capes.ui.CapeSelectorGUI;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;

@RegisterEvents
public class CapeMenuCommand {

    @HandleEvent
    public void onCommandRegistration(CommandRegistrationEvent event) {
        event.registerBrigadier("capes", builder -> {
            builder.description = "Open the cape selector GUI";
            builder.setCategory(CommandCategory.USERS_ACTIVE);
            builder.simpleCallback(() -> {
                if (MinecraftCompat.getLocalPlayer() == null) return;
                ATHRConfig.screenToOpen = new CapeSelectorGUI();
            });
        });
    }
}
