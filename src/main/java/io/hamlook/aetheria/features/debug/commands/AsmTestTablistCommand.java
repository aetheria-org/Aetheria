package io.hamlook.aetheria.features.debug.commands;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.command.brigadier.CommandCategory;
import io.hamlook.aetheria.command.brigadier.CommandRegistrationEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.compat.ClipboardCompat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.EnumChatFormatting;

import java.util.Arrays;

@RegisterEvents
public class AsmTestTablistCommand {

    @HandleEvent
    public void onCommandRegistration(CommandRegistrationEvent event) {
        event.registerBrigadier("asmtesttablist", builder -> {
            builder.description = "Toggles a fake tablist read from your clipboard";
            builder.setCategory(CommandCategory.DEVELOPER_DEBUG);
            builder.simpleCallback(() -> {
                if (TabListDebugCache.isActive()) {
                    TabListDebugCache.clear();
                    ChatUtils.sendMessage(EnumChatFormatting.YELLOW + "Disabled fake tablist debug.");
                    return;
                }

                String clipboard = ClipboardCompat.getClipboard();
                if (clipboard == null || clipboard.trim().isEmpty()) {
                    ChatUtils.sendMessage(EnumChatFormatting.RED + "Your clipboard is empty! Copy a tablist dump first.");
                    return;
                }

                TabListDebugCache.set(Arrays.asList(clipboard.split("\n")));
                ChatUtils.sendMessage(EnumChatFormatting.GREEN + "Enabled fake tablist debug from your clipboard. Run this again to disable.");
            });
        });
    }
}
