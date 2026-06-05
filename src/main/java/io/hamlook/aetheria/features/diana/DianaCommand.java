package io.hamlook.aetheria.features.diana;

import io.hamlook.aetheria.command.ASMCommand;
import io.hamlook.aetheria.init.RegisterCommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;


@RegisterCommand
public class DianaCommand extends ASMCommand {

    private static final String PREFIX = EnumChatFormatting.DARK_AQUA + "[Diana] " + EnumChatFormatting.RESET;

    @Override
    public String getName() {
        return "diana";
    }

    @Override
    public String getUsage() {
        return "/diana <reset|toggle>";
    }

    @Override
    public void execute(ICommandSender sender, String[] args) {
        DianaStats s = DianaStats.getInstance();

        if (args.length == 0) {
            sender.addChatMessage(new ChatComponentText(PREFIX + EnumChatFormatting.YELLOW + "Usage: /diana <reset|toggle>"));
            return;
        }

        switch (args[0].toLowerCase()) {
            case "reset":
                s.reset();
                s.save();
                sender.addChatMessage(new ChatComponentText(PREFIX + EnumChatFormatting.GREEN + "Diana stats have been reset."));
                break;

            case "toggle":
                boolean now = s.toggleTracking();
                sender.addChatMessage(new ChatComponentText(PREFIX + (now ? EnumChatFormatting.GREEN + "Tracking enabled." : EnumChatFormatting.RED + "Tracking paused.")));
                break;

            default:
                sender.addChatMessage(new ChatComponentText(PREFIX + EnumChatFormatting.RED + "Unknown subcommand. Use: reset, toggle"));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) return Arrays.asList("reset", "toggle");
        return Collections.emptyList();
    }
}