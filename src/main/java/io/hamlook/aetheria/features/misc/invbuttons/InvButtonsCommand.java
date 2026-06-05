package io.hamlook.aetheria.features.misc.invbuttons;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.command.ASMCommand;
import io.hamlook.aetheria.init.RegisterCommand;
import net.minecraft.command.ICommandSender;
import java.util.Arrays;
import java.util.List;

@RegisterCommand
public class InvButtonsCommand extends ASMCommand {
    @Override
    public String getName() { return "athrbuttons"; }

    @Override
    public List<String> getAliases() { return Arrays.asList("aetheriabuttons", "jefbuttons", "asmbuttons"); }

    @Override
    public String getUsage() {
        return "/athrbuttons";
    }

    @Override
    public void execute(ICommandSender sender, String[] args) {
        ATHRConfig.openInvButtonEditor();
    }
}