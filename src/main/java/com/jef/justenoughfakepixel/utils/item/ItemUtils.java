package com.jef.justenoughfakepixel.utils.item;

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

    public static @NotNull List<String> getLoreLines(@Nullable ItemStack item) {
        if (item == null || !item.hasTagCompound()) return Collections.emptyList();
        NBTTagCompound display = item.getTagCompound().getCompoundTag("display");
        if (display == null || !display.hasKey("Lore")) return Collections.emptyList();
        NBTTagList lore = display.getTagList("Lore", 8);
        List<String> lines = new ArrayList<>(lore.tagCount());
        for (int i = 0; i < lore.tagCount(); i++) lines.add(lore.getStringTagAt(i));
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
        if (item == null || !item.hasTagCompound()) return "";
        NBTTagCompound extra = item.getTagCompound().getCompoundTag("ExtraAttributes");
        return extra.hasKey("id") ? extra.getString("id") : "";
    }

    public static String getSkullTexture(@Nullable ItemStack item) {
        if (item == null || !item.hasTagCompound()) return "";
        if (!item.getTagCompound().hasKey("SkullOwner")) return "";
        NBTTagCompound skullOwner = item.getTagCompound().getCompoundTag("SkullOwner");
        if (!skullOwner.hasKey("Properties")) return "";
        NBTTagList textures = skullOwner.getCompoundTag("Properties").getTagList("textures", 10);
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
        if (item == null || !item.hasTagCompound()) return "";
        NBTTagCompound extra = item.getTagCompound().getCompoundTag("ExtraAttributes");
        String baseId = extra.hasKey("id") ? extra.getString("id") : "";
        if (!"ENCHANTED_BOOK".equals(baseId)) return baseId;
        if (!extra.hasKey("enchantments")) return baseId;
        NBTTagCompound enchants = extra.getCompoundTag("enchantments");
        for (String key : enchants.getKeySet()) {
            int level = enchants.getInteger(key);
            return key + "_" + level;
        }
        return baseId;
    }

    public static boolean isSkyblockItem(@Nullable ItemStack item) {
        if (item == null || !item.hasTagCompound()) return false;
        return item.getTagCompound().getCompoundTag("ExtraAttributes").hasKey("id");
    }

    public static @NotNull NBTTagCompound getOrCreateTag(@NotNull ItemStack item) {
        if (item.hasTagCompound()) return item.getTagCompound();
        NBTTagCompound tag = new NBTTagCompound();
        item.setTagCompound(tag);
        return tag;
    }

    @Nullable
    public static String getItemUuid(@Nullable ItemStack item) {
        if (item == null || !item.hasTagCompound()) return null;
        NBTTagCompound extra = item.getTagCompound().getCompoundTag("ExtraAttributes");
        return extra.hasKey("uuid") ? extra.getString("uuid") : null;
    }
}