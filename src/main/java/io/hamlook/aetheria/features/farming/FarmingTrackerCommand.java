package io.hamlook.aetheria.features.farming;

import io.hamlook.aetheria.command.ASMCommand;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.init.RegisterCommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RegisterCommand
public class FarmingTrackerCommand extends ASMCommand {

    private static final String PREFIX = EnumChatFormatting.AQUA + "[Farming Tracker] " + EnumChatFormatting.RESET;

    @Override
    public String getName() {
        return "asmfarming";
    }

    @Override
    public String getUsage() {
        return "/asmfarming <on|off|reset>";
    }

    @Override
    public void execute(ICommandSender sender, String[] args) {
        if (args.length == 0 || ATHRConfig.feature == null) {
            sender.addChatMessage(new ChatComponentText(PREFIX + EnumChatFormatting.YELLOW + "Usage: /asmfarming <on|off|reset>"));
            return;
        }

        switch (args[0].toLowerCase()) {
            case "on":
                ATHRConfig.feature.farming.farmingTracker.enabled = true;
                ATHRConfig.saveConfig();
                sender.addChatMessage(new ChatComponentText(PREFIX + EnumChatFormatting.GREEN + "Tracker enabled."));
                break;

            case "off":
                ATHRConfig.feature.farming.farmingTracker.enabled = false;
                ATHRConfig.saveConfig();
                sender.addChatMessage(new ChatComponentText(PREFIX + EnumChatFormatting.RED + "Tracker disabled."));
                break;

            case "reset":
                FarmingTracker.reset();
                sender.addChatMessage(new ChatComponentText(PREFIX + EnumChatFormatting.GREEN + "Tracker data has been reset."));
                break;

            default:
                sender.addChatMessage(new ChatComponentText(PREFIX + EnumChatFormatting.RED + "Unknown subcommand. Use: on, off, reset"));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) return Arrays.asList("on", "off", "reset");
        return Collections.emptyList();
    }
}
