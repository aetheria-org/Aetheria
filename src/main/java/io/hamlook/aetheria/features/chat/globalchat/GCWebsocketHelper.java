package io.hamlook.aetheria.features.chat.globalchat;

import com.google.gson.JsonObject;
import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.WebSocketClient;
import io.hamlook.aetheria.features.chat.globalchat.vars.MessageData;
import org.java_websocket.enums.ReadyState;

import java.util.concurrent.CompletableFuture;

public class GCWebsocketHelper {

    public static CompletableFuture<String> sendMessage(MessageData data){
        if(data == null) return null;
        WebSocketClient client = Aetheria.webSocketClient;
        if(!WebSocketClient.isConnected || client.getReadyState() != ReadyState.OPEN) return null;

        JsonObject obj = new JsonObject();
        obj.addProperty("command", "discord::sendchat");
        obj.add("message",GlobalChat.GSON.toJsonTree(data));
        obj.addProperty("isMC",true);
        return client.sendAndRecieve(GlobalChat.GSON.toJson(obj));
    }

}
