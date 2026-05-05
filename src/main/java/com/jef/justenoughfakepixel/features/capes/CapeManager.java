package com.jef.justenoughfakepixel.features.capes;

import com.jef.justenoughfakepixel.JefMod;
import com.jef.justenoughfakepixel.core.JefConfig;
import net.minecraft.client.Minecraft;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class CapeManager {

    public static HashMap<String,Cape> capes = new HashMap<>();
    public static HashMap<String,String> activeCapes = new HashMap<>();

    private static Long lastFetched = 0L;

    private static long POLL_INTERVAL_MS = 900000;

    private static final String API_URL = "https://cape-api.netlify.app";
    public static final String MOD_SECRET = "a7c0e73c-3b0b-4789-8c80-741dd09ba1bc";

    public static String CLIENT_SIDE_CAPE_ID = "";

    public static void equipCape(String playerName, Cape cape) {
        activeCapes.put(playerName, cape.id);
        lastFetched = System.currentTimeMillis();

        CLIENT_SIDE_CAPE_ID = cape.id;
        new Thread(() -> {
            boolean success = pushCapeToAPI(playerName, cape.id);
            if (!success) {
                JefMod.logger.info("[CapeManager] Failed to push cape for " + playerName);
                activeCapes.put(playerName, "none");
            }
        }, "CapePush-" + playerName).start();
    }

    public static void removeCape(String playerName) {
        activeCapes.put(playerName, "none");
        lastFetched =  System.currentTimeMillis();

        new Thread(() -> {
            deleteCapeFromAPI(playerName);
        }, "CapeDelete-" + playerName).start();
    }

    public static void fetchCapeAsync(String playerName) {
        String existing = activeCapes.get(playerName);
        long now = System.currentTimeMillis();

        boolean neverFetched = existing == null;
        boolean pollDue = !neverFetched
                && lastFetched != null
                && !"pending".equals(existing)
                && (now - lastFetched) > POLL_INTERVAL_MS;

        if (!neverFetched && !pollDue) return;

        if (neverFetched) activeCapes.put(playerName, "pending");

        lastFetched = now;

        new Thread(() -> {
            String id = fetchIDFromAPI(playerName);
            activeCapes.put(playerName, id == null ? "none" : id);
        }, "CapeFetch-" + playerName).start();
    }

    public static void refreshAll() {
        new Thread(() -> {
            for (String playerName : new HashSet<>(activeCapes.keySet())) {
                String id = fetchIDFromAPI(playerName);
                activeCapes.put(playerName, id == null ? "none" : id);
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            }
        }, "CapeRefreshAll").start();
    }


    public static String fetchIDFromAPI(String playerName) {
        try {
            URL url = new URL(API_URL + "/cape/" + URLEncoder.encode(playerName, "UTF-8"));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("x-mod-secret", MOD_SECRET);
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() != 200) return null;

            String json = readResponse(conn);
            if (json.contains("\"cape_id\":null")) return null;

            int idx = json.indexOf("\"cape_id\":\"");
            if (idx == -1) return null;
            int start = idx + 11;
            int end = json.indexOf("\"", start);
            return json.substring(start, end);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static boolean pushCapeToAPI(String playerName, String capeId) {
        try {
            URL url = new URL(API_URL + "/cape");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("x-mod-secret", MOD_SECRET);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            String body = "{\"player_name\":\"" + playerName.toLowerCase()
                    + "\",\"cape_id\":\"" + capeId + "\"}";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes("UTF-8"));
            }

            return conn.getResponseCode() == 200;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void deleteCapeFromAPI(String playerName) {
        try {
            URL url = new URL(API_URL + "/cape/" + URLEncoder.encode(playerName, "UTF-8"));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("x-mod-secret", MOD_SECRET);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.getResponseCode();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String readResponse(HttpURLConnection conn) throws Exception {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream())
        );
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();
        return sb.toString().trim();
    }

    public static boolean hasCape(String user) {
        if (!JefConfig.feature.cosmetics.capes.capesEnabled) return false;
        String id = activeCapes.get(user);
        return id != null && !id.equals("none") && !id.equals("pending");
    }

    public static void applyCape(String player,Cape cape){
        activeCapes.put(player,cape.id);
    }

    public static Cape getCapeForPlayer(String pl) {
        String capeID = activeCapes.get(pl);
        if (capeID == null || capeID.equals("pending")) return null;
        if(capeID.equals("none")) {
            if (pl.equals(Minecraft.getMinecraft().thePlayer.getGameProfile().getName())) {
                return getCape(CLIENT_SIDE_CAPE_ID);
            }
            return null;
        }
        Cape cape = capes.get(capeID);
        if (cape == null || !cape.isLoaded()) return null;
        return cape;
    }

    public static void initialise(boolean force) {
        if (!JefConfig.feature.cosmetics.capes.capesEnabled && !force) return;
        POLL_INTERVAL_MS = JefConfig.feature.cosmetics.capes.reloadInterval * 60000L;
        capes.clear();

        new Thread(CapeLoader::loadAllCapes, "CapeLoader-Init").start();
    }

    public static void register(Cape cape){
        capes.put(cape.id, cape);
    }

    public static void registerAll(List<Cape> capes){
        capes.forEach(CapeManager::register);
    }

    public static Cape getCape(String id){
        return capes.get(id);
    }

    public static void reload() {
        capes.clear();
        activeCapes.clear();
        lastFetched = 0L;
        initialise(true);
    }

}