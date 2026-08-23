package io.hamlook.aetheria.features.capes;

import com.google.gson.*;
import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.StorageManager;
import io.hamlook.aetheria.network.NetworkGuard;
import io.hamlook.aetheria.repo.ATHRRepo;
import io.hamlook.aetheria.repo.RepoHandler;
import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.HttpClient;
import io.hamlook.aetheria.utils.ThreadUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class CapeLoader {

    private static final String CONTENTS_API = "https://api.github.com/repos/aetheria-org/Aetheria-REPO/contents/capes";
    private static final File TEXTURE_DIR = new File(new File(ATHRConfig.configDirectory, "repo"), "capes");
    private static final HttpClient HTTP = new HttpClient();
    private static final Gson GSON = new Gson();
    private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9_-]+");

    public static void loadAllCapes() {
        if (!NetworkGuard.githubAllowed()) return;
        List<Cape> capes = loadCapeList();
        if (capes.isEmpty()) {
            Aetheria.logger.warning("[CapeLoader] No capes available (fetch failed and no usable cache)");
            return;
        }
        for (Cape cape : capes) loadTexture(cape);
        Aetheria.logger.info("[CapeLoader] Loading " + capes.size() + " capes.");
    }

    static String textureUrl(String texture) {
        return ATHRRepo.BASE + "capeTextures/" + texture;
    }

    private static List<Cape> loadCapeList() {
        File cache = RepoHandler.cacheFile(ATHRRepo.KEY_CAPES);
        if (shouldRefetch(cache)) {
            List<Cape> fresh = fetchAndStore(cache);
            if (!fresh.isEmpty()) return fresh;
        }
        List<Cape> cached = parseList(StorageManager.loadSafeRaw(cache));
        if (!cached.isEmpty()) return cached;
        return fetchAndStore(cache);
    }

    private static boolean shouldRefetch(File cache) {
        return RepoHandler.isUpdateNeeded(ATHRRepo.KEY_CAPES) || !cache.isFile();
    }

    private static List<Cape> fetchAndStore(File cache) {
        List<Cape> capes = fetchCapeList();
        if (capes.isEmpty()) return capes;
        if (StorageManager.saveAtomicRaw(cache, GSON.toJson(capes))) {
            RepoHandler.saveVersion(ATHRRepo.KEY_CAPES);
        }
        return capes;
    }

    private static List<Cape> fetchCapeList() {
        List<Cape> out = new ArrayList<>();
        try {
            String listing = HTTP.fetch(CONTENTS_API, null).body();
            JsonArray entries = JsonParser.parseString(listing).getAsJsonArray();
            for (JsonElement element : entries) {
                if (!element.isJsonObject()) continue;
                JsonObject entry = element.getAsJsonObject();
                if (!entry.has("name") || !entry.has("type")) continue;
                String name = entry.get("name").getAsString();
                String type = entry.get("type").getAsString();
                if (!"file".equals(type) || !name.endsWith(".json")) continue;
                Cape cape = fetchCapeMeta(name);
                if (cape != null) out.add(cape);
            }
        } catch (Exception e) {
            Aetheria.logger.warning("[CapeLoader] Failed to list capes: " + e.getMessage());
        }
        return out;
    }

    private static Cape fetchCapeMeta(String fileName) {
        try {
            String body = HTTP.fetch(ATHRRepo.BASE + "capes/" + fileName, null).body();
            Cape cape = GSON.fromJson(body, Cape.class);
            if (cape == null || cape.id == null || cape.name == null || cape.texture == null || cape.id.isEmpty() || !VALID_ID.matcher(cape.id).matches()) {
                Aetheria.logger.warning("[CapeLoader] Skipping invalid cape meta: " + fileName);
                return null;
            }
            return new Cape(cape.id, ColorUtils.stripColor(cape.name).trim(), cape.texture);
        } catch (Exception e) {
            Aetheria.logger.warning("[CapeLoader] Failed to fetch cape meta '" + fileName + "': " + e.getMessage());
            return null;
        }
    }

    private static List<Cape> parseList(String json) {
        List<Cape> out = new ArrayList<>();
        if (json == null || json.isEmpty()) return out;
        try {
            JsonArray entries = JsonParser.parseString(json).getAsJsonArray();
            for (JsonElement element : entries) {
                if (!element.isJsonObject()) continue;
                Cape cape = GSON.fromJson(element, Cape.class);
                if (cape != null && cape.id != null && cape.texture != null) out.add(cape);
            }
        } catch (Exception e) {
            Aetheria.logger.warning("[CapeLoader] Failed to parse cached cape list: " + e.getMessage());
        }
        return out;
    }

    private static void loadTexture(Cape cape) {
        File file = textureFile(cape.id);
        BufferedImage cached = readCachedTexture(file);
        if (cached != null) {
            uploadTexture(cape, cached);
            return;
        }
        ThreadUtils.run("CapeLoader-Texture-" + cape.id, () -> downloadTexture(cape, file));
    }

    private static void downloadTexture(Cape cape, File file) {
        try {
            BufferedImage image = HttpClient.fetchImage(textureUrl(cape.texture));
            if (StorageManager.saveAtomicImage(file, image)) {
                uploadTexture(cape, image);
            } else {
                Aetheria.logger.warning("[CapeLoader] Failed to cache texture for '" + cape.id + "'");
            }
        } catch (Exception e) {
            Aetheria.logger.warning("[CapeLoader] Failed to load texture for '" + cape.id + "': " + e.getMessage());
        }
    }

    private static File textureFile(String id) {
        return new File(TEXTURE_DIR, id + ".png");
    }

    private static BufferedImage readCachedTexture(File file) {
        try {
            if (!file.isFile()) return null;
            BufferedImage image = ImageIO.read(file);
            if (image != null) return image;
            Aetheria.logger.warning("[CapeLoader] Undecodable cached texture: " + file.getName());
            return null;
        } catch (Exception e) {
            Aetheria.logger.warning("[CapeLoader] Failed to read cached texture '" + file.getName() + "': " + e.getMessage());
            return null;
        }
    }

    private static void uploadTexture(Cape cape, BufferedImage image) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            ResourceLocation location = new ResourceLocation("aetheria", "capes/" + cape.id);
            Minecraft.getMinecraft().getTextureManager().loadTexture(location, new DynamicTexture(image));
            cape.resourceLocation = location;
        });
    }
}
