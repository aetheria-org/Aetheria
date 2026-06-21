package io.hamlook.aetheria.features.capes;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.network.NetworkGuard;
import io.hamlook.aetheria.repo.CapeAPI;
import net.minecraft.client.Minecraft;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class CapeManager {

    public static final Map<String, Cape> capes = new ConcurrentHashMap<>();
    public static final Map<String, String> activeCapes = new ConcurrentHashMap<>();

    private static long lastFetched = 0L;
    private static long POLL_INTERVAL_MS = 900000;


    public static String CLIENT_SIDE_CAPE_ID = "";

    private static final AtomicBoolean isFetching = new AtomicBoolean(false);
    private static final Gson gson = new Gson();

    private static final ExecutorService networkExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ATHR-CapeNetworkThread");
        t.setDaemon(true);
        return t;
    });

    public static void equipCape(String playerName, Cape cape) {
        activeCapes.put(playerName, cape.id);
        lastFetched = System.currentTimeMillis();

        CLIENT_SIDE_CAPE_ID = cape.id;
        networkExecutor.execute(() -> {
            if (!pushCapeToAPI(playerName, cape.id)) {
                Aetheria.logger.info("[CapeManager] Failed to push cape for " + playerName);
                activeCapes.put(playerName, "none");
            }
        });
    }

    public static void removeCape(String playerName) {
        activeCapes.put(playerName, "none");
        networkExecutor.execute(() -> deleteCapeFromAPI(playerName));
    }

    public static void fetchCapeAsync(String playerName) {
        if (!ATHRConfig.feature.cosmetics.capes.capesEnabled) return;
        String existing = activeCapes.get(playerName);
        long now = System.currentTimeMillis();
        if (now - lastFetched < POLL_INTERVAL_MS) {
            return;
        }
        if (existing == null) activeCapes.put(playerName, "pending");
        if (isFetching.compareAndSet(false, true)) {
            networkExecutor.execute(() -> {
                try {
                    fetchIDFromAPI();
                } finally {
                    isFetching.set(false);
                }
            });
        }
    }

    public static void refreshAll() {
        networkExecutor.execute(CapeManager::fetchIDFromAPI);
    }


    public static void fetchIDFromAPI() {
        if (!NetworkGuard.apiAllowed()) return;
        try {
            URL url = new URL(CapeAPI.getAPIUrl() + "/cape");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("x-mod-secret", CapeAPI.getModSecret());
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            if (conn.getResponseCode() == 200) {
                String json = readResponse(conn);
                Type type = new TypeToken<Map<String, String>>(){}.getType();
                Map<String, String> map = gson.fromJson(json, type);
                activeCapes.clear();
                activeCapes.putAll(map);
                lastFetched = System.currentTimeMillis();
            }
        } catch (Exception e) {
            Aetheria.logger.info("Failed to fetch capes: " + e.getMessage());
        }
    }

    private static boolean pushCapeToAPI(String playerName, String capeId) {
        if (!NetworkGuard.apiAllowed()) return false;
        try {
            URL url = new URL(CapeAPI.getAPIUrl() + "/cape");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("x-mod-secret", CapeAPI.getModSecret());
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            JsonObject body = new JsonObject();
            body.addProperty("player_name", playerName.toLowerCase());
            body.addProperty("cape_id", capeId);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(gson.toJson(body).getBytes(StandardCharsets.UTF_8));
            }
            return conn.getResponseCode() == 200;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void deleteCapeFromAPI(String playerName) {
        if (!NetworkGuard.apiAllowed()) return;
        try {
            URL url = new URL(CapeAPI.getAPIUrl() + "/cape/" + URLEncoder.encode(playerName, "UTF-8"));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("x-mod-secret", CapeAPI.getModSecret());
            conn.getResponseCode();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String readResponse(HttpURLConnection conn) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return sb.toString().trim();
        }
    }
    public static Cape getCapeForPlayer(String pl) {
        String capeID = activeCapes.get(pl);

        if (System.currentTimeMillis() - lastFetched > POLL_INTERVAL_MS) {
            fetchCapeAsync(pl);
        }
        if (capeID == null || capeID.equals("pending")) return null;
        if (capeID.equals("none")) {
            String ownName = Minecraft.getMinecraft().thePlayer.getGameProfile().getName();
            if (pl.equals(ownName) && !CLIENT_SIDE_CAPE_ID.isEmpty()) {
                return getCape(CLIENT_SIDE_CAPE_ID);
            }
            return null;
        }
        Cape cape = capes.get(capeID);
        return (cape != null && cape.isLoaded()) ? cape : null;
    }

    public static boolean doesntHaveCape(String user) {
        if (!ATHRConfig.feature.cosmetics.capes.capesEnabled) return true;
        String id = activeCapes.get(user);
        return id == null || id.equals("none") || id.equals("pending");
    }

    public static void applyCape(String player,Cape cape){
        activeCapes.put(player,cape.id);
    }

    public static void initialise(boolean force) {
        if (!ATHRConfig.feature.cosmetics.capes.capesEnabled && !force) return;
        if (!NetworkGuard.githubAllowed()) return;
        POLL_INTERVAL_MS = ATHRConfig.feature.cosmetics.capes.reloadInterval * 60000L;
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