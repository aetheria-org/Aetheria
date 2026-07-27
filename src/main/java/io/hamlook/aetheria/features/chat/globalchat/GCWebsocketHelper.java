package io.hamlook.aetheria.features.chat.globalchat;

import com.google.gson.JsonObject;
import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.WebSocketClient;
import io.hamlook.aetheria.features.chat.globalchat.vars.Message;
import org.java_websocket.enums.ReadyState;

import java.util.concurrent.CompletableFuture;

public class GCWebsocketHelper {

    public static CompletableFuture<String> sendMessage(Message data){
        if(data == null) return null;
        WebSocketClient client = Aetheria.webSocketClient;
        if(!WebSocketClient.isConnected || client.getReadyState() != ReadyState.OPEN) return null;

        JsonObject obj = new JsonObject();
        obj.addProperty("command", "discord::sendchat");
        obj.addProperty("isMC",true);
        obj.addProperty("messageID", data.messageID);
        obj.addProperty("id", data.id);
        obj.add("message", GlobalChat.GSON.toJsonTree(data.message));
        obj.addProperty("player", data.player);
        obj.addProperty("playerID", data.player + "-mc");
        obj.addProperty("skinURL", data.skin);
        if (data.replyTo != null) {
            obj.addProperty("replyTo", data.replyTo);
            obj.addProperty("replyAuthor", data.replyAuthor);
        }
        return client.sendAndRecieve(GlobalChat.GSON.toJson(obj));
    }

    public static CompletableFuture<String> editMessage(String messageID, Message.Content content, String player, String skinUrl){
        if(messageID == null || content == null) return null;
        WebSocketClient client = Aetheria.webSocketClient;
        if(!WebSocketClient.isConnected || client.getReadyState() != ReadyState.OPEN) return null;

        JsonObject obj = new JsonObject();
        obj.addProperty("command", "discord::editchat");
        obj.addProperty("messageID", messageID);
        obj.add("message", GlobalChat.GSON.toJsonTree(content));
        obj.addProperty("player", player);
        obj.addProperty("id", (String) null);
        obj.addProperty("playerID", player + "-mc");
        obj.addProperty("skinURL", skinUrl);
        return client.sendAndRecieve(GlobalChat.GSON.toJson(obj));
    }

    public static CompletableFuture<String> addReaction(String messageID, String emojiKey){
        if(messageID == null || emojiKey == null) return null;
        WebSocketClient client = Aetheria.webSocketClient;
        if(!WebSocketClient.isConnected || client.getReadyState() != ReadyState.OPEN) return null;

        JsonObject obj = new JsonObject();
        obj.addProperty("command", "discord::addreaction");
        obj.addProperty("messageID", messageID);
        obj.addProperty("emoji", emojiKey);
        return client.sendAndRecieve(GlobalChat.GSON.toJson(obj));
    }
}
