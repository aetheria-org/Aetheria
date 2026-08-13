package io.hamlook.aetheria.features.chat.globalchat.vars;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.features.chat.globalchat.GlobalChat;
import io.hamlook.aetheria.repo.CapeAPI;
import io.hamlook.aetheria.utils.ElectionUtils;
import io.hamlook.aetheria.utils.EmojiParser;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Channel {

    /** Bounds scrollback size so a long-lived session never grows unbounded. */
    public static final int MAX_HISTORY_LINES = 1000;

    public String channelID;
    public String channelName;
    public boolean canSend = true;
    public List<ChatLine> messageHistory;

    /** messageID → message: O(1) dedup/lookup index kept in sync with messageHistory. */
    public final Map<String, ChatMessage> byMessageID = new ConcurrentHashMap<>();
    /** discordID → message: O(1) reply/link lookup index kept in sync with messageHistory. */
    public final Map<String, ChatMessage> byDiscordID = new ConcurrentHashMap<>();

    /** Mentionable users of this channel (from the server's chat-history users list). */
    public final List<ChannelUser> userList = new CopyOnWriteArrayList<>();
    /** Lowercase username OR display name → user: powers the @ mention autocomplete. */
    public final Map<String, ChannelUser> usersByKey = new ConcurrentHashMap<>();

    private volatile boolean fetching = false;

    /** Unread-message count for this channel (messages received while it wasn't the selected channel). Drives the sidebar badge. */
    public final java.util.concurrent.atomic.AtomicInteger unreadCount = new java.util.concurrent.atomic.AtomicInteger(0);
    /** True when at least one unread message in this channel pings the local user (mention/reply/@everyone) — badge renders red instead of neutral. */
    public volatile boolean unreadHighlighted = false;

    /** True while this channel is the one currently shown in the chat UI; suppresses unread-count increments for it. Set by the UI layer. */
    public volatile boolean active = false;

    public void markRead() {
        unreadCount.set(0);
        unreadHighlighted = false;
    }

    public Channel(String channelID,String channelName) {
        this(channelID, channelName, true);
    }

    public Channel(String channelID,String channelName, boolean canSend) {
        messageHistory =  new CopyOnWriteArrayList<>();
        this.channelID = channelID;
        this.channelName = channelName;
        this.canSend = canSend;
        fetchHistory();
    }

    private void fetchHistory() {
        fetching = true;
        try{
            URL url = new URL(CapeAPI.getAPIUrl("chat-history?channelID=" + channelID));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Aetheria/" + Aetheria.VERSION);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("username", GlobalChat.getUsername());
            connection.setRequestProperty("x-timezone-offset", String.valueOf(io.hamlook.aetheria.utils.TimeUtils.getLocalOffsetMinutes()));
            connection.setReadTimeout(10000);
            connection.setConnectTimeout(10000);
            if(connection.getResponseCode() == 200){
                String response = ElectionUtils.readResponse(connection);
                JsonElement root = JsonParser.parseString(response);
                if(root == null || !root.isJsonArray() && !root.isJsonObject()) return;
                JsonArray array;
                if(root.isJsonObject()){
                    JsonObject obj = root.getAsJsonObject();
                    array = obj.has("messages") && obj.get("messages").isJsonArray() ? obj.getAsJsonArray("messages") : new JsonArray();
                    if(obj.has("users") && obj.get("users").isJsonArray()){
                        for(JsonElement element : obj.getAsJsonArray("users")){
                            if(!element.isJsonObject()) continue;
                            ChannelUser user = GlobalChat.GSON.fromJson(element, ChannelUser.class);
                            if(user != null) addUser(user);
                        }
                    }
                }else{
                    array = root.getAsJsonArray();
                }
                if(array.isEmpty()) return;
                for(JsonElement element : array){
                    ChatMessage message = GlobalChat.GSON.fromJson(element, ChatMessage.class);
                    if(message == null) continue;
                    receiveMessage(message);
                }
                messageHistory.sort(Comparator.comparingLong(a -> a.message == null ? Long.MAX_VALUE : a.message.timestamp));
                trimToMax();
                Aetheria.logger.info("[Channel]: Loaded " + messageHistory.size() + " messages and " + userList.size() + " users from DB.");
            }
        }catch(Exception e){
            Aetheria.logger.warning("[GlobalChat]: Failed to load history for channel " + channelID);
            e.printStackTrace();
        }finally{
            fetching = false;
        }
    }

    /** Registers a mentionable user under its username and display name (lowercased keys; display also without spaces). */
    public void addUser(ChannelUser user) {
        if (user == null || user.username == null || user.username.isEmpty()) return;
        userList.add(user);
        usersByKey.put(user.username.toLowerCase(), user);
        if (user.displayname != null && !user.displayname.isEmpty()) {
            usersByKey.put(user.displayname.toLowerCase(), user);
            usersByKey.put(user.displayname.toLowerCase().replace(" ", ""), user);
        }
    }

    /**
     * Re-fetches the channel's mentionable users from the server in the
     * background (reuses the chat-history response's users array, ignoring the
     * messages). Channels are created once and their user lists would otherwise
     * go stale forever if the server's list changes after launch — so the UI
     * refreshes on every channel switch.
     */
    public void refreshUsers() {
        CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL(CapeAPI.getAPIUrl("chat-history?channelID=" + channelID));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "Aetheria/" + Aetheria.VERSION);
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("username", GlobalChat.getUsername());
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                if (connection.getResponseCode() != 200) return;
                String response = ElectionUtils.readResponse(connection);
                JsonElement root = JsonParser.parseString(response);
                if (root == null || !root.isJsonObject()) return;
                JsonObject obj = root.getAsJsonObject();
                if (!obj.has("users") || !obj.get("users").isJsonArray()) return;
                List<ChannelUser> fresh = new ArrayList<>();
                for (JsonElement element : obj.getAsJsonArray("users")) {
                    if (!element.isJsonObject()) continue;
                    ChannelUser user = GlobalChat.GSON.fromJson(element, ChannelUser.class);
                    if (user != null && user.username != null && !user.username.isEmpty()) fresh.add(user);
                }
                if (fresh.isEmpty()) return; // never clobber a good list with an empty response
                userList.clear();
                usersByKey.clear();
                for (ChannelUser user : fresh) addUser(user);
                Aetheria.logger.info("[Channel]: Refreshed " + userList.size() + " mentionable users for " + channelName);
            } catch (Exception e) {
                Aetheria.logger.warning("[GlobalChat]: Failed to refresh users for channel " + channelID);
            }
        });
    }

    public void receiveMessage(ChatMessage message){
        if(message == null || message.messageID == null) return;
        String converted = EmojiParser.toShortcode(message.content);
        if (!converted.equals(message.content)) {
            message.content = converted;
            message.contentVersion++;
        }
        message.populateEmojiRefs(message.content);
        if(message.content.isEmpty() && message.stickers.isEmpty() && message.attachments.isEmpty()
                && (message.embeds == null || message.embeds.isEmpty())) return;
        message.highlighted = message.pingsMe(GlobalChat.getUsername(), this);
        ChatMessage existing = byMessageID.get(message.messageID);
        if (existing != null) {
            if (message.discordID != null && !message.discordID.isEmpty()
                    && (existing.discordID == null || existing.discordID.isEmpty())) {
                existing.discordID = message.discordID;
                byDiscordID.put(existing.discordID, existing);
            }
            if (!message.content.equals(existing.content)) {
                existing.content = message.content;
                existing.contentVersion++;
                existing.edited = true;
                existing.populateEmojiRefs(message.content);
            }
            return;
        }
        messageHistory.addAll(message.getLines());
        byMessageID.put(message.messageID, message);
        if (message.discordID != null && !message.discordID.isEmpty()) byDiscordID.put(message.discordID, message);
        if (!fetching) trimToMax();
        if (!fetching && !active) {
            boolean isOwnMessage = message.author != null && message.author.equalsIgnoreCase(GlobalChat.getUsername());
            if (!isOwnMessage) {
                unreadCount.incrementAndGet();
                if (message.highlighted) unreadHighlighted = true;
            }
        }
    }

    /** Drops the oldest lines once history exceeds {@link #MAX_HISTORY_LINES}. */
    private void trimToMax() {
        while (messageHistory.size() > MAX_HISTORY_LINES) {
            ChatLine oldest = messageHistory.remove(0);
            if (oldest != null && oldest.message != null) {
                byMessageID.remove(oldest.message.messageID);
                byDiscordID.remove(oldest.message.discordID);
            }
        }
    }

    /** Removes all lines belonging to a message matching the given messageID (and/or discordID). Thread-safe (CopyOnWriteArrayList). */
    public int removeMessage(String messageID, String discordID){
        boolean hasMID = messageID != null && !messageID.isEmpty();
        boolean hasDID = discordID != null && !discordID.isEmpty();
        if(!hasMID && !hasDID) return 0;
        int before = messageHistory.size();
        List<ChatMessage> removed = new ArrayList<>();
        messageHistory.removeIf(line -> {
            if(line.message == null) return false;
            boolean hit = (hasMID && messageID.equals(line.message.messageID))
                    || (hasDID && discordID.equals(line.message.discordID));
            if (hit) removed.add(line.message);
            return hit;
        });
        for (ChatMessage m : removed) {
            byMessageID.remove(m.messageID);
            byDiscordID.remove(m.discordID);
        }
        return before - messageHistory.size();
    }

    /** Attaches a Discord ID to any existing lines of the given messageID (used when a MessageLink arrives for a message already in history). */
    public void bindDiscordID(String messageID, String discordID){
        if(messageID == null || discordID == null) return;
        ChatMessage message = byMessageID.get(messageID);
        if(message == null) return;
        message.discordID = discordID;
        byDiscordID.put(discordID, message);
    }
}