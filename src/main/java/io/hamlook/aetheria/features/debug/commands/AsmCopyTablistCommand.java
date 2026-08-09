package io.hamlook.aetheria.features.debug.commands;

import io.hamlook.aetheria.command.ASMCommand;
import io.hamlook.aetheria.init.RegisterCommand;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * /asmcopytablist [-nocolor] — copies every tablist entry (raw + stripped) to the
 * clipboard. If /asmtesttablist has a fake tablist active, copies that instead.
 */
@RegisterCommand
public class AsmCopyTablistCommand extends ASMCommand {

    @Override
    public String getName() {
        return "asmcopytablist";
    }

    @Override
    public String getUsage() {
        return "/asmcopytablist [-nocolor]";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("asmcopytab");
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, net.minecraft.util.BlockPos pos) {
        if (args.length == 1) return Collections.singletonList("-nocolor");
        return Collections.emptyList();
    }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        boolean noColor = Arrays.asList(args).contains("-nocolor");

        if (TabListDebugCache.isActive()) {
            List<String> cached = TabListDebugCache.get();
            List<String> out = new ArrayList<>();
            out.add("=== FAKE TABLIST (" + cached.size() + " lines, from /asmtesttablist) ===");
            for (String line : cached) {
                out.add(noColor ? StringUtils.stripControlCodes(line) : line);
            }
            GuiScreen.setClipboardString(String.join("\n", out));
            ChatUtils.sendMessage(EnumChatFormatting.YELLOW + "Copied the active FAKE tablist. Run /asmtesttablist again to disable it.");
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) {
            ChatUtils.sendMessage(EnumChatFormatting.RED + "Not in a world.");
            return;
        }

        GuiPlayerTabOverlay tab = mc.ingameGUI.getTabList();
        List<NetworkPlayerInfo> infos = new ArrayList<>(mc.thePlayer.sendQueue.getPlayerInfoMap());

        StringBuilder sb = new StringBuilder();
        sb.append("=== TABLIST (").append(infos.size()).append(" entries) ===\n");

        for (NetworkPlayerInfo info : infos) {
            String raw = tab.getPlayerName(info);
            String stripped = StringUtils.stripControlCodes(raw != null ? raw : "").trim();

            sb.append(noColor ? stripped : (raw != null ? raw : "(null)")).append("\n");
        }

        GuiScreen.setClipboardString(sb.toString());
        ChatUtils.sendMessage(EnumChatFormatting.GREEN + "Copied " + infos.size() + " tablist entries to clipboard.");
    }
}
