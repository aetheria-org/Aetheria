package io.hamlook.aetheria.features.price;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.command.brigadier.CommandCategory;
import io.hamlook.aetheria.command.brigadier.CommandRegistrationEvent;
import io.hamlook.aetheria.init.RegisterEvents;

@RegisterEvents
public class SendPriceCommand {

    @HandleEvent
    public void onCommandRegistration(CommandRegistrationEvent event) {
        event.registerBrigadier("sendprice", builder -> {
            builder.description = "Sends price data now";
            builder.setCategory(CommandCategory.USERS_ACTIVE);
            builder.simpleCallback(() -> {
                PriceDetector.sendNow();
            });
        });
    }
}
