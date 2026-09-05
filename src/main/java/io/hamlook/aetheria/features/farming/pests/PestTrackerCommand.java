package io.hamlook.aetheria.features.farming.pests;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.command.brigadier.CommandCategory;
import io.hamlook.aetheria.command.brigadier.CommandRegistrationEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import net.minecraft.util.EnumChatFormatting;

import java.util.Arrays;

@RegisterEvents
public class PestTrackerCommand {

    private static final String PREFIX = EnumChatFormatting.DARK_AQUA + "[PestTracker] " + EnumChatFormatting.RESET;

    @HandleEvent
    public void onCommandRegistration(CommandRegistrationEvent event) {
        event.registerBrigadier("pesttracker", builder -> {
            builder.setAliases(Arrays.asList("pest", "pt"));
            builder.description = "Pest tracker commands";
            builder.setCategory(CommandCategory.USERS_ACTIVE);

            builder.literal("reset", resetBuilder -> {
                resetBuilder.simpleCallback(() -> {
                    PestStats.getInstance().reset();
                    ChatUtils.sendMessage(PREFIX + EnumChatFormatting.GREEN + "Pest Tracker data reset.");
                });
            });

            builder.literal("show", showBuilder -> {
                showBuilder.simpleCallback(() -> {
                    PestStats.getInstance().setOverlayVisible(true);
                    ChatUtils.sendMessage(PREFIX + EnumChatFormatting.GREEN + "Pest Tracker overlay shown.");
                });
            });

            builder.literal("hide", hideBuilder -> {
                hideBuilder.simpleCallback(() -> {
                    PestStats.getInstance().setOverlayVisible(false);
                    ChatUtils.sendMessage(PREFIX + EnumChatFormatting.RED + "Pest Tracker overlay hidden.");
                });
            });

            builder.literal("toggle", toggleBuilder -> {
                toggleBuilder.simpleCallback(() -> {
                    PestStats stats = PestStats.getInstance();
                    stats.setOverlayVisible(!stats.isOverlayVisible());
                    ChatUtils.sendMessage(PREFIX + (stats.isOverlayVisible() ? EnumChatFormatting.GREEN + "Pest Tracker overlay shown." : EnumChatFormatting.RED + "Pest Tracker overlay hidden."));
                });
            });
        });
    }
}
