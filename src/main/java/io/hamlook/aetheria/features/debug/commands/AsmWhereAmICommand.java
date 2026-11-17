package io.hamlook.aetheria.features.debug.commands;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.command.brigadier.CommandCategory;
import io.hamlook.aetheria.command.brigadier.CommandRegistrationEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.data.SkyblockData;
import io.hamlook.aetheria.utils.data.TablistParser;
import net.minecraft.util.EnumChatFormatting;

import java.util.Collections;

@RegisterEvents
public class AsmWhereAmICommand {

    @HandleEvent
    public void onCommandRegistration(CommandRegistrationEvent event) {
        event.registerBrigadier("asmwhereami", builder -> {
            builder.setAliases(Collections.singletonList("asmwhere"));
            builder.description = "Prints the currently detected island/location in chat";
            builder.setCategory(CommandCategory.DEVELOPER_DEBUG);
            builder.simpleCallback(() -> {
                if (!SkyblockData.isOnSkyblock()) {
                    ChatUtils.sendMessage(EnumChatFormatting.YELLOW + "You are not currently on SkyBlock.");
                    return;
                }

                SkyblockData.Location location = SkyblockData.getCurrentLocation();
                String prefix = TablistParser.getServerPrefix();

                if (location == SkyblockData.Location.NONE) {
                    ChatUtils.sendMessage(EnumChatFormatting.RED + "On SkyBlock, but the current location is unknown. Server prefix: '" + prefix + "'");
                    return;
                }

                ChatUtils.sendMessage(EnumChatFormatting.YELLOW + "You are currently in " + location + EnumChatFormatting.GRAY + " (server: '" + prefix + "')");
            });
        });
    }
}
