package io.hamlook.aetheria.features.debug.commands;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.command.brigadier.CommandCategory;
import io.hamlook.aetheria.command.brigadier.CommandRegistrationEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.data.TablistParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import io.hamlook.aetheria.utils.compat.ClipboardCompat;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * /asmcopytablist [-nocolor] — copies every tablist entry (raw + stripped) to the
 * clipboard, in the same order TablistParser iterates them (team-then-name
 * sorted). If /asmtesttablist has a fake tablist active, copies that instead.
 */
@RegisterEvents
public class AsmCopyTablistCommand {

    @HandleEvent
    public void onCommandRegistration(CommandRegistrationEvent event) {
        event.registerBrigadier("asmcopytablist", builder -> {
            builder.setAliases(Collections.singletonList("asmcopytab"));
            builder.description = "Copies every tablist entry (raw + stripped) to the clipboard";
            builder.setCategory(CommandCategory.DEVELOPER_DEBUG);

            builder.legacyCallbackArgs(args -> {
                boolean noColor = Arrays.asList(args).contains("-nocolor");

                if (TabListDebugCache.isActive()) {
                    List<String> cached = TabListDebugCache.get();
                    List<String> out = new ArrayList<>();
                    out.add("=== FAKE TABLIST (" + cached.size() + " lines, from /asmtesttablist) ===");
                    for (String line : cached) {
                        out.add(noColor ? StringUtils.stripControlCodes(line) : line);
                    }
                    ClipboardCompat.setClipboard(String.join("\n", out));
                    ChatUtils.sendMessage(EnumChatFormatting.YELLOW + "Copied the active FAKE tablist. Run /asmtesttablist again to disable it.");
                    return;
                }

                Minecraft mc = MinecraftCompat.getMinecraft();
                if (MinecraftCompat.getLocalPlayer() == null) {
                    ChatUtils.sendMessage(EnumChatFormatting.RED + "Not in a world.");
                    return;
                }

                GuiPlayerTabOverlay tab = mc.ingameGUI.getTabList();
                List<NetworkPlayerInfo> infos = TablistParser.getParserOrderedInfos(mc);

                StringBuilder sb = new StringBuilder();
                sb.append("=== TABLIST (").append(infos.size()).append(" entries) ===\n");

                for (NetworkPlayerInfo info : infos) {
                    String raw = tab.getPlayerName(info);
                    String stripped = StringUtils.stripControlCodes(raw != null ? raw : "").trim();

                    sb.append(noColor ? stripped : (raw != null ? raw : "(null)")).append("\n");
                }

                ClipboardCompat.setClipboard(sb.toString());
                ChatUtils.sendMessage(EnumChatFormatting.GREEN + "Copied " + infos.size() + " tablist entries to clipboard.");
            });
        });
    }
}
