package io.hamlook.aetheria;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.command.brigadier.CommandCategory;
import io.hamlook.aetheria.command.brigadier.CommandRegistrationEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.network.NetworkGuard;
import io.hamlook.aetheria.repo.CapeAPI;
import io.hamlook.aetheria.utils.ElectionUtils;
import io.hamlook.aetheria.utils.ThreadUtils;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.TextCompat;
import io.hamlook.aetheria.utils.data.SkyblockData;
import net.minecraft.util.IChatComponent;

import java.io.File;
import java.io.FileWriter;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.util.Base64;

@RegisterEvents
public class SyncCommand {

    private static String SYNC_CODE = "";
    private static long lastUse = 0;

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

    @HandleEvent
    public void onCommandRegistration(CommandRegistrationEvent event) {
        event.registerBrigadier("sync", builder -> {
            builder.description = "Sync your account with SkyAtlas";
            builder.setCategory(CommandCategory.EXTERNAL_SERVICE);

            builder.legacyCallbackArgs(args -> {
                if (MinecraftCompat.getLocalPlayer() == null) return;

                if (ATHRConfig.feature != null && !NetworkGuard.requiresApi("/sync")) {
                    return;
                }

                if (!SkyblockData.isOnSkyblock()) {
                    ChatUtils.sendMessage("§cPlease Join SkyBlock in order to sync, this is to prove that you are not using the username of someone else.");
                    return;
                }

                if (System.currentTimeMillis() - lastUse < 240000 && !SYNC_CODE.isEmpty()) {
                    IChatComponent text = TextCompat.createText("§a[SkyAtlas] Your sync code is: §e§l" + SYNC_CODE);
                    TextCompat.setClickSuggestCommand(TextCompat.getChatStyle(text), SYNC_CODE);
                    TextCompat.setHoverShowText(TextCompat.getChatStyle(text), "§aClick to show in chat");
                    MinecraftCompat.getMinecraft().addScheduledTask(() -> {
                        ChatUtils.sendMessage(text);
                        ChatUtils.sendMessage(
                                "§r§aPlease paste this code in the §9#sync§a channel on Discord within 5 minutes!");
                    });
                    return;
                }

                String playerName = MinecraftCompat.getLocalPlayer().getName();
                String syncCode = generateSyncCode();
                ThreadUtils.run(() -> {
                    try {
                        URL url = new URL(CapeAPI.getAPIUrl("pending-sync"));
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("x-playername", playerName);
                        conn.setRequestProperty("User-Agent", "Aetheria/" + Aetheria.VERSION);
                        conn.setRequestProperty("Accept", "*/*");
                        conn.setRequestProperty("x-code", syncCode);
                        conn.setRequestProperty("x-mod-secret", CapeAPI.getModSecret());
                        conn.setDoOutput(true);

                        try (OutputStream os = conn.getOutputStream()) {
                            os.write(new byte[0]);
                            os.flush();
                        }

                        int responseCode = conn.getResponseCode();

                        if (responseCode >= 200 && responseCode < 300) {
                            String responseBody = ElectionUtils.readResponse(conn);
                            String secretHash = "";
                            try {
                                JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
                                if (json.has("secretHash")) {
                                    secretHash = json.get("secretHash").getAsString();
                                }
                            } catch (Exception ignored) {}

                            if (!secretHash.isEmpty()) {
                                File secretFile = getSecretFile();
                                try (FileWriter writer = new FileWriter(secretFile)) {
                                    writer.write(secretHash);
                                }
                            }

                            IChatComponent text = TextCompat.createText("§a[SkyAtlas] Your sync code is: §e§l" + syncCode);
                            TextCompat.setClickSuggestCommand(TextCompat.getChatStyle(text), syncCode);
                            TextCompat.setHoverShowText(TextCompat.getChatStyle(text), "§aClick to show in chat");
                            MinecraftCompat.getMinecraft().addScheduledTask(() -> {
                                ChatUtils.sendMessage(text);
                                ChatUtils.sendMessage(
                                        "§r§aPlease paste this code in the §9#sync§a channel on Discord within 5 minutes!");
                            });
                            lastUse = System.currentTimeMillis();
                            SYNC_CODE = syncCode;
                        } else {
                            MinecraftCompat.getMinecraft().addScheduledTask(() -> ChatUtils.sendMessage("§c[SkyAtlas] Failed to generate sync code. API returned status " + responseCode));
                        }

                        conn.disconnect();

                    } catch (Exception e) {
                        e.printStackTrace();

                        MinecraftCompat.getMinecraft().addScheduledTask(() -> ChatUtils.sendMessage("§c[SkyAtlas] An error occurred while contacting the sync server."));
                    }
                });
            });
        });
    }

    private String generateSyncCode() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[6];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
