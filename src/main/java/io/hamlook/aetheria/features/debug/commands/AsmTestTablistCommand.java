package io.hamlook.aetheria.features.debug.commands;

import io.hamlook.aetheria.command.ASMCommand;
import io.hamlook.aetheria.init.RegisterCommand;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.EnumChatFormatting;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * /asmtesttablist — toggles a fake tablist read from your clipboard, so
 * /asmcopytablist (and anything you point at TabListDebugCache) reports that
 * instead of the real one. Run again to disable. Paste a tablist dump
 * (from someone else's /asmcopytablist) into your clipboard first.
 */
@RegisterCommand
public class AsmTestTablistCommand extends ASMCommand {

    @Override
    public String getName() {
        return "asmtesttablist";
    }

    @Override
    public String getUsage() {
        return "/asmtesttablist";
    }

    @Override
    public List<String> getAliases() {
        return Collections.emptyList();
    }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        if (TabListDebugCache.isActive()) {
            TabListDebugCache.clear();
            ChatUtils.sendMessage(EnumChatFormatting.YELLOW + "Disabled fake tablist debug.");
            return;
        }

        String clipboard = GuiScreen.getClipboardString();
        if (clipboard == null || clipboard.trim().isEmpty()) {
            ChatUtils.sendMessage(EnumChatFormatting.RED + "Your clipboard is empty! Copy a tablist dump first.");
            return;
        }

        TabListDebugCache.set(Arrays.asList(clipboard.split("\n")));
        ChatUtils.sendMessage(EnumChatFormatting.GREEN + "Enabled fake tablist debug from your clipboard. Run this again to disable.");
    }
}
