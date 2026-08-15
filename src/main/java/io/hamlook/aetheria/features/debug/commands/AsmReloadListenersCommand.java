package io.hamlook.aetheria.features.debug.commands;

import io.hamlook.aetheria.command.ASMCommand;
import io.hamlook.aetheria.init.EventRegistrar;
import io.hamlook.aetheria.init.RegisterCommand;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.EnumChatFormatting;

import java.util.Collections;
import java.util.List;

/**
 * /asmreloadlisteners — re-registers every tracked Aetheria event listener onto
 * the Forge event bus (safe to run even if they were never stopped — it
 * unregisters then re-registers, so it never double-fires).
 */
@RegisterCommand
public class AsmReloadListenersCommand extends ASMCommand {

    @Override
    public String getName() {
        return "asmreloadlisteners";
    }

    @Override
    public String getUsage() {
        return "/asmreloadlisteners";
    }

    @Override
    public List<String> getAliases() {
        return Collections.emptyList();
    }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        int count = EventRegistrar.getRegisteredEventInstances().size();
        EventRegistrar.reloadAllListeners();
        ChatUtils.sendMessage(EnumChatFormatting.YELLOW + "Reloaded " + count + " Aetheria event listeners.");
    }
}
