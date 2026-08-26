package io.hamlook.aetheria.features.dungeons.caseopening;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.*;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class CitManager {

    private static final String MC_DOMAIN = "minecraft";
    private static final String CIT_FOLDER = "mcpatcher/cit";
    private static final String PROPS_EXT = ".properties";
    private static final String ASSETS_PREFIX = "assets/minecraft/";
    private static final String FRAME_TIME_KEY = "frametime";

    private static final Map<String, TextureData> citCache = new HashMap<>();

    public CitManager() {
        ((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener(manager -> rebuildCache());
    }

    private static int getFrameTime(IResourceManager rm, ResourceLocation rl) {
        try {
            IResource res = rm.getResource(new ResourceLocation(rl.getResourceDomain(), rl.getResourcePath() + ".mcmeta"));
            if (res == null) return 1;
            JsonObject root = JsonParser.parseReader(new InputStreamReader(res.getInputStream())).getAsJsonObject();
            if (root.has("animation")) {
                JsonObject anim = root.getAsJsonObject("animation");
                if (anim.has(FRAME_TIME_KEY)) return anim.get(FRAME_TIME_KEY).getAsInt();
            }
        } catch (Exception ignored) {
        }
        return 1;
    }

    private static int getFrameCount(IResourceManager rm, ResourceLocation rl) {
        try (InputStream stream = rm.getResource(rl).getInputStream()) {
            BufferedImage img = ImageIO.read(stream);
            if (img == null) return 1;
            return Math.max(img.getHeight() / img.getWidth(), 1);
        } catch (Exception e) {
            return 1;
        }
    }

    public static TextureData getTextureData(String nbtId) {
        return citCache.getOrDefault(nbtId, new TextureData(ItemEnum.valueOf(nbtId).getDefaultRl(), 1, 1));
    }

    private static File getPackFile(IResourcePack pack) {
        try {
            Field f = AbstractResourcePack.class.getDeclaredField("resourcePackFile");
            f.setAccessible(true);
            return (File) f.get(pack);
        } catch (Exception e) {
            return null;
        }
    }

    private void rebuildCache() {
        citCache.clear();
        IResourceManager rm = Minecraft.getMinecraft().getResourceManager();
        for (ResourcePackRepository.Entry entry : Minecraft.getMinecraft().getResourcePackRepository().getRepositoryEntries()) {
            try {
                IResourcePack pack = entry.getResourcePack();
                for (String path : listCitFiles(pack)) {
                    try (InputStream in = pack.getInputStream(new ResourceLocation(MC_DOMAIN, path))) {
                        Properties props = new Properties();
                        props.load(in);
                        processProperties(props, path, rm);
                    } catch (Exception ignored) {
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void processProperties(Properties props, String path, IResourceManager rm) {
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

        ResourceLocation rl = new ResourceLocation(MC_DOMAIN, texturePath + ".png");
        citCache.put(nbtId, new TextureData(rl, getFrameTime(rm, rl), getFrameCount(rm, rl)));
    }

    private Set<String> listCitFiles(IResourcePack pack) {
        Set<String> result = new HashSet<>();
        File base = getPackFile(pack);
        if (base == null) return result;

        try {
            if (base.isDirectory()) {
                File target = new File(base, ASSETS_PREFIX + CIT_FOLDER);
                if (target.exists()) {
                    try (Stream<Path> walk = Files.walk(target.toPath())) {
                        walk.filter(p -> p.toString().endsWith(PROPS_EXT)).forEach(p -> result.add(base.toPath().relativize(p).toString().replace("\\", "/").substring(ASSETS_PREFIX.length())));
                    }
                }
            } else {
                try (ZipFile zip = new ZipFile(base)) {
                    Enumeration<? extends ZipEntry> entries = zip.entries();
                    while (entries.hasMoreElements()) {
                        String name = entries.nextElement().getName();
                        if (name.startsWith(ASSETS_PREFIX + CIT_FOLDER) && name.endsWith(PROPS_EXT))
                            result.add(name.substring(ASSETS_PREFIX.length()));
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return result;
    }
}