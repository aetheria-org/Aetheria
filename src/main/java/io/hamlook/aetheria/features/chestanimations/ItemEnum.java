package io.hamlook.aetheria.features.chestanimations;

import net.minecraft.util.ResourceLocation;

public enum ItemEnum {

    // ======================= FLOOR 1 =======================
    // Wood: Necromancer's Brooch, Feather Falling VI, Infinite Quiver VI, Rejuvenate I, Bank I, Ultimate Jerry I
    NECROMANCER_BROOCH, feather_falling_6, infinite_quiver_6, rejuvenate_1, bank_1, ultimate_jerry_1,
    // Gold: Hot Potato Book, Combo I, No Pain No Gain I
    HOT_POTATO_BOOK, ultimate_combo_1, ultimate_no_pain_no_gain_1,
    // Diamond: Bonzo's Mask, Red Nose
    BONZO_MASK, RED_NOSE,
    // Emerald: Bonzo's Staff, Fuming Potato Book
    BONZO_STAFF, FUMING_POTATO_BOOK,
    // Obsidian: Recombobulator 3000, Dungeon/Clown/Necron/Watcher Disc (in-game ids DUNGEON_DISC_1..5)
    RECOMBOBULATOR_3000, DUNGEON_DISC_1, CLOWN_DISC_2, NECRON_DISC_5, WATCHER_DISC_3,
    // Obsidian (Master only): Master Skull Tier 1, Old Disc
    MASTER_SKULL_TIER_1, OLD_DISC_4,

    // ======================= FLOOR 2 =======================
    // Gold: Scarf's Studies, Ultimate Wise I
    SCARF_STUDIES, ultimate_wise_1,
    // Diamond: Red Scarf
    RED_SCARF,
    // Emerald: Adaptive Blade
    ADAPTIVE_BLADE,
    // Obsidian (Master only): Master Skull Tier 2
    MASTER_SKULL_TIER_2,

    // ======================= FLOOR 3 =======================
    // Gold: Adaptive Boots, Wisdom I
    ADAPTIVE_BOOTS, wisdom_1,
    // Diamond: Adaptive Helmet, Suspicious Vial
    ADAPTIVE_HELMET, SUSPICIOUS_VIAL,
    // Emerald: Adaptive Chestplate, Adaptive Leggings, Rejuvenate II, Last Stand I, Ultimate Jerry II
    ADAPTIVE_CHESTPLATE, ADAPTIVE_LEGGINGS, rejuvenate_2, ultimate_last_stand_1, ultimate_jerry_2,
    // Obsidian: Bank II
    bank_2,
    // Obsidian (Master only): First Master Star, Master Skull Tier 3
    FIRST_MASTER_STAR, MASTER_SKULL_TIER_3,

    // ======================= FLOOR 4 =======================
    // Gold/Diamond/Emerald/Obsidian: Rend I
    rend_1,
    // Diamond: Epic Spirit Pet (NEU id "SPIRIT;3"), Spirit Stone
    SPIRIT("SPIRIT;3"), SPIRIT_STONE,
    // Emerald: Legendary Spirit Pet (NEU id "SPIRIT;4"), Spirit Bone, Spirit Boots
    SPIRIT_LEGENDARY("SPIRIT;4", "spirit"), SPIRIT_BONE, SPIRIT_BOOTS,
    // Obsidian: Spirit Bow, Spirit Sword, Spirit Wing
    SPIRIT_BOW, SPIRIT_SWORD, SPIRIT_WING,
    // Obsidian (Master only): Second Master Star (Master Skull Tier 4 shared with F5)
    SECOND_MASTER_STAR,

    // ======================= FLOOR 5 =======================
    // Gold: Dark Orb, Lethality VI, Overload I
    DARK_ORB, lethality_6, overload_1,
    // Diamond: Shadow Assassin Boots, Legion I
    SHADOW_ASSASSIN_BOOTS, ultimate_legion_1,
    // Emerald: Shadow Assassin Helmet, Warped Stone
    SHADOW_ASSASSIN_HELMET, WARPED_STONE,
    // Obsidian: Shadow Assassin Leggings, Livid Dagger, Lethality I
    SHADOW_ASSASSIN_LEGGINGS, LIVID_DAGGER, lethality_1,
    // Bedrock: Shadow Assassin Chestplate, Shadow Fury, Last Breath, Rejuvenate III, Bank III, Combo II, Last Stand II, No Pain No Gain II, Ultimate Jerry III, Ultimate Wise II, Wisdom II
    SHADOW_ASSASSIN_CHESTPLATE, SHADOW_FURY, LAST_BREATH, rejuvenate_3, bank_3, ultimate_combo_2, ultimate_last_stand_2, ultimate_no_pain_no_gain_2, ultimate_jerry_3, ultimate_wise_2, wisdom_2,
    // Obsidian/Bedrock (Master only): Third Master Star, Master Skull Tier 3/4
    THIRD_MASTER_STAR, MASTER_SKULL_TIER_4,

