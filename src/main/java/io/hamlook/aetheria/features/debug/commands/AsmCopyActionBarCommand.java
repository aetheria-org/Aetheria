package io.hamlook.aetheria.features.debug.commands;

import io.hamlook.aetheria.command.ASMCommand;
import io.hamlook.aetheria.features.misc.SkyblockExp.ActionBarDispatcher;
import io.hamlook.aetheria.init.RegisterCommand;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.EnumChatFormatting;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** /asmcopyactionbar [-nocolor] — copies the last received action bar text. */
@RegisterCommand
public class AsmCopyActionBarCommand extends ASMCommand {

    @Override
    public String getName() {
        return "asmcopyactionbar";
    }

    @Override
    public String getUsage() {
        return "/asmcopyactionbar [-nocolor]";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("asmcopyab");
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, net.minecraft.util.BlockPos pos) {
        if (args.length == 1) return Collections.singletonList("-nocolor");
        return Collections.emptyList();
    }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        boolean noColor = Arrays.asList(args).contains("-nocolor");

        String actionBar = noColor ? ActionBarDispatcher.lastActionBarStripped : ActionBarDispatcher.lastActionBarFormatted;
        if (actionBar == null || actionBar.isEmpty()) {
            ChatUtils.sendMessage(EnumChatFormatting.YELLOW + "No action bar text received yet.");
            return;
        }

        GuiScreen.setClipboardString(actionBar);
        ChatUtils.sendMessage(EnumChatFormatting.YELLOW + "Action bar copied to clipboard " + (noColor ? "without" : "with") + " formatting codes!");
    }
}
