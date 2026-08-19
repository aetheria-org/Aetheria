package io.hamlook.aetheria.features.chestanimations;

import io.hamlook.aetheria.DebugLogger;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.dungeons.DungeonChestAnimationConfig;
import io.hamlook.aetheria.features.chestanimations.caseopening.CustomDropAnimationGui;
import io.hamlook.aetheria.features.chestanimations.caseopening.DungeonDropData;
import io.hamlook.aetheria.utils.ContainerUtils;
import io.hamlook.aetheria.utils.item.ItemUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

/**
 * Registry of all available chest animations. To add a new animation:
 * 1. Add a name constant and put it into {@link #OPTIONS}.
 * 2. Add a case to {@link #create(String, ContainerChest, DungeonDropData.Floor, DungeonDropData.CaseMaterial)}.
 * The config dropdowns reference the same constants, so every chest type can pick any registered animation.
 */
public final class ChestAnimations {

    public static final String NONE = "None";
    public static final String CASE_OPENING = "Case Opening";

    public static final String[] OPTIONS = {NONE, CASE_OPENING};

    private static final int REWARD_SLOT_START = 10;
    private static final int REWARD_SLOT_END = 16;

    private ChestAnimations() {
    }

    public static String getOption(DungeonDropData.CaseMaterial material) {
        if (ATHRConfig.feature == null) return NONE;
        DungeonChestAnimationConfig config = ATHRConfig.feature.dungeons.caseOpening;
        int index;
        switch (material) {
            case WOOD: index = config.woodChestAnimation; break;
            case GOLD: index = config.goldChestAnimation; break;
            case EMERALD: index = config.emeraldChestAnimation; break;
            case DIAMOND: index = config.diamondChestAnimation; break;
            case OBSIDIAN: index = config.obsidianChestAnimation; break;
            case BEDROCK: index = config.bedrockChestAnimation; break;
            default: return NONE;
        }
        if (index < 0 || index >= OPTIONS.length) return NONE;
        return OPTIONS[index];
    }

    public static GuiScreen create(String option, ContainerChest container, DungeonDropData.Floor floor, DungeonDropData.CaseMaterial material) {
        if (CASE_OPENING.equals(option)) {
            return new CustomDropAnimationGui(container, floor, material);
        }
        return null;
    }

    /**
     * Scans the reward slots of a dungeon chest container and returns the best matching drop for the given floor/material.
     */
    public static DungeonDropData.Rule findBestReward(ContainerChest container, DungeonDropData.Floor floor, DungeonDropData.CaseMaterial material) {
        if (container == null) return null;
        IInventory lower = ContainerUtils.getLowerInventory(container);
        if (lower == null) return null;
        int size = lower.getSizeInventory();
        int dropCount = DungeonDropData.getDrops(material, floor).size();
        DebugLogger.log("[ChestAnimations] Scanning — floor=" + floor + ", material=" + material + ", possible drops=" + dropCount + ", inventory size=" + size);

        if (ATHRConfig.feature != null && ATHRConfig.feature.debug.enableDebug) {
            for (int i = 0; i < size; i++) {
                ItemStack s = lower.getStackInSlot(i);
                if (s == null || s.getItem() == null) continue;
                String id = ItemUtils.getEffectiveItemId(s);
                DebugLogger.log("[ChestAnimations] [ALL] Slot " + i + ": " + s.getDisplayName() + " (id=" + id + ")");
            }
        }

        DungeonDropData.Rule best = null;
        for (int i = REWARD_SLOT_START; i <= REWARD_SLOT_END; i++) {
            ItemStack stack = lower.getStackInSlot(i);
            if (stack == null || stack.getItem() == null) continue;

            String itemId = ItemUtils.getEffectiveItemId(stack);
            if (itemId.isEmpty()) continue;
            DebugLogger.log("[ChestAnimations] Slot " + i + ": " + itemId);

            DungeonDropData.Rule found = DungeonDropData.getDrops(material, floor).stream().filter(r -> r.item.name().equals(itemId)).findFirst().orElse(null);
            if (found == null) continue;

            if (best == null || found.rarity < best.rarity || (found.rarity == best.rarity && found.item.name().compareTo(best.item.name()) < 0)) {
                best = found;
                DebugLogger.log("[ChestAnimations] New best reward: " + best.item.name() + " (rarity " + best.rarity + ")");
            }
        }
        return best;
    }
}