package io.hamlook.aetheria.features.farming;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class Crop {

    private static final long RAW_TO_ENCHANTED_RATIO = 160L;
    private static final long ENCHANTED_TO_BLOCK_RATIO = 160L;

    public final String rawId;
    public final String enchantedId;
    public final String enchantedChatName;
    public final String blockId;
    public final String blockChatName;
    public final String blockDisplayName;
    public final String displayName;

    private Crop(String rawId, String enchantedId, String enchantedChatName, String blockId, String blockChatName, String displayName) {
        this.rawId = rawId;
        this.enchantedId = enchantedId;
        this.enchantedChatName = enchantedChatName;
        this.blockId = blockId;
        this.blockChatName = blockChatName;
        this.blockDisplayName = blockChatName != null && blockChatName.startsWith("Enchanted ")
                ? "E. " + blockChatName.substring("Enchanted ".length())
                : blockChatName;
        this.displayName = displayName;
    }

    private Crop(String rawId, String displayName) {
        this(rawId, null, null, null, null, displayName);
    }

    public ItemStack getIcon() {
        switch (rawId) {
            case "WHEAT": return new ItemStack(Items.wheat);
            case "CARROT_ITEM": return new ItemStack(Items.carrot);
            case "POTATO_ITEM": return new ItemStack(Items.potato);
            case "PUMPKIN": return new ItemStack(Item.getItemFromBlock(Blocks.pumpkin));
            case "MELON": return new ItemStack(Items.melon);
            case "SUGAR_CANE": return new ItemStack(Items.reeds);
            case "INK_SACK__3": return new ItemStack(Items.dye, 1, 3);
            case "CACTUS": return new ItemStack(Item.getItemFromBlock(Blocks.cactus));
            case "RED_MUSHROOM": return new ItemStack(Item.getItemFromBlock(Blocks.red_mushroom));
            case "BROWN_MUSHROOM": return new ItemStack(Item.getItemFromBlock(Blocks.brown_mushroom));
            case "NETHER_STALK": return new ItemStack(Items.nether_wart);
            case "WILD_ROSE": return new ItemStack(Item.getItemFromBlock(Blocks.double_plant), 1, 4);
            case "MOONFLOWER": return new ItemStack(Item.getItemFromBlock(Blocks.red_flower), 1, 1);
            case "SUNFLOWER": return new ItemStack(Item.getItemFromBlock(Blocks.double_plant), 1, 0);
            case "SEEDS": return new ItemStack(Items.wheat_seeds);
            default: return null;
        }
    }

    private static final Crop[] ALL = new Crop[]{
            new Crop("WHEAT", "ENCHANTED_WHEAT", "Enchanted Wheat", "ENCHANTED_HAY_BLOCK", "Enchanted Hay Bale", "Wheat"),
            new Crop("CARROT_ITEM", "ENCHANTED_CARROT", "Enchanted Carrot", "ENCHANTED_GOLDEN_CARROT", "Enchanted Golden Carrot", "Carrot"),
            new Crop("POTATO_ITEM", "ENCHANTED_POTATO", "Enchanted Potato", "ENCHANTED_BAKED_POTATO", "Enchanted Baked Potato", "Potato"),
            new Crop("PUMPKIN", "ENCHANTED_PUMPKIN", "Enchanted Pumpkin", "POLISHED_PUMPKIN", "Polished Pumpkin", "Pumpkin"),
            new Crop("MELON", "ENCHANTED_MELON", "Enchanted Melon", "ENCHANTED_MELON_BLOCK", "Enchanted Melon Block", "Melon"),
            new Crop("SUGAR_CANE", "ENCHANTED_SUGAR", "Enchanted Sugar", "ENCHANTED_SUGAR_CANE", "Enchanted Sugar Cane", "Sugar Cane"),
            new Crop("INK_SACK__3", "ENCHANTED_COCOA", "Enchanted Cocoa Beans", "ENCHANTED_COOKIE", "Enchanted Cookie", "Cocoa Beans"),
            new Crop("CACTUS", "ENCHANTED_CACTUS_GREEN", "Enchanted Cactus Green", "ENCHANTED_CACTUS", "Enchanted Cactus", "Cactus"),
            new Crop("RED_MUSHROOM", "ENCHANTED_RED_MUSHROOM", "Enchanted Red Mushroom", "ENCHANTED_RED_MUSHROOM_BLOCK", "Enchanted Red Mushroom Block", "Red Mushroom"),
            new Crop("BROWN_MUSHROOM", "ENCHANTED_BROWN_MUSHROOM", "Enchanted Brown Mushroom", "ENCHANTED_BROWN_MUSHROOM_BLOCK", "Enchanted Brown Mushroom Block", "Brown Mushroom"),
            new Crop("NETHER_STALK", "ENCHANTED_NETHER_STALK", "Enchanted Nether Wart", "MUTANT_NETHER_STALK", "Mutant Nether Wart", "Nether Wart"),
            new Crop("WILD_ROSE", "ENCHANTED_WILD_ROSE", "Enchanted Wild Rose", "COMPACTED_WILD_ROSE", "Compacted Wild Rose", "Wild Rose"),
            new Crop("MOONFLOWER", "ENCHANTED_MOONFLOWER", "Enchanted Moonflower", "COMPACTED_MOONFLOWER", "Compacted Moonflower", "Moonflower"),
            new Crop("SUNFLOWER", "ENCHANTED_SUNFLOWER", "Enchanted Sunflower", "COMPACTED_SUNFLOWER", "Compacted Sunflower", "Sunflower"),
            new Crop("SEEDS", "Seeds"),
    };

    private static final Map<String, Crop> BY_RAW_ID = new HashMap<>();
    private static final Map<String, String> CHAT_NAME_TO_ID = new HashMap<>();
    private static final Map<String, Long> RAW_EQUIVALENT = new HashMap<>();

    static {
        for (Crop crop : ALL) {
            BY_RAW_ID.put(crop.rawId, crop);

            if (crop.enchantedChatName != null) {
                CHAT_NAME_TO_ID.put(crop.enchantedChatName, crop.enchantedId);
            }
            if (crop.blockId != null) {
                CHAT_NAME_TO_ID.put(crop.blockChatName, crop.blockId);
            }

            RAW_EQUIVALENT.put(crop.rawId, 1L);
            if (crop.enchantedId != null) {
                RAW_EQUIVALENT.put(crop.enchantedId, RAW_TO_ENCHANTED_RATIO);
            }
            if (crop.blockId != null) {
                RAW_EQUIVALENT.put(crop.blockId, RAW_TO_ENCHANTED_RATIO * ENCHANTED_TO_BLOCK_RATIO);
            }
        }
    }

    public static Crop[] all() {
        return ALL;
    }

    public static Crop findByRawId(String rawId) {
        return BY_RAW_ID.get(rawId);
    }

    public static String findByChatName(String chatName) {
        return CHAT_NAME_TO_ID.get(chatName);
    }

    public static Long rawEquivalentOf(String id) {
        return RAW_EQUIVALENT.get(id);
    }
}
