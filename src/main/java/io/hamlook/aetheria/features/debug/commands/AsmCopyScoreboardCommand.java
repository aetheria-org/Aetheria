package io.hamlook.aetheria.features.debug.commands;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.command.brigadier.CommandCategory;
import io.hamlook.aetheria.command.brigadier.CommandRegistrationEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.compat.ClipboardCompat;
import io.hamlook.aetheria.utils.data.SkyblockData;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RegisterEvents
public class AsmCopyScoreboardCommand {

    @HandleEvent
    public void onCommandRegistration(CommandRegistrationEvent event) {
        event.registerBrigadier("asmcopyscoreboard", builder -> {
            builder.setAliases(Collections.singletonList("asmcopysb"));
            builder.description = "Copy the sidebar scoreboard title and lines to clipboard";
            builder.setCategory(CommandCategory.DEVELOPER_DEBUG);

            builder.legacyCallbackArgs(args -> {
                boolean noColor = Arrays.asList(args).contains("-nocolor");

                String title = SkyblockData.getScoreboardTitle();
                List<String> lines = SkyblockData.getScoreboardLines();

                if (title == null) {
                    ChatUtils.sendMessage(EnumChatFormatting.YELLOW + "No scoreboard sidebar objective is currently active.");
                    return;
                }

                List<String> result = new ArrayList<>();
                result.add("Title:");
                result.add(noColor ? StringUtils.stripControlCodes(title) : title);
                result.add("");
                result.add("Lines (" + lines.size() + "):");
                for (String line : lines) {
                    result.add(" '" + (noColor ? StringUtils.stripControlCodes(line) : line) + "'");
                }

                ClipboardCompat.setClipboard(String.join("\n", result));
                ChatUtils.sendMessage(EnumChatFormatting.YELLOW + "Scoreboard copied to clipboard!");
            });
        });
    }
}
