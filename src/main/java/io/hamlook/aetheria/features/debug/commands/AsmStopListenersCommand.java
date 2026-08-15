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
 * /asmstoplisteners — unregisters every tracked Aetheria event listener from the
 * Forge event bus. Use this to check whether a bug is caused by event ordering /
 * another mod's listener interfering, by isolating Aetheria's own listeners out.
 * Run /asmreloadlisteners to bring them back.
 */
@RegisterCommand
public class AsmStopListenersCommand extends ASMCommand {

    @Override
    public String getName() {
        return "asmstoplisteners";
    }

    @Override
    public String getUsage() {
        return "/asmstoplisteners";
    }

    @Override
    public List<String> getAliases() {
        return Collections.emptyList();
    }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        int count = EventRegistrar.getRegisteredEventInstances().size();
        EventRegistrar.stopAllListeners();
        ChatUtils.sendMessage(EnumChatFormatting.YELLOW + "Unregistered " + count + " Aetheria event listeners. Run /asmreloadlisteners to restore them.");
    }
}
