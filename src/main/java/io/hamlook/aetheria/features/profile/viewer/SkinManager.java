package io.hamlook.aetheria.features.profile.viewer;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.StorageManager;
import io.hamlook.aetheria.network.NetworkGuard;
import io.hamlook.aetheria.utils.HttpClient;
import io.hamlook.aetheria.utils.ThreadUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SkinManager {

    private static final File SKIN_DIR = new File(ATHRConfig.configDirectory, "cachedSkins");
    private static final Map<String, ResourceLocation> loadedSkins = new ConcurrentHashMap<>();
    private static final Set<String> fetching = new HashSet<>();
    private static final Set<String> sessionUpdated = new HashSet<>();

    public static ResourceLocation getSkin(String username) {
        if (loadedSkins.containsKey(username)) {
            return loadedSkins.get(username);
        }

        if (!fetching.contains(username)) {
            fetching.add(username);
            fetchSkinAsync(username);
        }

        return DefaultPlayerSkin.getDefaultSkinLegacy();
    }

    private static void fetchSkinAsync(String username) {
        if (!NetworkGuard.networkingEnabled()) return;
        ThreadUtils.run("ATHR-SkinFetcher-" + username, () -> {
            try {
                File skinFile = new File(SKIN_DIR, username + ".png");

                if (!skinFile.exists() || !sessionUpdated.contains(username)) {
                    BufferedImage img = HttpClient.fetchImage("https://mc-heads.net/skin/" + username);
                    if (img != null && StorageManager.saveAtomicImage(skinFile, img)) {
                        sessionUpdated.add(username);
                    }
                }

                if (skinFile.exists()) {
                    BufferedImage finalImg = ImageIO.read(skinFile);
                    if (finalImg != null) {
                        MinecraftCompat.getMinecraft().addScheduledTask(() -> {
                            DynamicTexture dynTex = new DynamicTexture(finalImg);
                            ResourceLocation loc = MinecraftCompat.getMinecraft().getTextureManager().getDynamicTextureLocation("skin_" + username, dynTex);
                            loadedSkins.put(username, loc);
                        });
                    }
                }

            } catch (Exception e) {
                Aetheria.logger.warning("[SkinManager] Failed to fetch skin for " + username + ": " + e.getMessage());
            } finally {
                if (!loadedSkins.containsKey(username)) {
                    fetching.remove(username);
                }
            }
        });
    }
}