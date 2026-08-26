package io.hamlook.aetheria.features.farming.visitors;

import io.hamlook.aetheria.command.ASMCommand;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.init.RegisterCommand;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.EnumChatFormatting;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RegisterCommand
public class VisitorTipCommand extends ASMCommand {

    @Override
    public String getName() {
        return "visitortip";
    }

    @Override
    public String getUsage() {
        return "/visitortip <hide|show>";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("asmvisitortip");
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, net.minecraft.util.BlockPos pos) {
        if (args.length == 1) return Arrays.asList("hide", "show");
        return Collections.emptyList();
    }

    @Override
    public void execute(ICommandSender sender, String[] args) {
        if (ATHRConfig.feature == null || ATHRConfig.feature.farming == null || ATHRConfig.feature.farming.visitors == null) {
            ChatUtils.sendMessage(EnumChatFormatting.RED + "[ASM] Config not loaded yet.");
            return;
        }
        if (args.length != 1) {
            ChatUtils.sendMessage(EnumChatFormatting.YELLOW + "Usage: " + getUsage());
            return;
        }
        switch (args[0].toLowerCase()) {
            case "hide":
                ATHRConfig.feature.farming.visitors.shoppingListTipHidden = true;
                ATHRConfig.saveConfig();
                ChatUtils.sendMessage(EnumChatFormatting.GREEN + "[ASM] Shopping list tip hidden. " + EnumChatFormatting.GRAY + "Use /visitortip show to bring it back.");
                break;
            case "show":
                ATHRConfig.feature.farming.visitors.shoppingListTipHidden = false;
                ATHRConfig.saveConfig();
                ChatUtils.sendMessage(EnumChatFormatting.GREEN + "[ASM] Shopping list tip re-enabled.");
                break;
            default:
                ChatUtils.sendMessage(EnumChatFormatting.YELLOW + "Usage: " + getUsage());
        }
    }
}
