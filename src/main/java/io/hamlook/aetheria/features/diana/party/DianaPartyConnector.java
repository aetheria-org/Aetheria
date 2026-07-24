package io.hamlook.aetheria.features.diana.party;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.network.NetworkGuard;
import io.hamlook.aetheria.repo.CapeAPI;
import io.hamlook.aetheria.utils.ElectionUtils;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import net.minecraft.client.Minecraft;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DianaPartyConnector {

    public static boolean isDiana = true;
    public static boolean isRitual = true;
    public static boolean isConnected = false;
    public static DianaPartyClient partyClient;
    public static Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static void checkForDiana() {
        if(ElectionUtils.currentMayor.equals("Diana")){
            isDiana = true;
        }
        if(ElectionUtils.perks != null){
            for(String perk : ElectionUtils.perks.perks){
                if (perk.toLowerCase().contains("mythological") && perk.toLowerCase().contains("ritual")) {
                    isRitual = true;
                    break;
                }
            }
        }
    }

    public static void initialise(){
        if(!NetworkGuard.apiAllowed()) return;
        checkForDiana();
        connectToAPI();
        long intervalSeconds = 120;
        scheduler.schedule(() -> {
            try {
                if (!isConnected) {
                    Aetheria.logger.severe("[D-Party] API disconnected. Attempting reconnection...");
                    connectToAPI();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        },intervalSeconds,TimeUnit.SECONDS);
    }

    public static CompletableFuture<String> joinParty(String partyID){
        if(!isConnected){
            connectToAPI();
            return null;
        }

        JsonObject cmd = new JsonObject();
        cmd.addProperty("command", "dpartyjoin");
        cmd.addProperty("partyID", partyID);
        cmd.addProperty("member", Minecraft.getMinecraft().getSession().getUsername().toLowerCase());

        return partyClient.sendAndRecieve(GSON.toJson(cmd));
    }

    public static CompletableFuture<String> createParty(String pName){
        if(!isConnected) connectToAPI();

        JsonObject obj = new JsonObject();
        obj.addProperty("command", "dpartycreate");
        obj.addProperty("partyName", pName);
        obj.addProperty("creator", Minecraft.getMinecraft().getSession().getUsername().toLowerCase());

        return partyClient.sendAndRecieve(GSON.toJson(obj));
    }

    public static CompletableFuture<String> leaveParty() {
        if(!isConnected) connectToAPI();

        JsonObject obj = new JsonObject();
        obj.addProperty("command", "dpartyleave");
        return partyClient.sendAndRecieve(GSON.toJson(obj));
    }
    public static CompletableFuture<String> disbandParty() {
        if(!isConnected) connectToAPI();

        JsonObject obj = new JsonObject();
        obj.addProperty("command", "dpartydisband");
        return partyClient.sendAndRecieve(GSON.toJson(obj));
    }

    public static CompletableFuture<String> transferParty(String member){
        if(!isConnected) connectToAPI();
        JsonObject obj = new JsonObject();
        obj.addProperty("command", "dpartytransfer");
        obj.addProperty("newCreator",member);
        return partyClient.sendAndRecieve(GSON.toJson(obj));
    }

    public static CompletableFuture<String> kickFromParty(String player) {
        if(!isConnected) connectToAPI();
        JsonObject obj = new JsonObject();
        obj.addProperty("command", "dpartykick");
        obj.addProperty("player",player.toLowerCase());
        return partyClient.sendAndRecieve(GSON.toJson(obj));
    }

    public static CompletableFuture<String> sendMessage(String msg) {
        if(!isConnected) connectToAPI();

        JsonObject obj = new JsonObject();
        obj.addProperty("command", "dpartychat");
        obj.addProperty("message", msg);
        //TODO: Maybe Attach Ranks
        obj.addProperty("player", Minecraft.getMinecraft().getSession().getUsername().toLowerCase());

        return partyClient.sendAndRecieve(GSON.toJson(obj));
    }

    public static boolean isInParty() {
        try {
            String response = partyClient.sendAndRecieve("{\"command\": \"dPartyCheck\"}").get(10, TimeUnit.SECONDS);
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            JsonObject data = json.get("data").getAsJsonObject();
            return data.get("code").getAsInt() == 200;
        } catch (Exception e) {
            Aetheria.logger.severe("[D-Party] ERROR: " + e.getMessage());
            return false;
        }
    }

    public static void connectToAPI() {
        if(partyClient == null || !isConnected){
            partyClient = new DianaPartyClient();
        }
        if(!isDiana && !isRitual){
            checkForDiana();
        }
        if(!isDiana && !isRitual) return;
        if(!NetworkGuard.apiAllowed()) return;
        Aetheria.logger.info("[D-Party] Trying to connect to Diana Party API");
        partyClient.connect();
    }

    //TODO: Process Diana Party Chat Stuff
    public static void process(String message) {
        JsonObject json = JsonParser.parseString(message).getAsJsonObject();
        if(json == null) return;
        String type = json.get("type").getAsString().toLowerCase();
        if(type.equalsIgnoreCase("dchatMessage")){
            String player = json.get("player").getAsString();
            String msg = json.get("message").getAsString();
            ChatUtils.sendMessage("§b[D-Party Chat] §a" + player + ": §f" + msg);
        }
        if(type.equalsIgnoreCase("dpartyLeave")){
            String player = json.get("player").getAsString();
            ChatUtils.sendMessage("§b[D-Party Chat] §c" + player + " has left the Diana Party.");
        }
        if(type.equalsIgnoreCase("dpartydisband")){
            String player = json.get("player").getAsString();
            ChatUtils.sendMessage("§b[D-Party Chat] §cThis Party has been disbanded by " + player);
        }
        if(type.equalsIgnoreCase("dpartyJoin")){
            String player = json.get("player").getAsString();
            ChatUtils.sendMessage("§b[D-Party Chat] §a" + player + " has joined the Diana Party.");
        }
    }

    //TODO: Process Diana Party Error Stuff
    public static void processError(Exception ex) {
        ChatUtils.sendMessage("§b[D-Party Chat]§c Error in Diana Party Finder: " + ex.getMessage());
        ChatUtils.sendMessage("§cReconnecting to API.");
        if (partyClient != null) {
            partyClient.close(1012, "Restarting");
        }
        connectToAPI();
    }

    //TODO: Process Diana Party Disconnect Stuff
    public static void processClose(int code, String reason, boolean remote) {
        if(code == 1012){
            isConnected = false;
        }
    }



}
