package io.hamlook.aetheria.features.capes;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.network.NetworkGuard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CapeLoader {

    private static final String RAW_BASE =
            "https://raw.githubusercontent.com/aetheria-org/Aetheria-REPO/refs/heads/main/";

    private static final String CONTENTS_API =
            "https://api.github.com/repos/aetheria-org/Aetheria-REPO/contents/capes";

    public static void loadAllCapes() {
        if (!NetworkGuard.githubAllowed()) return;
        Aetheria.logger.info("[CapeLoader] Starting cape load...");

        String rawJson = httpGet(CONTENTS_API);
        if (rawJson == null) {
            Aetheria.logger.info("[CapeLoader] Contents API returned null — network failure or bad URL");
            return;
        }

        List<String> capeIds = parseCapeIds(rawJson);
        Aetheria.logger.info("[CapeLoader] Found " + capeIds.size() + " cape ID(s): ");

        if (capeIds.isEmpty()) {
            Aetheria.logger.info("[CapeLoader] No IDs parsed — check the log output above");
            return;
        }

        for (String id : capeIds) {
            try {
                loadCape(id);
            } catch (Exception e) {
                Aetheria.logger.info("[CapeLoader] Failed to load cape: " + id + " — " + e.getMessage());
                e.printStackTrace();
            }
        }

        Aetheria.logger.info("[CapeLoader] Done. Total capes registered: " + CapeManager.capes.size());
    }

    private static List<String> parseCapeIds(String json) {
        List<String> ids = new ArrayList<>();
        int searchFrom = 0;

        while (true) {
            int nameIdx = json.indexOf("\"name\":\"", searchFrom);
            if (nameIdx == -1) break;

            int start = nameIdx + 8;
            int end = json.indexOf("\"", start);
            if (end == -1) break;

            String filename = json.substring(start, end);

            if (filename.endsWith(".json")) {
                ids.add(filename.substring(0, filename.length() - 5));
            }

            searchFrom = end + 1;
        }

        return ids;
    }

    private static void loadCape(String id) throws Exception {
        String url = RAW_BASE + "/capes/" + id + ".json";

        String json = httpGet(url);
        if (json == null) throw new Exception("Failed to fetch JSON for: " + id);

        String parsedId      = parseJsonString(json, "id");
        String parsedName    = parseJsonString(json, "name");
        String parsedTexture = parseJsonString(json, "texture");

        if (parsedId == null || parsedName == null || parsedTexture == null) {
            throw new Exception("Invalid JSON for cape: " + id);
        }

        Cape cape = new Cape(parsedId, parsedName, parsedTexture);
        loadTextureForCape(cape);

        CapeManager.register(cape);
        Aetheria.logger.info("[CapeLoader] Registered cape: " + cape.id);
    }

    private static void loadTextureForCape(Cape cape) throws Exception {
        String textureUrl = RAW_BASE + "/capeTextures/" + cape.texture;

        BufferedImage image = fetchImage(textureUrl);
        if (image == null) throw new Exception("Failed to fetch texture: " + cape.texture);

        final BufferedImage finalImage = image;
        Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(() -> {
            DynamicTexture texture = new DynamicTexture(finalImage);
            ResourceLocation location = new ResourceLocation("aetheria", "capes/" + cape.id);
            mc.getTextureManager().loadTexture(location, texture);
            cape.resourceLocation = location;
        });
    }

    private static String httpGet(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("User-Agent", "Aetheria/1.0");
            conn.setRequestProperty("Accept", "application/vnd.github+json");

            int status = conn.getResponseCode();
            if (status != 200) return null;

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)
            );
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            return sb.toString().trim();

        } catch (Exception e) {
            Aetheria.logger.info("[CapeLoader] httpGet failed for: " + urlStr + " — " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static BufferedImage fetchImage(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("User-Agent", "Aetheria/1.0");

            int status = conn.getResponseCode();
            if (status != 200) {
                Aetheria.logger.info("[CapeLoader] Texture fetch failed: " + status + " for " + urlStr);
                return null;
            }

            return ImageIO.read(conn.getInputStream());

        } catch (Exception e) {
            Aetheria.logger.info("[CapeLoader] fetchImage failed: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static String parseJsonString(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx == -1) return null;

        int pos = idx + search.length();
        while (pos < json.length() && (json.charAt(pos) == ' ' || json.charAt(pos) == ':')) {
            pos++;
        }

        if (pos >= json.length() || json.charAt(pos) != '"') return null;
        pos++;

        int end = json.indexOf("\"", pos);
        if (end == -1) return null;
        return json.substring(pos, end);
    }
}