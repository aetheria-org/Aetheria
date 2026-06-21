package io.hamlook.aetheria.features.profile.viewer;

import com.google.gson.*;
import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.network.NetworkGuard;
import io.hamlook.aetheria.repo.CapeAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileViewerAPI {

    public static ConcurrentHashMap<String,Long> lastFetches = new ConcurrentHashMap<>();
    public static HashMap<String,PlayerProfile> profileHashMap = new HashMap<>();
    public static List<String> cachedPlayerList = new ArrayList<>();

    public static final long FETCH_INTERVAL = 1800000;

    private static final Gson gson = new GsonBuilder()
            .registerTypeHierarchyAdapter(EnumMap.class, (JsonDeserializer<EnumMap<?, ?>>) (json, typeOfT, context) -> {
                if (!(typeOfT instanceof ParameterizedType)) return null;

                Type[] typeArguments = ((ParameterizedType) typeOfT).getActualTypeArguments();
                Class enumClass = (Class) typeArguments[0];
                EnumMap map = new EnumMap(enumClass);

                for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject().entrySet()) {
                    try {
                        Enum key = Enum.valueOf(enumClass, entry.getKey());
                        Object value = context.deserialize(entry.getValue(), typeArguments[1]);
                        map.put(key, value);
                    } catch (IllegalArgumentException e) {
                        Aetheria.logger.info("Skipped unknown enum key from API: " + entry.getKey());
                    }
                }
                return map;
            })
            .create();
    // -----------------------------------------------------------------------

    private static final ExecutorService networkExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ATHR-ProfileViewerAPI");
        t.setDaemon(true);
        return t;
    });

    public static void fetchPlayerListAsync() {
        if (!cachedPlayerList.isEmpty()) return;
        if (!NetworkGuard.apiAllowed()) return;
        networkExecutor.execute(() -> {
            try {
                URL url = new URL("https://capeapi.qzz.io/game/players");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
                conn.setRequestProperty("x-mod-secret", CapeAPI.getModSecret());
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                if (conn.getResponseCode() == 200) {
                    String json = readResponse(conn);
                    String[] players = gson.fromJson(json, String[].class);
                    if (players != null) {
                        cachedPlayerList = Arrays.asList(players);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static PlayerProfile getData(String user){
        fetchFromAPI(user);
        return profileHashMap.getOrDefault(user,null);
    }

    public static void fetchFromAPI(String username){
        if (!NetworkGuard.apiAllowed()) return;
        if(System.currentTimeMillis() - lastFetches.getOrDefault(username,0L) <= FETCH_INTERVAL) return;
        networkExecutor.execute(() -> {
            try{
                PlayerProfile profile = fetchUser(username);
                if(profile == null){
                    Aetheria.logger.info("Null Profile");
                }
                profileHashMap.put(username,profile);
                lastFetches.put(username,System.currentTimeMillis());
                Aetheria.logger.info("Added " + username + " to profile list.");
            } catch (Exception e) {
                Aetheria.logger.info("Error While Fetching Profiles For: " + username);
                e.printStackTrace();
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§cCould Not Fetch Profile For: §4" + username));
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§cPlease Share the Following Message with the devs to find a fix."));
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§c" + e.getMessage()));
            }
        });
    }

    public static PlayerProfile fetchUser(String username) throws Exception{
        URL url = new URL("https://capeapi.qzz.io/game/profile/" + username);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
        conn.setRequestProperty("x-mod-secret", CapeAPI.getModSecret());
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        Aetheria.logger.info("Code: " + conn.getResponseCode());

        if (conn.getResponseCode() == 200) {
            String json = readResponse(conn);
            File file = new File(ATHRConfig.configDirectory, "test_profiles.json");
            if(!file.exists()){
                file.createNewFile();
            }
            FileWriter writer = new FileWriter(file);
            writer.write(json);
            writer.close();
            PlayerProfile profile = gson.fromJson(json, PlayerProfile.class);
            if(profile == null) throw new Exception("Null DATA for: " + username);
            return profile;
        }
        return null;
    }

    private static String readResponse(HttpURLConnection conn) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return sb.toString().trim();
        }
    }
}