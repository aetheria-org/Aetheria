package io.hamlook.aetheria.features.chatfilters;

import io.hamlook.aetheria.command.SimpleCommand;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.chatfilters.ui.ChatFilterGUI;
import io.hamlook.aetheria.init.RegisterCommand;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;

@RegisterCommand
public class ChatFilterCommand extends SimpleCommand {
    @Override
    public String getName() {
        return "athrFilters";
    }

    @Override
    public String getUsage() {
        return "/" + getName();
    }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        if(!(sender instanceof EntityPlayer)) return;
        ATHRConfig.screenToOpen = new ChatFilterGUI();
    }
}
