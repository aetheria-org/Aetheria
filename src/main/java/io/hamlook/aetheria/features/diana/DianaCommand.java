package io.hamlook.aetheria.features.diana;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.command.brigadier.CommandCategory;
import io.hamlook.aetheria.command.brigadier.CommandRegistrationEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import net.minecraft.util.EnumChatFormatting;

@RegisterEvents
public class DianaCommand {

    private static final String PREFIX = EnumChatFormatting.DARK_AQUA + "[Diana] " + EnumChatFormatting.RESET;

    @HandleEvent
    public void onCommandRegistration(CommandRegistrationEvent event) {
        event.registerBrigadier("diana", builder -> {
            builder.description = "Diana tracker commands";
            builder.setCategory(CommandCategory.USERS_ACTIVE);

            builder.literal("reset", resetBuilder -> {
                resetBuilder.description = "Reset diana stats";
                resetBuilder.simpleCallback(() -> {
                    DianaStats s = DianaStats.getInstance();
                    s.reset();
                    s.save();
                    ChatUtils.sendMessage(PREFIX + EnumChatFormatting.GREEN + "Diana stats have been reset.");
                });
            });

            builder.literal("toggle", toggleBuilder -> {
                toggleBuilder.description = "Toggle diana tracking";
                toggleBuilder.simpleCallback(() -> {
                    DianaStats s = DianaStats.getInstance();
                    boolean now = s.toggleTracking();
                    ChatUtils.sendMessage(PREFIX + (now ? EnumChatFormatting.GREEN + "Tracking enabled." : EnumChatFormatting.RED + "Tracking paused."));
                });
            });
        });
    }
}