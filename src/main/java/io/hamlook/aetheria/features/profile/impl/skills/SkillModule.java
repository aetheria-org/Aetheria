package io.hamlook.aetheria.features.profile.impl.skills;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.features.profile.ProfileModule;
import io.hamlook.aetheria.utils.ContainerUtils;
import io.hamlook.aetheria.utils.data.SkillApi;
import io.hamlook.aetheria.utils.item.ItemUtils;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.List;

public class SkillModule extends ProfileModule {

    private static final int AVERAGE_SKILL_SLOT = 4;

    public HashMap<Skill, SkillData> skills = new HashMap<>();
    public float averageSkillLvl;

    public SkillModule() {
        super("Player Skills", "skills", "Your Skills");
    }

    @Override
    public void parse(GuiContainer container) {
        ContainerChest chest = ContainerUtils.getOpenChest(container);
        if (chest == null) return;

        for (Skill skill : Skill.values()) {
            ItemStack stack = stackAt(chest, skill.slot);
            if (stack == null) {
                Aetheria.logger.warning("No skill item in slot " + skill.slot + " for " + skill.name());
                continue;
            }
            List<String> lore = ItemUtils.getLoreLinesWithoutColor(stack);
            skills.put(skill, new SkillData(
                    SkillApi.currentXp(lore),
                    SkillApi.requiredXp(lore),
                    SkillApi.rawLevel(lore)));
        }

        ItemStack averageStack = stackAt(chest, AVERAGE_SKILL_SLOT);
        if (averageStack == null) return;
        averageSkillLvl = SkillApi.average(ItemUtils.getLoreLinesWithoutColor(averageStack));
    }

    private static ItemStack stackAt(ContainerChest chest, int index) {
        Slot slot = chest.getSlot(index);
        if (slot == null || !slot.getHasStack()) return null;
        ItemStack stack = slot.getStack();
        return stack == null || stack.getItem() == null ? null : stack;
    }
}
