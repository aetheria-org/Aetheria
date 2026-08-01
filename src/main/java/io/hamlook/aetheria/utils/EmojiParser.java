package io.hamlook.aetheria.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.features.chat.globalchat.vars.IEmoji;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads Discord's default emoji data (bundled {@code emoji_pretty.json}, sourced from
 * the iamcal/emoji-data project) and converts between raw unicode emoji characters
 * and Discord shortcodes ({@code :name:}).
 * <p>
 * Textures are resolved from the same repo ({@code img-twitter-72/{unified}.png}), so an
 * emoji that exists in the data always has an image. The {@code unified} string (e.g.
 * {@code "0023-FE0F-20E3"}) is a hyphen-separated list of hex code points: {@code 0023}
 * is the base character (#), {@code FE0F} is Variation Selector-16 (render as emoji instead
 * of text), and {@code 20E3} is the combining keycap box. The raw unicode symbol is built
 * from those code points.
 */
public class EmojiParser {

    private static final String DATA_PATH = "/emoji_pretty.json";
    private static final String IMG_BASE = "https://raw.githubusercontent.com/iamcal/emoji-data/master/img-twitter-72/";
    private static final char VARIATION_SELECTOR = '\uFE0F';
    private static final Pattern SHORTCODE_PATTERN = Pattern.compile(":([a-zA-Z0-9_~+-]+):");

    private static final Map<String, String> unicodeToName = new HashMap<>();
    private static final Map<String, String> nameToUnicode = new HashMap<>();
    private static int maxEmojiLength = 1;
    private static boolean loaded = false;

    private EmojiParser() {}

    /**
     * Loads the bundled emoji data and returns an {@link IEmoji} for every shortcode
     * (aliases included) so defaults can be registered into the usable emoji list.
     * Idempotent - the data maps are only populated once.
     */
    public static List<IEmoji> loadDefaults() {
        if (loaded) return new ArrayList<>();
        List<IEmoji> result = new ArrayList<>();
        try (InputStream in = EmojiParser.class.getResourceAsStream(DATA_PATH)) {
            if (in == null) {
                Aetheria.logger.warning("[EmojiParser] Could not find " + DATA_PATH + " in the jar.");
                return result;
            }
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
            }
            JsonArray emojis = JsonParser.parseString(sb.toString()).getAsJsonArray();
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
                unicodeToName.putIfAbsent(surrogates, primary);
                maxEmojiLength = Math.max(maxEmojiLength, surrogates.length());
                if (obj.has("non_qualified") && !obj.get("non_qualified").isJsonNull()) {
                    String nonQualified = obj.get("non_qualified").getAsString();
                    if (!nonQualified.isEmpty() && !nonQualified.equals(unified)) {
                        String nqSurrogates = toSurrogates(nonQualified);
                        if (!nqSurrogates.isEmpty()) {
                            unicodeToName.putIfAbsent(nqSurrogates, primary);
                            maxEmojiLength = Math.max(maxEmojiLength, nqSurrogates.length());
                        }
                    }
                }
                for (JsonElement nameElement : names) {
                    String name = nameElement.getAsString();
                    nameToUnicode.putIfAbsent(name, surrogates);
                    IEmoji emoji = new IEmoji();
                    emoji.id = imageName;
                    emoji.identifier = primary;
                    emoji.shortcode = ":" + name + ":";
                    emoji.url = IMG_BASE + imageName + ".png";
                    emoji.surrogates = surrogates;
                    result.add(emoji);
                }
            }
            loaded = true;
            Aetheria.logger.info("[EmojiParser]: Loaded " + result.size() + " default emoji shortcodes.");
        } catch (Exception e) {
            Aetheria.logger.warning("[EmojiParser]: Failed to load emoji data: " + e.getMessage());
        }
        return result;
    }

    /** Converts an emoji-data unified string ("0023-FE0F-20E3") into the actual unicode characters. */
    private static String toSurrogates(String unified) {
        String[] parts = unified.split("-");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            try {
                int codePoint = Integer.parseInt(part, 16);
                sb.append(Character.toChars(codePoint));
            } catch (Exception e) {
                return "";
            }
        }
        return sb.toString();
    }

    /** Replaces every unicode emoji sequence in the text with its primary shortcode ({@code :name:}). */
    public static String toShortcode(String text) {
        if (text == null || text.isEmpty()) return text;
        if (!loaded) loadDefaults();
        StringBuilder sb = new StringBuilder(text.length() + 16);
        int i = 0, n = text.length();
        while (i < n) {
            boolean matched = false;
            int maxLen = Math.min(maxEmojiLength, n - i);
            for (int len = maxLen; len >= 1; len--) {
                String sub = text.substring(i, i + len);
                String name = unicodeToName.get(sub);
                if (name == null && sub.indexOf(VARIATION_SELECTOR) >= 0) {
                    name = unicodeToName.get(sub.replace(String.valueOf(VARIATION_SELECTOR), ""));
                }
                if (name != null) {
                    sb.append(':').append(name).append(':');
                    i += len;
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                sb.append(text.charAt(i));
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
        String name = unicodeToName.get(surrogates);
        if (name == null && surrogates.indexOf(VARIATION_SELECTOR) >= 0) {
            name = unicodeToName.get(surrogates.replace(String.valueOf(VARIATION_SELECTOR), ""));
        }
        return name;
    }
}
