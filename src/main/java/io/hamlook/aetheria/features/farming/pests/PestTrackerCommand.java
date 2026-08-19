package io.hamlook.aetheria.features.farming.pests;

import io.hamlook.aetheria.command.ASMCommand;
import io.hamlook.aetheria.init.RegisterCommand;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RegisterCommand
public class PestTrackerCommand extends ASMCommand {

    private static final String PREFIX = EnumChatFormatting.DARK_AQUA + "[PestTracker] " + EnumChatFormatting.RESET;

    @Override
    public String getName() {
        return "pesttracker";
    }

    @Override
    public String getUsage() {
        return "/pesttracker <reset|show|hide|toggle>";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("pest", "pt");
    }

    @Override
    public void execute(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            ChatUtils.sendMessage(PREFIX + EnumChatFormatting.YELLOW + "Usage: /pesttracker <reset|show|hide|toggle>");
            return;
        }

        PestStats stats = PestStats.getInstance();
        switch (args[0].toLowerCase()) {
            case "reset":
                stats.reset();
                ChatUtils.sendMessage(PREFIX + EnumChatFormatting.GREEN + "Pest Tracker data reset.");
                break;
            case "show":
                stats.setOverlayVisible(true);
                ChatUtils.sendMessage(PREFIX + EnumChatFormatting.GREEN + "Pest Tracker overlay shown.");
                break;
            case "hide":
                stats.setOverlayVisible(false);
                ChatUtils.sendMessage(PREFIX + EnumChatFormatting.RED + "Pest Tracker overlay hidden.");
                break;
            case "toggle":
                stats.setOverlayVisible(!stats.isOverlayVisible());
                ChatUtils.sendMessage(PREFIX + (stats.isOverlayVisible() ? EnumChatFormatting.GREEN + "Pest Tracker overlay shown." : EnumChatFormatting.RED + "Pest Tracker overlay hidden."));
                break;
            default:
                ChatUtils.sendMessage(PREFIX + EnumChatFormatting.RED + "Unknown subcommand. Use: reset, show, hide, toggle");
        }
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) return Arrays.asList("reset", "show", "hide", "toggle");
        return Collections.emptyList();
    }
}
