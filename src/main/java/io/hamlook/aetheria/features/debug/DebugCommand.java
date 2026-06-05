package io.hamlook.aetheria.features.debug;

import io.hamlook.aetheria.command.ASMCommand;
import io.hamlook.aetheria.init.RegisterCommand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StringUtils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RegisterCommand
public class DebugCommand extends ASMCommand {

    private static final String PREFIX = EnumChatFormatting.GRAY + "[ATHR Debug] " + EnumChatFormatting.RESET;

    private static void copyTablist(ICommandSender sender) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) {
            sender.addChatMessage(new ChatComponentText(PREFIX + EnumChatFormatting.RED + "Not in a world."));
            return;
        }

        GuiPlayerTabOverlay tab = mc.ingameGUI.getTabList();
        List<NetworkPlayerInfo> infos = mc.thePlayer.sendQueue.getPlayerInfoMap().stream().sorted((a, b) -> {
            String ta = a.getPlayerTeam() != null ? a.getPlayerTeam().getRegisteredName() : "";
            String tb = b.getPlayerTeam() != null ? b.getPlayerTeam().getRegisteredName() : "";
            int cmp = ta.compareTo(tb);
            return cmp != 0 ? cmp : a.getGameProfile().getName().compareTo(b.getGameProfile().getName());
        }).collect(java.util.stream.Collectors.toList());

        StringBuilder sb = new StringBuilder();
        sb.append("=== TABLIST (").append(infos.size()).append(" entries) ===\n");

        for (NetworkPlayerInfo info : infos) {
            String raw = tab.getPlayerName(info);
            String stripped = StringUtils.stripControlCodes(raw != null ? raw : "").trim();

            sb.append("[RAW] ").append(raw != null ? raw : "(null)").append("\n");
            sb.append("[STR] ").append(stripped).append("\n");
            sb.append("---\n");
        }

        GuiScreen.setClipboardString(sb.toString());
        sender.addChatMessage(new ChatComponentText(PREFIX + EnumChatFormatting.GREEN + "Copied " + infos.size() + " tablist entries to clipboard."));
    }

    private static void copyTabFooter(ICommandSender sender) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) {
            sender.addChatMessage(new ChatComponentText(PREFIX + EnumChatFormatting.RED + "Not in a world."));
            return;
        }

        try {
            Field f = mc.ingameGUI.getTabList().getClass().getDeclaredField("field_175255_h");
            f.setAccessible(true);
            net.minecraft.util.IChatComponent footer = (net.minecraft.util.IChatComponent) f.get(mc.ingameGUI.getTabList());

            if (footer == null) {
                sender.addChatMessage(new ChatComponentText(PREFIX + EnumChatFormatting.YELLOW + "Tab footer is null (no footer present)."));
                return;
            }

            String formatted = footer.getFormattedText();
            String stripped = StringUtils.stripControlCodes(formatted);

            String sb = "=== TAB FOOTER ===\n" + "[RAW]\n" + formatted + "\n" + "[STRIPPED]\n" + stripped + "\n";

            GuiScreen.setClipboardString(sb);
            sender.addChatMessage(new ChatComponentText(PREFIX + EnumChatFormatting.GREEN + "Copied tab footer to clipboard."));

        } catch (Exception e) {
            sender.addChatMessage(new ChatComponentText(PREFIX + EnumChatFormatting.RED + "Failed to read tab footer: " + e.getMessage()));
        }
    }

    @Override
    public String getName() {
        return "athrdebug";
    }

    @Override
    public String getUsage() {
        return "/athrdebug <tablist|tabfooter>";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("aetheriadebug", "jdebug", "asmdebug");
    }

    @Override
    public void execute(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.addChatMessage(new ChatComponentText(PREFIX + EnumChatFormatting.YELLOW + "Usage: /athrdebug<tablist|tabfooter>"));
            return;
        }

        switch (args[0].toLowerCase()) {
            case "tablist":
                copyTablist(sender);
                break;
            case "tabfooter":
                copyTabFooter(sender);
                break;
            default:
                sender.addChatMessage(new ChatComponentText(PREFIX + EnumChatFormatting.RED + "Unknown subcommand. Use: tablist, tabfooter"));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) return Arrays.asList("tablist", "tabfooter");
        return Collections.emptyList();
    }
}