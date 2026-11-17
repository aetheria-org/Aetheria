package io.hamlook.aetheria.features.farming.mouse;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.command.brigadier.CommandCategory;
import io.hamlook.aetheria.command.brigadier.CommandRegistrationEvent;
import io.hamlook.aetheria.init.RegisterEvents;

@RegisterEvents
public class LockMouseCommand {

    @HandleEvent
    public void onCommandRegistration(CommandRegistrationEvent event) {
        event.registerBrigadier("lockyp", builder -> {
            builder.description = "Toggle mouse lock";
            builder.setCategory(CommandCategory.USERS_ACTIVE);
            builder.simpleCallback(() -> {
                LockMouse.setLocked(!LockMouse.isLocked());
            });
        });
    }
}