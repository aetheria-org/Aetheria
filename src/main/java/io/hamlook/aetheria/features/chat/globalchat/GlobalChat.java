package io.hamlook.aetheria.features.chat.globalchat;

import com.google.gson.*;
import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.features.chat.globalchat.vars.Message;
import io.hamlook.aetheria.features.chat.globalchat.vars.MessageData;
import io.hamlook.aetheria.features.chat.globalchat.vars.Reaction;
import io.hamlook.aetheria.utils.ElectionUtils;

import net.minecraft.client.Minecraft;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class GlobalChat {

    public static final Gson GSON = new Gson();
    public static List<MessageData> messages = new ArrayList<>();

    public static void initialise(){
        messages.clear();
        loadHistoryFromDB();
    }

    private static void loadHistoryFromDB() {
        try{
            URL url = new URL("http://localhost:2999/chat-history");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            if(conn.getResponseCode() == 200){
                String response = ElectionUtils.readResponse(conn);
                JsonArray array = JsonParser.parseString(response).getAsJsonArray();
                if(array == null || array.isEmpty()) return;
                for(JsonElement element : array){
                    JsonObject jsonObject = element.getAsJsonObject();
                    MessageData messageData = GSON.fromJson(jsonObject, MessageData.class);
                    if(messageData == null) return;
                    if(messageData.message == null){
                        Aetheria.logger.info("[G-Chat] Could not Parse message from: " + jsonObject);
                    }
                    messages.add(messageData);
                }
                Aetheria.logger.info("[G-CHAT] Loaded " + messages.size() + " messages from DB");
            }else{
                Aetheria.logger.info("[G-CHAT] Could not load history: " + conn.getResponseCode());
            }
        }catch(Exception e){
            Aetheria.logger.info("[G-Chat] Error Loading Message History: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void process(String message) {
        Aetheria.logger.info("[G-Chat] Received Raw After Diana Checked");
        JsonObject object = JsonParser.parseString(message).getAsJsonObject();
        if(object == null) return;
        if(object.has("command") && "discord::reactionUpdate".equals(object.get("command").getAsString())) {
            handleReactionUpdate(object);
            return;
        }
        if(object.has("message")){
            receive(message);
        }
    }

    public static void receive(String raw){
        Aetheria.logger.info("[G-Chat] Received Message");
        Message data = GSON.fromJson(raw,Message.class);
        if(data ==null) return;
        if(data.messageID != null){
            for(int i = 0; i < messages.size(); i++){
                Message existing = messages.get(i).message;
                if(existing != null && data.messageID.equals(existing.messageID)){
                    messages.set(i, new MessageData(data, messages.get(i).timestamp));
                    return;
                }
            }
        }
        messages.add(new MessageData(data,System.currentTimeMillis()));
    }

    private static void handleReactionUpdate(JsonObject object) {
        if (!object.has("messageID") || !object.has("reactions")) return;
        String messageID = object.get("messageID").getAsString();
        JsonObject reactionsJson = object.getAsJsonObject("reactions");

        for (MessageData md : messages) {
            if (md.message != null && messageID.equals(md.message.messageID)) {
                md.message.reactions = parseReactions(reactionsJson);
                return;
            }
        }
    }

    private static HashMap<String, Reaction> parseReactions(JsonObject reactionsJson) {
        HashMap<String, Reaction> map = new HashMap<>();
        if (reactionsJson == null) return map;
        for (Map.Entry<String, JsonElement> entry : reactionsJson.entrySet()) {
            JsonObject r = entry.getValue().getAsJsonObject();
            Reaction reaction = new Reaction();
            String[] parts = entry.getKey().split(":", 3);
            if (parts.length == 1) {
                reaction.name = parts[0];
            } else if (parts.length == 3) {
                reaction.name = parts[1];
                reaction.id = parts[2];
                reaction.animated = parts[0].equals("a");
            }
            reaction.count = r.has("count") ? r.get("count").getAsInt() : 0;
            map.put(entry.getKey(), reaction);
        }
        return map;
    }

    public static void send(Message data){
        CompletableFuture<String> response = GCWebsocketHelper.sendMessage(data);
        if(response == null) return;
        response.thenAccept(res -> {
            JsonObject object = JsonParser.parseString(res).getAsJsonObject();
            if(object == null) return;
            if(!object.has("data")) return;
            JsonObject resData = object.get("data").getAsJsonObject();
            if(resData == null) return;
            if(resData.get("code").getAsInt() != 200) {
                Aetheria.logger.info("[Error in GC][" + resData.get("code").getAsInt() + "]: " + resData.get("message").getAsString());
                return;
            }
            if(resData.has("messageID")){
                Aetheria.logger.info("[G-Chat] Sent messageID: " + resData.get("messageID").getAsString());
            }
        });
    }

    public static void editMessage(String messageID, String newText){
        String username = Minecraft.getMinecraft().getSession().getUsername();
        String skinUrl = "https://capeapi.qzz.io/avatar/" + username.toLowerCase() + ".png";

        Message.Content content = new Message.Content();
        content.content = newText;
        content.stickers = new HashMap<>();
        content.emojiRefs = new HashMap<>();
        content.attachments = new HashMap<>();

        CompletableFuture<String> response = GCWebsocketHelper.editMessage(messageID, content, username, skinUrl);
        if(response == null) return;
        response.thenAccept(res -> {
            JsonObject object = JsonParser.parseString(res).getAsJsonObject();
            if(object == null) return;
            if(!object.has("data")) return;
            JsonObject resData = object.get("data").getAsJsonObject();
            if(resData == null) return;
            if(resData.get("code").getAsInt() != 200) {
                Aetheria.logger.info("[Error in GC Edit][" + resData.get("code").getAsInt() + "]: " + resData.get("message").getAsString());
                return;
            }
            Aetheria.logger.info("[G-Chat] Message edited: " + messageID);
        });
    }

    public static Message findMessageByID(String messageID) {
        if (messageID == null) return null;
        for (MessageData md : messages) {
            if (md.message != null && messageID.equals(md.message.messageID)) {
                return md.message;
            }
        }
        return null;
    }
}
