package io.hamlook.aetheria.utils.item;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.compat.InventoryCompat;
import io.hamlook.aetheria.utils.compat.ItemCompatKt;
import io.hamlook.aetheria.utils.compat.NbtCompat;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
//#if MC > 1.21
//$$ import net.minecraft.component.DataComponentTypes;
//$$ import net.minecraft.component.type.ProfileComponent;
//#endif

import java.nio.charset.StandardCharsets;
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
        if (ItemCompatKt.isEmptyCompat(item)) return null;
        return CachedItemData.of(item).nbt.get(item, ItemUtils::loadNbtData).extraAttributes;
    }

    public static @Nullable NBTTagCompound getEnchantments(@Nullable ItemStack item) {
        NBTTagCompound extra = getExtraAttributes(item);
        if (extra == null || !NbtCompat.hasKey(extra, "enchantments", NbtCompat.TAG_COMPOUND)) return null;
        return NbtCompat.getCompound(extra, "enchantments");
    }



    public static @NotNull List<String> getLoreLines(@Nullable ItemStack item) {
        if (ItemCompatKt.isEmptyCompat(item)) return Collections.emptyList();
        return CachedItemData.of(item).lore.get(item, ItemUtils::loadLore);
    }

    public static @NotNull List<String> getLoreLinesWithoutColor(@Nullable ItemStack item) {
        if (ItemCompatKt.isEmptyCompat(item)) return Collections.emptyList();
        return CachedItemData.of(item).loreStripped.get(item, ItemUtils::loadLoreStripped);
    }

    private static List<String> loadLore(ItemStack item) {
        return Collections.unmodifiableList(NbtCompat.getLoreLines(item));
    }

    public static List<String> getLore(ItemStack stack) {
        List<String> lore = new ArrayList<>();
        if (stack == null || !stack.hasTagCompound()) return lore;
        NBTTagCompound display = stack.getTagCompound().getCompoundTag("display");
        if (!display.hasKey("Lore", 9)) return lore;
        NBTTagList loreList = display.getTagList("Lore", 8);
        for (int i = 0; i < loreList.tagCount(); i++)
            lore.add(ColorUtils.stripColor(loreList.getStringTagAt(i)).trim());
        return lore;
    }

    public static List<String> getLoreColored(ItemStack stack) {
        List<String> lore = new ArrayList<>();
        if (stack == null || !stack.hasTagCompound()) return lore;
        NBTTagCompound display = stack.getTagCompound().getCompoundTag("display");
        if (!display.hasKey("Lore", 9)) return lore;
        NBTTagList loreList = display.getTagList("Lore", 8);
        for (int i = 0; i < loreList.tagCount(); i++)
            lore.add(loreList.getStringTagAt(i));
        return lore;
    }

    private static List<String> loadLoreStripped(ItemStack item) {
        List<String> lines = getLoreLines(item);
        if (lines.isEmpty()) return lines;
        List<String> out = new ArrayList<>(lines.size());
        for (String line : lines) out.add(ColorUtils.stripColor(line));
        return Collections.unmodifiableList(out);
    }

    private static CachedItemData.NbtData loadNbtData(ItemStack item) {
        NBTTagCompound extra = NbtCompat.getExtraAttributes(item);
        String internalName = extra != null && extra.hasKey("id") ? extra.getString("id") : "";
        String uuid = extra != null && extra.hasKey("uuid") ? extra.getString("uuid") : null;
        return new CachedItemData.NbtData(extra, internalName, computeEffectiveItemId(extra), uuid);
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
        if (ItemCompatKt.isEmptyCompat(item)) return "";
        return CachedItemData.of(item).nbt.get(item, ItemUtils::loadNbtData).internalName;
    }

    public static @NotNull String getDisplayName(@Nullable ItemStack item) {
        if (item == null) return "";
        return CachedItemData.of(item).displayName.get(item, ItemUtils::loadDisplayName);
    }

    private static String loadDisplayName(ItemStack item) {
        return ItemCompatKt.getDisplayNameCompat(item);
    }

    // hotbar check, uses the per tick snapshot
    public static boolean isItemInHotbar(String internalId) {
        return PlayerInventoryCache.isItemInHotbar(internalId);
    }

    // main inventory, read fresh, no snapshot
    public static ItemStack[] getMainInventory() {
        return InventoryCompat.getMainInventory();
    }


    public static final class SkullIdentity {
        public final String textureValue;
        public final String profileId;
        public final String profileName;
        public final String signature;
        public final GameProfile profile;

        private SkullIdentity(String textureValue, String profileId, String profileName, String signature) {
            this.textureValue = textureValue;
            this.profileId = profileId;
            this.profileName = profileName;
            this.signature = signature;
            UUID uuid;
            try {
                uuid = profileId.isEmpty() ? UUID.nameUUIDFromBytes(textureValue.getBytes(StandardCharsets.UTF_8)) : UUID.fromString(profileId);
            } catch (IllegalArgumentException e) {
                uuid = UUID.nameUUIDFromBytes(textureValue.getBytes(StandardCharsets.UTF_8));
            }
            GameProfile profile = new GameProfile(uuid, profileName);
            profile.getProperties().put("textures", signature.isEmpty()
                    ? new Property("textures", textureValue)
                    : new Property("textures", textureValue, signature));
            this.profile = profile;
        }

        public static SkullIdentity of(String textureValue, String profileId, String profileName, String signature) {
            return new SkullIdentity(textureValue, profileId, profileName, signature);
        }
    }

    public static @Nullable SkullIdentity skullIdentity(@Nullable ItemStack item) {
        if (ItemCompatKt.isEmptyCompat(item)) return null;
        NBTTagCompound tag = NbtCompat.getTagCompound(item);
        if (tag == null || !NbtCompat.hasKey(tag, "SkullOwner")) return null;
        NBTTagCompound skullOwner = NbtCompat.getCompound(tag, "SkullOwner");
        NBTTagList textures = NbtCompat.getTagList(NbtCompat.getCompound(skullOwner, "Properties"), "textures", NbtCompat.TAG_COMPOUND);
        if (NbtCompat.tagCount(textures) == 0) return null;
        NBTTagCompound entry = NbtCompat.getCompoundAt(textures, 0);
        if (!NbtCompat.hasKey(entry, "Value")) return null;
        String value = NbtCompat.getString(entry, "Value");
        if (value.isEmpty()) return null;
        String signature = NbtCompat.hasKey(entry, "Signature") ? NbtCompat.getString(entry, "Signature") : "";
        String id = NbtCompat.hasKey(skullOwner, "Id") ? NbtCompat.getString(skullOwner, "Id") : "";
        String name = NbtCompat.hasKey(skullOwner, "Name") ? NbtCompat.getString(skullOwner, "Name") : "";
        return SkullIdentity.of(value, id, name, signature);
    }

    private static final Map<String, SkullIdentity> SKULL_IDENTITIES = new ConcurrentHashMap<>();

    public static void registerSkullIdentity(String textureValue, String profileId, String profileName, String signature) {
        if (textureValue == null || textureValue.isEmpty()) return;
        SKULL_IDENTITIES.put(textureValue, SkullIdentity.of(textureValue, profileId, profileName, signature));
    }

    /** The identity registered for this texture value, or a synthetic one without a head item behind it. */
    public static @Nullable SkullIdentity skullIdentity(@Nullable String textureValue) {
        if (textureValue == null || textureValue.isEmpty()) return null;
        SkullIdentity identity = SKULL_IDENTITIES.get(textureValue);
        return identity != null ? identity : SkullIdentity.of(textureValue, "", "", "");
    }

    public static @Nullable ItemStack createSkullWithTexture(@Nullable String textureValue) {
        SkullIdentity identity = skullIdentity(textureValue);
        if (identity == null) return null;
        ItemStack skull = ItemCompatKt.createPlayerHeadStack();
        //#if MC < 1.21
        NBTTagCompound tag = NbtCompat.createCompound();
        NBTTagCompound skullOwner = NbtCompat.createCompound();
        NbtCompat.setString(skullOwner, "Id", identity.profile.getId().toString());
        NbtCompat.setString(skullOwner, "Name", identity.profileName);
        NBTTagCompound properties = NbtCompat.createCompound();
        NBTTagList textures = NbtCompat.createList();
        NBTTagCompound textureTag = NbtCompat.createCompound();
        NbtCompat.setString(textureTag, "Value", identity.textureValue);
        NbtCompat.setString(textureTag, "Signature", identity.signature);
        NbtCompat.appendTag(textures, textureTag);
        NbtCompat.setTagList(properties, "textures", textures);
        NbtCompat.setTag(skullOwner, "Properties", properties);
        NbtCompat.setTag(tag, "SkullOwner", skullOwner);
        NbtCompat.setTagCompound(skull, tag);
        //#else
        //$$ skull.set(DataComponentTypes.PROFILE, new ProfileComponent(identity.profile));
        //#endif
        return skull;
    }

    private static final Map<String, ItemStack> SKULL_CACHE = new ConcurrentHashMap<>();

    public static @Nullable ItemStack getCachedSkull(@Nullable String textureValue) {
        if (textureValue == null || textureValue.isEmpty()) return null;
        return SKULL_CACHE.computeIfAbsent(textureValue, ItemUtils::createSkullWithTexture);
    }


    public static String getEffectiveItemId(@Nullable ItemStack item) {
        if (ItemCompatKt.isEmptyCompat(item)) return "";
        return CachedItemData.of(item).nbt.get(item, ItemUtils::loadNbtData).effectiveId;
    }

    private static String computeEffectiveItemId(@Nullable NBTTagCompound extra) {
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

    public static @NotNull NBTTagCompound getOrCreateTag(@NotNull ItemStack item) {
        NBTTagCompound existing = NbtCompat.getTagCompound(item);
        if (existing != null) return existing;
        NBTTagCompound tag = NbtCompat.createCompound();
        NbtCompat.setTagCompound(item, tag);
        return tag;
    }

    @Nullable
    public static String getItemUuid(@Nullable ItemStack item) {
        if (ItemCompatKt.isEmptyCompat(item)) return null;
        return CachedItemData.of(item).nbt.get(item, ItemUtils::loadNbtData).uuid;
    }
}
