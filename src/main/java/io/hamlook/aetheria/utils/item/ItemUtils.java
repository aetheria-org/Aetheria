package io.hamlook.aetheria.utils.item;

import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.compat.ItemCompatKt;
import io.hamlook.aetheria.utils.compat.NbtCompat;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class ItemUtils {

    private ItemUtils() {
    }



    public static @Nullable NBTTagCompound getExtraAttributes(@Nullable ItemStack item) {
        if (item == null) return null;
        return NbtCompat.getExtraAttributes(item);
    }

    public static @Nullable NBTTagCompound getDisplayCompound(@Nullable ItemStack item) {
        if (item == null) return null;
        return NbtCompat.getDisplayCompound(item);
    }

    public static @Nullable NBTTagCompound getEnchantments(@Nullable ItemStack item) {
        NBTTagCompound extra = getExtraAttributes(item);
        if (extra == null || !NbtCompat.hasKey(extra, "enchantments", NbtCompat.TAG_COMPOUND)) return null;
        return NbtCompat.getCompound(extra, "enchantments");
    }



    public static @NotNull List<String> getLoreLines(@Nullable ItemStack item) {
        NBTTagCompound display = getDisplayCompound(item);
        if (display == null || !NbtCompat.hasKey(display, "Lore", NbtCompat.TAG_LIST)) return Collections.emptyList();
        NBTTagList lore = NbtCompat.getTagList(display, "Lore", NbtCompat.TAG_STRING);
        List<String> lines = new ArrayList<>(NbtCompat.tagCount(lore));
        for (int i = 0; i < NbtCompat.tagCount(lore); i++) lines.add(NbtCompat.getStringAt(lore, i));
        return lines;
    }

    public static @NotNull List<String> getLoreLinesWithoutColor(@Nullable ItemStack item) {
        NBTTagCompound display = getDisplayCompound(item);
        if (display == null || !NbtCompat.hasKey(display, "Lore", NbtCompat.TAG_LIST)) return Collections.emptyList();
        NBTTagList lore = NbtCompat.getTagList(display, "Lore", NbtCompat.TAG_STRING);
        List<String> lines = new ArrayList<>(NbtCompat.tagCount(lore));
        for (int i = 0; i < NbtCompat.tagCount(lore); i++) lines.add(ColorUtils.stripColor(NbtCompat.getStringAt(lore, i)));
        return lines;
    }

    @Nullable
    public static String getLoreLine(@Nullable ItemStack item, String contains) {
        for (String line : getLoreLines(item)) {
            if (line.contains(contains)) return line;
        }
        return null;
    }

    @Nullable
    public static String getLoreLine(@Nullable ItemStack item, Pattern pattern) {
        for (String line : getLoreLines(item)) {
            if (pattern.matcher(line).find()) return line;
        }
        return null;
    }



    public static String getInternalName(@Nullable ItemStack item) {
        NBTTagCompound extra = getExtraAttributes(item);
        return extra != null && extra.hasKey("id") ? extra.getString("id") : "";
    }


    public static String getSkullTexture(@Nullable ItemStack item) {
        NBTTagCompound tag = NbtCompat.getTagCompound(item);
        if (tag == null) return "";
        if (!NbtCompat.hasKey(tag, "SkullOwner")) return "";
        NBTTagCompound skullOwner = NbtCompat.getCompound(tag, "SkullOwner");
        if (!NbtCompat.hasKey(skullOwner, "Properties")) return "";
        NBTTagList textures = NbtCompat.getTagList(NbtCompat.getCompound(skullOwner, "Properties"), "textures", NbtCompat.TAG_COMPOUND);
        if (NbtCompat.tagCount(textures) == 0) return "";
        NBTTagCompound entry = NbtCompat.getCompoundAt(textures, 0);
        return NbtCompat.hasKey(entry, "Value") ? NbtCompat.getString(entry, "Value") : "";
    }

    public static ItemStack createSkullWithTexture(String textureValue) {
        ItemStack skull = ItemCompatKt.createPlayerHeadStack();
        NBTTagCompound tag = NbtCompat.createCompound();
        NBTTagCompound skullOwner = NbtCompat.createCompound();
        NbtCompat.setString(skullOwner, "Id", UUID.randomUUID().toString());
        NBTTagCompound properties = NbtCompat.createCompound();
        NBTTagList textures = NbtCompat.createList();
        NBTTagCompound textureTag = NbtCompat.createCompound();
        NbtCompat.setString(textureTag, "Value", textureValue);
        NbtCompat.appendTag(textures, textureTag);
        NbtCompat.setTagList(properties, "textures", textures);
        NbtCompat.setTag(skullOwner, "Properties", properties);
        NbtCompat.setTag(tag, "SkullOwner", skullOwner);
        NbtCompat.setTagCompound(skull, tag);
        return skull;
    }

    private static final Map<String, ItemStack> SKULL_CACHE = new ConcurrentHashMap<>();

    public static ItemStack getCachedSkull(String textureValue) {
        return SKULL_CACHE.computeIfAbsent(textureValue, ItemUtils::createSkullWithTexture);
    }


    public static String getEffectiveItemId(@Nullable ItemStack item) {
        NBTTagCompound extra = getExtraAttributes(item);
        if (extra == null) return "";
        String baseId = extra.hasKey("id") ? extra.getString("id") : "";
        if ("PET".equals(baseId)) {
            NBTTagCompound petInfo = extra.getCompoundTag("petInfo");
            String type = petInfo.getString("type");
            String tier = petInfo.getString("tier");
            if (!type.isEmpty() && !tier.isEmpty()) {
                return type + ";" + rarityToInt(tier);
            }
            return baseId;
        }
        if (!"ENCHANTED_BOOK".equals(baseId)){
            if(!"POTION".equals(baseId)) return baseId;
            NBTTagList potionEffects = NbtCompat.getTagList(extra, "effects", NbtCompat.TAG_COMPOUND);

            StringBuilder id = new StringBuilder("POTION");
            for (int i = 0; i < NbtCompat.tagCount(potionEffects); i++) {
                NBTTagCompound effectCompound = NbtCompat.getCompoundAt(potionEffects, i);
                if (NbtCompat.hasKey(effectCompound, "effect")) {
                    String effectName = NbtCompat.getString(effectCompound, "effect");
                    int effectLvL = NbtCompat.getInt(effectCompound, "lvl");
                    if(effectLvL == 0 || effectName == null || effectName.isEmpty()) continue;
                    id.append("_").append(effectName.toUpperCase()).append(";").append(effectLvL);
                }
            }
            return id.toString();
        }
        if (!NbtCompat.hasKey(extra, "enchantments")) return baseId;
        NBTTagCompound enchants = NbtCompat.getCompound(extra, "enchantments");
        for (String key : NbtCompat.keySet(enchants)) {
            int level = NbtCompat.getInt(enchants, key);
            return key + "_" + level;
        }
        return baseId;
    }

    private static int rarityToInt(String tier) {
        switch (tier) {
            case "COMMON": return 0;
            case "UNCOMMON": return 1;
            case "RARE": return 2;
            case "EPIC": return 3;
            case "LEGENDARY": return 4;
            case "MYTHIC": return 5;
            default: return -1;
        }
    }

    public static boolean isSkyblockItem(@Nullable ItemStack item) {
        NBTTagCompound extra = getExtraAttributes(item);
        return extra != null && extra.hasKey("id");
    }

    /**
     * Returns the stack's tag for mutation. Legacy returns the live compound;
     * on modern the returned compound is detached — callers must write it back
     * with {@link NbtCompat#setTagCompound} after mutating (see SkyblockItem).
     */
    public static @NotNull NBTTagCompound getOrCreateTag(@NotNull ItemStack item) {
        NBTTagCompound existing = NbtCompat.getTagCompound(item);
        if (existing != null) return existing;
        NBTTagCompound tag = NbtCompat.createCompound();
        NbtCompat.setTagCompound(item, tag);
        return tag;
    }

    @Nullable
    public static String getItemUuid(@Nullable ItemStack item) {
        NBTTagCompound extra = getExtraAttributes(item);
        return extra != null && extra.hasKey("uuid") ? extra.getString("uuid") : null;
    }
}
