package io.hamlook.aetheria.features.debug.commands;

import io.hamlook.aetheria.command.ASMCommand;
import io.hamlook.aetheria.init.RegisterCommand;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StringUtils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** /asmcopytabfooter [-nocolor] — copies the tab list footer text to the clipboard. */
@RegisterCommand
public class AsmCopyTabFooterCommand extends ASMCommand {

    @Override
    public String getName() {
        return "asmcopytabfooter";
    }

    @Override
    public String getUsage() {
        return "/asmcopytabfooter [-nocolor]";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("asmcopytf");
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, net.minecraft.util.BlockPos pos) {
        if (args.length == 1) return Collections.singletonList("-nocolor");
        return Collections.emptyList();
    }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        boolean noColor = Arrays.asList(args).contains("-nocolor");

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) {
            ChatUtils.sendMessage(EnumChatFormatting.RED + "Not in a world.");
            return;
        }

        IChatComponent footer = readFooterField();
        if (footer == null) {
            ChatUtils.sendMessage(EnumChatFormatting.YELLOW + "Tab footer is null (no footer present).");
            return;
        }

        String formatted = footer.getFormattedText();
        String text = noColor ? StringUtils.stripControlCodes(formatted) : formatted;

        GuiScreen.setClipboardString(text);
        ChatUtils.sendMessage(EnumChatFormatting.GREEN + "Copied tab footer to clipboard.");
    }

    /** Reflection lookup for GuiPlayerTabOverlay's footer field, since the SRG/MCP
     * name (field_175255_h) isn't stable across mappings. */
    private static IChatComponent readFooterField() {
        try {
            Object tabList = Minecraft.getMinecraft().ingameGUI.getTabList();
            Field f = tabList.getClass().getDeclaredField("field_175255_h");
            f.setAccessible(true);
            return (IChatComponent) f.get(tabList);
        } catch (Exception ignored) {
            return null;
        }
    }
}
