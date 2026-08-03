package io.hamlook.aetheria.features.chat.globalchat.vars;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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

    private volatile boolean fetching = false;

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
                JsonArray array = JsonParser.parseString(response).getAsJsonArray();
                if(array == null || array.isEmpty()) return;
                for(JsonElement element : array){
                    ChatMessage message = GlobalChat.GSON.fromJson(element, ChatMessage.class);
                    if(message == null) continue;
                    receiveMessage(message);
                }
                messageHistory.sort(Comparator.comparingLong(a -> a.message == null ? Long.MAX_VALUE : a.message.timestamp));
                trimToMax();
                Aetheria.logger.info("[Channel]: Loaded " + messageHistory.size() + " messages from DB.");
            }
        }catch(Exception e){
            Aetheria.logger.warning("[GlobalChat]: Failed to load history for channel " + channelID);
            e.printStackTrace();
        }finally{
            fetching = false;
        }
    }

    public void receiveMessage(ChatMessage message){
        if(message == null) return;
        if(message.discordID == null || message.messageID == null) return;
        String converted = EmojiParser.toShortcode(message.content);
        if (!converted.equals(message.content)) {
            message.content = converted;
            message.contentVersion++;
        }
        message.populateEmojiRefs(message.content);
        if(message.content.isEmpty() && message.stickers.isEmpty() && message.attachments.isEmpty() && message.embeds.isEmpty()) return;
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
