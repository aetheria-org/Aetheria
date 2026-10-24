package io.hamlook.aetheria.features.debug.commands;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.command.brigadier.CommandCategory;
import io.hamlook.aetheria.command.brigadier.CommandRegistrationEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.compat.ClipboardCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.TextCompat;
import io.hamlook.aetheria.utils.data.TablistParser;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;

/** /asmcopytabfooter [-nocolor] — copies the tab list footer text to the clipboard. */
@RegisterEvents
public class AsmCopyTabFooterCommand {

    @HandleEvent
    public void onCommandRegistration(CommandRegistrationEvent event) {
        event.registerBrigadier("asmcopytabfooter", builder -> {
            builder.setAliases(Collections.singletonList("asmcopytf"));
            builder.description = "Copies the tab list footer text to the clipboard";
            builder.setCategory(CommandCategory.DEVELOPER_DEBUG);

            builder.legacyCallbackArgs(args -> {
                boolean noColor = Arrays.asList(args).contains("-nocolor");

                if (MinecraftCompat.getLocalPlayer() == null) {
                    ChatUtils.sendMessage(EnumChatFormatting.RED + "Not in a world.");
                    return;
                }

                IChatComponent footer = TablistParser.getTabFooter();
                if (footer == null) {
                    ChatUtils.sendMessage(EnumChatFormatting.YELLOW + "Tab footer is null (no footer present).");
                    return;
                }

                String formatted = TextCompat.getFormattedTextWithResets(footer);
                String text = noColor ? StringUtils.stripControlCodes(formatted) : formatted;

                ClipboardCompat.setClipboard(text);
                ChatUtils.sendMessage(EnumChatFormatting.GREEN + "Copied tab footer to clipboard.");
            });
        });
    }
}
