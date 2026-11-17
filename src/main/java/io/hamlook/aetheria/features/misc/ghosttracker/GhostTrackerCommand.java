package io.hamlook.aetheria.features.misc.ghosttracker;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.command.brigadier.CommandCategory;
import io.hamlook.aetheria.command.brigadier.CommandRegistrationEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import net.minecraft.util.EnumChatFormatting;

import java.util.Arrays;

@RegisterEvents
public class GhostTrackerCommand {

    private static final String PREFIX = EnumChatFormatting.DARK_AQUA + "[GhostTracker] " + EnumChatFormatting.RESET;

    @HandleEvent
    public void onCommandRegistration(CommandRegistrationEvent event) {
        event.registerBrigadier("ghosttracker", builder -> {
            builder.setAliases(Arrays.asList("gt", "ghost"));
            builder.description = "Ghost tracker commands";
            builder.setCategory(CommandCategory.USERS_ACTIVE);

            builder.literal("reset", resetBuilder -> {
                resetBuilder.description = "Reset ghost tracker data";
                resetBuilder.simpleCallback(() -> {
                    GhostStats.getInstance().reset();
                    ChatUtils.sendMessage(PREFIX + EnumChatFormatting.GREEN + "Ghost Tracker data reset.");
                });
            });

            builder.literal("toggle", toggleBuilder -> {
                toggleBuilder.description = "Toggle ghost tracker";
                toggleBuilder.simpleCallback(() -> {
                    ATHRConfig.feature.misc.ghostTrackerConfig.ghostTrackerEnabled = !ATHRConfig.feature.misc.ghostTrackerConfig.ghostTrackerEnabled;
                    ATHRConfig.saveConfig();
                    boolean enabled = ATHRConfig.feature.misc.ghostTrackerConfig.ghostTrackerEnabled;
                    ChatUtils.sendMessage(PREFIX + (enabled ? EnumChatFormatting.GREEN + "Ghost Tracker enabled." : EnumChatFormatting.RED + "Ghost Tracker disabled."));
                });
            });
        });
    }
}
