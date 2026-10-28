package io.hamlook.aetheria.features.profile.impl.skills;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.features.profile.ProfileModule;
import io.hamlook.aetheria.utils.Utils;
import io.hamlook.aetheria.utils.item.ItemUtils;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import java.rmi.UnexpectedException;
import java.util.HashMap;
import java.util.List;

public class SkillModule extends ProfileModule {

    public HashMap<Skill,SkillData> skills = new HashMap<>();
    public float averageSkillLvl;

    public SkillModule() {
        super("Player Skills", "skills", "Your Skills");
    }

    @Override
    public void parse(GuiContainer container) throws Exception{
        if(!(container.inventorySlots instanceof ContainerChest)) return;
        ContainerChest containerChest = (ContainerChest) container.inventorySlots;
        for(Skill s : Skill.values()) {
            Slot slot = containerChest.getSlot(s.slot);
            if(slot == null || !slot.getHasStack() || slot.getStack().getItem() == null) throw new UnexpectedException("Null ItemStack for Skill: " + s.name());
            skills.put(s,parseSkill(slot.getStack()));
            Aetheria.logger.info("Added Skill Data for Skill: " + s.name());
            Aetheria.logger.info("Skill Data: " + skills.get(s).toString());
        }
        Slot slot = containerChest.getSlot(4);
        if(slot == null || !slot.getHasStack() || slot.getStack().getItem() == null) throw new UnexpectedException("Null ItemStack for AVG Skill");
        ItemStack stack = slot.getStack();
        for(String s : ItemUtils.getLore(stack)){
            if(s.contains("Skill Avg.")){
                String num = s.split(" ")[0];
                averageSkillLvl = (float)Double.parseDouble(num.trim());
                Aetheria.logger.info("Skill Average: " + averageSkillLvl);
            }
        }
    }

    private SkillData parseSkill(ItemStack stack) {
        List<String> lore = ItemUtils.getLore(stack);
        int level = -1;
        long skilLXP = -1;
        long reqXP = -1;
        for(String s : lore){
            if(s.startsWith("Progress to Level")){
                String num = s.substring(17,s.indexOf(":"));
                Aetheria.logger.info("Skill Level: " + num);
                level = Integer.parseInt(num.trim());
                continue;
            }
            if(s.contains("/")){
                String[] parts = s.split("/");
                String req = parts[1];
                String cur = parts[0].substring(parts[0].lastIndexOf(' ')+1);
                Aetheria.logger.info("Cur: " + cur + " | Req: " + req);
                skilLXP = Utils.parseRawNumber(cur.trim());
                reqXP = Utils.parseRawNumber(req.trim());
                continue;
            }
        }
        return new SkillData(skilLXP,reqXP,level);
    }
}
