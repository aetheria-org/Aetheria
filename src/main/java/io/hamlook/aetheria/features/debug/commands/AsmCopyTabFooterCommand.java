package io.hamlook.aetheria.features.debug.commands;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.command.brigadier.CommandCategory;
import io.hamlook.aetheria.command.brigadier.CommandRegistrationEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.TextCompat;
import io.hamlook.aetheria.utils.compat.ClipboardCompat;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StringUtils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;

/** /asmcopytabfooter [-nocolor] — copies the tab list footer text to the clipboard. */
@RegisterEvents
public class AsmCopyTabFooterCommand {

    @HandleEvent
    public void onCommandRegistration(CommandRegistrationEvent event) {
        event.registerBrigadier("asmcopytabfooter", builder -> {
            builder.setAliases(Collections.singletonList("asmcopytf"));
            builder.description = "Copies the tab list footer text to the clipboard";
            builder.setCategory(CommandCategory.DEVELOPER_DEBUG);

            builder.legacyCallbackArgs(args -> {
                boolean noColor = Arrays.asList(args).contains("-nocolor");

                if (MinecraftCompat.getLocalPlayer() == null) {
                    ChatUtils.sendMessage(EnumChatFormatting.RED + "Not in a world.");
                    return;
                }

                IChatComponent footer = readFooterField();
                if (footer == null) {
                    ChatUtils.sendMessage(EnumChatFormatting.YELLOW + "Tab footer is null (no footer present).");
                    return;
                }

                String formatted = TextCompat.getFormattedText(footer);
                String text = noColor ? StringUtils.stripControlCodes(formatted) : formatted;

                ClipboardCompat.setClipboard(text);
                ChatUtils.sendMessage(EnumChatFormatting.GREEN + "Copied tab footer to clipboard.");
            });
        });
    }

    /** Reflection lookup for GuiPlayerTabOverlay's footer field, since the SRG/MCP
     * name (field_175255_h) isn't stable across mappings. */
    private static IChatComponent readFooterField() {
        try {
            Object tabList = MinecraftCompat.getMinecraft().ingameGUI.getTabList();
            Field f = tabList.getClass().getDeclaredField("field_175255_h");
            f.setAccessible(true);
            return (IChatComponent) f.get(tabList);
        } catch (Exception ignored) {
            return null;
        }
    }
}
