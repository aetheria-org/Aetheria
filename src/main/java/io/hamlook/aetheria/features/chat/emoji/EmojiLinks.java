package io.hamlook.aetheria.features.chat.emoji;

import io.hamlook.aetheria.core.ATHRConfig;
import net.minecraft.util.ResourceLocation;

import java.io.File;

public class EmojiLinks {

    private static final String BASE_URL = "https://raw.githubusercontent.com/iamcal/emoji-data/master/";
    private static final String CUSTOM_BASE_URL = "https://raw.githubusercontent.com/aetheria-org/Aetheria-REPO/refs/heads/main/emojis/";

    private static final String BASE_RESOURCE = "aetheria";
    public static final int SHEET_RESOLUTION = 32;
    public static int SHEET_SIZE = 2108;

    public static final String GOOGLE_URL = "sheet_google_";
    public static final String IOS_URL = "sheet_apple_";
    public static final String DISCORD_URL = "sheet_twitter_";

    private static final File BASE_DIR = new File(ATHRConfig.configDirectory, "emojis");

    public static final String GOOGLE_SHEET = "google.png";
    public static final String IOS_SHEET = "ios.png";
    public static final String DISCORD_SHEET = "discord.png";
    public static final String CUSTOM_SHEET = "custom.png";

    public static String sheetToURL(String sheet){
        if (CUSTOM_SHEET.equals(sheet)) return CUSTOM_SHEET;
        switch (sheet){
            case GOOGLE_SHEET:
                return GOOGLE_URL;
            case IOS_SHEET:
                return IOS_URL;
            default:
                return DISCORD_URL;
        }
    }
    public static String getSpriteURL(String identifier){
        if (CUSTOM_SHEET.equals(identifier)) return getCustomSpriteURL();
        return BASE_URL + identifier + SHEET_RESOLUTION + ".png";
    }

    public static File getSpriteFile(String sprite){
        return new File(BASE_DIR, sprite);
    }

    public static ResourceLocation getSpriteResource(String sprite){
        return new ResourceLocation(BASE_RESOURCE, sprite);
    }

    public static String getEmojiJSON() {
        return "https://raw.githubusercontent.com/iamcal/emoji-data/master/emoji.json";
    }

    public static String getCustomEmojiJSON() {
        return CUSTOM_BASE_URL + "emojis.json";
    }

    public static String getCustomSpriteURL() {
        return CUSTOM_BASE_URL + "emojis.png";
    }
}
