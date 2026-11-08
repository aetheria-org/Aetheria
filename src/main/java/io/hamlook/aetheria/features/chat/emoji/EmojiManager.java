package io.hamlook.aetheria.features.chat.emoji;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.network.NetworkGuard;
import io.hamlook.aetheria.repo.ATHRRepo;
import io.hamlook.aetheria.repo.RepoHandler;
import io.hamlook.aetheria.utils.ElectionUtils;
import io.hamlook.aetheria.utils.ThreadUtils;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Downloads, caches and serves the emoji sprite sheets used for :name: chat tokens.
 * Coordinates and sheets both come from the repo, so they share one revision.
 */
public class EmojiManager {

    private static final Map<String, Emoji> emojis = new ConcurrentHashMap<>();
    private static final Map<String, String> aliases = new ConcurrentHashMap<>();
    private static final Map<String, CustomEmoji> customEmojis = new ConcurrentHashMap<>();
    private static final Map<String, String> customAliases = new ConcurrentHashMap<>();

    private static final AtomicBoolean loaded = new AtomicBoolean(false);

    public static void init() {
        ThreadUtils.run(EmojiManager::startInitialisation);
    }

    public static void startInitialisation() {
        EmojiSheetStore.loadResident();
        registerEmojis();
        if (!NetworkGuard.githubAllowed()) return;
        Consumer<Boolean> onDone = ok -> {
            if (ok) RepoHandler.saveVersion(ATHRRepo.KEY_EMOJIS);
        };
        if (RepoHandler.isUpdateNeeded(ATHRRepo.KEY_EMOJIS)) {
            EmojiSheetStore.downloadAll(onDone);
        } else {
            EmojiSheetStore.downloadMissing(onDone);
        }
    }

    private static void registerEmojis() {
        emojis.clear();
        aliases.clear();
        customEmojis.clear();
        customAliases.clear();
        String json = RepoHandler.getJson(ATHRRepo.KEY_EMOJI_DATA);
        if (json == null || json.isEmpty()) {
            Aetheria.logger.info("[EMOJI] emoji data not fetched yet - defaults arrive when the repo refresh lands");
            RepoHandler.addListener(ATHRRepo.KEY_EMOJI_DATA, EmojiManager::loadDefaultEmojisFromRepo);
        } else {
            loadDefaultEmojis(json);
        }
        loadCustomEmojis();
        Aetheria.logger.info("[EMOJI] Loaded " + emojis.size() + " emojis, " + aliases.size() + " aliases and " + customEmojis.size() + " custom emojis.");
    }

    private static void loadDefaultEmojisFromRepo() {
        String json = RepoHandler.getJson(ATHRRepo.KEY_EMOJI_DATA);
        if (json == null || json.isEmpty()) return;
        loadDefaultEmojis(json);
    }

    private static void loadDefaultEmojis(String json) {
        try {
            JsonArray data = JsonParser.parseString(json).getAsJsonArray();
            int columns = 0;
            for (JsonElement element : data) {
                JsonObject object = element.getAsJsonObject();
                if (!object.has("short_name") || !object.has("sheet_x") || !object.has("sheet_y")) continue;

                String shortName = object.get("short_name").getAsString();
                int column = object.get("sheet_x").getAsInt();
                int row = object.get("sheet_y").getAsInt();
                columns = Math.max(columns, column + 1);

                emojis.put(shortName, new Emoji(shortName,
                        column * EmojiLinks.SHEET_CELL + 1,
                        row * EmojiLinks.SHEET_CELL + 1));

                if (!object.has("short_names")) continue;
                for (JsonElement name : object.get("short_names").getAsJsonArray()) {
                    aliases.put(name.getAsString(), shortName);
                }
            }
            if (emojis.isEmpty()) {
                Aetheria.logger.info("[EMOJI] emoji data held no usable entries");
                return;
            }
            loaded.set(true);
            Aetheria.logger.info("[EMOJI] Parsed " + emojis.size() + " default emojis (" + columns + " columns).");
            EmojiSheetStore.ensureGrid(columns);
        } catch (Exception e) {
            Aetheria.logger.warning("[EMOJI] Failed to parse repo emoji data: " + e.getMessage());
        }
    }

    private static void loadCustomEmojis() {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(EmojiLinks.getCustomEmojiJSON()).openConnection();
            connection.setRequestProperty("User-Agent", "Aetheria");
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);
            if (connection.getResponseCode() != 200) return;

            String json = ElectionUtils.readResponse(connection);
            if (json.isEmpty()) return;

            for (JsonElement element : JsonParser.parseString(json).getAsJsonArray()) {
                JsonObject object = element.getAsJsonObject();
                if (!object.has("short_name") || !object.has("sprite_coords")) continue;

                CustomEmoji emoji = new CustomEmoji();
                emoji.shortcode = object.get("short_name").getAsString();
                emoji.width = object.get("width").getAsInt();
                emoji.height = object.get("height").getAsInt();
                emoji.animated = object.has("animated") && object.get("animated").getAsBoolean();
                emoji.frametime = emoji.animated && object.has("frametime") ? object.get("frametime").getAsInt() : 0;
                for (JsonElement coord : object.get("sprite_coords").getAsJsonArray()) {
                    JsonObject sprite = coord.getAsJsonObject();
                    emoji.sprites.add(new SpritePos(sprite.get("x").getAsInt(), sprite.get("y").getAsInt()));
                }
                customEmojis.put(emoji.shortcode, emoji);
                if (!object.has("short_names")) continue;
                for (JsonElement alias : object.get("short_names").getAsJsonArray()) {
                    customAliases.put(alias.getAsString(), emoji.shortcode);
                }
            }
            if (!customEmojis.isEmpty()) loaded.set(true);
            validateCustomSheetSize();
        } catch (Exception e) {
            Aetheria.logger.warning("[EMOJI] Failed to load custom emojis - " + e.getMessage());
        }
    }

    private static void validateCustomSheetSize() {
        if (customEmojis.isEmpty()) return;
        int maxRequired = 0;
        for (CustomEmoji emoji : customEmojis.values()) {
            for (SpritePos pos : emoji.sprites) {
                maxRequired = Math.max(maxRequired, Math.max(pos.x + emoji.width, pos.y + emoji.height));
            }
        }
        EmojiSheetStore.ensureCustomSheet(maxRequired);
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
        return EmojiSheetStore.width(sheetName);
    }

    public static String themeSheet() {
        return EmojiSheetStore.theme();
    }

    // true when the token would draw as an icon right now. exists() alone turns true
    // as soon as the data is parsed, which is before any sheet is uploaded, and the
    // modern renderer measures width and draws in two separate passes
    public static boolean canRender(String nameOrAlias) {
        if (!exists(nameOrAlias)) return false;
        if (getEmoji(nameOrAlias) != null) {
            String sheet = themeSheet();
            return EmojiSheetStore.isResident(sheet) && EmojiSheetStore.width(sheet) > 0;
        }
        return EmojiSheetStore.isResident(EmojiLinks.CUSTOM_SHEET)
                && EmojiSheetStore.width(EmojiLinks.CUSTOM_SHEET) > 0;
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
