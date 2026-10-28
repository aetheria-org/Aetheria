package io.hamlook.aetheria.features.profile.impl.skills;

public class SkillData {

    public long skillXP;
    public long requiredXP;
    public int currentLevel;

    public SkillData(long skillXP, long requiredXP, int currentLevel) {
        this.skillXP = skillXP;
        this.requiredXP = requiredXP;
        this.currentLevel = currentLevel;
    }

    @Override
    public String toString() {
        return "Data: " + skillXP + "/"+requiredXP + " | " + currentLevel + " LVL";
    }
}
