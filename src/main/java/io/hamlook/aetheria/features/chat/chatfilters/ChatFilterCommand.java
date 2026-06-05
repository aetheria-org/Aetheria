package io.hamlook.aetheria.features.chat.chatfilters;

import io.hamlook.aetheria.command.ASMCommand;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.chat.chatfilters.ui.ChatFilterGUI;
import io.hamlook.aetheria.init.RegisterCommand;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;

import java.util.Arrays;
import java.util.List;

@RegisterCommand
public class ChatFilterCommand extends ASMCommand {
    @Override
    public String getName() {
        return "chatfilters";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("athrChatFilters",
                "athrchatfilters","acf","asmChatFilters","asmchatfilters",
                "aetheriaChatFilters","aetheriachatfilters");
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
