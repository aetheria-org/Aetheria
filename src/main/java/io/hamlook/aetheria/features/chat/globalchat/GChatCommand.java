package io.hamlook.aetheria.features.chat.globalchat;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.command.brigadier.CommandCategory;
import io.hamlook.aetheria.command.brigadier.CommandRegistrationEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.chat.globalchat.ui.ChatUI;
import io.hamlook.aetheria.features.chat.globalchat.vars.Channel;
import io.hamlook.aetheria.features.chat.globalchat.vars.ChatMessage;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.network.NetworkGuard;
import io.hamlook.aetheria.repo.CapeAPI;
import io.hamlook.aetheria.utils.CommunityAccess;
import io.hamlook.aetheria.utils.ElectionUtils;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

@RegisterEvents
public class GChatCommand {

    @HandleEvent
    public void onCommandRegistration(CommandRegistrationEvent event) {
        event.registerBrigadier("globalchat", builder -> {
            builder.setAliases(Arrays.asList("gchat", "g-chat"));
            builder.description = "Global Chat commands";
            builder.setCategory(CommandCategory.COMMUNITY);

            builder.legacyCallbackArgs(args -> {
                if (args.length > 0) {
                    if (trySend(args)) return;
                    ChatUtils.sendMessage("§c[G-CHAT] Could not Send Message. Please Try Again.");
                    return;
                }
                tryOpen();
            });
        });
    }

    public static boolean trySend(String[] args) {
        if (!NetworkGuard.requiresApi("Global Chat")) return false;
        if (!CommunityAccess.isAllowedNow()) {
            ChatUtils.sendMessage("§cGlobal Chat requires your account to be Synced (use /sync) or to be on SkyBlock.");
            return false;
        }
        if (args.length < 2) return false;
        String channel = args[0];
        Channel chnl = GlobalChat.getChannelByName(channel);
        if (chnl == null) {
            ChatUtils.sendMessage("§cCould not Find the channel of name: " + channel);
            return true;
        }
        StringBuilder builder = new StringBuilder();
        for (String s : Arrays.asList(args).subList(1, args.length)) {
            builder.append(s).append(" ");
        }
        return GlobalChat.sendMessage(new ChatMessage(builder.toString(), chnl.channelID, null));
    }

    public static void tryOpen() {
        if (!NetworkGuard.requiresApi("Global Chat")) return;
        CommunityAccess.runIfAllowed(
                "§cGlobal Chat requires your account to be Synced (use /sync) or to be on SkyBlock.",
                GChatCommand::openChatUi
        );
    }

    private static void openChatUi() {
        io.hamlook.aetheria.WebSocketClient.markActivity();
        ChatUtils.sendMessage("§7Checking Global Chat access...");
        CompletableFuture.runAsync(() -> {
            CheckResult result = checkAccess();
            MinecraftCompat.getMinecraft().addScheduledTask(() -> {
                if (result.status == 403) {
                    GlobalChat.pushSystemNotice(result.message != null ? result.message : "You are banned from Global Chat.");
                } else if (result.status == -1) {
                    GlobalChat.pushSystemNotice("Couldn't verify Global Chat access - opening anyway, some actions may fail.");
                    GlobalChat.refreshChannels(false);
                } else {
                    GlobalChat.refreshChannels(false);
                }
                ATHRConfig.screenToOpen = new ChatUI();
            });
        });
    }

    private static class CheckResult {
        final int status;
        final String message;
        CheckResult(int status, String message) {
            this.status = status;
            this.message = message;
        }
    }

    private static CheckResult checkAccess() {
        try {
            URL url = new URL(CapeAPI.getAPIUrl("chat-access"));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Aetheria/" + io.hamlook.aetheria.Aetheria.VERSION);
            connection.setRequestProperty("username", MinecraftCompat.getMinecraft().getSession().getUsername().toLowerCase());
            connection.setRequestProperty("x-timezone-offset", String.valueOf(io.hamlook.aetheria.utils.TimeUtils.getLocalOffsetMinutes()));
            connection.setReadTimeout(10000);
            connection.setConnectTimeout(10000);
            int code = connection.getResponseCode();
            String body = ElectionUtils.readResponse(connection);
            String message = null;
            if (!body.trim().isEmpty() && (body.contains("error") || body.contains("message"))) {
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
