package io.hamlook.aetheria.features.profile.saving;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.profile.ProfileCompressor;
import io.hamlook.aetheria.network.NetworkGuard;
import io.hamlook.aetheria.features.profile.ProfileParser;
import io.hamlook.aetheria.features.profile.WaiterLogs;
import io.hamlook.aetheria.features.profile.data.ProfileData;
import io.hamlook.aetheria.repo.CapeAPI;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class SupabaseHandler {


    private static final HashMap<String, Long> lastUploaded = new HashMap<>();

    public static void pushProfileAsync(String playerName, ProfileData data) {
        if (ATHRConfig.feature != null && !NetworkGuard.apiAllowed()) return;
        long now = System.currentTimeMillis();
        long lastUploadTime = lastUploaded.getOrDefault(playerName, 0L);

        if (now - lastUploadTime < 30_000) {
            long secondsLeft = (30_000 - (now - lastUploadTime)) / 1000;
            Aetheria.logger.info("[SupabaseHandler] Upload for " + playerName + " is on cooldown. Please wait " + secondsLeft + "s.");
            WaiterLogs.addLog("[SupabaseHandler] Upload for " + playerName + " is on cooldown. Please wait " + secondsLeft + "s.");
            return;
        }

        lastUploaded.put(playerName, now);

        new Thread(() -> {
            try {
                Aetheria.logger.info("[SupabaseHandler] Initiating upload for: " + playerName);
                WaiterLogs.addLog("[SupabaseHandler] Initiating upload for: " + playerName);
                boolean success = pushProfileToAPI(playerName, data);
                trySaveProfile(data);

                if (success) {
                    Aetheria.logger.info("[SupabaseHandler] Successfully uploaded profile to cloud for: " + playerName);
                    WaiterLogs.addLog("[SupabaseHandler] Successfully uploaded profile to cloud for: " + playerName);
                } else {
                    Aetheria.logger.info("[SupabaseHandler] Failed to upload profile to cloud for: " + playerName);
                    WaiterLogs.addLog("[SupabaseHandler] Failed to upload profile to cloud for: " + playerName);
                    lastUploaded.remove(playerName);
                }
            } finally {
                // --- THE FIX ---
                // Force the background thread to save the logs to the file once it finishes!
                WaiterLogs.saveLogs();
            }
        }, "ProfilePush-" + playerName).start();
    }

    private static void trySaveProfile(ProfileData data) {
        try {
            String API = CapeAPI.getAPIUrl("profile-upload-data");
            WaiterLogs.addLog("[SupabaseHandler] API URL: " + API);
            URL url = new URL(API);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("x-mod-secret", CapeAPI.getModSecret());
            conn.setRequestProperty("User-Agent", "Aetheria/" + Aetheria.VERSION);
            conn.setRequestProperty("Accept", "*/*");
            // 1. Changed Content-Type to JSON
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(15000);

            WaiterLogs.addLog("[SupabaseHandler] Starting GSON serialization...");
            String jsonBody = ProfileParser.GSON.toJson(data);

            WaiterLogs.addLog("[SupabaseHandler] GSON success. Length: " + jsonBody.length() + ". Converting to UTF-8 bytes...");
            byte[] jsonData = jsonBody.getBytes(StandardCharsets.UTF_8);

            WaiterLogs.addLog("[SupabaseHandler] Conversion success. Bytes: " + jsonData.length + ". Opening Output Stream...");
            conn.setFixedLengthStreamingMode(jsonData.length);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonData);
                os.flush();
                WaiterLogs.addLog("[SupabaseHandler] Wrote Data to OS");
            } catch (IOException e) {
                WaiterLogs.addLog("[SupabaseHandler] Could not write data to OS: " + e.getMessage());
                e.printStackTrace();
                return;
            }

            int responseCode = conn.getResponseCode();
            WaiterLogs.addLog("[SupabaseHandler] Response Code: " + responseCode);

        } catch (Throwable t) {
            Aetheria.logger.info("[SupabaseHandler] CRITICAL THREAD CRASH: " + t.getMessage());
            WaiterLogs.addLog("[SupabaseHandler] CRITICAL THREAD CRASH: " + t.getClass().getSimpleName() + " - " + t.getMessage());
            t.printStackTrace();
        }
    }

    private static boolean pushProfileToAPI(String playerName, ProfileData data) {
        try {
            String API = CapeAPI.getAPIUrl("profile");
            WaiterLogs.addLog("[SupabaseHandler] API URL: " + API);
            URL url = new URL(API);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("x-mod-secret", CapeAPI.getModSecret());
            conn.setRequestProperty("User-Agent", "Aetheria/" + Aetheria.VERSION);
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("Content-Type", "application/octet-stream");
            conn.setRequestProperty("x-player-name", playerName);
            conn.setDoOutput(true);
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(15000);

            WaiterLogs.addLog("[SupabaseHandler] Starting GSON serialization...");
            String jsonBody = ProfileParser.GSON.toJson(data);

            WaiterLogs.addLog("[SupabaseHandler] GSON success. Length: " + jsonBody.length() + ". Starting compression...");
            byte[] compressedData = ProfileCompressor.compressJSON(jsonBody);
            WaiterLogs.addLog("[SupabaseHandler] Compression success. Bytes: " + compressedData.length + ". Opening Output Stream...");
            conn.setFixedLengthStreamingMode(compressedData.length);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(compressedData);
                os.flush();
                WaiterLogs.addLog("[SupabaseHandler] Wrote Data to OS");
            } catch (IOException e) {
                WaiterLogs.addLog("[SupabaseHandler] Could not write data to OS: " + e.getMessage());
                e.printStackTrace();
                return false;
            }

            int responseCode = conn.getResponseCode();
            WaiterLogs.addLog("[SupabaseHandler] Response Code: " + responseCode);
            return responseCode == 200 || responseCode == 201;

        } catch (Throwable t) {
            Aetheria.logger.info("[SupabaseHandler] CRITICAL THREAD CRASH: " + t.getMessage());
            WaiterLogs.addLog("[SupabaseHandler] CRITICAL THREAD CRASH: " + t.getClass().getSimpleName() + " - " + t.getMessage());
            t.printStackTrace();
            return false;
        }
    }
}