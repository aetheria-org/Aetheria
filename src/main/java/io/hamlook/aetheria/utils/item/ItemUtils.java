package io.hamlook.aetheria.utils.item;

import io.hamlook.aetheria.utils.ColorUtils;
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
import java.util.UUID;
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
        return extra.getCompoundTag("enchantments");
    }



    public static @NotNull List<String> getLoreLines(@Nullable ItemStack item) {
        NBTTagCompound display = getDisplayCompound(item);
        if (display == null || !NbtCompat.hasKey(display, "Lore", NbtCompat.TAG_LIST)) return Collections.emptyList();
        NBTTagList lore = NbtCompat.getTagList(display, "Lore", NbtCompat.TAG_STRING);
        List<String> lines = new ArrayList<>(lore.tagCount());
        for (int i = 0; i < lore.tagCount(); i++) lines.add(lore.getStringTagAt(i));
        return lines;
    }

    public static @NotNull List<String> getLoreLinesWithoutColor(@Nullable ItemStack item) {
        NBTTagCompound display = getDisplayCompound(item);
        if (display == null || !NbtCompat.hasKey(display, "Lore", NbtCompat.TAG_LIST)) return Collections.emptyList();
        NBTTagList lore = NbtCompat.getTagList(display, "Lore", NbtCompat.TAG_STRING);
        List<String> lines = new ArrayList<>(lore.tagCount());
        for (int i = 0; i < lore.tagCount(); i++) lines.add(ColorUtils.stripColor(lore.getStringTagAt(i)));
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
        if (!tag.hasKey("SkullOwner")) return "";
        NBTTagCompound skullOwner = tag.getCompoundTag("SkullOwner");
        if (!skullOwner.hasKey("Properties")) return "";
        NBTTagList textures = skullOwner.getCompoundTag("Properties").getTagList("textures", NbtCompat.TAG_COMPOUND);
        if (textures.tagCount() == 0) return "";
        NBTTagCompound entry = textures.getCompoundTagAt(0);
        return entry.hasKey("Value") ? entry.getString("Value") : "";
    }

    public static ItemStack createSkullWithTexture(String textureValue) {
        ItemStack skull = new ItemStack(Items.skull, 1, 3);
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagCompound skullOwner = new NBTTagCompound();
        skullOwner.setString("Id", UUID.randomUUID().toString());
        NBTTagCompound properties = new NBTTagCompound();
        NBTTagList textures = new NBTTagList();
        NBTTagCompound textureTag = new NBTTagCompound();
        textureTag.setString("Value", textureValue);
        textures.appendTag(textureTag);
        properties.setTag("textures", textures);
        skullOwner.setTag("Properties", properties);
        tag.setTag("SkullOwner", skullOwner);
        skull.setTagCompound(tag);
        return skull;
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
            NBTTagList potionEffects = extra.getTagList("effects", NbtCompat.TAG_COMPOUND);

            StringBuilder id = new StringBuilder("POTION");
            for (int i = 0; i < potionEffects.tagCount(); i++) {
                NBTTagCompound effectCompound = potionEffects.getCompoundTagAt(i);
                if (effectCompound.hasKey("effect")) {
                    String effectName = effectCompound.getString("effect");
                    int effectLvL = effectCompound.getInteger("lvl");
                    if(effectLvL == 0 || effectName == null || effectName.isEmpty()) continue;
                    id.append("_").append(effectName.toUpperCase()).append(";").append(effectLvL);
                }
            }
            return id.toString();
        }
        if (!extra.hasKey("enchantments")) return baseId;
        NBTTagCompound enchants = extra.getCompoundTag("enchantments");
        for (String key : enchants.getKeySet()) {
            int level = enchants.getInteger(key);
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

    public static @NotNull NBTTagCompound getOrCreateTag(@NotNull ItemStack item) {
        if (item.hasTagCompound()) return item.getTagCompound();
        NBTTagCompound tag = new NBTTagCompound();
        item.setTagCompound(tag);
        return tag;
    }

    @Nullable
    public static String getItemUuid(@Nullable ItemStack item) {
        NBTTagCompound extra = getExtraAttributes(item);
        return extra != null && extra.hasKey("uuid") ? extra.getString("uuid") : null;
    }
}
