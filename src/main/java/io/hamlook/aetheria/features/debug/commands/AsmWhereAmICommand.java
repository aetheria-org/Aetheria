package io.hamlook.aetheria.features.debug.commands;

import io.hamlook.aetheria.command.ASMCommand;
import io.hamlook.aetheria.init.RegisterCommand;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.data.SkyblockData;
import io.hamlook.aetheria.utils.data.TablistParser;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.EnumChatFormatting;

import java.util.Collections;
import java.util.List;

/** /asmwhereami — prints the currently detected island/location in chat. */
@RegisterCommand
public class AsmWhereAmICommand extends ASMCommand {

    @Override
    public String getName() {
        return "asmwhereami";
    }

    @Override
    public String getUsage() {
        return "/asmwhereami";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("asmwhere");
    }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
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
    }
}
