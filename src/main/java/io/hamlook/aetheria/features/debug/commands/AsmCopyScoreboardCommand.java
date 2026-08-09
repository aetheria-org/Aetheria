package io.hamlook.aetheria.features.debug.commands;

import io.hamlook.aetheria.command.ASMCommand;
import io.hamlook.aetheria.init.RegisterCommand;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.data.SkyblockData;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** /asmcopyscoreboard [-nocolor] — copies the sidebar title + every line to the clipboard. */
@RegisterCommand
public class AsmCopyScoreboardCommand extends ASMCommand {

    @Override
    public String getName() {
        return "asmcopyscoreboard";
    }

    @Override
    public String getUsage() {
        return "/asmcopyscoreboard [-nocolor]";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("asmcopysb");
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, net.minecraft.util.BlockPos pos) {
        if (args.length == 1) return Collections.singletonList("-nocolor");
        return Collections.emptyList();
    }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        boolean noColor = Arrays.asList(args).contains("-nocolor");

        String title = SkyblockData.getScoreboardTitle();
        List<String> lines = SkyblockData.getScoreboardLines();

        if (title == null) {
            ChatUtils.sendMessage(EnumChatFormatting.YELLOW + "No scoreboard sidebar objective is currently active.");
            return;
        }

        List<String> result = new ArrayList<>();
        result.add("Title:");
        result.add(noColor ? StringUtils.stripControlCodes(title) : title);
        result.add("");
        result.add("Lines (" + lines.size() + "):");
        for (String line : lines) {
            result.add(" '" + (noColor ? StringUtils.stripControlCodes(line) : line) + "'");
        }

        GuiScreen.setClipboardString(String.join("\n", result));
        ChatUtils.sendMessage(EnumChatFormatting.YELLOW + "Scoreboard copied to clipboard!");
    }
}