    // ======================= FLOOR 6 =======================
    // Gold: Giant Tooth, Swarm I
    GIANT_TOOTH, ultimate_swarm_1,
    // Diamond: Ancient Rose, Necromancer Lord Boots
    ANCIENT_ROSE, NECROMANCER_LORD_BOOTS,
    // Emerald: Necromancer Lord Helmet, Sadan's Brooch
    NECROMANCER_LORD_HELMET, SADAN_BROOCH,
    // Obsidian: Necromancer Lord Leggings, Necromancer Sword, Summoning Ring
    NECROMANCER_LORD_LEGGINGS, NECROMANCER_SWORD, SUMMONING_RING,
    // Bedrock: Necromancer Lord Chestplate, Precursor Eye, Giant's Sword
    NECROMANCER_LORD_CHESTPLATE, PRECURSOR_EYE, GIANTS_SWORD,
    // Obsidian/Bedrock (Master only): Fourth Master Star (Master Skull Tier 4 shared with F5)
    FOURTH_MASTER_STAR,

    // ======================= FLOOR 7 =======================
    // Gold: Precursor Gear, Wither Boots, Wither Catalyst
    PRECURSOR_GEAR, WITHER_BOOTS, WITHER_CATALYST,
    // Diamond: Wither Helmet
    WITHER_HELMET,
    // Emerald: Wither Blood, Wither Cloak (Cloak Sword), Wither Leggings, Soul Eater I
    WITHER_BLOOD, WITHER_CLOAK, WITHER_LEGGINGS, ultimate_soul_eater_1,
    // Obsidian: Wither Chestplate, One For All I
    WITHER_CHESTPLATE, ultimate_one_for_all_1,
    // Bedrock: Auto Recombobulator, Implosion Scroll, Shadow Warp Scroll, Wither Shield Scroll, Necron's Handle, Storm/Maxor/Goldor the Fish, Feather Falling VII, Infinite Quiver VII
    AUTO_RECOMBOBULATOR, IMPLOSION_SCROLL, SHADOW_WARP_SCROLL, WITHER_SHIELD_SCROLL, NECRON_HANDLE, STORM_THE_FISH, MAXOR_THE_FISH, GOLDOR_THE_FISH, feather_falling_7, infinite_quiver_7,
    // Obsidian (Master only): Fifth Master Star, Master Skull Tier 4
    FIFTH_MASTER_STAR,
    // Bedrock (Master only): Dark Claymore, Necron Dye, Thunderlord VII, Master Skull Tier 5
    DARK_CLAYMORE, NECRON_DYE, thunderlord_7, MASTER_SKULL_TIER_5,

    // ===== Retained for compatibility (not part of current loot tables) =====
    ADAPTIVE_BELT, BALLOON_SNAKE, FEL_SKULL, SOULWEAVER_GLOVES, SHADOW_ASSASSIN_CLOAK, SPIRIT_SHORTBOW;

    private final String id;
    private final String texture;

    ItemEnum() {
        this(null, null);
    }

    ItemEnum(String id) {
        this(id, null);
    }

    ItemEnum(String id, String texture) {
        this.id = id;
        this.texture = texture;
    }

    /**
     * Effective id as used by {@link io.hamlook.aetheria.utils.item.ItemUtils#getEffectiveItemId}.
     * Defaults to the enum name; overridden for ids that cannot be Java identifiers (e.g. pets: "SPIRIT;3").
     */
    public String getId() {
        return id != null ? id : name();
    }

    public ResourceLocation getDefaultRl() {
        String tex = texture != null ? texture : name().toLowerCase();
        return new ResourceLocation("aetheria", "textures/dungeons/caseopening/" + tex + ".png");
    }

    /**
     * True for enchantment-book drops (all lowercase keys, e.g. "bank_1", "rend_1").
     * These share the same vanilla enchanted-book icon, so they don't need per-item textures.
     */
    public boolean isBook() {
        String id = getId();
        return id.equals(id.toLowerCase());
    }
}