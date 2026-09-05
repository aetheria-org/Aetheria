package io.hamlook.aetheria.features.farming.trevor;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.command.brigadier.CommandCategory;
import io.hamlook.aetheria.command.brigadier.CommandRegistrationEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import net.minecraft.util.EnumChatFormatting;

@RegisterEvents
public class PeltResetCommand {

    private static final String PREFIX = EnumChatFormatting.GOLD + "[Pelt Tracker] " + EnumChatFormatting.RESET;

    @HandleEvent
    public void onCommandRegistration(CommandRegistrationEvent event) {
        event.registerBrigadier("resetpelt", builder -> {
            builder.description = "Reset pelt tracker data";
            builder.setCategory(CommandCategory.USERS_ACTIVE);
            builder.simpleCallback(() -> {
                PeltOverlay.reset();
                ChatUtils.sendMessage(PREFIX + EnumChatFormatting.GREEN + "Pelt tracker reset: pelts and active time cleared.");
            });
        });
    }
}
