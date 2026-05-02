package com.jef.justenoughfakepixel.features.profile.data.skills;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum Skill {

    FARMING("Farming",19),
    MINING("Mining",20),
    COMBAT("Combat",21),
    FORAGING("Foraging",22),
    FISHING("Fishing",23),
    ENCHANTING("Enchanting",24),
    ALCHEMY("Alchemy",25),
    RUNECRAFTING("Runecrafting",29),
    SOCIAL("Social",30),
    TAMING("Taming",32),
    CARPENTRY("Carpentry",33);

    public final String name;
    public final int slot;

    public static Skill get(String s) {
        for(Skill skill : Skill.values()) {
            if(skill.name.equalsIgnoreCase(s)) return skill;
        }
        return null;
    }
}
