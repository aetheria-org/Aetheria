package io.hamlook.aetheria.features.chat.globalchat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.repo.CapeAPI;
import io.hamlook.aetheria.utils.ElectionUtils;
import net.minecraft.client.Minecraft;

import java.net.HttpURLConnection;
import java.net.URL;

public class SyncChecker {

    private static Boolean cached = null;
    private static long lastCheck = 0;
    private static final long CACHE_TTL = 60000;
    private static boolean refreshing = false;
    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        initialized = true;
        refreshAsync();
    }

    public static boolean isSynced() {
        if (cached != null && System.currentTimeMillis() - lastCheck < CACHE_TTL) {
            return cached;
        }
        if (!refreshing) {
            refreshAsync();
        }
        return cached != null && cached;
    }

    public static String getStatusMessage() {
        if (cached == null) {
            return "§7Checking Discord sync...";
        }
        if (!cached) {
            return "§cNot synced — use §6/sync §cto link Discord";
        }
        return null;
    }

    private static synchronized void refreshAsync() {
        if (refreshing) return;
        refreshing = true;
        String username = Minecraft.getMinecraft().getSession().getUsername().toLowerCase();
        new Thread(() -> {
            try {
                URL url = new URL(CapeAPI.getAPIUrl("is-synced") + "?player=" + username);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("User-Agent", "Aetheria/" + Aetheria.VERSION);

                if (conn.getResponseCode() == 200) {
                    String response = ElectionUtils.readResponse(conn);
                    JsonObject obj = JsonParser.parseString(response).getAsJsonObject();
                    cached = obj.get("synced").getAsBoolean();
                    lastCheck = System.currentTimeMillis();
                } else {
                    cached = false;
                    lastCheck = System.currentTimeMillis();
                }
            } catch (Exception e) {
                cached = false;
                lastCheck = System.currentTimeMillis();
            } finally {
                refreshing = false;
            }
        }).start();
    }
}
