package io.hamlook.aetheria.features.farming.organicmatter;

import io.hamlook.aetheria.features.farming.data.Crop;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrganicMatterCrop {

    public final String rawId;
    public final String enchantedId;
    public final String enchantedChatName;
    public final String blockId;
    public final String blockChatName;
    public final String blockDisplayName;
    public final String displayName;
    private final ItemStack icon;

    private OrganicMatterCrop(String rawId, String enchantedId, String enchantedChatName,
                               String blockId, String blockChatName, String blockDisplayName,
                               String displayName, ItemStack icon) {
        this.rawId = rawId;
        this.enchantedId = enchantedId;
        this.enchantedChatName = enchantedChatName;
        this.blockId = blockId;
        this.blockChatName = blockChatName;
        this.blockDisplayName = blockDisplayName;
        this.displayName = displayName;
        this.icon = icon;
    }

    private static OrganicMatterCrop fromCrop(Crop c) {
        return new OrganicMatterCrop(c.rawId, c.enchantedId, c.enchantedChatName,
                c.blockId, c.blockChatName, c.blockDisplayName, c.displayName, c.getIcon());
    }

    public ItemStack getIcon() {
        return icon;
    }

    private static final List<OrganicMatterCrop> ALL = buildAll();
    private static final Map<String, OrganicMatterCrop> BY_RAW_ID = new HashMap<>();
    private static final Map<String, String> CHAT_NAME_TO_ID = new HashMap<>();

    private static List<OrganicMatterCrop> buildAll() {
        List<OrganicMatterCrop> list = new ArrayList<>();

        for (Crop c : Crop.all()) {
            if (!OrganicMatterValues.hasValue(c.rawId)) continue;
            list.add(fromCrop(c));
        }

        list.add(new OrganicMatterCrop("SEEDS", "ENCHANTED_SEEDS", "Enchanted Seeds",
                "SEED_BOX", "Box of Seeds", "E. Box of Seeds", "Seeds", new ItemStack(Items.wheat_seeds)));

        list.add(new OrganicMatterCrop("SQUASH", null, null, null, null, null, "Squash", null));
        list.add(new OrganicMatterCrop("CROPIE", null, null, null, null, null, "Cropie", null));
        list.add(new OrganicMatterCrop("FERMENTO", null, null,
                "CONDENSED_FERMENTO", "Condensed Fermento", "Condensed Fermento", "Fermento", null));

        return list;
    }

    static {
        for (OrganicMatterCrop c : ALL) {
            BY_RAW_ID.put(c.rawId, c);
            if (c.enchantedId != null && c.enchantedChatName != null) {
                CHAT_NAME_TO_ID.put(c.enchantedChatName, c.enchantedId);
            }
            if (c.blockId != null && c.blockChatName != null) {
                CHAT_NAME_TO_ID.put(c.blockChatName, c.blockId);
            }
        }
    }

    public static OrganicMatterCrop[] all() {
        return ALL.toArray(new OrganicMatterCrop[0]);
    }

    public static OrganicMatterCrop findByRawId(String rawId) {
        return BY_RAW_ID.get(rawId);
    }

    public static String findByChatName(String chatName) {
        return CHAT_NAME_TO_ID.get(chatName);
    }

    public static String findRawByChatName(String chatName) {
        for (OrganicMatterCrop c : ALL) {
            if (c.displayName.equals(chatName)) return c.rawId;
        }
        return null;
    }
}

