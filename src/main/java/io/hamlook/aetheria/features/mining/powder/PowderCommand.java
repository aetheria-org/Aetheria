package io.hamlook.aetheria.features.mining.powder;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.command.brigadier.CommandCategory;
import io.hamlook.aetheria.command.brigadier.CommandRegistrationEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import net.minecraft.util.EnumChatFormatting;

import java.util.Collections;

@RegisterEvents
public class PowderCommand {

    private static final String PREFIX = EnumChatFormatting.AQUA + "[Powder] " + EnumChatFormatting.RESET;

    @HandleEvent
    public void onCommandRegistration(CommandRegistrationEvent event) {
        event.registerBrigadier("powdertracker", builder -> {
            builder.setAliases(Collections.singletonList("pdt"));
            builder.description = "Powder tracker commands";
            builder.setCategory(CommandCategory.USERS_ACTIVE);

            builder.literal("reset", resetBuilder -> {
                resetBuilder.description = "Reset powder tracker data";
                resetBuilder.simpleCallback(() -> {
                    PowderStats.getInstance().reset();
                    ChatUtils.sendMessage(PREFIX + EnumChatFormatting.GREEN + "Powder tracker data has been reset.");
                });
            });

            builder.literal("toggle", toggleBuilder -> {
                toggleBuilder.description = "Toggle powder tracking";
                toggleBuilder.simpleCallback(() -> {
                    boolean now = PowderStats.getInstance().toggleTracking();
                    ChatUtils.sendMessage(PREFIX + (now ? EnumChatFormatting.GREEN + "Tracker enabled." : EnumChatFormatting.RED + "Tracker paused."));
                });
            });
        });
    }
}