package io.hamlook.aetheria.features.misc;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.events.ASMTooltipEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.ContainerUtils;
import io.hamlook.aetheria.utils.Utils;
import io.hamlook.aetheria.utils.compat.KeyboardCompat;
import io.hamlook.aetheria.utils.data.SkillApi;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Keyboard;

import java.util.List;

@RegisterEvents
public class SkillXpDisplay {

    private static final String HINT = EnumChatFormatting.DARK_GRAY + "[SHIFT: XP to max level]";

    @HandleEvent
    public void onTooltip(ASMTooltipEvent event) {
        if (ATHRConfig.feature == null || !ATHRConfig.feature.misc.skillXpDisplay) return;
        if (event.toolTip == null || event.itemStack == null) return;
        if (!ContainerUtils.isChestOpen()) return;

        String containerName = ContainerUtils.getContainerName();
        if (containerName == null) return;
        boolean isSkill = containerName.contains("View Skills") || containerName.contains("Your Skills");
        boolean isDungeon = containerName.contains("View Dungeon Stats") || containerName.contains("Dungeoneering");
        if (!isSkill && !isDungeon) return;

        List<String> tooltip = event.toolTip;
        if (tooltip.isEmpty()) return;

        String name = SkillApi.stripColour(tooltip.get(0)).toLowerCase();

        int currentLevel = SkillApi.level(tooltip);
        long currentXp = SkillApi.currentXp(tooltip);
        if (currentLevel < 0 || currentXp < 0) return;

        boolean shifting = KeyboardCompat.isKeyDown(Keyboard.KEY_LSHIFT) || KeyboardCompat.isKeyDown(Keyboard.KEY_RSHIFT);

        if (!shifting) {
            if (!tooltip.contains(HINT)) tooltip.add(HINT);
            return;
        }

        tooltip.remove(HINT);

        long[] table = SkillApi.tableFor(isDungeon ? "dungeon" : name);
        int maxLevel = SkillApi.maxLevelFor(table);

        if (currentLevel >= maxLevel) {
            tooltip.add(EnumChatFormatting.GREEN + "Max level reached!");
            return;
        }

        long totalAtCurrent = SkillApi.totalXpForLevel(table, currentLevel);
        long totalAbsolute = totalAtCurrent + currentXp;
        long totalAtMax = SkillApi.totalXpForLevel(table, maxLevel);
        long remaining = totalAtMax - totalAbsolute;

        tooltip.add(EnumChatFormatting.AQUA + "XP to max (Lv" + maxLevel + "): " + EnumChatFormatting.YELLOW + Utils.shortNumberFormat(remaining));
    }
}
