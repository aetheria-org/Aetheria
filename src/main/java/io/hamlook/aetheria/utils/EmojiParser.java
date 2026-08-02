package io.hamlook.aetheria.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.chat.globalchat.vars.IEmoji;
import io.hamlook.aetheria.network.NetworkGuard;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads Discord's default emoji data and converts between raw unicode emoji characters
 * and Discord shortcodes ({@code :name:}). The data (from the iamcal/emoji-data project)
 * is cached at {@code config/Aetheria/emoji_pretty.json}; if the cache is missing it is
 * downloaded once from GitHub on a background thread.
 * <p>
 * Textures are resolved from the same repo ({@code img-twitter-72/{unified}.png}), so an
 * emoji that exists in the data always has an image. The {@code unified} string (e.g.
 * {@code "0023-FE0F-20E3"}) is a hyphen-separated list of hex code points: {@code 0023}
 * is the base character (#), {@code FE0F} is Variation Selector-16 (render as emoji instead
 * of text), and {@code 20E3} is the combining keycap box. The raw unicode symbol is built
 * from those code points.
 */
public class EmojiParser {

    private static final String DATA_URL = "https://raw.githubusercontent.com/iamcal/emoji-data/master/emoji_pretty.json";
    private static final File DATA_FILE = new File(ATHRConfig.configDirectory, "emoji_pretty.json");
    private static final String IMG_BASE = "https://raw.githubusercontent.com/iamcal/emoji-data/master/img-twitter-72/";
    private static final char VARIATION_SELECTOR = '\uFE0F';
    private static final Pattern SHORTCODE_PATTERN = Pattern.compile(":([a-zA-Z0-9_~+-]+):");

    private static final Map<String, String> unicodeToName = new ConcurrentHashMap<>();
    private static final Map<String, String> nameToUnicode = new ConcurrentHashMap<>();
    private static final Set<Character> possibleStarts = ConcurrentHashMap.newKeySet();
    private static volatile int maxEmojiLength = 1;
    private static volatile boolean loaded = false;
    private static final AtomicBoolean fetching = new AtomicBoolean(false);

    private EmojiParser() {}

    /**
     * Loads the emoji data and returns an {@link IEmoji} for every shortcode
     * (aliases included) so defaults can be registered into the usable emoji list.
     * Idempotent - the data maps are only populated once. Reads the cached file first;
     * if it is missing, starts an async download from GitHub and returns empty.
     */
    public static List<IEmoji> loadDefaults() {
        if (loaded) return new ArrayList<>();
        if (DATA_FILE.exists()) {
            try {
                String content = new String(Files.readAllBytes(DATA_FILE.toPath()), StandardCharsets.UTF_8);
                List<IEmoji> result = parseJson(content);
                Aetheria.logger.info("[EmojiParser]: Loaded " + result.size() + " default emoji shortcodes.");
                return result;
            } catch (Exception e) {
                Aetheria.logger.warning("[EmojiParser] Failed to read cached emoji data: " + e.getMessage());
            }
        }
        if (NetworkGuard.githubAllowed()) {
            if (fetching.compareAndSet(false, true)) {
                fetchEmojiDataAsync();
            }
        } else {
            Aetheria.logger.info("[EmojiParser] Network disabled and no cached emoji data - defaults unavailable this session.");
        }
        return new ArrayList<>();
    }

    private static void fetchEmojiDataAsync() {
        new Thread(() -> {
            try {
                HttpClient.FetchResult result = new HttpClient().fetch(DATA_URL, null);
                if (result == null || result.body() == null || result.body().isEmpty()) {
                    Aetheria.logger.warning("[EmojiParser] Failed to fetch emoji data from GitHub.");
                    return;
                }
                File parent = DATA_FILE.getParentFile();
                if (parent != null && !parent.exists() && !parent.mkdirs()) {
                    Aetheria.logger.warning("[EmojiParser] Could not create config directory: " + parent.getAbsolutePath());
                    return;
                }
                File tmp = new File(parent != null ? parent : new File("."), DATA_FILE.getName() + ".tmp");
                Files.write(tmp.toPath(), result.body().getBytes(StandardCharsets.UTF_8));
                Files.move(tmp.toPath(), DATA_FILE.toPath(), StandardCopyOption.REPLACE_EXISTING);
                Aetheria.logger.info("[EmojiParser] Downloaded emoji data to " + DATA_FILE.getAbsolutePath());
                if (!loaded) {
                    List<IEmoji> emojis = parseJson(new String(Files.readAllBytes(DATA_FILE.toPath()), StandardCharsets.UTF_8));
                    Aetheria.logger.info("[EmojiParser]: Loaded " + emojis.size() + " default emoji shortcodes.");
                }
            } catch (Exception e) {
                Aetheria.logger.warning("[EmojiParser] Failed to download emoji data: " + e.getMessage());
            } finally {
                fetching.set(false);
            }
        }, "Aetheria-Emoji-Fetch").start();
    }

