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
import io.hamlook.aetheria.utils.CommunityAccess;
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
        CommunityAccess.runIfAllowed(
                "§cGlobal Chat requires your account to be Synced (use /sync) or to be on SkyBlock.",
                this::openChatUi
        );
    }

    private void openChatUi() {
        ChatUtils.sendMessage("§7Checking Global Chat access...");
        CompletableFuture.runAsync(() -> {
            CheckResult result = checkAccess();
            Minecraft.getMinecraft().addScheduledTask(() -> {
                if (result.status == 403) {
                    GlobalChat.pushSystemNotice(result.message != null ? result.message : "You are banned from Global Chat.");
                } else if (result.status == -1) {
                    // Pre-check itself failed (timeout/DNS/etc), as opposed to succeeding with a
                    // non-403 status. The server is still the source of truth for send permission,
                    // so we still open the UI - just let the user know the check didn't complete
                    // rather than silently treating it the same as a passed check.
                    GlobalChat.pushSystemNotice("Couldn't verify Global Chat access - opening anyway, some actions may fail.");
                    GlobalChat.refreshChannels(false);
                } else {
                    GlobalChat.refreshChannels(false);
                }
                ATHRConfig.screenToOpen = new ChatUI();
            });
        });
    }

    /** Server-side access check. The server is the source of truth; this is only a cosmetic pre-check. */
    private static class CheckResult {
        final int status;
        final String message;
        CheckResult(int status, String message) {
            this.status = status;
            this.message = message;
        }
    }

    private CheckResult checkAccess() {
        try {
            URL url = new URL(CapeAPI.getAPIUrl("chat-access"));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Aetheria/" + io.hamlook.aetheria.Aetheria.VERSION);
            connection.setRequestProperty("username", Minecraft.getMinecraft().getSession().getUsername().toLowerCase());
            connection.setRequestProperty("x-timezone-offset", String.valueOf(io.hamlook.aetheria.utils.TimeUtils.getLocalOffsetMinutes()));
            connection.setReadTimeout(10000);
            connection.setConnectTimeout(10000);
            int code = connection.getResponseCode();
            String body = ElectionUtils.readResponse(connection);
            String message = null;
            if (body != null && !body.trim().isEmpty() && (body.contains("error") || body.contains("message"))) {
                try {
                    com.google.gson.JsonObject obj = com.google.gson.JsonParser.parseString(body).getAsJsonObject();
                    if (obj.has("error")) message = obj.get("error").getAsString();
                    else if (obj.has("message")) message = obj.get("message").getAsString();
                } catch (Exception ignored) {
                }
            }
            return new CheckResult(code, message);
        } catch (Exception e) {
            return new CheckResult(-1, null);
        }
    }
}