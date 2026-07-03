package io.hamlook.aetheria.features.capes;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.command.ASMCommand;
import io.hamlook.aetheria.features.capes.ui.CapeSelectorGUI;
import io.hamlook.aetheria.init.RegisterCommand;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;

@RegisterCommand
public class CapeMenuCommand extends ASMCommand {
    @Override
    public String getName() {
        return "capes";
    }

    @Override
    public String getUsage() {
        return "/" + getName();
    }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        if(!(sender instanceof EntityPlayer)) return;
        if(args.length > 0 && args[0].equalsIgnoreCase("capeCalls")){
            ChatUtils.sendMessage("§7[§6DEBUG§7]§a Cape Calls: §f" + CapeManager.capeCalls);
        }
        ATHRConfig.screenToOpen = new CapeSelectorGUI();
    }
}
