package io.hamlook.aetheria.features.chat.emoji;

import com.google.gson.*;
import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.network.NetworkGuard;
import io.hamlook.aetheria.repo.ATHRRepo;
import io.hamlook.aetheria.repo.RepoHandler;
import io.hamlook.aetheria.utils.ElectionUtils;
import io.hamlook.aetheria.utils.HttpClient;
import io.hamlook.aetheria.utils.ThreadUtils;
import io.hamlook.aetheria.core.StorageManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Downloads, caches, and serves the emoji texture set used for :name: chat
 * tokens and the chat-box suggestion popup.
 * <p>
 * Everything is packed into ONE json manifest (name + aliases + a small base64
 * PNG per emoji) hosted alongside Aetheria's other repo data files. Update
 * detection is delegated to {@link RepoHandler} via the shared
 * ASMDataVersions.json manifest: the emoji version is only pulled down - and
 * the sprite sheets re-decoded - when it actually changed or no sprites are
 * cached. The four sprite sheets download in parallel. All network access is
 * gated through {@link NetworkGuard}.
 */
public class EmojiManager {

    private static final Map<String, Emoji> emojis = new ConcurrentHashMap<>();
    private static final Map<String, String> aliases = new ConcurrentHashMap<>();
    private static final Map<String, CustomEmoji> customEmojis = new ConcurrentHashMap<>();
    public  static final Map<String,String> idToShortcode = new ConcurrentHashMap<>();
    private static final Map<String, String> customAliases = new ConcurrentHashMap<>();
    private static final Map<String, Integer> sheetSizes = new ConcurrentHashMap<>();

    private static final AtomicBoolean loaded = new AtomicBoolean(false);

    public static final String[] EMOJI_THEMES = {EmojiLinks.DISCORD_SHEET,EmojiLinks.GOOGLE_SHEET,EmojiLinks.IOS_SHEET, EmojiLinks.CUSTOM_SHEET};

    public static void init() {
        ThreadUtils.run(EmojiManager::startInitialisation);
    }

    public static void startInitialisation() {
        if(!NetworkGuard.githubAllowed()) return;
        boolean update = RepoHandler.isUpdateNeeded(ATHRRepo.KEY_EMOJIS) || spritesCorrupted();
        if(update){
            downloadSheetsParallel();
        }
        loadSpritesFromFile();
        registerEmojis();
        if(update){
            RepoHandler.saveVersion(ATHRRepo.KEY_EMOJIS);
        }
    }

