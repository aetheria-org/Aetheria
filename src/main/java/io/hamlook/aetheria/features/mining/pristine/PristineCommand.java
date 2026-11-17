package io.hamlook.aetheria.features.mining.pristine;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.command.brigadier.CommandCategory;
import io.hamlook.aetheria.command.brigadier.CommandRegistrationEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import net.minecraft.util.EnumChatFormatting;

import java.util.Collections;

@RegisterEvents
public class PristineCommand {

    private static final String PREFIX = EnumChatFormatting.LIGHT_PURPLE + "[Pristine] " + EnumChatFormatting.RESET;

    @HandleEvent
    public void onCommandRegistration(CommandRegistrationEvent event) {
        event.registerBrigadier("pristinetracker", builder -> {
            builder.setAliases(Collections.singletonList("prt"));
            builder.description = "Pristine tracker commands";
            builder.setCategory(CommandCategory.USERS_ACTIVE);

            builder.literal("reset", resetBuilder -> {
                resetBuilder.description = "Reset pristine tracker data";
                resetBuilder.simpleCallback(() -> {
                    PristineStats.getInstance().reset();
                    ChatUtils.sendMessage(PREFIX + EnumChatFormatting.GREEN + "Pristine tracker data has been reset.");
                });
            });

            builder.literal("toggle", toggleBuilder -> {
                toggleBuilder.description = "Toggle pristine tracking";
                toggleBuilder.simpleCallback(() -> {
                    boolean now = PristineStats.getInstance().toggleTracking();
                    ChatUtils.sendMessage(PREFIX + (now ? EnumChatFormatting.GREEN + "Tracker enabled." : EnumChatFormatting.RED + "Tracker paused."));
                });
            });
        });
    }
}
