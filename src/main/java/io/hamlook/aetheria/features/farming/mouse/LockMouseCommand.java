package io.hamlook.aetheria.features.farming.mouse;

import io.hamlook.aetheria.command.ASMCommand;
import io.hamlook.aetheria.init.RegisterCommand;
import net.minecraft.command.ICommandSender;

@RegisterCommand
public class LockMouseCommand extends ASMCommand {

    @Override
    public String getName() {
        return "lockyp";
    }

    @Override
    public String getUsage() {
        return "/lockyp";
    }

    @Override
    public void execute(ICommandSender sender, String[] args) {
        LockMouse.setLocked(!LockMouse.isLocked());
    }
}