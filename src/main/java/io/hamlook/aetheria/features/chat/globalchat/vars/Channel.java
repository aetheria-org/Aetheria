package io.hamlook.aetheria.features.chat.globalchat.vars;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.features.chat.globalchat.GlobalChat;
import io.hamlook.aetheria.repo.CapeAPI;
import io.hamlook.aetheria.utils.ElectionUtils;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Channel {

    public String channelID;
    public String channelName;
    public List<ChatLine> messageHistory;

    public Channel(String channelID,String channelName) {
        messageHistory =  new CopyOnWriteArrayList<>();
        this.channelID = channelID;
        this.channelName = channelName;
        fetchHistory();
    }

    private void fetchHistory() {
        try{
            URL url = new URL(CapeAPI.getAPIUrl("chat-history?channelID=" + channelID));
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
                    ChatMessage message = GlobalChat.GSON.fromJson(element, ChatMessage.class);
                    if(message == null) continue;
                    receiveMessage(message);
                }
                messageHistory.sort(Comparator.comparingLong(a -> a.message == null ? Long.MAX_VALUE : a.message.timestamp));
                Aetheria.logger.info("[Channel]: Loaded " + messageHistory.size() + " messages from DB.");
            }
        }catch(Exception e){
            Aetheria.logger.warning("[GlobalChat]: Failed to load history for channel " + channelID);
            e.printStackTrace();
        }
    }

    public void receiveMessage(ChatMessage message){
        if(message == null) return;
        if(message.discordID == null || message.messageID == null) return;
        if(message.content.isEmpty() && message.stickers.isEmpty() && message.attachments.isEmpty()) return;
        for (ChatLine line : messageHistory) {
            if (line.message != null && message.messageID.equals(line.message.messageID)) {
                if (message.discordID != null && !message.discordID.isEmpty()
                        && (line.message.discordID == null || line.message.discordID.isEmpty())) {
                    line.message.discordID = message.discordID;
                }
                if (!message.content.equals(line.message.content)) {
                    line.message.content = message.content;
                    line.message.contentVersion++;
                    line.message.edited = true;
                }
                return;
            }
        }
        messageHistory.addAll(message.getLines());
    }

    /** Removes all lines belonging to a message matching the given messageID (and/or discordID). Thread-safe (CopyOnWriteArrayList). */
    public int removeMessage(String messageID, String discordID){
        boolean hasMID = messageID != null && !messageID.isEmpty();
        boolean hasDID = discordID != null && !discordID.isEmpty();
        if(!hasMID && !hasDID) return 0;
        int before = messageHistory.size();
        messageHistory.removeIf(line -> {
            if(line.message == null) return false;
            if(hasMID && messageID.equals(line.message.messageID)) return true;
            return hasDID && discordID.equals(line.message.discordID);
        });
        return before - messageHistory.size();
    }

    /** Attaches a Discord ID to any existing lines of the given messageID (used when a MessageLink arrives for a message already in history). */
    public void bindDiscordID(String messageID, String discordID){
        if(messageID == null || discordID == null) return;
        for(ChatLine line : messageHistory){
            if(line.message != null && messageID.equals(line.message.messageID)){
                line.message.discordID = discordID;
            }
        }
    }
}
