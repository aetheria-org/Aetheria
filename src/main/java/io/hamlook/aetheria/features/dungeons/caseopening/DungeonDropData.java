package io.hamlook.aetheria.features.dungeons.caseopening;

import io.hamlook.aetheria.features.dungeons.utils.DungeonFloor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static io.hamlook.aetheria.features.dungeons.caseopening.ItemEnum.*;

public class DungeonDropData {

    private static final List<Rule> RULES = new ArrayList<>();

    static {
        registerDrops(CaseMaterial.BEDROCK, Floor.VII, rule(NECRON_HANDLE, 1), rule(IMPLOSION_SCROLL, 2), rule(SHADOW_WARP_SCROLL, 2), rule(WITHER_SHIELD_SCROLL, 2), rule(AUTO_RECOMBOBULATOR, 3), rule(ultimate_one_for_all_1, 3), rule(WITHER_CHESTPLATE, 3), rule(RECOMBOBULATOR_3000, 4), rule(WITHER_LEGGINGS, 4), rule(ultimate_soul_eater_1, 5), rule(WITHER_CLOAK, 5), rule(WITHER_HELMET, 5), rule(WITHER_BLOOD, 5), rule(FUMING_POTATO_BOOK, 5), rule(WITHER_BOOTS, 5), rule(WITHER_CATALYST, 5), rule(HOT_POTATO_BOOK, 5), rule(PRECURSOR_GEAR, 5), rule(STORM_THE_FISH, 6), rule(MAXOR_THE_FISH, 6), rule(GOLDOR_THE_FISH, 6), rule(ultimate_wisdom_2, 7), rule(ultimate_last_stand_2, 7), rule(ultimate_wise_2, 7), rule(ultimate_combo_2, 7), rule(ultimate_jerry_3, 7), rule(ultimate_bank_3, 7), rule(ultimate_no_pain_no_gain_2, 7), rule(infinite_quiver_7, 7), rule(feather_falling_7, 7), rule(rejuvenate_3, 7));

        registerDrops(CaseMaterial.OBSIDIAN, Floor.VII, rule(WITHER_CHESTPLATE, 3), rule(ultimate_one_for_all_1, 3), rule(RECOMBOBULATOR_3000, 4), rule(WITHER_LEGGINGS, 4), rule(WITHER_CLOAK, 5), rule(WITHER_HELMET, 5), rule(WITHER_BLOOD, 5), rule(ultimate_soul_eater_1, 5), rule(FUMING_POTATO_BOOK, 5), rule(WITHER_BOOTS, 5), rule(WITHER_CATALYST, 5), rule(HOT_POTATO_BOOK, 5), rule(PRECURSOR_GEAR, 7), rule(ultimate_no_pain_no_gain_1, 7), rule(ultimate_combo_1, 7), rule(ultimate_bank_2, 7), rule(rejuvenate_2, 7), rule(ultimate_wisdom_1, 7));

        registerDrops(CaseMaterial.BEDROCK, Floor.MVII, rule(NECRON_HANDLE, 1), rule(SHADOW_WARP_SCROLL, 2), rule(WITHER_SHIELD_SCROLL, 2), rule(IMPLOSION_SCROLL, 2), rule(DARK_CLAYMORE, 1), rule(AUTO_RECOMBOBULATOR, 3), rule(FIFTH_MASTER_STAR, 3), rule(WITHER_CHESTPLATE, 3), rule(ultimate_one_for_all_1, 3), rule(MASTER_SKULL_TIER_5, 3), rule(RECOMBOBULATOR_3000, 4), rule(WITHER_LEGGINGS, 4), rule(WITHER_CLOAK, 5), rule(WITHER_HELMET, 5), rule(WITHER_BLOOD, 5), rule(thunderlord_7, 4), rule(ultimate_soul_eater_1, 5), rule(FUMING_POTATO_BOOK, 5), rule(WITHER_BOOTS, 5), rule(WITHER_CATALYST, 5), rule(HOT_POTATO_BOOK, 5), rule(PRECURSOR_GEAR, 5), rule(ultimate_no_pain_no_gain_2, 7), rule(ultimate_combo_2, 7), rule(rejuvenate_3, 7), rule(ultimate_bank_3, 7), rule(ultimate_wisdom_2, 7), rule(ultimate_wise_2, 7), rule(ultimate_jerry_3, 7), rule(ultimate_last_stand_2, 7), rule(infinite_quiver_7, 7), rule(feather_falling_7, 7), rule(STORM_THE_FISH, 6), rule(MAXOR_THE_FISH, 6), rule(GOLDOR_THE_FISH, 6));

        registerDrops(CaseMaterial.OBSIDIAN, Floor.MVII, rule(FIFTH_MASTER_STAR, 3), rule(WITHER_CHESTPLATE, 3), rule(ultimate_one_for_all_1, 3), rule(MASTER_SKULL_TIER_4, 4), rule(RECOMBOBULATOR_3000, 4), rule(WITHER_LEGGINGS, 4), rule(WITHER_CLOAK, 5), rule(WITHER_HELMET, 5), rule(WITHER_BLOOD, 5), rule(ultimate_soul_eater_1, 4), rule(FUMING_POTATO_BOOK, 5), rule(WITHER_BOOTS, 5), rule(WITHER_CATALYST, 5), rule(HOT_POTATO_BOOK, 5), rule(PRECURSOR_GEAR, 5), rule(ultimate_no_pain_no_gain_1, 7), rule(ultimate_combo_1, 7), rule(ultimate_bank_2, 7), rule(rejuvenate_2, 7), rule(ultimate_wisdom_1, 7));

        registerDrops(CaseMaterial.BEDROCK, Floor.VI, rule(PRECURSOR_EYE, 2), rule(GIANTS_SWORD, 2), rule(NECROMANCER_LORD_CHESTPLATE, 3), rule(SUMMONING_RING, 3), rule(FEL_SKULL, 3), rule(NECROMANCER_SWORD, 4), rule(NECROMANCER_LORD_LEGGINGS, 4), rule(SOULWEAVER_GLOVES, 4), rule(RECOMBOBULATOR_3000, 4), rule(NECROMANCER_LORD_HELMET, 5), rule(SADAN_BROOCH, 5), rule(NECROMANCER_LORD_BOOTS, 5), rule(FUMING_POTATO_BOOK, 5), rule(ultimate_swarm_1, 5), rule(GIANT_TOOTH, 5), rule(ultimate_combo_2, 7), rule(ultimate_no_pain_no_gain_2, 7), rule(ultimate_wise_2, 7), rule(ultimate_jerry_3, 7), rule(rejuvenate_3, 7), rule(ultimate_last_stand_2, 7), rule(ultimate_bank_3, 7));

        registerDrops(CaseMaterial.OBSIDIAN, Floor.VI, rule(SUMMONING_RING, 3), rule(NECROMANCER_SWORD, 4), rule(NECROMANCER_LORD_LEGGINGS, 4), rule(SOULWEAVER_GLOVES, 4), rule(RECOMBOBULATOR_3000, 4), rule(NECROMANCER_LORD_HELMET, 5), rule(SADAN_BROOCH, 5), rule(NECROMANCER_LORD_BOOTS, 5), rule(FUMING_POTATO_BOOK, 5), rule(ultimate_swarm_1, 5), rule(GIANT_TOOTH, 5), rule(HOT_POTATO_BOOK, 5), rule(ultimate_combo_1, 7), rule(ultimate_no_pain_no_gain_1, 7), rule(ultimate_last_stand_1, 7), rule(ultimate_wise_1, 7), rule(ultimate_jerry_2, 7), rule(ultimate_bank_2, 7));

        registerDrops(CaseMaterial.BEDROCK, Floor.MVI, rule(PRECURSOR_EYE, 2), rule(GIANTS_SWORD, 2), rule(NECROMANCER_LORD_CHESTPLATE, 3), rule(FOURTH_MASTER_STAR, 3), rule(SUMMONING_RING, 3), rule(FEL_SKULL, 3), rule(NECROMANCER_SWORD, 4), rule(MASTER_SKULL_TIER_4, 4), rule(NECROMANCER_LORD_LEGGINGS, 4), rule(SOULWEAVER_GLOVES, 4), rule(RECOMBOBULATOR_3000, 4), rule(NECROMANCER_LORD_HELMET, 5), rule(SADAN_BROOCH, 5), rule(NECROMANCER_LORD_BOOTS, 5), rule(FUMING_POTATO_BOOK, 5), rule(ultimate_swarm_1, 5), rule(GIANT_TOOTH, 5), rule(ultimate_combo_2, 7), rule(ultimate_no_pain_no_gain_2, 7), rule(ultimate_wise_2, 7), rule(ultimate_jerry_3, 7), rule(rejuvenate_3, 7), rule(ultimate_last_stand_2, 7), rule(ultimate_bank_3, 7));

        registerDrops(CaseMaterial.OBSIDIAN, Floor.MVI, rule(FOURTH_MASTER_STAR, 3), rule(SUMMONING_RING, 3), rule(NECROMANCER_SWORD, 4), rule(NECROMANCER_LORD_LEGGINGS, 4), rule(SOULWEAVER_GLOVES, 4), rule(MASTER_SKULL_TIER_4, 4), rule(RECOMBOBULATOR_3000, 4), rule(NECROMANCER_LORD_HELMET, 5), rule(SADAN_BROOCH, 5), rule(NECROMANCER_LORD_BOOTS, 5), rule(FUMING_POTATO_BOOK, 5), rule(ultimate_swarm_1, 5), rule(GIANT_TOOTH, 5), rule(HOT_POTATO_BOOK, 5), rule(ultimate_combo_2, 7), rule(ultimate_no_pain_no_gain_2, 7), rule(ultimate_wise_2, 7), rule(ultimate_jerry_3, 7), rule(rejuvenate_3, 7), rule(ultimate_last_stand_2, 7), rule(ultimate_bank_3, 7));

        registerDrops(CaseMaterial.BEDROCK, Floor.V, rule(SHADOW_FURY, 2), rule(LAST_BREATH, 2), rule(SHADOW_ASSASSIN_CHESTPLATE, 3), rule(LIVID_DAGGER, 3), rule(SHADOW_ASSASSIN_LEGGINGS, 4), rule(SHADOW_ASSASSIN_CLOAK, 4), rule(WARPED_STONE, 4), rule(RECOMBOBULATOR_3000, 4), rule(SHADOW_ASSASSIN_HELMET, 5), rule(SHADOW_ASSASSIN_BOOTS, 5), rule(FUMING_POTATO_BOOK, 5), rule(overload_1, 5), rule(ultimate_legion_1, 5), rule(lethality_6, 5), rule(DARK_ORB, 5), rule(ultimate_combo_2, 7), rule(ultimate_no_pain_no_gain_2, 7), rule(ultimate_wise_2, 7), rule(ultimate_jerry_3, 7), rule(rejuvenate_3, 7), rule(ultimate_last_stand_2, 7), rule(ultimate_bank_3, 7));

        registerDrops(CaseMaterial.OBSIDIAN, Floor.V, rule(LIVID_DAGGER, 3), rule(SHADOW_ASSASSIN_LEGGINGS, 4), rule(SHADOW_ASSASSIN_CLOAK, 4), rule(WARPED_STONE, 4), rule(RECOMBOBULATOR_3000, 4), rule(SHADOW_ASSASSIN_HELMET, 5), rule(FUMING_POTATO_BOOK, 5), rule(SHADOW_ASSASSIN_BOOTS, 5), rule(overload_1, 5), rule(ultimate_legion_1, 5), rule(DARK_ORB, 5), rule(HOT_POTATO_BOOK, 5), rule(lethality_6, 5), rule(ultimate_combo_1, 7), rule(ultimate_no_pain_no_gain_1, 7), rule(ultimate_last_stand_1, 7), rule(ultimate_wise_1, 7), rule(ultimate_jerry_2, 7), rule(ultimate_bank_2, 7));

        registerDrops(CaseMaterial.BEDROCK, Floor.MV, rule(SHADOW_FURY, 2), rule(LAST_BREATH, 2), rule(SHADOW_ASSASSIN_CHESTPLATE, 3), rule(LIVID_DAGGER, 3), rule(THIRD_MASTER_STAR, 3), rule(SHADOW_ASSASSIN_LEGGINGS, 4), rule(SHADOW_ASSASSIN_CLOAK, 4), rule(WARPED_STONE, 4), rule(RECOMBOBULATOR_3000, 4), rule(MASTER_SKULL_TIER_4, 4), rule(SHADOW_ASSASSIN_HELMET, 5), rule(SHADOW_ASSASSIN_BOOTS, 5), rule(FUMING_POTATO_BOOK, 5), rule(overload_1, 5), rule(ultimate_legion_1, 5), rule(DARK_ORB, 5), rule(lethality_6, 5), rule(ultimate_combo_2, 7), rule(ultimate_no_pain_no_gain_2, 7), rule(ultimate_wise_2, 7), rule(ultimate_jerry_3, 7), rule(rejuvenate_3, 7), rule(ultimate_last_stand_2, 7), rule(ultimate_bank_3, 7));

        registerDrops(CaseMaterial.OBSIDIAN, Floor.MV, rule(LIVID_DAGGER, 3), rule(THIRD_MASTER_STAR, 3), rule(SHADOW_ASSASSIN_LEGGINGS, 4), rule(SHADOW_ASSASSIN_CLOAK, 4), rule(WARPED_STONE, 4), rule(RECOMBOBULATOR_3000, 4), rule(MASTER_SKULL_TIER_3, 4), rule(SHADOW_ASSASSIN_HELMET, 5), rule(FUMING_POTATO_BOOK, 5), rule(SHADOW_ASSASSIN_BOOTS, 5), rule(overload_1, 5), rule(ultimate_legion_1, 5), rule(DARK_ORB, 5), rule(HOT_POTATO_BOOK, 5), rule(lethality_6, 5), rule(ultimate_combo_1, 7), rule(ultimate_no_pain_no_gain_1, 7), rule(ultimate_last_stand_1, 7), rule(ultimate_wise_1, 7), rule(ultimate_jerry_2, 7), rule(ultimate_bank_2, 7));

        registerDrops(CaseMaterial.OBSIDIAN, Floor.IV, rule(SPIRIT_SWORD, 3), rule(SPIRIT_SHORTBOW, 3), rule(SPIRIT_BOOTS, 3), rule(SPIRIT_WING, 4), rule(RECOMBOBULATOR_3000, 4), rule(SPIRIT_BONE, 4), rule(FUMING_POTATO_BOOK, 5), rule(SPIRIT, 4), rule(SPIRIT_STONE, 5), rule(ultimate_rend_2, 5), rule(HOT_POTATO_BOOK, 5), rule(ultimate_combo_1, 7), rule(ultimate_no_pain_no_gain_1, 7), rule(ultimate_last_stand_1, 7), rule(ultimate_wise_1, 7), rule(ultimate_jerry_2, 7));

        registerDrops(CaseMaterial.OBSIDIAN, Floor.MIV, rule(SECOND_MASTER_STAR, 3), rule(SPIRIT_SWORD, 3), rule(SPIRIT_SHORTBOW, 3), rule(SPIRIT_BOOTS, 3), rule(SPIRIT_WING, 4), rule(RECOMBOBULATOR_3000, 4), rule(MASTER_SKULL_TIER_3, 4), rule(SPIRIT_BONE, 4), rule(FUMING_POTATO_BOOK, 5), rule(SPIRIT, 4), rule(SPIRIT_STONE, 5), rule(ultimate_rend_2, 5), rule(HOT_POTATO_BOOK, 5), rule(ultimate_combo_1, 7), rule(ultimate_no_pain_no_gain_1, 7), rule(ultimate_last_stand_1, 7), rule(ultimate_wise_1, 7), rule(ultimate_jerry_2, 7));

        registerDrops(CaseMaterial.OBSIDIAN, Floor.III, rule(ADAPTIVE_CHESTPLATE, 3), rule(RECOMBOBULATOR_3000, 4), rule(FUMING_POTATO_BOOK, 5), rule(ADAPTIVE_LEGGINGS, 4), rule(ADAPTIVE_HELMET, 5), rule(ADAPTIVE_BOOTS, 5), rule(HOT_POTATO_BOOK, 5), rule(SUSPICIOUS_VIAL, 5), rule(ultimate_combo_1, 7), rule(ultimate_no_pain_no_gain_1, 7), rule(NECROMANCER_BROOCH, 5), rule(ultimate_last_stand_1, 7), rule(ultimate_wise_1, 7), rule(ultimate_jerry_2, 7), rule(ultimate_bank_2, 7));

        registerDrops(CaseMaterial.OBSIDIAN, Floor.MIII, rule(FIRST_MASTER_STAR, 3), rule(ADAPTIVE_CHESTPLATE, 3), rule(RECOMBOBULATOR_3000, 4), rule(MASTER_SKULL_TIER_3, 4), rule(FUMING_POTATO_BOOK, 5), rule(ADAPTIVE_LEGGINGS, 4), rule(ADAPTIVE_HELMET, 5), rule(ADAPTIVE_BOOTS, 5), rule(HOT_POTATO_BOOK, 5), rule(SUSPICIOUS_VIAL, 5), rule(ultimate_combo_1, 7), rule(ultimate_no_pain_no_gain_1, 7), rule(NECROMANCER_BROOCH, 5), rule(ultimate_last_stand_1, 7), rule(ultimate_wise_1, 7), rule(ultimate_jerry_2, 7), rule(ultimate_bank_2, 7));

        registerDrops(CaseMaterial.OBSIDIAN, Floor.II, rule(RECOMBOBULATOR_3000, 4), rule(ADAPTIVE_BLADE, 4), rule(ADAPTIVE_BELT, 4), rule(FUMING_POTATO_BOOK, 5), rule(RED_SCARF, 5), rule(SCARF_STUDIES, 5), rule(HOT_POTATO_BOOK, 5), rule(ultimate_combo_1, 7), rule(ultimate_no_pain_no_gain_1, 7), rule(NECROMANCER_BROOCH, 5));

        registerDrops(CaseMaterial.OBSIDIAN, Floor.MII, rule(RECOMBOBULATOR_3000, 4), rule(ADAPTIVE_BLADE, 4), rule(ADAPTIVE_BELT, 4), rule(MASTER_SKULL_TIER_2, 4), rule(FUMING_POTATO_BOOK, 5), rule(RED_SCARF, 5), rule(SCARF_STUDIES, 5), rule(HOT_POTATO_BOOK, 5), rule(ultimate_combo_1, 7), rule(ultimate_no_pain_no_gain_1, 7), rule(NECROMANCER_BROOCH, 5));

        registerDrops(CaseMaterial.OBSIDIAN, Floor.I, rule(RECOMBOBULATOR_3000, 4), rule(BONZO_STAFF, 3), rule(FUMING_POTATO_BOOK, 5), rule(BONZO_MASK, 4), rule(RED_NOSE, 5), rule(BALLOON_SNAKE, 4), rule(HOT_POTATO_BOOK, 5), rule(ultimate_combo_1, 7), rule(ultimate_no_pain_no_gain_1, 7), rule(NECROMANCER_BROOCH, 5), rule(ultimate_bank_1, 7), rule(ultimate_jerry_1, 7));


        registerDrops(CaseMaterial.OBSIDIAN, Floor.MI, rule(RECOMBOBULATOR_3000, 4), rule(BONZO_STAFF, 3), rule(MASTER_SKULL_TIER_1, 4), rule(FUMING_POTATO_BOOK, 5), rule(BONZO_MASK, 4), rule(RED_NOSE, 5), rule(BALLOON_SNAKE, 4), rule(HOT_POTATO_BOOK, 5), rule(ultimate_combo_1, 7), rule(ultimate_no_pain_no_gain_1, 7), rule(NECROMANCER_BROOCH, 5), rule(ultimate_bank_1, 7), rule(ultimate_jerry_1, 7));
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