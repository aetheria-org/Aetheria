package io.hamlook.aetheria.features.profile.viewer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.StorageManager;
import io.hamlook.aetheria.network.NetworkGuard;
import io.hamlook.aetheria.utils.HttpClient;
import io.hamlook.aetheria.utils.ThreadUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
                        Minecraft.getMinecraft().addScheduledTask(() -> {
                            DynamicTexture dynTex = new DynamicTexture(finalImg);
                            ResourceLocation loc = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("skin_" + username, dynTex);
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

    /** Same fetch/cache/register pipeline as {@link #getSkin(String)}, but keyed by an arbitrary
     *  cache key and fed a Mojang session-server "Value" property (base64 JSON containing a
     *  {@code textures.SKIN.url}) instead of a username — e.g. the {@code texture} field already
     *  stored on a {@link io.hamlook.aetheria.features.misc.itemList.SkyblockItem}. */
    public static ResourceLocation getSkinFromValue(String cacheKey, String base64Value) {
        if (loadedSkins.containsKey(cacheKey)) {
            return loadedSkins.get(cacheKey);
        }

        if (!fetching.contains(cacheKey)) {
            fetching.add(cacheKey);
            fetchSkinFromValueAsync(cacheKey, base64Value);
        }

        return DefaultPlayerSkin.getDefaultSkinLegacy();
    }

    /** Pulls the same "Value" property straight out of a skull {@link ItemStack}'s
     *  {@code SkullOwner} NBT (as built by {@code ItemUtils.createSkullWithTexture}), so any
     *  ItemRegistry-sourced skull with an embedded texture can be head-cropped without needing a
     *  bundled PNG asset at all. Returns {@code null} for anything that isn't a textured player
     *  head, so callers can fall back to normal item rendering. */
    public static ResourceLocation getSkinFromStack(ItemStack stack) {
        if (stack == null || stack.getItem() != Items.skull || stack.getMetadata() != 3 || !stack.hasTagCompound()) {
            return null;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (!tag.hasKey("SkullOwner", 10)) return null;
        NBTTagCompound owner = tag.getCompoundTag("SkullOwner");
        NBTTagList textures = owner.getCompoundTag("Properties").getTagList("textures", 10);
        if (textures.tagCount() == 0) return null;
        String value = textures.getCompoundTagAt(0).getString("Value");
        if (value.isEmpty()) return null;
        String cacheKey = owner.hasKey("Id") ? owner.getString("Id") : String.valueOf(value.hashCode());
        return getSkinFromValue(cacheKey, value);
    }

    private static void fetchSkinFromValueAsync(String cacheKey, String base64Value) {
        if (!NetworkGuard.networkingEnabled()) return;
        ThreadUtils.run("ATHR-SkinFetcher-" + cacheKey, () -> {
            try {
                File skinFile = new File(SKIN_DIR, cacheKey.replaceAll("[^a-zA-Z0-9_.-]", "_") + ".png");

                if (!skinFile.exists()) {
                    String json = new String(Base64.getDecoder().decode(base64Value), StandardCharsets.UTF_8);
                    JsonObject obj = new JsonParser().parse(json).getAsJsonObject();
                    String skinUrl = obj.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();

                    BufferedImage img = HttpClient.fetchImage(skinUrl);
                    StorageManager.saveAtomicImage(skinFile, img);
                }

                if (skinFile.exists()) {
                    BufferedImage finalImg = ImageIO.read(skinFile);
                    if (finalImg != null) {
                        Minecraft.getMinecraft().addScheduledTask(() -> {
                            DynamicTexture dynTex = new DynamicTexture(finalImg);
                            ResourceLocation loc = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("skinval_" + cacheKey, dynTex);
                            loadedSkins.put(cacheKey, loc);
                        });
                    }
                }

            } catch (Exception e) {
                Aetheria.logger.warning("[SkinManager] Failed to fetch skin for " + cacheKey + ": " + e.getMessage());
            } finally {
                if (!loadedSkins.containsKey(cacheKey)) {
                    fetching.remove(cacheKey);
                }
            }
        });
    }
}