package io.hamlook.aetheria.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;

import java.util.Collections;
import java.util.List;

public abstract class ASMCommand extends CommandBase {

    protected ASMCommand() {
        CommandRegistry.register(getName(), getAliases());
    }

    public abstract String getName();
    public abstract String getUsage();

    public abstract void execute(ICommandSender sender, String[] args) throws CommandException;

    public List<String> getAliases() { return Collections.emptyList(); }

    @Override public List<String> getCommandAliases()                     { return getAliases(); }
    @Override public String getCommandName()                              { return getName(); }
    @Override public String getCommandUsage(ICommandSender sender)        { return getUsage(); }
    @Override public boolean canCommandSenderUseCommand(ICommandSender s) { return true; }
    @Override public int getRequiredPermissionLevel()                     { return 0; }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        return Collections.emptyList();
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        execute(sender, args);
    }

}
