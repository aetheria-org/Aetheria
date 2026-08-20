package io.hamlook.aetheria.features.chestanimations.caseopening;

import io.hamlook.aetheria.features.chestanimations.ItemEnum;
import io.hamlook.aetheria.features.dungeons.utils.DungeonFloor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static io.hamlook.aetheria.features.chestanimations.ItemEnum.*;

public class DungeonDropData {

    private static final List<Rule> RULES = new ArrayList<>();

    static {
        // ======================= FLOOR 1 =======================
        // Wood
        registerDrops(CaseMaterial.WOOD, Floor.I, rule(NECROMANCER_BROOCH, 5), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_jerry_1, 2));
        // Gold
        registerDrops(CaseMaterial.GOLD, Floor.I, rule(HOT_POTATO_BOOK, 3), rule(NECROMANCER_BROOCH, 5), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_combo_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(ultimate_jerry_1, 2));
        // Diamond
        registerDrops(CaseMaterial.DIAMOND, Floor.I, rule(BONZO_MASK, 2), rule(HOT_POTATO_BOOK, 3), rule(NECROMANCER_BROOCH, 5), rule(RED_NOSE, 3), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_combo_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(ultimate_jerry_1, 2));
        // Emerald
        registerDrops(CaseMaterial.EMERALD, Floor.I, rule(BONZO_MASK, 2), rule(BONZO_STAFF, 2), rule(FUMING_POTATO_BOOK, 3), rule(HOT_POTATO_BOOK, 3), rule(NECROMANCER_BROOCH, 5), rule(RED_NOSE, 3), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_combo_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(ultimate_jerry_1, 2));
        // Obsidian
        registerDrops(CaseMaterial.OBSIDIAN, Floor.I, rule(BONZO_MASK, 2), rule(BONZO_STAFF, 2), rule(FUMING_POTATO_BOOK, 3), rule(HOT_POTATO_BOOK, 3), rule(NECROMANCER_BROOCH, 5), rule(RECOMBOBULATOR_3000, 3), rule(RED_NOSE, 3), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_combo_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(ultimate_jerry_1, 2), rule(DUNGEON_DISC_1, 2), rule(CLOWN_DISC_2, 2), rule(WATCHER_DISC_3, 2), rule(NECRON_DISC_5, 2));

