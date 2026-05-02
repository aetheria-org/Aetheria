package com.jef.justenoughfakepixel.features.profile.data.collection;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum CollectionType {

    // Farming
    CACTUS("Cactus", CollectionBase.FARMING),
    CARROT("Carrot", CollectionBase.FARMING),
    COCOA_BEANS("Cocoa Beans", CollectionBase.FARMING),
    FEATHER("Feather", CollectionBase.FARMING),
    LEATHER("Leather", CollectionBase.FARMING),
    MELON("Melon", CollectionBase.FARMING),
    MUSHROOM("Mushroom", CollectionBase.FARMING),
    MUTTON("Mutton", CollectionBase.FARMING),
    NETHER_WART("Nether Wart", CollectionBase.FARMING),
    POTATO("Potato", CollectionBase.FARMING),
    PUMPKIN("Pumpkin", CollectionBase.FARMING),
    RAW_CHICKEN("Raw Chicken", CollectionBase.FARMING),
    RAW_PORKCHOP("Raw Porkchop", CollectionBase.FARMING),
    RAW_RABBIT("Raw Rabbit", CollectionBase.FARMING),
    SEEDS("Seeds", CollectionBase.FARMING),
    SUGAR_CANE("Sugar Cane", CollectionBase.FARMING),
    WHEAT("Wheat", CollectionBase.FARMING),

    // Mining
    COAL("Coal", CollectionBase.MINING),
    COBBLESTONE("Cobblestone", CollectionBase.MINING),
    DIAMOND("Diamond", CollectionBase.MINING),
    EMERALD("Emerald", CollectionBase.MINING),
    END_STONE("End Stone", CollectionBase.MINING),
    GEMSTONE("Gemstone", CollectionBase.MINING),
    GLOWSTONE_DUST("Glowstone Dust", CollectionBase.MINING),
    GOLD("Gold", CollectionBase.MINING),
    GRAVEL("Gravel", CollectionBase.MINING),
    HARD_STONE("Hard Stone", CollectionBase.MINING),
    ICE("Ice", CollectionBase.MINING),
    IRON("Iron", CollectionBase.MINING),
    LAPIS_LAZULI("Lapis Lazuli", CollectionBase.MINING),
    MITHRIL("Mithril", CollectionBase.MINING),
    NETHER_QUARTZ("Nether Quartz", CollectionBase.MINING),
    NETHERRACK("Netherrack", CollectionBase.MINING),
    OBSIDIAN("Obsidian", CollectionBase.MINING),
    REDSTONE("Redstone", CollectionBase.MINING),
    RED_SAND("Red Sand", CollectionBase.MINING),
    SAND("Sand", CollectionBase.MINING),
    MYCELIUM("Mycelium", CollectionBase.MINING),

    // Combat
    BLAZE_ROD("Blaze Rod", CollectionBase.COMBAT),
    BONE("Bone", CollectionBase.COMBAT),
    CHILI_PEPPER("Chili Pepper", CollectionBase.COMBAT),
    ENDER_PEARL("Ender Pearl", CollectionBase.COMBAT),
    GHAST_TEAR("Ghast Tear", CollectionBase.COMBAT),
    GUNPOWDER("Gunpowder", CollectionBase.COMBAT),
    MAGMA_CREAM("Magma Cream", CollectionBase.COMBAT),
    ROTTEN_FLESH("Rotten Flesh", CollectionBase.COMBAT),
    SLIMEBALL("Slimeball", CollectionBase.COMBAT),
    SPIDER_EYE("Spider Eye", CollectionBase.COMBAT),
    STRING("String", CollectionBase.COMBAT),

    // Foraging
    ACACIA_WOOD("Acacia Wood", CollectionBase.FORAGING),
    BIRCH_WOOD("Birch Wood", CollectionBase.FORAGING),
    DARK_OAK_WOOD("Dark Oak Wood", CollectionBase.FORAGING),
    JUNGLE_WOOD("Jungle Wood", CollectionBase.FORAGING),
    OAK_WOOD("Oak Wood", CollectionBase.FORAGING),
    SPRUCE_WOOD("Spruce Wood", CollectionBase.FORAGING),

    // Fishing
    CLAY("Clay", CollectionBase.FISHING),
    CLOWNFISH("Clownfish", CollectionBase.FISHING),
    INK_SACK("Ink Sack", CollectionBase.FISHING),
    LILY_PAD("Lily Pad", CollectionBase.FISHING),
    PRISMARINE_CRYSTALS("Prismarine Crystals", CollectionBase.FISHING),
    PRISMARINE_SHARD("Prismarine Shard", CollectionBase.FISHING),
    PUFFERFISH("Pufferfish", CollectionBase.FISHING),
    RAW_FISH("Raw Fish", CollectionBase.FISHING),
    RAW_SALMON("Raw Salmon", CollectionBase.FISHING),
    SPONGE("Sponge", CollectionBase.FISHING),
    MAGMAFISH("Magmafish", CollectionBase.FISHING),

    // Boss
    BONZO("Bonzo", CollectionBase.BOSS),
    SCARF("Scarf", CollectionBase.BOSS),
    PROFESSOR("Professor", CollectionBase.BOSS),
    THORN("Thorn", CollectionBase.BOSS),
    LIVID("Livid", CollectionBase.BOSS),
    SADAN("Sadan", CollectionBase.BOSS),
    NECRON("Necron", CollectionBase.BOSS);

    public final String itemName;
    public final CollectionBase base;

    public static CollectionType get(String name) {
        for(CollectionType type : CollectionType.values()){
            if(type.itemName.equalsIgnoreCase(name)) return type;
        }
        return null;
    }
}