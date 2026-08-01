package io.hamlook.aetheria.features.chat.globalchat;

import io.hamlook.aetheria.command.ASMCommand;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.chat.globalchat.image.GCImage;
import io.hamlook.aetheria.features.chat.globalchat.image.ImageManager;
import io.hamlook.aetheria.features.chat.globalchat.ui.ChatUI;
import io.hamlook.aetheria.features.chat.globalchat.vars.*;
import io.hamlook.aetheria.init.RegisterCommand;
import io.hamlook.aetheria.network.NetworkGuard;
import io.hamlook.aetheria.repo.CapeAPI;
import io.hamlook.aetheria.utils.ElectionUtils;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RegisterCommand
public class GChatCommand extends ASMCommand {

    @Override
    public String getName() {
        return "globalchat";
    }

    @Override
    public String getUsage() {
        return "/" + getName();
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("gchat","g-chat");
    }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        if(!NetworkGuard.apiAllowed()){
            ChatUtils.sendMessage("§cYou cannot use Global Chat without API Access, Enable API Access in Config First.");
            return;
        }
        ChatUtils.sendMessage("§7Checking Global Chat access...");
        CompletableFuture.runAsync(() -> {
            int status = checkAccess();
            Minecraft.getMinecraft().addScheduledTask(() -> {
                if (status == 403) {
                    ChatUtils.sendMessage("§cYou are banned from Global Chat.");
                } else {
                    GlobalChat.refreshChannels(false);
                    ATHRConfig.screenToOpen = new ChatUI();
                }
            });
        });
    }

    /** Server-side access check. The server is the source of truth; this is only a cosmetic pre-check. */
    private int checkAccess() {
        try {
            URL url = new URL(CapeAPI.getAPIUrl("chat-access"));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Aetheria/" + io.hamlook.aetheria.Aetheria.VERSION);
            connection.setRequestProperty("username", Minecraft.getMinecraft().getSession().getUsername().toLowerCase());
            connection.setReadTimeout(10000);
            connection.setConnectTimeout(10000);
            int code = connection.getResponseCode();
            ElectionUtils.readResponse(connection);
            return code;
        } catch (Exception e) {
            return -1;
        }
    }
}