    private static List<IEmoji> parseJson(String content) {
        List<IEmoji> result = new ArrayList<>();
        JsonArray emojis = JsonParser.parseString(content).getAsJsonArray();
        for (JsonElement element : emojis) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has("has_img_twitter") && !obj.get("has_img_twitter").getAsBoolean()) continue;
            if (!obj.has("unified") || obj.get("unified").getAsString().isEmpty()) continue;
            String unified = obj.get("unified").getAsString();
            String imageName = unified.toLowerCase();
            String surrogates = toSurrogates(unified);
            if (surrogates.isEmpty()) continue;
            JsonArray names = obj.getAsJsonArray("short_names");
            if (names == null || names.size() == 0) continue;
            String primary = names.get(0).getAsString();
            registerKey(surrogates, primary);
            if (obj.has("non_qualified") && !obj.get("non_qualified").isJsonNull()) {
                String nonQualified = obj.get("non_qualified").getAsString();
                if (!nonQualified.isEmpty() && !nonQualified.equals(unified)) {
                    String nqSurrogates = toSurrogates(nonQualified);
                    if (!nqSurrogates.isEmpty()) registerKey(nqSurrogates, primary);
                }
            }
            for (JsonElement nameElement : names) {
                String name = nameElement.getAsString();
                nameToUnicode.putIfAbsent(name, surrogates);
                result.add(createEmoji(name, primary, surrogates, imageName));
            }
        }
        loaded = true;
        return result;
    }

    private static void registerKey(String surrogates, String primary) {
        possibleStarts.add(surrogates.charAt(0));
        unicodeToName.putIfAbsent(surrogates, primary);
        maxEmojiLength = Math.max(maxEmojiLength, surrogates.length());
    }

    private static IEmoji createEmoji(String name, String primary, String surrogates, String imageName) {
        IEmoji emoji = new IEmoji();
        emoji.id = imageName;
        emoji.identifier = primary;
        emoji.shortcode = ":" + name + ":";
        emoji.url = IMG_BASE + imageName + ".png";
        emoji.surrogates = surrogates;
        return emoji;
    }

    /** Converts an emoji-data unified string ("0023-FE0F-20E3") into the actual unicode characters. */
    private static String toSurrogates(String unified) {
        String[] parts = unified.split("-");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            try {
                int codePoint = Integer.parseInt(part, 16);
                sb.append(Character.toChars(codePoint));
            } catch (IllegalArgumentException e) {
                return "";
            }
        }
        return sb.toString();
    }

    private static String lookupUnicode(String key) {
        String name = unicodeToName.get(key);
        if (name == null && key.indexOf(VARIATION_SELECTOR) >= 0) {
            name = unicodeToName.get(key.replace(String.valueOf(VARIATION_SELECTOR), ""));
        }
        return name;
    }

    /** Replaces every unicode emoji sequence in the text with its primary shortcode ({@code :name:}). */
    public static String toShortcode(String text) {
        if (text == null || text.isEmpty()) return text;
        if (!loaded) loadDefaults();
        StringBuilder sb = new StringBuilder(text.length() + 16);
        int i = 0, n = text.length();
        while (i < n) {
            char c = text.charAt(i);
            if (!possibleStarts.contains(c)) {
                sb.append(c);
                i++;
                continue;
            }
            boolean matched = false;
            int maxLen = Math.min(maxEmojiLength, n - i);
            for (int len = maxLen; len >= 1; len--) {
                String sub = text.substring(i, i + len);
                String name = lookupUnicode(sub);
                if (name != null) {
                    sb.append(':').append(name).append(':');
                    i += len;
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    /** Replaces every {@code :shortcode:} in the text with its unicode emoji (unknown shortcodes stay as-is). */
    public static String toUnicode(String text) {
        if (text == null || text.isEmpty()) return text;
        if (!loaded) loadDefaults();
        Matcher matcher = SHORTCODE_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String unicode = nameToUnicode.get(matcher.group(1));
            matcher.appendReplacement(sb, unicode != null ? Matcher.quoteReplacement(unicode) : matcher.group());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /** Returns the primary shortcode for a unicode emoji, or null if unknown. */
    public static String shortcodeFor(String surrogates) {
        if (surrogates == null || surrogates.isEmpty()) return null;
        if (!loaded) loadDefaults();
        return lookupUnicode(surrogates);
    }
}
