package io.hamlook.aetheria.features.farming.trevor;

import io.hamlook.aetheria.command.ASMCommand;
import io.hamlook.aetheria.init.RegisterCommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

@RegisterCommand
public class PeltResetCommand extends ASMCommand {

    private static final String PREFIX = EnumChatFormatting.GOLD + "[Pelt Tracker] " + EnumChatFormatting.RESET;

    @Override
    public String getName() {
        return "resetpelt";
    }

    @Override
    public String getUsage() {
        return "/resetpelt";
    }

    @Override
    public void execute(ICommandSender sender, String[] args) {
        PeltOverlay.reset();
        sender.addChatMessage(new ChatComponentText(PREFIX + EnumChatFormatting.GREEN + "Pelt tracker reset: pelts and active time cleared."));
    }
}
