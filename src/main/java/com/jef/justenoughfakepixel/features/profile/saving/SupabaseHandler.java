package com.jef.justenoughfakepixel.features.profile.saving;

import com.google.gson.JsonObject;
import com.jef.justenoughfakepixel.JefMod;
import com.jef.justenoughfakepixel.features.profile.ProfileCompressor;
import com.jef.justenoughfakepixel.features.profile.ProfileParser;
import com.jef.justenoughfakepixel.features.profile.data.ProfileData;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class SupabaseHandler {

    private static final String API_URL = "https://cape-api.netlify.app/profile";
    public static final String MOD_SECRET = "a7c0e73c-3b0b-4789-8c80-741dd09ba1bc";

    private static final HashMap<String, Long> lastUploaded = new HashMap<>();

    public static void pushProfileAsync(String playerName, ProfileData data) {
        long now = System.currentTimeMillis();
        long lastUploadTime = lastUploaded.getOrDefault(playerName, 0L);

        if (now - lastUploadTime < 30_000) {
            long secondsLeft = (30_000 - (now - lastUploadTime)) / 1000;
            JefMod.logger.info("[SupabaseHandler] Upload for " + playerName + " is on cooldown. Please wait " + secondsLeft + "s.");
            return;
        }

        lastUploaded.put(playerName, now);

        new Thread(() -> {
            JefMod.logger.info("[SupabaseHandler] Initiating upload for: " + playerName);
            boolean success = pushProfileToAPI(playerName, data);

            if (success) {
                JefMod.logger.info("[SupabaseHandler] Successfully uploaded profile to cloud for: " + playerName);
            } else {
                JefMod.logger.info("[SupabaseHandler] Failed to upload profile to cloud for: " + playerName);
                 lastUploaded.remove(playerName);
            }
        }, "ProfilePush-" + playerName).start();
    }

    private static boolean pushProfileToAPI(String playerName, ProfileData data) {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("x-mod-secret", MOD_SECRET);
            conn.setRequestProperty("Content-Type", "application/octet-stream");
            conn.setRequestProperty("x-player-name", playerName);
            conn.setDoOutput(true);
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(15000);
            String jsonBody = ProfileParser.GSON.toJson(data);

            byte[] compressedData = ProfileCompressor.compressJSON(jsonBody);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(compressedData);
            }

            int responseCode = conn.getResponseCode();
            return responseCode == 200 || responseCode == 201;

        } catch (Exception e) {
            JefMod.logger.info("[SupabaseHandler] Exception pushing profile: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}