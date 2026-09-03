package io.hamlook.aetheria.features.mining.pristine;

import io.hamlook.aetheria.command.ASMCommand;
import io.hamlook.aetheria.init.RegisterCommand;
import io.hamlook.aetheria.utils.compat.TextCompat;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RegisterCommand
public class PristineCommand extends ASMCommand {

    private static final String PREFIX = EnumChatFormatting.LIGHT_PURPLE + "[Pristine] " + EnumChatFormatting.RESET;

    @Override
    public String getName() {
        return "pristinetracker";
    }

    @Override
    public String getUsage() {
        return "/pristinetracker <reset|toggle>";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("prt");
    }

    @Override
    public void execute(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.addChatMessage(TextCompat.createText(PREFIX + EnumChatFormatting.YELLOW + "Usage: /prt <reset|toggle>"));
            return;
        }

        PristineStats stats = PristineStats.getInstance();

        switch (args[0].toLowerCase()) {
            case "reset":
                stats.reset();
                sender.addChatMessage(TextCompat.createText(PREFIX + EnumChatFormatting.GREEN + "Pristine tracker data has been reset."));
                break;

            case "toggle":
                boolean now = stats.toggleTracking();
                sender.addChatMessage(TextCompat.createText(PREFIX + (now ? EnumChatFormatting.GREEN + "Tracker enabled." : EnumChatFormatting.RED + "Tracker paused.")));
                break;

            default:
                sender.addChatMessage(TextCompat.createText(PREFIX + EnumChatFormatting.RED + "Unknown subcommand. Use: reset, toggle"));
        }
    }

    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) return Arrays.asList("reset", "toggle");
        return Collections.emptyList();
    }
}
