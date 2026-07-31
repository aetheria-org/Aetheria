package io.hamlook.aetheria.features.chat.globalchat;

import com.google.gson.*;
import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.features.chat.globalchat.vars.*;
import io.hamlook.aetheria.repo.CapeAPI;
import io.hamlook.aetheria.utils.ElectionUtils;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GlobalChat {

    public static ConcurrentHashMap<String,Channel> channels = new ConcurrentHashMap<>();
    public static final Gson GSON = new Gson();
    public static HashMap<String,ChatMessage> pendingMessages = new HashMap<>();
    public static HashMap<String, IEmoji> usableEmojis = new HashMap<>();

    public static void initialise(){
        try{
            usableEmojis.clear();
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
                if(array == null || array.isEmpty()) return;
                for(JsonElement element : array){
                    IEmoji emoji = GSON.fromJson(element, IEmoji.class);
                    if(emoji == null) continue;
                    registerIEmoji(emoji);
                }
                Aetheria.logger.info("[GlobalChat]: Successfully loaded " + usableEmojis.size() + " emojis.");
            }else{
                Aetheria.logger.info("[GlobalChat]: Could not load emojis: " + connection.getResponseCode());
            }

            url = new URL(CapeAPI.getAPIUrl("channels"));
            loadChannels(url);
        }catch(Exception e){
            Aetheria.logger.warning("[GlobalChat]: Failed to load emoji-list/channels: " + Arrays.toString(e.getStackTrace()));
            e.printStackTrace();
        }
    }

    private static void loadChannels(URL url) {
        try{
            HttpURLConnection connection2 = (HttpURLConnection) url.openConnection();
            connection2.setRequestMethod("GET");
            connection2.setRequestProperty("User-Agent", "Aetheria/" + Aetheria.VERSION);
            connection2.setRequestProperty("Accept", "application/json");
            connection2.setReadTimeout(10000);
            connection2.setConnectTimeout(10000);
            if(connection2.getResponseCode() == 200){
                String response = ElectionUtils.readResponse(connection2);
                JsonArray obj = JsonParser.parseString(response).getAsJsonArray();
                if(obj == null) return;
                for(JsonElement element : obj){
                    JsonObject channel = element.getAsJsonObject();
                    if(channel == null || channel.isEmpty()) continue;
                    channels.put(channel.get("id").getAsString(),new Channel(channel.get("id").getAsString(),channel.get("name").getAsString()));
                }
                Aetheria.logger.info("[GlobalChat]: Successfully loaded " + channels.size() + " channels.");
            }else{
                Aetheria.logger.info("[GlobalChat]: Could not load channels: " + connection2.getResponseCode());
            }
        }catch(Exception e){
            Aetheria.logger.warning("[GlobalChat]: Failed to load channels: " + Arrays.toString(e.getStackTrace()));
            e.printStackTrace();
        }
    }

    /** Re-fetches the channel list (used after the server resets the Global Chat system, e.g. /reset-gchat). */
    public static void refreshChannels() {
        Aetheria.logger.info("[GlobalChat]: Refreshing channels after global reset...");
        channels.clear();
        try {
            loadChannels(new URL(CapeAPI.getAPIUrl("channels")));
        } catch (Exception e) {
            Aetheria.logger.warning("[GlobalChat]: Failed to refresh channels: " + e.getMessage());
        }
    }

    private static final Pattern ENDS_WITH_NUM = Pattern.compile("~(\\d+)\\z");

    private static void registerIEmoji(IEmoji emoji) {
        String name = emoji.shortcode.replace(":", "");
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

    public static void sendMessage(ChatMessage chatMessage) {
        JsonObject obj = new JsonObject();
        obj.addProperty("command","discord::sendchat");
        obj.add("message",GSON.toJsonTree(chatMessage));
        CompletableFuture<String> future = Aetheria.webSocketClient.sendAndRecieve(GSON.toJson(obj));
        if(future == null){
            return;
        }
        future.thenAccept(response -> {
           JsonObject responseJson = JsonParser.parseString(response).getAsJsonObject();
           if(!responseJson.has("data")) return;
           JsonObject data = responseJson.getAsJsonObject("data");
           if(data.has("code")){
               int code = data.get("code").getAsInt();
               if(code != 200){
                   Aetheria.logger.warning("[G-Chat] Error Sending Message: " + data.get("message").getAsString());
               }else{
                   pendingMessages.put(chatMessage.messageID,chatMessage);
               }
           }
        });
    }

    public static void deleteMessage(ChatMessage chatMessage) {
        if(chatMessage == null || chatMessage.discordID == null || chatMessage.discordID.isEmpty()) return;
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
                Aetheria.logger.warning("[G-Chat] Error Deleting Message: " + data.get("message").getAsString());
            }
        });
    }

    public static void editMessage(ChatMessage chatMessage, String newContent) {
        if(chatMessage == null || chatMessage.discordID == null || chatMessage.discordID.isEmpty()) return;
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
                Aetheria.logger.warning("[G-Chat] Error Editing Message: " + data.get("message").getAsString());
            }
        });
    }

    public static void receive(String response){
        JsonObject responseJson = JsonParser.parseString(response).getAsJsonObject();
        if(responseJson == null) return;
        if(responseJson.has("command")){
            String command = responseJson.get("command").getAsString();
            if("discord::deleteMessage".equals(command)){
                handleMessageDeleted(responseJson);
                return;
            }
            if("discord::sync-channels".equals(command)){
                refreshChannels();
                return;
            }
        }
        if(responseJson.has("messageID") && responseJson.has("discordID")){
            if(responseJson.has("content")){
                ChatMessage message = GSON.fromJson(responseJson, ChatMessage.class);
                if(message == null) return;
                Channel channel = channels.get(message.channelId);
                if(channel == null) return;
                channel.receiveMessage(message);
            }else {
                MessageLink link = GSON.fromJson(responseJson, MessageLink.class);
                if (link == null) return;
                if (pendingMessages.containsKey(link.messageID)) {
                    ChatMessage msg = pendingMessages.get(link.messageID);
                    msg.discordID = link.discordID;
                    Channel channel = channels.get(msg.channelId);
                    if(channel == null) return;
                    channel.receiveMessage(msg);
                    pendingMessages.remove(link.messageID);
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
        if(json.has("messages")){
            for(JsonElement element : json.getAsJsonArray("messages")){
                if(!element.isJsonObject()) continue;
                JsonObject msg = element.getAsJsonObject();
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

    private static int removeFromChannel(JsonObject msg, String idField) {
        Channel channel = channels.get(msg.get("channelId").getAsString());
        if(channel == null) return 0;
        String messageID = "messageID".equals(idField) ? msg.get(idField).getAsString() : null;
        String discordID = msg.has("discordID") ? msg.get("discordID").getAsString()
                : ("discordID".equals(idField) ? msg.get(idField).getAsString() : null);
        return channel.removeMessage(messageID, discordID);
    }
}
