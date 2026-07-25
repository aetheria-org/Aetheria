package io.hamlook.aetheria;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.hamlook.aetheria.features.diana.party.DianaPartyConnector;
import io.hamlook.aetheria.repo.CapeAPI;
import net.minecraft.client.Minecraft;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketClient extends org.java_websocket.client.WebSocketClient {

    public static boolean isConnected = false;
    private final Map<String, CompletableFuture<String>> pendingRequests = new ConcurrentHashMap<>();

    public WebSocketClient() {
        super(URI.create(CapeAPI.getWebsocketURL()));
        addHeader("username", Minecraft.getMinecraft().getSession().getUsername().toLowerCase());
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        isConnected = true;
    }

    @Override
    public void onMessage(String message) {
        String requestId = extractID(message);
        if(requestId != null) {
            CompletableFuture<String> future = pendingRequests.remove(requestId);
            if (future != null) {
                future.complete(message);
                return;
            }
        }
        if(DianaPartyConnector.process(message)) return;

    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        DianaPartyConnector.processClose(code);
        isConnected = false;
    }

    public CompletableFuture<String> sendAndRecieve(String message) {
        String id = UUID.randomUUID().toString();
        CompletableFuture<String> future = new CompletableFuture<>();
        pendingRequests.put(id, future);
        String formattedMessage = attachIdToJson(message, id);
        send(formattedMessage);

        return future;
    }

    private String attachIdToJson(String message, String id) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", id);
        jsonObject.addProperty("message", message);
        return DianaPartyConnector.GSON.toJson(jsonObject);
    }

    private String extractID(String data) {
        JsonObject obj = JsonParser.parseString(data).getAsJsonObject();
        if(obj == null) return null;
        return obj.has("id") ? obj.get("id").getAsString() : null;
    }


    @Override
    public void onError(Exception ex) {
        DianaPartyConnector.processError(ex);
    }
}