        // ======================= MASTER 1 =======================
        registerDrops(CaseMaterial.WOOD, Floor.MI, rule(NECROMANCER_BROOCH, 5), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_jerry_1, 2));
        registerDrops(CaseMaterial.GOLD, Floor.MI, rule(HOT_POTATO_BOOK, 3), rule(NECROMANCER_BROOCH, 5), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_combo_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(ultimate_jerry_1, 2));
        registerDrops(CaseMaterial.DIAMOND, Floor.MI, rule(BONZO_MASK, 2), rule(HOT_POTATO_BOOK, 3), rule(NECROMANCER_BROOCH, 5), rule(RED_NOSE, 3), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_combo_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(ultimate_jerry_1, 2));
        registerDrops(CaseMaterial.EMERALD, Floor.MI, rule(BONZO_MASK, 2), rule(BONZO_STAFF, 2), rule(FUMING_POTATO_BOOK, 3), rule(HOT_POTATO_BOOK, 3), rule(NECROMANCER_BROOCH, 5), rule(RED_NOSE, 3), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_combo_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(ultimate_jerry_1, 2));
        registerDrops(CaseMaterial.OBSIDIAN, Floor.MI, rule(BONZO_MASK, 2), rule(BONZO_STAFF, 2), rule(FUMING_POTATO_BOOK, 3), rule(HOT_POTATO_BOOK, 3), rule(MASTER_SKULL_TIER_1, 4), rule(NECROMANCER_BROOCH, 5), rule(RECOMBOBULATOR_3000, 3), rule(RED_NOSE, 3), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_combo_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(ultimate_jerry_1, 2), rule(DUNGEON_DISC_1, 2), rule(CLOWN_DISC_2, 2), rule(WATCHER_DISC_3, 2), rule(NECRON_DISC_5, 2), rule(OLD_DISC_4, 2));

        // ======================= FLOOR 2 =======================
        // Wood
        registerDrops(CaseMaterial.WOOD, Floor.II, rule(NECROMANCER_BROOCH, 5), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_jerry_1, 2));
        // Gold
        registerDrops(CaseMaterial.GOLD, Floor.II, rule(HOT_POTATO_BOOK, 3), rule(NECROMANCER_BROOCH, 5), rule(SCARF_STUDIES, 4), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_combo_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(ultimate_jerry_1, 2), rule(ultimate_wise_1, 6));
        // Diamond
        registerDrops(CaseMaterial.DIAMOND, Floor.II, rule(HOT_POTATO_BOOK, 3), rule(NECROMANCER_BROOCH, 5), rule(RED_SCARF, 2), rule(SCARF_STUDIES, 4), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_combo_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(ultimate_jerry_1, 2), rule(ultimate_wise_1, 6));
        // Emerald
        registerDrops(CaseMaterial.EMERALD, Floor.II, rule(ADAPTIVE_BLADE, 3), rule(FUMING_POTATO_BOOK, 3), rule(HOT_POTATO_BOOK, 3), rule(NECROMANCER_BROOCH, 5), rule(RED_SCARF, 2), rule(SCARF_STUDIES, 4), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_combo_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(ultimate_jerry_1, 2), rule(ultimate_wise_1, 6));
        // Obsidian
        registerDrops(CaseMaterial.OBSIDIAN, Floor.II, rule(ADAPTIVE_BLADE, 3), rule(FUMING_POTATO_BOOK, 3), rule(HOT_POTATO_BOOK, 3), rule(NECROMANCER_BROOCH, 5), rule(RECOMBOBULATOR_3000, 3), rule(RED_SCARF, 2), rule(SCARF_STUDIES, 4), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_combo_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(ultimate_jerry_1, 2), rule(ultimate_wise_1, 6), rule(DUNGEON_DISC_1, 2), rule(CLOWN_DISC_2, 2), rule(WATCHER_DISC_3, 2), rule(NECRON_DISC_5, 2));

        // ======================= MASTER 2 =======================
        registerDrops(CaseMaterial.WOOD, Floor.MII, rule(NECROMANCER_BROOCH, 5), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_jerry_1, 2));
        registerDrops(CaseMaterial.GOLD, Floor.MII, rule(HOT_POTATO_BOOK, 3), rule(NECROMANCER_BROOCH, 5), rule(SCARF_STUDIES, 4), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_combo_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(ultimate_jerry_1, 2), rule(ultimate_wise_1, 6));
        registerDrops(CaseMaterial.DIAMOND, Floor.MII, rule(HOT_POTATO_BOOK, 3), rule(NECROMANCER_BROOCH, 5), rule(RED_SCARF, 2), rule(SCARF_STUDIES, 4), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_combo_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(ultimate_jerry_1, 2), rule(ultimate_wise_1, 6));
        registerDrops(CaseMaterial.EMERALD, Floor.MII, rule(ADAPTIVE_BLADE, 3), rule(FUMING_POTATO_BOOK, 3), rule(HOT_POTATO_BOOK, 3), rule(NECROMANCER_BROOCH, 5), rule(RED_SCARF, 2), rule(SCARF_STUDIES, 4), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_combo_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(ultimate_jerry_1, 2), rule(ultimate_wise_1, 6));
        registerDrops(CaseMaterial.OBSIDIAN, Floor.MII, rule(ADAPTIVE_BLADE, 3), rule(FUMING_POTATO_BOOK, 3), rule(HOT_POTATO_BOOK, 3), rule(MASTER_SKULL_TIER_2, 4), rule(NECROMANCER_BROOCH, 5), rule(RECOMBOBULATOR_3000, 3), rule(RED_SCARF, 2), rule(SCARF_STUDIES, 4), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_combo_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(ultimate_jerry_1, 2), rule(ultimate_wise_1, 6), rule(DUNGEON_DISC_1, 2), rule(CLOWN_DISC_2, 2), rule(WATCHER_DISC_3, 2), rule(NECRON_DISC_5, 2), rule(OLD_DISC_4, 2));

        // ======================= FLOOR 3 =======================
        // Wood
        registerDrops(CaseMaterial.WOOD, Floor.III, rule(NECROMANCER_BROOCH, 5), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_jerry_1, 2));
        // Gold
        registerDrops(CaseMaterial.GOLD, Floor.III, rule(ADAPTIVE_BOOTS, 4), rule(HOT_POTATO_BOOK, 3), rule(NECROMANCER_BROOCH, 5), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_combo_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(ultimate_jerry_1, 2), rule(ultimate_wise_1, 6), rule(wisdom_1, 6));
        // Diamond
        registerDrops(CaseMaterial.DIAMOND, Floor.III, rule(ADAPTIVE_BOOTS, 4), rule(ADAPTIVE_HELMET, 4), rule(HOT_POTATO_BOOK, 3), rule(NECROMANCER_BROOCH, 5), rule(SUSPICIOUS_VIAL, 4), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_combo_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(ultimate_jerry_1, 2), rule(ultimate_wise_1, 6), rule(wisdom_1, 6));
        // Emerald
        registerDrops(CaseMaterial.EMERALD, Floor.III, rule(ADAPTIVE_BOOTS, 4), rule(ADAPTIVE_CHESTPLATE, 4), rule(ADAPTIVE_HELMET, 4), rule(ADAPTIVE_LEGGINGS, 4), rule(FUMING_POTATO_BOOK, 3), rule(HOT_POTATO_BOOK, 3), rule(NECROMANCER_BROOCH, 5), rule(SUSPICIOUS_VIAL, 4), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_1, 7), rule(rejuvenate_2, 6), rule(bank_1, 6), rule(ultimate_combo_1, 6), rule(ultimate_last_stand_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(ultimate_jerry_2, 3), rule(ultimate_wise_1, 6), rule(wisdom_1, 6));
        // Obsidian
        registerDrops(CaseMaterial.OBSIDIAN, Floor.III, rule(ADAPTIVE_BOOTS, 4), rule(ADAPTIVE_CHESTPLATE, 4), rule(ADAPTIVE_HELMET, 4), rule(ADAPTIVE_LEGGINGS, 4), rule(FUMING_POTATO_BOOK, 3), rule(HOT_POTATO_BOOK, 3), rule(NECROMANCER_BROOCH, 5), rule(RECOMBOBULATOR_3000, 3), rule(SUSPICIOUS_VIAL, 4), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_2, 6), rule(bank_2, 5), rule(ultimate_combo_1, 6), rule(ultimate_last_stand_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(ultimate_jerry_2, 3), rule(ultimate_wise_1, 6), rule(wisdom_1, 6), rule(DUNGEON_DISC_1, 2), rule(CLOWN_DISC_2, 2), rule(WATCHER_DISC_3, 2), rule(NECRON_DISC_5, 2));

        // ======================= MASTER 3 =======================
        registerDrops(CaseMaterial.WOOD, Floor.MIII, rule(NECROMANCER_BROOCH, 5), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_jerry_1, 2));
        registerDrops(CaseMaterial.GOLD, Floor.MIII, rule(ADAPTIVE_BOOTS, 4), rule(HOT_POTATO_BOOK, 3), rule(NECROMANCER_BROOCH, 5), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_combo_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(ultimate_jerry_1, 2), rule(ultimate_wise_1, 6), rule(wisdom_1, 6));
        registerDrops(CaseMaterial.DIAMOND, Floor.MIII, rule(ADAPTIVE_BOOTS, 4), rule(ADAPTIVE_HELMET, 4), rule(HOT_POTATO_BOOK, 3), rule(NECROMANCER_BROOCH, 5), rule(SUSPICIOUS_VIAL, 4), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_combo_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(ultimate_jerry_1, 2), rule(ultimate_wise_1, 6), rule(wisdom_1, 6));
        registerDrops(CaseMaterial.EMERALD, Floor.MIII, rule(ADAPTIVE_BOOTS, 4), rule(ADAPTIVE_CHESTPLATE, 4), rule(ADAPTIVE_HELMET, 4), rule(ADAPTIVE_LEGGINGS, 4), rule(FUMING_POTATO_BOOK, 3), rule(HOT_POTATO_BOOK, 3), rule(NECROMANCER_BROOCH, 5), rule(SUSPICIOUS_VIAL, 4), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_1, 7), rule(rejuvenate_2, 6), rule(bank_1, 6), rule(ultimate_combo_1, 6), rule(ultimate_last_stand_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(ultimate_jerry_2, 3), rule(ultimate_wise_1, 6), rule(wisdom_1, 6));
        registerDrops(CaseMaterial.OBSIDIAN, Floor.MIII, rule(FIRST_MASTER_STAR, 3), rule(ADAPTIVE_BOOTS, 4), rule(ADAPTIVE_CHESTPLATE, 4), rule(ADAPTIVE_HELMET, 4), rule(ADAPTIVE_LEGGINGS, 4), rule(FUMING_POTATO_BOOK, 3), rule(HOT_POTATO_BOOK, 3), rule(MASTER_SKULL_TIER_3, 4), rule(NECROMANCER_BROOCH, 5), rule(RECOMBOBULATOR_3000, 3), rule(SUSPICIOUS_VIAL, 4), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_2, 6), rule(bank_2, 5), rule(ultimate_combo_1, 6), rule(ultimate_last_stand_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(ultimate_jerry_2, 3), rule(ultimate_wise_1, 6), rule(wisdom_1, 6), rule(DUNGEON_DISC_1, 2), rule(CLOWN_DISC_2, 2), rule(WATCHER_DISC_3, 2), rule(NECRON_DISC_5, 2), rule(OLD_DISC_4, 2));

        // ======================= FLOOR 4 =======================
        // Wood (no brooch)
        registerDrops(CaseMaterial.WOOD, Floor.IV, rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_jerry_1, 2));
        // Gold
        registerDrops(CaseMaterial.GOLD, Floor.IV, rule(HOT_POTATO_BOOK, 3), rule(NECROMANCER_BROOCH, 5), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_combo_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(rend_1, 4), rule(ultimate_jerry_1, 2), rule(ultimate_wise_1, 6), rule(wisdom_1, 6));
        // Diamond (Epic Spirit Pet)
        registerDrops(CaseMaterial.DIAMOND, Floor.IV, rule(SPIRIT, 2), rule(HOT_POTATO_BOOK, 3), rule(NECROMANCER_BROOCH, 5), rule(SPIRIT_STONE, 4), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_combo_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(rend_1, 4), rule(ultimate_jerry_1, 2), rule(ultimate_wise_1, 6), rule(wisdom_1, 6));
        // Emerald (Legendary Spirit Pet, no brooch)
        registerDrops(CaseMaterial.EMERALD, Floor.IV, rule(FUMING_POTATO_BOOK, 3), rule(HOT_POTATO_BOOK, 3), rule(SPIRIT_LEGENDARY, 2), rule(SPIRIT_BONE, 4), rule(SPIRIT_BOOTS, 4), rule(SPIRIT_STONE, 4), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_2, 6), rule(bank_1, 6), rule(ultimate_combo_1, 6), rule(ultimate_last_stand_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(rend_1, 4), rule(ultimate_jerry_2, 3), rule(ultimate_wise_1, 6), rule(wisdom_1, 6));
        // Obsidian (Legendary Spirit Pet, no brooch)
        registerDrops(CaseMaterial.OBSIDIAN, Floor.IV, rule(FUMING_POTATO_BOOK, 3), rule(HOT_POTATO_BOOK, 3), rule(SPIRIT_LEGENDARY, 2), rule(RECOMBOBULATOR_3000, 3), rule(SPIRIT_BONE, 4), rule(SPIRIT_BOOTS, 4), rule(SPIRIT_BOW, 3), rule(SPIRIT_STONE, 4), rule(SPIRIT_SWORD, 3), rule(SPIRIT_WING, 4), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_2, 6), rule(bank_2, 5), rule(ultimate_combo_1, 6), rule(ultimate_last_stand_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(rend_1, 4), rule(ultimate_jerry_2, 3), rule(ultimate_wise_1, 6), rule(wisdom_1, 6), rule(DUNGEON_DISC_1, 2), rule(CLOWN_DISC_2, 2), rule(WATCHER_DISC_3, 2), rule(NECRON_DISC_5, 2));

        // ======================= MASTER 4 =======================
        registerDrops(CaseMaterial.WOOD, Floor.MIV, rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_jerry_1, 2));
        registerDrops(CaseMaterial.GOLD, Floor.MIV, rule(HOT_POTATO_BOOK, 3), rule(NECROMANCER_BROOCH, 5), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_combo_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(rend_1, 4), rule(ultimate_jerry_1, 2), rule(ultimate_wise_1, 6), rule(wisdom_1, 6));
        registerDrops(CaseMaterial.DIAMOND, Floor.MIV, rule(SPIRIT, 2), rule(HOT_POTATO_BOOK, 3), rule(NECROMANCER_BROOCH, 5), rule(SPIRIT_STONE, 4), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_combo_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(rend_1, 4), rule(ultimate_jerry_1, 2), rule(ultimate_wise_1, 6), rule(wisdom_1, 6));
        registerDrops(CaseMaterial.EMERALD, Floor.MIV, rule(FUMING_POTATO_BOOK, 3), rule(HOT_POTATO_BOOK, 3), rule(SPIRIT_LEGENDARY, 2), rule(SPIRIT_BONE, 4), rule(SPIRIT_BOOTS, 4), rule(SPIRIT_STONE, 4), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_2, 6), rule(bank_1, 6), rule(ultimate_combo_1, 6), rule(ultimate_last_stand_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(rend_1, 4), rule(ultimate_jerry_2, 3), rule(ultimate_wise_1, 6), rule(wisdom_1, 6));
        registerDrops(CaseMaterial.OBSIDIAN, Floor.MIV, rule(SECOND_MASTER_STAR, 3), rule(FUMING_POTATO_BOOK, 3), rule(HOT_POTATO_BOOK, 3), rule(SPIRIT_LEGENDARY, 2), rule(MASTER_SKULL_TIER_4, 4), rule(RECOMBOBULATOR_3000, 3), rule(SPIRIT_BONE, 4), rule(SPIRIT_BOOTS, 4), rule(SPIRIT_BOW, 3), rule(SPIRIT_STONE, 4), rule(SPIRIT_SWORD, 3), rule(SPIRIT_WING, 4), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_2, 6), rule(bank_2, 5), rule(ultimate_combo_1, 6), rule(ultimate_last_stand_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(rend_1, 4), rule(ultimate_jerry_2, 3), rule(ultimate_wise_1, 6), rule(wisdom_1, 6), rule(DUNGEON_DISC_1, 2), rule(CLOWN_DISC_2, 2), rule(WATCHER_DISC_3, 2), rule(NECRON_DISC_5, 2), rule(OLD_DISC_4, 2));

        // ======================= FLOOR 5 =======================
        // Wood (books only)
        registerDrops(CaseMaterial.WOOD, Floor.V, rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_jerry_1, 2));
        // Gold
        registerDrops(CaseMaterial.GOLD, Floor.V, rule(DARK_ORB, 3), rule(HOT_POTATO_BOOK, 3), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(lethality_6, 5), rule(overload_1, 5), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_combo_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(ultimate_jerry_1, 2), rule(ultimate_wise_1, 6), rule(wisdom_1, 6));
        // Diamond
        registerDrops(CaseMaterial.DIAMOND, Floor.V, rule(DARK_ORB, 3), rule(HOT_POTATO_BOOK, 3), rule(SHADOW_ASSASSIN_BOOTS, 4), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(lethality_6, 5), rule(overload_1, 5), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_combo_1, 6), rule(ultimate_legion_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(ultimate_jerry_1, 2), rule(ultimate_wise_1, 6), rule(wisdom_1, 6));
        // Emerald
        registerDrops(CaseMaterial.EMERALD, Floor.V, rule(FUMING_POTATO_BOOK, 3), rule(HOT_POTATO_BOOK, 3), rule(SHADOW_ASSASSIN_BOOTS, 4), rule(SHADOW_ASSASSIN_HELMET, 4), rule(WARPED_STONE, 3), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(lethality_6, 5), rule(overload_1, 5), rule(rejuvenate_2, 6), rule(bank_1, 6), rule(ultimate_combo_1, 6), rule(ultimate_last_stand_1, 6), rule(ultimate_legion_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(ultimate_jerry_2, 3), rule(ultimate_wise_1, 6), rule(wisdom_1, 6));
        // Obsidian (Lethality I quirk)
        registerDrops(CaseMaterial.OBSIDIAN, Floor.V, rule(FUMING_POTATO_BOOK, 3), rule(HOT_POTATO_BOOK, 3), rule(LIVID_DAGGER, 3), rule(RECOMBOBULATOR_3000, 3), rule(SHADOW_ASSASSIN_BOOTS, 4), rule(SHADOW_ASSASSIN_HELMET, 4), rule(SHADOW_ASSASSIN_LEGGINGS, 4), rule(WARPED_STONE, 3), rule(lethality_1, 5), rule(overload_1, 5), rule(rejuvenate_2, 6), rule(bank_2, 5), rule(ultimate_combo_1, 6), rule(ultimate_last_stand_1, 6), rule(ultimate_legion_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(ultimate_jerry_2, 3), rule(ultimate_wise_1, 6), rule(wisdom_1, 6), rule(DUNGEON_DISC_1, 2), rule(CLOWN_DISC_2, 2), rule(NECRON_DISC_5, 2), rule(WATCHER_DISC_3, 2));
        // Bedrock
        registerDrops(CaseMaterial.BEDROCK, Floor.V, rule(FUMING_POTATO_BOOK, 3), rule(LAST_BREATH, 2), rule(LIVID_DAGGER, 3), rule(RECOMBOBULATOR_3000, 3), rule(SHADOW_ASSASSIN_BOOTS, 4), rule(SHADOW_ASSASSIN_CHESTPLATE, 3), rule(SHADOW_ASSASSIN_HELMET, 4), rule(SHADOW_ASSASSIN_LEGGINGS, 4), rule(SHADOW_FURY, 2), rule(WARPED_STONE, 3), rule(lethality_6, 5), rule(overload_1, 5), rule(rejuvenate_3, 5), rule(bank_3, 4), rule(ultimate_combo_2, 4), rule(ultimate_last_stand_2, 4), rule(ultimate_legion_1, 6), rule(ultimate_no_pain_no_gain_2, 4), rule(ultimate_jerry_3, 3), rule(ultimate_wise_2, 4), rule(wisdom_2, 5), rule(DUNGEON_DISC_1, 2), rule(CLOWN_DISC_2, 2), rule(NECRON_DISC_5, 2), rule(WATCHER_DISC_3, 2));

        // ======================= MASTER 5 =======================
        registerDrops(CaseMaterial.WOOD, Floor.MV, rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_jerry_1, 2));
        registerDrops(CaseMaterial.GOLD, Floor.MV, rule(DARK_ORB, 3), rule(HOT_POTATO_BOOK, 3), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(lethality_6, 5), rule(overload_1, 5), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_combo_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(ultimate_jerry_1, 2), rule(ultimate_wise_1, 6), rule(wisdom_1, 6));
        registerDrops(CaseMaterial.DIAMOND, Floor.MV, rule(DARK_ORB, 3), rule(HOT_POTATO_BOOK, 3), rule(SHADOW_ASSASSIN_BOOTS, 4), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(lethality_6, 5), rule(overload_1, 5), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_combo_1, 6), rule(ultimate_legion_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(ultimate_jerry_1, 2), rule(ultimate_wise_1, 6), rule(wisdom_1, 6));
        registerDrops(CaseMaterial.EMERALD, Floor.MV, rule(FUMING_POTATO_BOOK, 3), rule(HOT_POTATO_BOOK, 3), rule(SHADOW_ASSASSIN_BOOTS, 4), rule(SHADOW_ASSASSIN_HELMET, 4), rule(WARPED_STONE, 3), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(lethality_6, 5), rule(overload_1, 5), rule(rejuvenate_2, 6), rule(bank_1, 6), rule(ultimate_combo_1, 6), rule(ultimate_last_stand_1, 6), rule(ultimate_legion_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(ultimate_jerry_2, 3), rule(ultimate_wise_1, 6), rule(wisdom_1, 6));
        registerDrops(CaseMaterial.OBSIDIAN, Floor.MV, rule(FUMING_POTATO_BOOK, 3), rule(HOT_POTATO_BOOK, 3), rule(LIVID_DAGGER, 3), rule(MASTER_SKULL_TIER_3, 4), rule(RECOMBOBULATOR_3000, 3), rule(SHADOW_ASSASSIN_BOOTS, 4), rule(SHADOW_ASSASSIN_HELMET, 4), rule(SHADOW_ASSASSIN_LEGGINGS, 4), rule(THIRD_MASTER_STAR, 3), rule(WARPED_STONE, 3), rule(lethality_1, 5), rule(overload_1, 5), rule(rejuvenate_2, 6), rule(bank_2, 5), rule(ultimate_combo_1, 6), rule(ultimate_last_stand_1, 6), rule(ultimate_legion_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(ultimate_jerry_2, 3), rule(ultimate_wise_1, 6), rule(wisdom_1, 6), rule(DUNGEON_DISC_1, 2), rule(CLOWN_DISC_2, 2), rule(NECRON_DISC_5, 2), rule(WATCHER_DISC_3, 2), rule(OLD_DISC_4, 2));
        registerDrops(CaseMaterial.BEDROCK, Floor.MV, rule(FUMING_POTATO_BOOK, 3), rule(LAST_BREATH, 2), rule(LIVID_DAGGER, 3), rule(MASTER_SKULL_TIER_4, 4), rule(RECOMBOBULATOR_3000, 3), rule(SHADOW_ASSASSIN_BOOTS, 4), rule(SHADOW_ASSASSIN_CHESTPLATE, 3), rule(SHADOW_ASSASSIN_HELMET, 4), rule(SHADOW_ASSASSIN_LEGGINGS, 4), rule(SHADOW_FURY, 2), rule(THIRD_MASTER_STAR, 3), rule(WARPED_STONE, 3), rule(lethality_6, 5), rule(overload_1, 5), rule(rejuvenate_3, 5), rule(bank_3, 4), rule(ultimate_combo_2, 4), rule(ultimate_last_stand_2, 4), rule(ultimate_legion_1, 6), rule(ultimate_no_pain_no_gain_2, 4), rule(ultimate_jerry_3, 3), rule(ultimate_wise_2, 4), rule(wisdom_2, 5), rule(DUNGEON_DISC_1, 2), rule(CLOWN_DISC_2, 2), rule(NECRON_DISC_5, 2), rule(WATCHER_DISC_3, 2), rule(OLD_DISC_4, 2));

        // ======================= FLOOR 6 =======================
        // Wood (books only)
        registerDrops(CaseMaterial.WOOD, Floor.VI, rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_jerry_1, 2));
        // Gold
        registerDrops(CaseMaterial.GOLD, Floor.VI, rule(GIANT_TOOTH, 3), rule(HOT_POTATO_BOOK, 3), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_combo_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(ultimate_swarm_1, 6), rule(ultimate_jerry_1, 2), rule(ultimate_wise_1, 6), rule(wisdom_1, 6));
        // Diamond
        registerDrops(CaseMaterial.DIAMOND, Floor.VI, rule(ANCIENT_ROSE, 3), rule(GIANT_TOOTH, 3), rule(HOT_POTATO_BOOK, 3), rule(NECROMANCER_LORD_BOOTS, 4), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_combo_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(ultimate_swarm_1, 6), rule(ultimate_jerry_1, 2), rule(ultimate_wise_1, 6), rule(wisdom_1, 6));
        // Emerald
        registerDrops(CaseMaterial.EMERALD, Floor.VI, rule(ANCIENT_ROSE, 3), rule(GIANT_TOOTH, 3), rule(HOT_POTATO_BOOK, 3), rule(NECROMANCER_LORD_BOOTS, 4), rule(NECROMANCER_LORD_HELMET, 4), rule(SADAN_BROOCH, 4), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_2, 6), rule(bank_1, 6), rule(ultimate_combo_1, 6), rule(ultimate_last_stand_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(ultimate_swarm_1, 6), rule(ultimate_jerry_2, 3), rule(ultimate_wise_1, 6), rule(wisdom_1, 6));
        // Obsidian
        registerDrops(CaseMaterial.OBSIDIAN, Floor.VI, rule(ANCIENT_ROSE, 3), rule(FUMING_POTATO_BOOK, 3), rule(GIANT_TOOTH, 3), rule(HOT_POTATO_BOOK, 3), rule(NECROMANCER_LORD_BOOTS, 4), rule(NECROMANCER_LORD_HELMET, 4), rule(NECROMANCER_LORD_LEGGINGS, 4), rule(NECROMANCER_SWORD, 3), rule(RECOMBOBULATOR_3000, 3), rule(SADAN_BROOCH, 4), rule(SUMMONING_RING, 3), rule(rejuvenate_2, 6), rule(bank_2, 5), rule(ultimate_combo_1, 6), rule(ultimate_last_stand_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(ultimate_swarm_1, 6), rule(ultimate_jerry_2, 3), rule(ultimate_wise_1, 6), rule(wisdom_1, 6), rule(DUNGEON_DISC_1, 2), rule(CLOWN_DISC_2, 2), rule(NECRON_DISC_5, 2), rule(WATCHER_DISC_3, 2));
        // Bedrock
        registerDrops(CaseMaterial.BEDROCK, Floor.VI, rule(ANCIENT_ROSE, 3), rule(FUMING_POTATO_BOOK, 3), rule(GIANT_TOOTH, 3), rule(GIANTS_SWORD, 2), rule(NECROMANCER_LORD_BOOTS, 4), rule(NECROMANCER_LORD_CHESTPLATE, 3), rule(NECROMANCER_LORD_HELMET, 4), rule(NECROMANCER_LORD_LEGGINGS, 4), rule(NECROMANCER_SWORD, 3), rule(PRECURSOR_EYE, 2), rule(RECOMBOBULATOR_3000, 3), rule(SADAN_BROOCH, 4), rule(SUMMONING_RING, 3), rule(rejuvenate_3, 5), rule(bank_3, 4), rule(ultimate_combo_2, 4), rule(ultimate_last_stand_2, 4), rule(ultimate_no_pain_no_gain_2, 4), rule(ultimate_swarm_1, 6), rule(ultimate_jerry_3, 3), rule(ultimate_wise_2, 4), rule(wisdom_2, 5), rule(DUNGEON_DISC_1, 2), rule(CLOWN_DISC_2, 2), rule(NECRON_DISC_5, 2), rule(WATCHER_DISC_3, 2));

        // ======================= MASTER 6 =======================
        registerDrops(CaseMaterial.WOOD, Floor.MVI, rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_jerry_1, 2));
        registerDrops(CaseMaterial.GOLD, Floor.MVI, rule(GIANT_TOOTH, 3), rule(HOT_POTATO_BOOK, 3), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_combo_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(ultimate_swarm_1, 6), rule(ultimate_jerry_1, 2), rule(ultimate_wise_1, 6), rule(wisdom_1, 6));
        registerDrops(CaseMaterial.DIAMOND, Floor.MVI, rule(ANCIENT_ROSE, 3), rule(GIANT_TOOTH, 3), rule(HOT_POTATO_BOOK, 3), rule(NECROMANCER_LORD_BOOTS, 4), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_combo_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(ultimate_swarm_1, 6), rule(ultimate_jerry_1, 2), rule(ultimate_wise_1, 6), rule(wisdom_1, 6));
        registerDrops(CaseMaterial.EMERALD, Floor.MVI, rule(ANCIENT_ROSE, 3), rule(GIANT_TOOTH, 3), rule(HOT_POTATO_BOOK, 3), rule(NECROMANCER_LORD_BOOTS, 4), rule(NECROMANCER_LORD_HELMET, 4), rule(SADAN_BROOCH, 4), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_2, 6), rule(bank_1, 6), rule(ultimate_combo_1, 6), rule(ultimate_last_stand_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(ultimate_swarm_1, 6), rule(ultimate_jerry_2, 3), rule(ultimate_wise_1, 6), rule(wisdom_1, 6));
        registerDrops(CaseMaterial.OBSIDIAN, Floor.MVI, rule(ANCIENT_ROSE, 3), rule(FOURTH_MASTER_STAR, 3), rule(FUMING_POTATO_BOOK, 3), rule(GIANT_TOOTH, 3), rule(HOT_POTATO_BOOK, 3), rule(MASTER_SKULL_TIER_4, 4), rule(NECROMANCER_LORD_BOOTS, 4), rule(NECROMANCER_LORD_HELMET, 4), rule(NECROMANCER_LORD_LEGGINGS, 4), rule(NECROMANCER_SWORD, 3), rule(RECOMBOBULATOR_3000, 3), rule(SADAN_BROOCH, 4), rule(SUMMONING_RING, 3), rule(rejuvenate_2, 6), rule(bank_2, 5), rule(ultimate_combo_1, 6), rule(ultimate_last_stand_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(ultimate_swarm_1, 6), rule(ultimate_jerry_2, 3), rule(ultimate_wise_1, 6), rule(wisdom_1, 6), rule(DUNGEON_DISC_1, 2), rule(CLOWN_DISC_2, 2), rule(NECRON_DISC_5, 2), rule(WATCHER_DISC_3, 2), rule(OLD_DISC_4, 2));
        registerDrops(CaseMaterial.BEDROCK, Floor.MVI, rule(ANCIENT_ROSE, 3), rule(FOURTH_MASTER_STAR, 3), rule(FUMING_POTATO_BOOK, 3), rule(GIANT_TOOTH, 3), rule(GIANTS_SWORD, 2), rule(MASTER_SKULL_TIER_4, 4), rule(NECROMANCER_LORD_BOOTS, 4), rule(NECROMANCER_LORD_CHESTPLATE, 3), rule(NECROMANCER_LORD_HELMET, 4), rule(NECROMANCER_LORD_LEGGINGS, 4), rule(NECROMANCER_SWORD, 3), rule(PRECURSOR_EYE, 2), rule(RECOMBOBULATOR_3000, 3), rule(SADAN_BROOCH, 4), rule(SUMMONING_RING, 3), rule(rejuvenate_3, 5), rule(bank_3, 4), rule(ultimate_combo_2, 4), rule(ultimate_last_stand_2, 4), rule(ultimate_no_pain_no_gain_2, 4), rule(ultimate_swarm_1, 6), rule(ultimate_jerry_3, 3), rule(ultimate_wise_2, 4), rule(wisdom_2, 5), rule(DUNGEON_DISC_1, 2), rule(CLOWN_DISC_2, 2), rule(NECRON_DISC_5, 2), rule(WATCHER_DISC_3, 2), rule(OLD_DISC_4, 2));

        // ======================= FLOOR 7 =======================
        // Wood (books only)
        registerDrops(CaseMaterial.WOOD, Floor.VII, rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_jerry_1, 2));
        // Gold
        registerDrops(CaseMaterial.GOLD, Floor.VII, rule(HOT_POTATO_BOOK, 3), rule(PRECURSOR_GEAR, 4), rule(WITHER_BOOTS, 4), rule(WITHER_CATALYST, 4), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_combo_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(ultimate_jerry_1, 2), rule(ultimate_wise_1, 6), rule(wisdom_1, 6));
        // Diamond
        registerDrops(CaseMaterial.DIAMOND, Floor.VII, rule(HOT_POTATO_BOOK, 3), rule(PRECURSOR_GEAR, 4), rule(WITHER_BOOTS, 4), rule(WITHER_CATALYST, 4), rule(WITHER_HELMET, 4), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_combo_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(ultimate_soul_eater_1, 4), rule(ultimate_jerry_1, 2), rule(ultimate_wise_1, 6), rule(wisdom_1, 6));
        // Emerald
        registerDrops(CaseMaterial.EMERALD, Floor.VII, rule(HOT_POTATO_BOOK, 3), rule(PRECURSOR_GEAR, 4), rule(WITHER_BLOOD, 4), rule(WITHER_BOOTS, 4), rule(WITHER_CATALYST, 4), rule(WITHER_CLOAK, 4), rule(WITHER_HELMET, 4), rule(WITHER_LEGGINGS, 4), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_2, 6), rule(bank_1, 6), rule(ultimate_combo_1, 6), rule(ultimate_last_stand_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(ultimate_soul_eater_1, 4), rule(ultimate_jerry_2, 3), rule(ultimate_wise_1, 6), rule(wisdom_1, 6));
        // Obsidian
        registerDrops(CaseMaterial.OBSIDIAN, Floor.VII, rule(FUMING_POTATO_BOOK, 3), rule(HOT_POTATO_BOOK, 3), rule(PRECURSOR_GEAR, 4), rule(RECOMBOBULATOR_3000, 3), rule(WITHER_BLOOD, 4), rule(WITHER_BOOTS, 4), rule(WITHER_CATALYST, 4), rule(WITHER_CHESTPLATE, 3), rule(WITHER_CLOAK, 4), rule(WITHER_HELMET, 4), rule(WITHER_LEGGINGS, 4), rule(rejuvenate_2, 6), rule(bank_2, 5), rule(ultimate_combo_1, 6), rule(ultimate_last_stand_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(ultimate_one_for_all_1, 3), rule(ultimate_soul_eater_1, 4), rule(ultimate_jerry_2, 3), rule(ultimate_wise_1, 6), rule(wisdom_1, 6), rule(DUNGEON_DISC_1, 2), rule(CLOWN_DISC_2, 2), rule(NECRON_DISC_5, 2), rule(WATCHER_DISC_3, 2));
        // Bedrock
        registerDrops(CaseMaterial.BEDROCK, Floor.VII, rule(AUTO_RECOMBOBULATOR, 2), rule(FUMING_POTATO_BOOK, 3), rule(GOLDOR_THE_FISH, 1), rule(HOT_POTATO_BOOK, 3), rule(IMPLOSION_SCROLL, 2), rule(MAXOR_THE_FISH, 1), rule(NECRON_HANDLE, 1), rule(PRECURSOR_GEAR, 4), rule(RECOMBOBULATOR_3000, 3), rule(SHADOW_WARP_SCROLL, 2), rule(STORM_THE_FISH, 1), rule(WITHER_BLOOD, 4), rule(WITHER_BOOTS, 4), rule(WITHER_CATALYST, 4), rule(WITHER_CHESTPLATE, 3), rule(WITHER_CLOAK, 4), rule(WITHER_HELMET, 4), rule(WITHER_LEGGINGS, 4), rule(WITHER_SHIELD_SCROLL, 2), rule(feather_falling_7, 5), rule(infinite_quiver_7, 5), rule(rejuvenate_3, 5), rule(bank_3, 4), rule(ultimate_combo_2, 4), rule(ultimate_last_stand_2, 4), rule(ultimate_no_pain_no_gain_2, 4), rule(ultimate_one_for_all_1, 3), rule(ultimate_soul_eater_1, 4), rule(ultimate_jerry_3, 3), rule(ultimate_wise_2, 4), rule(wisdom_2, 5), rule(DUNGEON_DISC_1, 2), rule(CLOWN_DISC_2, 2), rule(NECRON_DISC_5, 2), rule(WATCHER_DISC_3, 2));

        // ======================= MASTER 7 =======================
        registerDrops(CaseMaterial.WOOD, Floor.MVII, rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_jerry_1, 2));
        registerDrops(CaseMaterial.GOLD, Floor.MVII, rule(HOT_POTATO_BOOK, 3), rule(PRECURSOR_GEAR, 4), rule(WITHER_BOOTS, 4), rule(WITHER_CATALYST, 4), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_combo_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(ultimate_jerry_1, 2), rule(ultimate_wise_1, 6), rule(wisdom_1, 6));
        registerDrops(CaseMaterial.DIAMOND, Floor.MVII, rule(HOT_POTATO_BOOK, 3), rule(PRECURSOR_GEAR, 4), rule(WITHER_BOOTS, 4), rule(WITHER_CATALYST, 4), rule(WITHER_HELMET, 4), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_1, 7), rule(bank_1, 6), rule(ultimate_combo_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(ultimate_soul_eater_1, 4), rule(ultimate_jerry_1, 2), rule(ultimate_wise_1, 6), rule(wisdom_1, 6));
        registerDrops(CaseMaterial.EMERALD, Floor.MVII, rule(HOT_POTATO_BOOK, 3), rule(PRECURSOR_GEAR, 4), rule(WITHER_BLOOD, 4), rule(WITHER_BOOTS, 4), rule(WITHER_CATALYST, 4), rule(WITHER_CLOAK, 4), rule(WITHER_HELMET, 4), rule(WITHER_LEGGINGS, 4), rule(feather_falling_6, 7), rule(infinite_quiver_6, 7), rule(rejuvenate_2, 6), rule(bank_1, 6), rule(ultimate_combo_1, 6), rule(ultimate_last_stand_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(ultimate_soul_eater_1, 4), rule(ultimate_jerry_2, 3), rule(ultimate_wise_1, 6), rule(wisdom_1, 6));
        registerDrops(CaseMaterial.OBSIDIAN, Floor.MVII, rule(FIFTH_MASTER_STAR, 3), rule(FUMING_POTATO_BOOK, 3), rule(HOT_POTATO_BOOK, 3), rule(MASTER_SKULL_TIER_4, 4), rule(PRECURSOR_GEAR, 4), rule(RECOMBOBULATOR_3000, 3), rule(WITHER_BLOOD, 4), rule(WITHER_BOOTS, 4), rule(WITHER_CATALYST, 4), rule(WITHER_CHESTPLATE, 3), rule(WITHER_CLOAK, 4), rule(WITHER_HELMET, 4), rule(WITHER_LEGGINGS, 4), rule(rejuvenate_2, 6), rule(bank_2, 5), rule(ultimate_combo_1, 6), rule(ultimate_last_stand_1, 6), rule(ultimate_no_pain_no_gain_1, 6), rule(ultimate_one_for_all_1, 3), rule(ultimate_soul_eater_1, 4), rule(ultimate_jerry_2, 3), rule(ultimate_wise_1, 6), rule(wisdom_1, 6), rule(DUNGEON_DISC_1, 2), rule(CLOWN_DISC_2, 2), rule(NECRON_DISC_5, 2), rule(WATCHER_DISC_3, 2), rule(OLD_DISC_4, 2));
        registerDrops(CaseMaterial.BEDROCK, Floor.MVII, rule(AUTO_RECOMBOBULATOR, 2), rule(DARK_CLAYMORE, 1), rule(FIFTH_MASTER_STAR, 3), rule(FUMING_POTATO_BOOK, 3), rule(GOLDOR_THE_FISH, 1), rule(HOT_POTATO_BOOK, 3), rule(IMPLOSION_SCROLL, 2), rule(MASTER_SKULL_TIER_5, 4), rule(MAXOR_THE_FISH, 1), rule(NECRON_HANDLE, 1), rule(NECRON_DYE, 1), rule(PRECURSOR_GEAR, 4), rule(RECOMBOBULATOR_3000, 3), rule(SHADOW_WARP_SCROLL, 2), rule(STORM_THE_FISH, 1), rule(WITHER_BLOOD, 4), rule(WITHER_BOOTS, 4), rule(WITHER_CATALYST, 4), rule(WITHER_CHESTPLATE, 3), rule(WITHER_CLOAK, 4), rule(WITHER_HELMET, 4), rule(WITHER_LEGGINGS, 4), rule(WITHER_SHIELD_SCROLL, 2), rule(feather_falling_7, 5), rule(infinite_quiver_7, 5), rule(rejuvenate_3, 5), rule(thunderlord_7, 3), rule(bank_3, 4), rule(ultimate_combo_2, 4), rule(ultimate_last_stand_2, 4), rule(ultimate_no_pain_no_gain_2, 4), rule(ultimate_one_for_all_1, 3), rule(ultimate_soul_eater_1, 4), rule(ultimate_jerry_3, 3), rule(ultimate_wise_2, 4), rule(wisdom_2, 5), rule(DUNGEON_DISC_1, 2), rule(CLOWN_DISC_2, 2), rule(NECRON_DISC_5, 2), rule(WATCHER_DISC_3, 2), rule(OLD_DISC_4, 2));
    }

    private static Rule rule(ItemEnum item, int rarity) {
        return new Rule(item, null, null, rarity);
    }

    private static void registerDrops(CaseMaterial material, Floor floor, Rule... rules) {
        for (Rule r : rules) RULES.add(new Rule(r.item, material, floor, r.rarity));
    }

    public static List<Rule> getDrops(CaseMaterial material, Floor floor) {
        return RULES.stream().filter(r -> r.material == material && r.floor == floor).collect(Collectors.toList());
    }

    public enum CaseMaterial {WOOD, GOLD, DIAMOND, EMERALD, OBSIDIAN, BEDROCK}

    public enum Floor {
        I(1), II(2), III(3), IV(4), V(5), VI(6), VII(7), MI(8), MII(9), MIII(10), MIV(11), MV(12), MVI(13), MVII(14);

        public final int number;

        Floor(int n) {
            this.number = n;
        }

        public static Floor fromDungeonFloor(DungeonFloor df) {
            if (df == null || df == DungeonFloor.NONE) return null;
            String name = df.name();
            boolean master = name.startsWith("M");
            int num;
            try {
                num = Integer.parseInt(name.substring(1));
            } catch (NumberFormatException e) {
                return null;
            }
            if (num < 1 || num > 7) return null;
            String[] roman = {"I", "II", "III", "IV", "V", "VI", "VII"};
            String key = (master ? "M" : "") + roman[num - 1];
            try {
                return Floor.valueOf(key);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    public static class Rule {
        public final ItemEnum item;
        public final CaseMaterial material;
        public final Floor floor;
        public final int rarity;

        public Rule(ItemEnum item, CaseMaterial material, Floor floor, int rarity) {
            this.item = item;
            this.material = material;
            this.floor = floor;
            this.rarity = rarity;
        }
    }
}