    private static void downloadSheetsParallel() {
        CountDownLatch latch = new CountDownLatch(EMOJI_THEMES.length);
        for(String theme : EMOJI_THEMES){
            ThreadUtils.run(() -> {
                try {
                    downloadSheet(theme);
                    Aetheria.logger.info("[EMOJI] Downloaded Sheet for " + theme);
                } finally {
                    latch.countDown();
                }
            });
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        int max = 0;
        for (int size : sheetSizes.values()) {
            if (size > max) max = size;
        }
        if (max > 0) EmojiLinks.SHEET_SIZE = max;
    }

    private static void registerEmojis() {
        emojis.clear();
        aliases.clear();
        customEmojis.clear();
        customAliases.clear();
        try {
            URL url = new URL(EmojiLinks.getEmojiJSON());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "Aetheria");
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);

            int responseCode = connection.getResponseCode();
            Aetheria.logger.info("[EMOJI] Fetching emoji.json — response code: " + responseCode);
            if(responseCode == 200){
                String json = ElectionUtils.readResponse(connection);
                if(json.isEmpty()){
                    Aetheria.logger.info("[EMOJI] emoji.json was empty");
                    return;
                }
                JsonArray obj = JsonParser.parseString(json).getAsJsonArray();
                if(obj == null || obj.isEmpty()){
                    Aetheria.logger.info("[EMOJI] emoji.json parsed to empty array");
                    return;
                }
                for(JsonElement element : obj){
                    JsonObject object = element.getAsJsonObject();
                    if(!object.has("short_name") ||
                    !object.has("sheet_x") || !object.has("sheet_y")) continue;

                    String shortName = object.get("short_name").getAsString();
                    int rawX = object.get("sheet_x").getAsInt();
                    int rawY = object.get("sheet_y").getAsInt();

                    int sheetX = (rawX * (EmojiLinks.SHEET_RESOLUTION + 2)) + 1;
                    int sheetY = (rawY * (EmojiLinks.SHEET_RESOLUTION + 2)) + 1;
                    Emoji emoji = new Emoji(shortName, sheetX, sheetY);
                    emojis.put(shortName,emoji);
                    if(object.has("short_names")){
                        JsonArray names = object.get("short_names").getAsJsonArray();
                        for (JsonElement name : names) {
                            aliases.put(name.getAsString(),shortName);
                        }
                    }

                }
            }
            if (!emojis.isEmpty()) loaded.set(true);
        }catch (Exception e){
            Aetheria.logger.warning("[EMOJI] Failed to load emojis from github: " + e.getMessage());
        }
        loadCustomEmojis();
        Aetheria.logger.info("[EMOJI] Successfully Loaded " + emojis.size() + " emojis, " + aliases.size() + " aliases, & " + customEmojis.size() + " custom emojis.");
    }

    private static void loadCustomEmojis() {
        try {
            URL url = new URL(EmojiLinks.getCustomEmojiJSON());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "Aetheria");
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                String json = ElectionUtils.readResponse(connection);
                if (json.isEmpty()) return;
                JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
                if (arr == null || arr.isEmpty()) return;

                for (JsonElement element : arr) {
                    JsonObject object = element.getAsJsonObject();
                    if (!object.has("short_name") || !object.has("sprite_coords")) continue;

                    String shortName = object.get("short_name").getAsString();
                    String discordID = object.has("discord_id") ? object.get("discord_id").getAsString() : "";
                    JsonArray coordsArr = object.get("sprite_coords").getAsJsonArray();
                    List<SpritePos> sprites = new ArrayList<>();
                    for (JsonElement coord : coordsArr) {
                        JsonObject c = coord.getAsJsonObject();
                        sprites.add(new SpritePos(c.get("x").getAsInt(), c.get("y").getAsInt()));
                    }
                    int width = object.get("width").getAsInt();
                    int height = object.get("height").getAsInt();
                    boolean animated = object.has("animated") && object.get("animated").getAsBoolean();
                    int frametime = animated && object.has("frametime") ? object.get("frametime").getAsInt() : 0;
                    if(!discordID.isEmpty()){
                        idToShortcode.put(discordID,shortName);
                    }
                    CustomEmoji emoji = new CustomEmoji();
                    emoji.shortcode = shortName;
                    emoji.discordID = discordID;
                    emoji.sprites = sprites;
                    emoji.width = width;
                    emoji.height = height;
                    emoji.animated = animated;
                    emoji.frametime = frametime;
                    customEmojis.put(shortName, emoji);

                    if (object.has("short_names")) {
                        JsonArray names = object.get("short_names").getAsJsonArray();
                        for (JsonElement alias : names) {
                            customAliases.put(alias.getAsString(), shortName);
                        }
                    }
                }
                if (!customEmojis.isEmpty()) loaded.set(true);
                validateCustomSheetSize();
            }
        } catch (Exception e) {
            Aetheria.logger.info("[EMOJI] Failed to load custom emojis: " + e.getMessage());
        }
    }

    private static void validateCustomSheetSize() {
        if (customEmojis.isEmpty()) return;
        int maxRequired = 0;
        for (CustomEmoji emoji : customEmojis.values()) {
            for (SpritePos pos : emoji.sprites) {
                int right = pos.x + emoji.width;
                int bottom = pos.y + emoji.height;
                if (right > maxRequired) maxRequired = right;
                if (bottom > maxRequired) maxRequired = bottom;
            }
        }
        int actualWidth = getSheetWidth(EmojiLinks.CUSTOM_SHEET);
        if (actualWidth < maxRequired) {
            Aetheria.logger.info("[EMOJI] Custom sheet too small (" + actualWidth + " px), need at least " + maxRequired + ", re-downloading...");
            File file = EmojiLinks.getSpriteFile(EmojiLinks.CUSTOM_SHEET);
            if (file.exists()) file.delete();
            downloadSheet(EmojiLinks.CUSTOM_SHEET);
            try {
                BufferedImage fImg = ImageIO.read(file);
                if (fImg != null && fImg.getWidth() >= maxRequired) {
                    sheetSizes.put(EmojiLinks.CUSTOM_SHEET, fImg.getWidth());
                    EmojiLinks.SHEET_SIZE = fImg.getWidth();
                    Minecraft.getMinecraft().addScheduledTask(() -> {
                        try {
                            DynamicTexture texture = new DynamicTexture(fImg);
                            ResourceLocation loc = EmojiLinks.getSpriteResource(EmojiLinks.CUSTOM_SHEET);
                            Minecraft.getMinecraft().getTextureManager().loadTexture(loc, texture);
                        } catch (Exception e) {
                            Aetheria.logger.info("[EMOJI] Error re-uploading custom sheet: " + e.getMessage());
                        }
                    });
                }
            } catch (IOException e) {
                Aetheria.logger.info("[EMOJI] Failed to reload custom sheet: " + e.getMessage());
            }
        }
    }

    private static void loadSpritesFromFile() {
        Map<String, BufferedImage> images = new HashMap<>();
        for (String sheet : EMOJI_THEMES) {
            File spriteFile = EmojiLinks.getSpriteFile(sheet);
            if (!spriteFile.exists()) {
                downloadSheet(sheet);
            }
            try {
                BufferedImage sheetImage = ImageIO.read(spriteFile);
                if (sheetImage == null || sheetImage.getWidth() < 32) continue;
                images.put(sheet, sheetImage);
                sheetSizes.put(sheet, sheetImage.getWidth());
                Aetheria.logger.info("[EMOJI] Sheet Size for " + sheet + " = " + sheetSizes.get(sheet));
            } catch (IOException e) {
                Aetheria.logger.warning("[EMOJI] Error Loading " + sheet + " from file at path: " + spriteFile.getPath() + " — " + e.getMessage());
            }
        }
        if (!images.isEmpty()) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                for (Map.Entry<String, BufferedImage> entry : images.entrySet()) {
                    try {
                        String sheetName = entry.getKey();
                        BufferedImage img = entry.getValue();
                        sheetSizes.put(sheetName, img.getWidth());
                        Aetheria.logger.info("[EMOJI] Sheet Size for " + sheetName + " = " + sheetSizes.get(sheetName));
                        EmojiLinks.SHEET_SIZE = img.getWidth();
                        DynamicTexture texture = new DynamicTexture(img);
                        ResourceLocation location = EmojiLinks.getSpriteResource(sheetName);
                        Minecraft.getMinecraft().getTextureManager().loadTexture(location, texture);
                    } catch (Exception e) {
                        Aetheria.logger.info("[EMOJI] Error uploading texture for " + entry.getKey() + ": " + e.getMessage());
                    }
                }
            });
        }
    }

    private static void downloadSheet(String sheet) {
        String urlSuffix = EmojiLinks.sheetToURL(sheet);
        try {
            BufferedImage image = HttpClient.fetchImage(EmojiLinks.getSpriteURL(urlSuffix));
            if (image.getWidth() < 32) return;
            File path = EmojiLinks.getSpriteFile(sheet);
            if (StorageManager.saveAtomicImage(path, image)) {
                sheetSizes.put(sheet, image.getWidth());
                Aetheria.logger.info("[EMOJI] Successfully downloaded Sheet for " + sheet + " (" + image.getWidth() + " px)");
            } else {
                Aetheria.logger.warning("[EMOJI] Failed to save downloaded Sheet " + sheet + " to " + path.getPath());
            }
        } catch (Exception e) {
            Aetheria.logger.warning("[EMOJI] Error Downloading " + sheet + " from url: " + urlSuffix + " — " + e.getMessage());
        }
    }

    private static boolean spritesCorrupted() {
        for (String sheet : EMOJI_THEMES) {
            File file = EmojiLinks.getSpriteFile(sheet);
            if (!file.exists()) return true;
            try {
                BufferedImage img = ImageIO.read(file);
                if (img == null || img.getWidth() < 32) return true;
            } catch (IOException e) {
                return true;
            }
        }
        return false;
    }

    public static boolean isLoaded() {
        return !emojis.isEmpty() && loaded.get();
    }

    public static boolean exists(String nameOrAlias) {
        String lower = nameOrAlias != null ? nameOrAlias.toLowerCase() : null;
        return lower != null && (
                emojis.containsKey(lower) || aliases.containsKey(lower) ||
                customEmojis.containsKey(lower) || customAliases.containsKey(lower));
    }
    public static List<String> search(String partial, int limit) {
        String lower = partial.toLowerCase();
        Set<String> seen = new HashSet<>();
        List<String> results = new ArrayList<>();

        for (String name : emojis.keySet()) {
            if (results.size() >= limit) break;
            if (name.toLowerCase().startsWith(lower)) {
                results.add(name);
                seen.add(name);
            }
        }

        for (String name : customEmojis.keySet()) {
            if (results.size() >= limit) break;
            if (name.toLowerCase().startsWith(lower) && !seen.contains(name)) {
                results.add(name);
                seen.add(name);
            }
        }

        if (results.size() < limit) {
            for (Map.Entry<String, String> alias : aliases.entrySet()) {
                if (results.size() >= limit) break;
                if (alias.getKey().toLowerCase().startsWith(lower) && !seen.contains(alias.getValue())) {
                    results.add(alias.getKey());
                    seen.add(alias.getValue());
                }
            }
        }

        if (results.size() < limit) {
            for (Map.Entry<String, String> alias : customAliases.entrySet()) {
                if (results.size() >= limit) break;
                if (alias.getKey().toLowerCase().startsWith(lower) && !seen.contains(alias.getValue())) {
                    results.add(alias.getKey());
                    seen.add(alias.getValue());
                }
            }
        }

        return results;
    }

    public static Emoji getEmoji(String nameOrAlias) {
        String lower = nameOrAlias.toLowerCase();
        if (emojis.containsKey(lower)) return emojis.get(lower);
        if (aliases.containsKey(lower)) return emojis.get(aliases.get(lower));
        return null;
    }

    public static CustomEmoji getCustomEmoji(String nameOrAlias) {
        String lower = nameOrAlias.toLowerCase();
        if (customEmojis.containsKey(lower)) return customEmojis.get(lower);
        if (customAliases.containsKey(lower)) return customEmojis.get(customAliases.get(lower));
        return null;
    }

    public static int getSheetWidth(String sheetName) {
        return sheetSizes.getOrDefault(sheetName, EmojiLinks.SHEET_SIZE);
    }

    public static int getAnimationTime() {
        return (int)(System.currentTimeMillis() % 86400000);
    }

    public static class Emoji {
        public String name;
        public int sheetX, sheetY;

        public Emoji(String name, int sheetX, int sheetY) {
            this.name = name;
            this.sheetX = sheetX;
            this.sheetY = sheetY;
        }

    }
}
