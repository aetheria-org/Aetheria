package io.hamlook.aetheria;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.hamlook.aetheria.features.chat.globalchat.GlobalChat;
import io.hamlook.aetheria.features.diana.party.DianaPartyConnector;
import io.hamlook.aetheria.repo.CapeAPI;
import io.hamlook.aetheria.utils.TimeUtils;
import net.minecraft.client.Minecraft;
import org.java_websocket.handshake.ServerHandshake;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketClient extends org.java_websocket.client.WebSocketClient {

    public static boolean isConnected = false;
    private static boolean connecting = false;
    private final Map<String, CompletableFuture<String>> pendingRequests = new ConcurrentHashMap<>();

    public WebSocketClient() {
        super(URI.create(CapeAPI.getWebsocketURL()));
        addHeader("username", Minecraft.getMinecraft().getSession().getUsername().toLowerCase());
        addHeader("x-timezone-offset", String.valueOf(TimeUtils.getLocalOffsetMinutes()));
    }

    @Override
    public void connect() {
        try {
            super.connect();
            connecting = true;
        } catch (Exception e) {
            connecting = false;
            throw e;
        }
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        isConnected = true;
        connecting = false;
        GlobalChat.onSocketConnected();
    }

    @Override
    public void onMessage(String message) {
        String requestId = extractID(message);
        if(requestId != null) {
            CompletableFuture<String> future = pendingRequests.remove(requestId);
            if (future != null) {
                future.complete(message);
            }
        }
        Aetheria.logger.info("[Websocket] Test Log: " + message);
        DianaPartyConnector.process(message);
        GlobalChat.receive(message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        DianaPartyConnector.processClose(code);
        isConnected = false;
        connecting = false;
        IOException closed = new IOException("Websocket closed (" + code + ")");
        for (CompletableFuture<String> future : pendingRequests.values()) {
            future.completeExceptionally(closed);
        }
        pendingRequests.clear();
    }

    public CompletableFuture<String> sendAndRecieve(String message) {
        if (!isConnected || !isOpen()) {
            Aetheria.logger.warning("[Websocket] Not connected, dropping send.");
            return null;
        }
        String id = UUID.randomUUID().toString();
        CompletableFuture<String> future = new CompletableFuture<>();
        pendingRequests.put(id, future);
        try {
            String formattedMessage = attachIdToJson(message, id);
            send(formattedMessage);
        } catch (Exception e) {
            pendingRequests.remove(id);
            Aetheria.logger.warning("[Websocket] Send failed: " + e.getMessage());
            return null;
        }

        return future;
    }

    /** Recreates and reconnects the socket if it is closed or never connected. Safe to call repeatedly. */
    public static void reconnectIfNeeded() {
        try {
            if (Aetheria.webSocketClient != null && (WebSocketClient.isConnected || connecting)) {
                return;
            }
            if (Aetheria.webSocketClient != null) {
                try {
                    Aetheria.webSocketClient.close(1012, "Reconnecting");
                } catch (Exception ignored) {
                }
            }
            Aetheria.webSocketClient = new WebSocketClient();
            Aetheria.logger.info("[Websocket] Reconnecting to Global Chat API");
            Aetheria.webSocketClient.connect();
        } catch (Exception e) {
            Aetheria.logger.warning("[Websocket] Reconnect failed: " + e.getMessage());
        }
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
        connecting = false;
    }
}
