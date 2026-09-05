package io.hamlook.aetheria.features.farming.farmingtracker;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.command.brigadier.CommandCategory;
import io.hamlook.aetheria.command.brigadier.CommandRegistrationEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import net.minecraft.util.EnumChatFormatting;

@RegisterEvents
public class FarmingTrackerCommand {

    private static final String PREFIX = EnumChatFormatting.AQUA + "[Farming Tracker] " + EnumChatFormatting.RESET;

    @HandleEvent
    public void onCommandRegistration(CommandRegistrationEvent event) {
        event.registerBrigadier("asmfarming", builder -> {
            builder.description = "Farming tracker commands";
            builder.setCategory(CommandCategory.USERS_ACTIVE);

            builder.literal("on", onBuilder -> {
                onBuilder.simpleCallback(() -> {
                    if (ATHRConfig.feature == null) {
                        ChatUtils.sendMessage(PREFIX + EnumChatFormatting.RED + "Config not loaded yet.");
                        return;
                    }
                    ATHRConfig.feature.farming.farmingTracker.enabled = true;
                    ATHRConfig.saveConfig();
                    ChatUtils.sendMessage(PREFIX + EnumChatFormatting.GREEN + "Tracker enabled.");
                });
            });

            builder.literal("off", offBuilder -> {
                offBuilder.simpleCallback(() -> {
                    if (ATHRConfig.feature == null) {
                        ChatUtils.sendMessage(PREFIX + EnumChatFormatting.RED + "Config not loaded yet.");
                        return;
                    }
                    ATHRConfig.feature.farming.farmingTracker.enabled = false;
                    ATHRConfig.saveConfig();
                    ChatUtils.sendMessage(PREFIX + EnumChatFormatting.RED + "Tracker disabled.");
                });
            });

            builder.literal("reset", resetBuilder -> {
                resetBuilder.simpleCallback(() -> {
                    FarmingTracker.reset();
                    ChatUtils.sendMessage(PREFIX + EnumChatFormatting.GREEN + "Tracker data has been reset.");
                });
            });
        });
    }
}
