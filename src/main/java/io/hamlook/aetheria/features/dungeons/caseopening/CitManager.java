package io.hamlook.aetheria.features.dungeons.caseopening;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.hamlook.aetheria.utils.compat.ResourceCompat;
import io.hamlook.aetheria.utils.compat.TextCompat;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class CitManager {

    private static final String MC_DOMAIN = "minecraft";
    private static final String CIT_FOLDER = "mcpatcher/cit";
    private static final String PROPS_EXT = ".properties";
    private static final String ASSETS_PREFIX = "assets/minecraft/";
    private static final String FRAME_TIME_KEY = "frametime";

    private static final Map<String, TextureData> citCache = new HashMap<>();

    public CitManager() {
        ResourceCompat.registerReloadListener("cit", () -> rebuildCache());
    }

    private static int getFrameTime(String namespace, String path) {
        try {
            byte[] data = ResourceCompat.openResource(TextCompat.createResourceLocation(namespace, path + ".mcmeta"));
            if (data == null) return 1;
            JsonObject root = JsonParser.parseReader(new InputStreamReader(new ByteArrayInputStream(data))).getAsJsonObject();
            if (root.has("animation")) {
                JsonObject anim = root.getAsJsonObject("animation");
                if (anim.has(FRAME_TIME_KEY)) return anim.get(FRAME_TIME_KEY).getAsInt();
            }
        } catch (Exception ignored) {
        }
        return 1;
    }

    private static int getFrameCount(String namespace, String path) {
        try {
            byte[] data = ResourceCompat.openResource(TextCompat.createResourceLocation(namespace, path));
            if (data == null) return 1;
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
            if (img == null) return 1;
            return Math.max(img.getHeight() / img.getWidth(), 1);
        } catch (Exception e) {
            return 1;
        }
    }

    public static TextureData getTextureData(String nbtId) {
        return citCache.getOrDefault(nbtId, new TextureData(ItemEnum.valueOf(nbtId).getDefaultRl(), 1, 1));
    }

    private void rebuildCache() {
        citCache.clear();
        for (Map.Entry<String, byte[]> entry : ResourceCompat.findResources(MC_DOMAIN, CIT_FOLDER, PROPS_EXT).entrySet()) {
            String path = entry.getKey();
            try (InputStream in = new ByteArrayInputStream(entry.getValue())) {
                Properties props = new Properties();
                props.load(in);
                processProperties(props, path);
            } catch (Exception ignored) {
            }
        }
    }

    private void processProperties(Properties props, String path) {
        String nbtId = props.getProperty("nbt.ExtraAttributes.id");
        String itemType = props.getProperty("items", "");

        if ((MC_DOMAIN + ":enchanted_book").equals(itemType)) {
            for (String key : props.stringPropertyNames()) {
                if (key.startsWith("nbt.ExtraAttributes.enchantments.")) {
                    String enchantName = key.substring("nbt.ExtraAttributes.enchantments.".length());
                    nbtId = enchantName + "_" + props.getProperty(key);
                    break;
                }
            }
        }

        if (nbtId == null) return;
        try {
            ItemEnum.valueOf(nbtId);
        } catch (IllegalArgumentException e) {
            return;
        }

        int lastSlash = path.lastIndexOf('/');
        String pathFolder = lastSlash >= 0 ? path.substring(0, lastSlash) : "";
        String textureProp = props.getProperty("texture");
        String texturePath;

        if (textureProp != null && !textureProp.isEmpty()) {
            texturePath = textureProp.startsWith("/") ? textureProp.substring(1) : pathFolder + "/" + textureProp;
            if (texturePath.endsWith(".png")) texturePath = texturePath.substring(0, texturePath.length() - 4);
        } else {
            String baseName = path.substring(lastSlash + 1, path.length() - PROPS_EXT.length());
            texturePath = pathFolder + "/" + baseName;
        }

        if (texturePath.startsWith(ASSETS_PREFIX)) texturePath = texturePath.substring(ASSETS_PREFIX.length());

        ResourceLocation rl = TextCompat.createResourceLocation(MC_DOMAIN, texturePath + ".png");
        citCache.put(nbtId, new TextureData(rl, getFrameTime(MC_DOMAIN, texturePath + ".png"), getFrameCount(MC_DOMAIN, texturePath + ".png")));
    }
}
