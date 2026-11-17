package io.hamlook.aetheria.utils;

import com.google.gson.JsonParser;
import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.network.NetworkGuard;
import io.hamlook.aetheria.repo.CapeAPI;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.data.SkyblockData;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Gates community features (Global Chat, Diana Parties) behind a player being
 * Synced (account verified via /sync) or currently playing SkyBlock. The server
 * is the source of truth; this is a cosmetic client-side pre-check.
 */
public final class CommunityAccess {

    private static volatile Boolean synced = null;
    private static volatile long lastChecked = 0;
    private static final long CACHE_TTL = 300_000;

    private CommunityAccess() {
    }

    private static File getGlobalConfigDir() {
        String os = System.getProperty("os.name").toLowerCase();
        String baseDir;
        if (os.contains("win")) {
            baseDir = System.getenv("APPDATA");
        } else {
            String xdgConfig = System.getenv("XDG_CONFIG_HOME");
            if (xdgConfig != null && !xdgConfig.isEmpty()) {
                baseDir = xdgConfig;
            } else {
                baseDir = System.getProperty("user.home") + "/.config";
            }
        }
        return new File(baseDir, "Aetheria");
    }

    private static File getSecretFile() {
        File dir = getGlobalConfigDir();
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "synccode.secret");
    }

    /** True if the player is in Skyblock OR is known to be verified (cached sync status). */
    public static boolean isAllowedNow() {
        if (SkyblockData.isOnSkyblock()) return true;
        if (synced != null && (System.currentTimeMillis() - lastChecked) < CACHE_TTL) {
            return synced;
        }
        return false;
    }

    /**
     * Runs {@code action} when access is allowed. If sync status is unknown it
     * is checked against the API first (async), then the action runs on the MC
     * thread. Otherwise sends {@code deniedMessage}.
     */
    public static void runIfAllowed(String deniedMessage, Runnable action) {
        if (SkyblockData.isOnSkyblock()) {
            action.run();
            return;
        }
        if (synced != null && (System.currentTimeMillis() - lastChecked) < CACHE_TTL) {
            if (synced) {
                action.run();
            } else {
                ChatUtils.sendMessage(deniedMessage);
            }
            return;
        }
        ChatUtils.sendMessage("§7Checking account access...");
        ThreadUtils.run("AccessCheck", () -> {
            boolean allowed = checkSynced();
            synced = allowed;
            lastChecked = System.currentTimeMillis();
            MinecraftCompat.getMinecraft().addScheduledTask(() -> {
                if (allowed) {
                    action.run();
                } else {
                    ChatUtils.sendMessage(deniedMessage);
                }
            });
        });
    }

    /** No-op if already allowed / in skyblock; otherwise gives a hint on how to get access. */
    public static void hintIfBlocked() {
        if (isAllowedNow()) return;
        ChatUtils.sendMessage("§cThis feature requires your account to be Synced (use /sync) or to be on SkyBlock.");
    }

    private static boolean checkSynced() {
        if (!NetworkGuard.apiAllowed()) return false;
        try {
            String secretHash = "";
            File secretFile = getSecretFile();
            if (secretFile.exists()) {
                secretHash = new String(Files.readAllBytes(secretFile.toPath()), StandardCharsets.UTF_8);
            }
            if (secretHash.isEmpty()) {
                return false;
            }

            URL url = new URL(CapeAPI.getAPIUrl("is-synced"));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Aetheria/" + Aetheria.VERSION);
            connection.setRequestProperty("x-playername",
                    MinecraftCompat.getMinecraft().getSession().getUsername().toLowerCase());
            connection.setRequestProperty("x-sync-code", secretHash);
            connection.setReadTimeout(10000);
            connection.setConnectTimeout(10000);
            int code = connection.getResponseCode();
            if (code == 200) {
                String body = ElectionUtils.readResponse(connection);
                if (!body.trim().isEmpty()) {
                    return JsonParser.parseString(body).getAsJsonObject().get("synced").getAsBoolean();
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}