package io.hamlook.aetheria.features.profile.impl.skills;

public enum Skill {

    FARMING(19),
    MINING(20),
    COMBAT(21),
    FORAGING(22),
    FISHING(23),
    ENCHANTING(24),
    ALCHEMY(25),
    RUNECRAFTING(29),
    SOCIAL(30),
    CARPENTRY(31),
    TAMING(33);

    public final int slot;

    Skill(int slot){
        this.slot = slot;
    }

}
