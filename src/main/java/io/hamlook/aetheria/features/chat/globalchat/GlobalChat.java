package io.hamlook.aetheria.features.chat.globalchat;

import com.google.gson.*;
import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.WebSocketClient;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.chat.globalchat.ui.Notification;
import io.hamlook.aetheria.features.chat.globalchat.vars.*;
import io.hamlook.aetheria.repo.CapeAPI;
import io.hamlook.aetheria.utils.ElectionUtils;
import io.hamlook.aetheria.utils.EmojiParser;
import io.hamlook.aetheria.utils.ThreadUtils;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.chat.ExpiringArrayList;
import net.minecraft.client.Minecraft;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GlobalChat {

    public static ConcurrentHashMap<String,Channel> channels = new ConcurrentHashMap<>();
    /** Bumped whenever the channel list changes shape or channel metadata; lets UI caches invalidate cheaply. */
    public static volatile int channelsVersion = 0;
    /** Persistent server-driven system notices (mute/ban/permission errors) shown in the G-Chat sidebar. */
    public static final CopyOnWriteArrayList<String> systemNotices = new CopyOnWriteArrayList<>();
    /** Highest system-notice sequence seen; lets the UI poll for new ones cheaply. */
    public static volatile long systemNoticesVersion = 0;
    public static final Gson GSON = new Gson();
    public static ConcurrentHashMap<String,ChatMessage> pendingMessages = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, IEmoji> usableEmojis = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, Sticker> usableStickers = new ConcurrentHashMap<>();

    public static ExpiringArrayList<Notification> notifications = new ExpiringArrayList<>();
    /** True once the missed-mentions fetch ran this session (prevents duplicate toasts on reconnect). */
    private static volatile boolean missedMentionsFetched = false;
    /** True while a fetch is in flight; blocks a second concurrent request (e.g. onOpen + channel refresh racing). */
    private static volatile boolean missedMentionsInFlight = false;
    /** Background retry timer for the initial channel load, so a failed API call at client startup isn't fatal. */
    private static final ScheduledExecutorService CHANNEL_RETRY_EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "GlobalChat-ChannelRetry");
        t.setDaemon(true);
        return t;
    });
    private static final int MAX_CHANNEL_LOAD_ATTEMPTS = 5;
    /** True while channels have never successfully loaded this session; drives the "retrying..." UI state. */
    public static volatile boolean channelsLoadFailed = false;
    /** Lower-cased Minecraft session username (the identity the server enforces against). */
    public static String getUsername() {
        return Minecraft.getMinecraft().getSession().getUsername().toLowerCase();
    }

    public static void initialise(){
        ThreadUtils.run("GlobalChat-Init", () -> {
            try{
                usableEmojis.clear();
                List<IEmoji> defaults = EmojiParser.loadDefaults();
                for (IEmoji emoji : defaults) {
                    registerIEmoji(emoji);
                }
                Aetheria.logger.info("[GlobalChat]: " + usableEmojis.size() + " default emojis usable.");
                if (defaults.isEmpty()) {
                    EmojiParser.onDefaultsLoaded(() -> {
                        for (IEmoji emoji : EmojiParser.loadDefaults()) {
                            registerIEmoji(emoji);
                        }
                        Aetheria.logger.info("[GlobalChat]: " + usableEmojis.size() + " default emojis usable.");
                    });
                }

                URL url = new URL(CapeAPI.getAPIUrl("channels"));
                loadChannels(url);
                onSocketConnected();
            }catch(Exception e){
                Aetheria.logger.log(Level.SEVERE, "[GlobalChat] Failed to initialise", e);
            }
        });
    }

    /** Called whenever the websocket (re)connects: deferred resource loads + missed-mention catch-up. */
    public static void onSocketConnected(){
        loadRemoteResourcesIfNeeded();
        fetchMissedMentions();
    }

    private static volatile boolean remoteResourcesLoaded = false;
    private static volatile boolean remoteResourcesLoading = false;

    /**
     * Loads the API-provided emoji + sticker lists. Deferred until the
     * websocket connects (see {@link #onSocketConnected()}) so a restarted API
     * is re-fetched on reconnect instead of the lists staying empty until the
     * game restarts. Also retried whenever the user opens the emoji/sticker
     * panels, in case the first attempt failed. Only marked loaded on success.
     */
    public static void loadRemoteResourcesIfNeeded(){
        if (remoteResourcesLoaded || remoteResourcesLoading) return;
        remoteResourcesLoading = true;
        CompletableFuture.runAsync(() -> {
            try {
                loadRemoteEmojis();
                loadStickers();
                remoteResourcesLoaded = true;
                Aetheria.logger.info("[GlobalChat]: Remote emoji/sticker lists loaded.");
            } catch (Exception e) {
                Aetheria.logger.warning("[GlobalChat]: Failed to load remote emoji/sticker lists: " + e.getMessage());
            } finally {
                remoteResourcesLoading = false;
            }
        });
    }

    private static void loadRemoteEmojis() throws Exception {
        URL url = new URL(CapeAPI.getAPIUrl("emoji-list"));
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Aetheria/" + Aetheria.VERSION);
        connection.setRequestProperty("Accept", "application/json");
        connection.setReadTimeout(10000);
        connection.setConnectTimeout(10000);
        if(connection.getResponseCode() == 200){
            String response = ElectionUtils.readResponse(connection);
            JsonArray array = JsonParser.parseString(response).getAsJsonArray();
            if(array != null && !array.isEmpty()){
                for(JsonElement element : array){
                    IEmoji emoji = GSON.fromJson(element, IEmoji.class);
                    if(emoji == null) continue;
                    registerIEmoji(emoji);
                }
                Aetheria.logger.info("[GlobalChat]: Successfully loaded " + usableEmojis.size() + " emojis.");
            }else if(array == null || array.isEmpty()){
                Aetheria.logger.warning("[GlobalChat]: Emoji list came back empty.");
            }
        }else{
            Aetheria.logger.info("[GlobalChat]: Could not load emojis: " + connection.getResponseCode());
        }
    }

    private static void loadStickers() {
        try{
            usableStickers.clear();
            URL url = new URL(CapeAPI.getAPIUrl("sticker-list"));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Aetheria/" + Aetheria.VERSION);
            connection.setRequestProperty("Accept", "application/json");
            connection.setReadTimeout(10000);
            connection.setConnectTimeout(10000);
            if(connection.getResponseCode() == 200){
                String response = ElectionUtils.readResponse(connection);
                JsonArray array = JsonParser.parseString(response).getAsJsonArray();
                if(array == null || array.isEmpty()) return;
                for(JsonElement element : array){
                    if(!element.isJsonObject()) continue;
                    JsonObject object = element.getAsJsonObject();
                    if(!object.has("id") || !object.has("url")) continue;
                    String id = object.get("id").getAsString();
                    String name = object.has("name") ? object.get("name").getAsString().trim() : "unknown";
                    String stickerUrl = object.get("url").getAsString();
                    List<String> tags = new ArrayList<>();
                    if(object.has("tags")){
                        JsonElement tagsEl = object.get("tags");
                        if(tagsEl.isJsonArray()){
                            for(JsonElement tag : tagsEl.getAsJsonArray()) tags.add(tag.getAsString());
                        }else if(tagsEl.isJsonPrimitive() && !tagsEl.getAsString().isEmpty()){
                            for(String tag : tagsEl.getAsString().split(",")) tags.add(tag.trim());
                        }
                    }
                    usableStickers.put(id, new Sticker(id, stickerUrl, name, tags));
                }
                Aetheria.logger.info("[GlobalChat]: Successfully loaded " + usableStickers.size() + " stickers.");
            }else{
                Aetheria.logger.info("[GlobalChat]: Could not load stickers: " + connection.getResponseCode());
            }
        }catch(Exception e){
            Aetheria.logger.warning("[GlobalChat]: Failed to load stickers: " + e.getMessage());
        }
    }

    private static void loadChannels(URL url) {
        loadChannelsWithRetry(url, 1);
    }

    /**
     * Loads the initial channel list, retrying with backoff on failure instead of leaving
     * {@code channels} permanently empty (which previously required the server to proactively
     * push a sync/refresh command to recover). Attempt 1 runs inline (matches prior behavior at
     * client startup); further attempts are scheduled on a background timer so they never block
     * the game thread.
     */
    private static void loadChannelsWithRetry(URL url, int attempt) {
        try{
            List<JsonObject> fetched = fetchChannels(url);
            if(fetched == null){
                scheduleChannelRetry(url, attempt);
                return;
            }
            for(JsonObject channel : fetched){
                boolean canSend = !channel.has("canSend") || channel.get("canSend").getAsBoolean();
                channels.put(channel.get("id").getAsString(),new Channel(channel.get("id").getAsString(),channel.get("name").getAsString(),canSend));
            }
            channelsVersion++;
            channelsLoadFailed = false;
            Aetheria.logger.info("[GlobalChat]: Successfully loaded " + channels.size() + " channels.");
            // Loaded later than startup (i.e. a retry): missed-mention catch-up was previously
            // skipped because channels.isEmpty() short-circuited it. Fire it now that we have channels.
            if(attempt > 1) fetchMissedMentions();
        }catch(Exception e){
            Aetheria.logger.warning("[GlobalChat]: Failed to load channels (attempt " + attempt + "): " + Arrays.toString(e.getStackTrace()));
            scheduleChannelRetry(url, attempt);
        }
    }

    private static void scheduleChannelRetry(URL url, int attempt) {
        channelsLoadFailed = true;
        if(attempt >= MAX_CHANNEL_LOAD_ATTEMPTS){
            Aetheria.logger.warning("[GlobalChat]: Giving up loading channels after " + attempt + " attempts.");
            pushSystemNotice("Couldn't reach Global Chat. Try /globalchat again in a moment.");
            return;
        }
        long delaySeconds = Math.min(30L, attempt * 4L);
        CHANNEL_RETRY_EXECUTOR.schedule(() -> loadChannelsWithRetry(url, attempt + 1), delaySeconds, TimeUnit.SECONDS);
    }

    private static List<JsonObject> fetchChannels(URL url) throws Exception {
        HttpURLConnection connection2 = (HttpURLConnection) url.openConnection();
        connection2.setRequestMethod("GET");
        connection2.setRequestProperty("User-Agent", "Aetheria/" + Aetheria.VERSION);
        connection2.setRequestProperty("Accept", "application/json");
        connection2.setRequestProperty("username", getUsername());
        connection2.setRequestProperty("x-timezone-offset", String.valueOf(io.hamlook.aetheria.utils.TimeUtils.getLocalOffsetMinutes()));
        connection2.setReadTimeout(10000);
        connection2.setConnectTimeout(10000);
        if(connection2.getResponseCode() != 200){
            Aetheria.logger.info("[GlobalChat]: Could not load channels: " + connection2.getResponseCode());
            return null;
        }
        String response = ElectionUtils.readResponse(connection2);
        JsonArray obj = JsonParser.parseString(response).getAsJsonArray();
        if(obj == null) return null;
        List<JsonObject> out = new ArrayList<>();
        for(JsonElement element : obj){
            JsonObject channel = element.getAsJsonObject();
            if(channel == null || channel.isEmpty()) continue;
            out.add(channel);
        }
        return out;
    }

    /**
     * Re-fetches the channel list on a background thread (never blocks the game).
     * With {@code replaceAll} (used after a Global Chat reset) the list is rebuilt
     * fresh; otherwise existing channels are kept (message history preserved), new
     * ones are added, and channels the user no longer has access to are dropped.
     * On failure the current list is left untouched.
     */
    public static void refreshChannels(boolean replaceAll) {
        CompletableFuture.runAsync(() -> {
            try {
                List<JsonObject> fetched = fetchChannels(new URL(CapeAPI.getAPIUrl("channels")));
                if(fetched == null) return;
                if(replaceAll) channels.clear();
                Set<String> keep = new HashSet<>();
                for(JsonObject channel : fetched){
                    String id = channel.get("id").getAsString();
                    boolean canSend = !channel.has("canSend") || channel.get("canSend").getAsBoolean();
                    keep.add(id);
                    Channel existing = channels.get(id);
                    if(existing != null){
                        existing.canSend = canSend;
                    }else{
                        channels.put(id, new Channel(id, channel.get("name").getAsString(), canSend));
                    }
                }
                channels.keySet().removeIf(id -> !keep.contains(id));
                channelsVersion++;
                Aetheria.logger.info("[GlobalChat]: Refreshed " + channels.size() + " channels.");
                fetchMissedMentions();
            }catch(Exception e){
                Aetheria.logger.warning("[GlobalChat]: Failed to refresh channels: " + e.getMessage());
            }
        });
    }

    private static final Pattern ENDS_WITH_NUM = Pattern.compile("~(\\d+)\\z");

    private static void registerIEmoji(IEmoji emoji) {        String name = emoji.shortcode.replace(":", "");
        if (usableEmojis.containsKey(name)) {
            Matcher matcher = ENDS_WITH_NUM.matcher(name);

            if (matcher.find()) {
                int currentNumber = Integer.parseInt(matcher.group(1));
                int nextNumber = currentNumber + 1;

                String baseName = name.substring(0, name.lastIndexOf("~"));
                String newName = baseName + "~" + nextNumber;

                emoji.shortcode = ":" + newName + ":";
                Aetheria.logger.info("[GlobalChat]: Emoji with name: " + name + " already exists, registering with name: " + newName);
                registerIEmoji(emoji);
            } else {
                emoji.shortcode = ":" + name + "~1:";
                registerIEmoji(emoji);
            }
        } else {
            usableEmojis.put(name, emoji);
        }
    }

    /** True if the message was queued for the server; false if the websocket is down (a reconnect is attempted). */
    public static boolean sendMessage(ChatMessage chatMessage) {
        if (!ensureSocket()) return false;
        JsonObject obj = new JsonObject();
        obj.addProperty("command","discord::sendchat");
        obj.add("message",GSON.toJsonTree(chatMessage));
        CompletableFuture<String> future = Aetheria.webSocketClient.sendAndRecieve(GSON.toJson(obj));
        if(future == null){
            return false;
        }
        future.thenAccept(response -> {
            JsonObject responseJson = JsonParser.parseString(response).getAsJsonObject();
            if(!responseJson.has("data")) return;
            JsonObject data = responseJson.getAsJsonObject("data");
            if(data.has("code")){
                int code = data.get("code").getAsInt();
                if(code != 200){
                    String error = errorMessageOf(data);
                    Aetheria.logger.warning("[G-Chat] Error Sending Message: " + error);
                    pushSystemNotice(error);
                    // Locally-echoed messages (see ChatUI's optimistic send) are otherwise left
                    // stuck showing "Sending..." forever with no indication the server rejected it.
                    chatMessage.sendFailed = true;
                }else{
                    pendingMessages.put(chatMessage.messageID,chatMessage);
                }
            }
        });
        return true;
    }

    /** Shows a server/system message in the G-Chat sidebar instead of mc-chat (mc-chat is invisible to the user). */
    public static void pushSystemNotice(String text) {
        if (text == null || text.trim().isEmpty()) return;
        systemNotices.add(text.trim());
        while (systemNotices.size() > 20) systemNotices.remove(0);
        systemNoticesVersion++;
    }

    /** Safely reads a "message" field off a data object, falling back when the server omits it. */
    private static String errorMessageOf(JsonObject data) {
        return data.has("message") && !data.get("message").isJsonNull() ? data.get("message").getAsString() : "Unknown error";
    }

    /** Attempts a reconnect and reports whether the socket is usable for sends. */
    private static boolean ensureSocket() {
        if (Aetheria.webSocketClient == null || !WebSocketClient.isConnected) {
            WebSocketClient.reconnectIfNeeded();
            return false;
        }
        return true;
    }

    public static void deleteMessage(ChatMessage chatMessage) {
        if(chatMessage == null || chatMessage.discordID == null || chatMessage.discordID.isEmpty()) return;
        if (!ensureSocket()) return;
        JsonObject obj = new JsonObject();
        obj.addProperty("command","discord::deletechat");
        obj.addProperty("discordID",chatMessage.discordID);
        obj.addProperty("channelId",chatMessage.channelId);
        CompletableFuture<String> future = Aetheria.webSocketClient.sendAndRecieve(GSON.toJson(obj));
        if(future == null) return;
        future.thenAccept(response -> {
            JsonObject responseJson = JsonParser.parseString(response).getAsJsonObject();
            if(!responseJson.has("data")) return;
            JsonObject data = responseJson.getAsJsonObject("data");
            if(!data.has("code")) return;
            int code = data.get("code").getAsInt();
            if(code != 200){
                Aetheria.logger.warning("[G-Chat] Error Deleting Message: " + errorMessageOf(data));
            }
        });
    }

    public static void editMessage(ChatMessage chatMessage, String newContent) {
        if(chatMessage == null || chatMessage.discordID == null || chatMessage.discordID.isEmpty()) return;
        if (!ensureSocket()) return;
        JsonObject obj = new JsonObject();
        obj.addProperty("command","discord::editchat");
        obj.addProperty("discordID",chatMessage.discordID);
        obj.addProperty("channelId",chatMessage.channelId);
        obj.addProperty("content",newContent);
        obj.add("message",GSON.toJsonTree(chatMessage));
        CompletableFuture<String> future = Aetheria.webSocketClient.sendAndRecieve(GSON.toJson(obj));
        if(future == null) return;
        future.thenAccept(response -> {
            JsonObject responseJson = JsonParser.parseString(response).getAsJsonObject();
            if(!responseJson.has("data")) return;
            JsonObject data = responseJson.getAsJsonObject("data");
            if(!data.has("code")) return;
            int code = data.get("code").getAsInt();
            if(code != 200){
                Aetheria.logger.warning("[G-Chat] Error Editing Message: " + errorMessageOf(data));
            }
        });
    }

    /**
     * Offline mention catch-up ("missed pings" on login): asks the server for
     * @mentions / @everyone pings / replies-to-me newer than the stored
     * watermark, toasts them, and reports the unread message count. The
     * watermark advances only after a successful fetch, so a failed connect
     * never loses pings. Fired once per session, from the socket open and from
     * channel loading (whichever happens last, so channels exist when toasts resolve).
     */
    public static void fetchMissedMentions() {
        if (missedMentionsFetched || missedMentionsInFlight) return;
        if (channels.isEmpty()) return;
        if (Aetheria.webSocketClient == null || !WebSocketClient.isConnected) return;
        missedMentionsInFlight = true;
        JsonObject obj = new JsonObject();
        obj.addProperty("command", "discord::fetch-mentions");
        obj.addProperty("since", ATHRConfig.feature.network.globalChatConfig.lastSeenPings);
        final long requestedAt = System.currentTimeMillis();
        CompletableFuture<String> future = Aetheria.webSocketClient.sendAndRecieve(GSON.toJson(obj));
        if (future == null) {
            missedMentionsInFlight = false;
            return;
        }
        future.whenComplete((response, error) -> {
            missedMentionsInFlight = false;
            if (error != null || response == null) return;
            try {
                JsonObject responseJson = JsonParser.parseString(response).getAsJsonObject();
                if (!responseJson.has("data")) return;
                JsonObject data = responseJson.getAsJsonObject("data");
                if (!data.has("code") || data.get("code").getAsInt() != 200) return;
                if (data.has("mentions") && data.get("mentions").isJsonArray()) {
                    for (JsonElement element : data.getAsJsonArray("mentions")) {
                        if (!element.isJsonObject()) continue;
                        ChatMessage message = GSON.fromJson(element, ChatMessage.class);
                        if (message == null) continue;
                        Channel channel = channels.get(message.channelId);
                        if (channel == null) continue;
                        channel.receiveMessage(message);
                        Notification notification = Notification.createFromMessage(message, channel);
                        if (notification != null) {
                            synchronized (notifications) { notifications.add(notification); }
                        }
                    }
                }
                if (data.has("unreadCount") && data.get("unreadCount").getAsInt() > 0) {
                    int unread = data.get("unreadCount").getAsInt();
                    String summary = unread + " new message" + (unread == 1 ? "" : "s") + " while you were away";
                    synchronized (notifications) {
                        notifications.add(Notification.createTextNotif("Unread Messages", summary, 5000L));
                    }
                }
                missedMentionsFetched = true;
                ATHRConfig.feature.network.globalChatConfig.lastSeenPings = requestedAt;
            } catch (Exception e) {
                Aetheria.logger.warning("[GlobalChat]: Failed to process missed mentions: " + e.getMessage());
            }
        });
    }

    public static void receive(String response){
        try{
            receiveUnsafe(response);
        }catch(Exception e){
            Aetheria.logger.warning("[GlobalChat]: Failed to process websocket message: " + e.getMessage());
        }
    }

    private static void receiveUnsafe(String response){
        JsonObject responseJson = JsonParser.parseString(response).getAsJsonObject();
        if(responseJson == null) return;
        if(responseJson.has("command")){
            String command = responseJson.get("command").getAsString();
            if("discord::deleteMessage".equals(command)){
                handleMessageDeleted(responseJson);
                return;
            }
            if("discord::sync-channels".equals(command)){
                refreshChannels(true);
                return;
            }
            if("discord::refresh-channels".equals(command)){
                refreshChannels(false);
                return;
            }
            if("discord::punishment-lifted".equals(command)){
                String message = responseJson.has("message") ? responseJson.get("message").getAsString() : "Your Global Chat punishment has been lifted.";
                pushSystemNotice(message);
                synchronized (notifications) { notifications.add(Notification.createPunishmentNotif(message)); }
                ChatUtils.sendMessage("§a[G-Chat] " + message);
                return;
            }
        }
        if(responseJson.has("messageID") && responseJson.has("discordID")){
            if(responseJson.has("content")){
                ChatMessage message = GSON.fromJson(responseJson, ChatMessage.class);
                if(message == null || message.channelId == null) return;
                Channel channel = channels.get(message.channelId);
                if(channel == null) return;
                channel.receiveMessage(message);
                // The server sometimes echoes a sent message back with content populated (rather
                // than as a bare MessageLink); clear the pending entry here too, or it's only ever
                // removed on the MessageLink path below and pendingMessages leaks forever.
                if(message.messageID != null) pendingMessages.remove(message.messageID);
                Notification notification = Notification.createFromMessage(message, channel);
                if(notification != null){
                    synchronized (notifications) { notifications.add(notification); }
                }
            }else {
                MessageLink link = GSON.fromJson(responseJson, MessageLink.class);
                if (link == null || link.messageID == null) return;
                ChatMessage pending = pendingMessages.remove(link.messageID);
                if (pending != null) {
                    pending.discordID = link.discordID;
                    if(pending.channelId != null){
                        Channel channel = channels.get(pending.channelId);
                        if(channel != null) channel.receiveMessage(pending);
                    }
                } else {
                    for(Channel channel : channels.values()){
                        channel.bindDiscordID(link.messageID, link.discordID);
                    }
                }
                Aetheria.logger.info("[GlobalChat]: Bound discordID " + link.discordID + " to message " + link.messageID);
            }
        }

    }

    private static void handleMessageDeleted(JsonObject json) {
        int removed = 0;
        int deleted = 0;
        if(json.has("messages") && json.get("messages").isJsonArray()){
            for(JsonElement element : json.getAsJsonArray("messages")){
                if(!element.isJsonObject()) continue;
                JsonObject msg = element.getAsJsonObject();
                if(!msg.has("channelId")) continue;
                String idField = msg.has("messageID") ? "messageID"
                        : msg.has("discordID") ? "discordID" : null;
                if(idField == null) continue;
                deleted++;
                removed += removeFromChannel(msg, idField);
            }
        }else if(json.has("messageID") && json.has("channelId")){
            deleted = 1;
            removed = removeFromChannel(json, "messageID");
        }else if(json.has("discordID") && json.has("channelId")){
            deleted = 1;
            removed = removeFromChannel(json, "discordID");
        }
        Aetheria.logger.info("[GlobalChat]: Removed " + removed + " line(s) for " + deleted + " deleted message(s).");
    }

    /** Assumes the caller already verified {@code msg.has("channelId")}. */
    private static int removeFromChannel(JsonObject msg, String idField) {
        if(!msg.has("channelId") || msg.get("channelId").isJsonNull()) return 0;
        Channel channel = channels.get(msg.get("channelId").getAsString());
        if(channel == null) return 0;
        String messageID = msg.has("messageID") && !msg.get("messageID").isJsonNull() ? msg.get("messageID").getAsString() : null;
        String discordID = msg.has("discordID") && !msg.get("discordID").isJsonNull() ? msg.get("discordID").getAsString() : null;
        return channel.removeMessage(messageID, discordID);
    }
}