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

/**
 * /asmcopybossbar [-nocolor] — copies the vanilla title/subtitle text currently
 * displayed (used by some SkyBlock effects as a pseudo boss bar on this MC version,
 * since real boss bar packets don't exist pre-1.9).
 *
 * <p>Reads GuiIngame's title/subtitle fields via reflection by matching on
 * declared type + field name, so it doesn't depend on exact SRG/MCP field names.
 */
@RegisterCommand
public class AsmCopyBossbarCommand extends ASMCommand {

    @Override
    public String getName() {
        return "asmcopybossbar";
    }

    @Override
    public String getUsage() {
        return "/asmcopybossbar [-nocolor]";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("asmcopybb");
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, net.minecraft.util.BlockPos pos) {
        if (args.length == 1) return Collections.singletonList("-nocolor");
        return Collections.emptyList();
    }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        boolean noColor = Arrays.asList(args).contains("-nocolor");

        String title = readTitleField("title");
        String subTitle = readTitleField("subtitle");

        if ((title == null || title.isEmpty()) && (subTitle == null || subTitle.isEmpty())) {
            ChatUtils.sendMessage(EnumChatFormatting.YELLOW + "No title/subtitle text is currently displayed.");
            return;
        }

        if (noColor) {
            if (title != null) title = StringUtils.stripControlCodes(title);
            if (subTitle != null) subTitle = StringUtils.stripControlCodes(subTitle);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== TITLE ===\n").append(title == null ? "(none)" : title).append("\n");
        sb.append("=== SUBTITLE ===\n").append(subTitle == null ? "(none)" : subTitle).append("\n");

        GuiScreen.setClipboardString(sb.toString());
        ChatUtils.sendMessage(EnumChatFormatting.YELLOW + "Copied title/subtitle text to clipboard "
            + (noColor ? "without" : "with") + " formatting codes!");
    }

    /** Best-effort reflection lookup: any GuiIngame field whose name contains the
     * given keyword ("title"/"subtitle") and whose type is IChatComponent or String. */
    private static String readTitleField(String keyword) {
        try {
            Object tabList = Minecraft.getMinecraft().ingameGUI;
            for (Field field : Minecraft.getMinecraft().ingameGUI.getClass().getDeclaredFields()) {
                String name = field.getName().toLowerCase();
                if (!name.contains(keyword)) continue;
                if (name.contains("sub") && keyword.equals("title")) continue; // don't match subTitle when looking for title
                if (!IChatComponent.class.isAssignableFrom(field.getType()) && field.getType() != String.class) continue;

                field.setAccessible(true);
                Object value = field.get(tabList);
                if (value == null) return null;
                if (value instanceof IChatComponent) return ((IChatComponent) value).getFormattedText();
                return value.toString();
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
