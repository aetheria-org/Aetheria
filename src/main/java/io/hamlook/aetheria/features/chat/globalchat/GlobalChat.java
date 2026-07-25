package io.hamlook.aetheria.features.chat.globalchat;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.features.chat.globalchat.vars.MessageData;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

public class GlobalChat {

    public static final Gson GSON = new Gson();
    public static HashMap<MessageData,Long> messageHistory = new HashMap<>();

    public static void initialise(){
        messageHistory.clear();
    }

    public static void receive(String raw){
        MessageData data = GSON.fromJson(raw,MessageData.class);
        if(data ==null) return;
        messageHistory.put(data,System.currentTimeMillis());
    }

    public static void send(MessageData data){
        CompletableFuture<String> response = GCWebsocketHelper.sendMessage(data);
        if(response == null) return;
        response.thenAccept(res -> {
            JsonObject object = JsonParser.parseString(res).getAsJsonObject();
            if(object == null) return;
            if(!object.has("data")) return;
            JsonObject resData = object.get("data").getAsJsonObject();
            if(resData == null) return;
            if(resData.get("code").getAsInt() != 200) {
                //TODO: Sow Error to user
                Aetheria.logger.info("[Error in GC][" + resData.get("code").getAsInt() + "]: " + resData.get("message").getAsString());
                return;
            }
        });
    }

}
