package io.hamlook.aetheria.features.profile.viewer.cmd;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.command.ASMCommand;
import io.hamlook.aetheria.features.profile.viewer.ui.ProfileViewerGUI;
import io.hamlook.aetheria.init.RegisterCommand;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;

@RegisterCommand
public class ProfileViewCommand extends ASMCommand {
    @Override
    public String getName() {
        return "pv";
    }

    @Override
    public String getUsage() {
        return "/" + getName();
    }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        if(!(sender instanceof EntityPlayer)) return;
        if(args.length < 1){
            ATHRConfig.screenToOpen = new ProfileViewerGUI(Minecraft.getMinecraft().getSession().getUsername());
            return;
        }
        String user = args[0];
        ATHRConfig.screenToOpen = new ProfileViewerGUI(user);
    }
}
