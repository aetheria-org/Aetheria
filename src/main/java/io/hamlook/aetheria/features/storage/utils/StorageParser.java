package io.hamlook.aetheria.features.storage.utils;

import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.item.ItemUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockStainedGlassPane;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.regex.Pattern;

public class StorageParser {

    public static final Pattern BACKPACK_N_N = Pattern.compile("(.+) Backpack (\\d+)/(\\d+)");
    public static final Pattern ECHEST_PAGE_N = Pattern.compile("Ender Chest \\(Page (\\d+)\\)");

    public static boolean isStorageContainer(String title) {
        return ECHEST_PAGE_N.matcher(title).matches() || BACKPACK_N_N.matcher(title).matches();
    }

    public static int getBackpackRenderHeight(String title) {
        if (title == null || title.isEmpty()) return 200;

        String[] words = title.split(" ");
        if (words.length == 0) return 200;

        return StorageUtils.getBackpackRenderHeight(words[0]);
    }

    public static LinkedHashMap<String, SContainer> parseOverlay(ContainerChest chest, LinkedHashMap<String, SContainer> loadedContainers) {
        LinkedHashMap<String, SContainer> detectedContainers = new LinkedHashMap<>();

        parseEchestSlots(chest, loadedContainers, detectedContainers);
        parseBackpackSlots(chest, loadedContainers, detectedContainers);

        return detectedContainers;
    }

    private static void parseEchestSlots(ContainerChest chest, LinkedHashMap<String, SContainer> loadedContainers, LinkedHashMap<String, SContainer> detectedContainers) {
        for (int j = 0; j < 9; j++) {
            int slot = 9 + j;
            ItemStack stack = chest.getSlot(slot).getStack();
            int page = j + 1;

            if (stack == null) continue;

            String id = Type.ECHEST.prefix + "-" + page;
            String title = ColorUtils.stripColor(stack.getDisplayName());

            if (!title.contains("Ender") || !(Block.getBlockFromItem(stack.getItem()) instanceof BlockStainedGlassPane)) {
                continue;
            }

            boolean locked = title.contains("Locked");
            SContainer container = getOrCreateContainer(loadedContainers, id, page, Type.ECHEST, locked, 200, 45);
            detectedContainers.put(id, container);
        }
    }

    private static void parseBackpackSlots(ContainerChest chest, LinkedHashMap<String, SContainer> loadedContainers, LinkedHashMap<String, SContainer> detectedContainers) {
        for (int j = 0; j < 18; j++) {
            int slot = 27 + j;
            ItemStack stack = chest.getSlot(slot).getStack();
            int page = j + 1;

            if (stack == null) continue;

            String id = Type.BAG.prefix + "-" + page;

            // Skip empty backpack placeholder slots (stained glass pane named "§cEmpty Backpack Slot N")
            if (Block.getBlockFromItem(stack.getItem()) instanceof BlockStainedGlassPane) {
                String rawName = stack.getDisplayName();
                if (ColorUtils.stripColor(rawName).startsWith("Empty Backpack Slot")) continue;
            }

            int renderH = extractBackpackRenderHeight(stack);
            int slotCount = StorageUtils.getSlotCountFromRenderHeight(renderH);

            SContainer container = getOrCreateContainer(loadedContainers, id, page, Type.BAG, false, renderH, slotCount);
            detectedContainers.put(id, container);
        }
    }

    private static SContainer getOrCreateContainer(LinkedHashMap<String, SContainer> loadedContainers, String id, int page, Type type, boolean specialFlag, int renderH, int slotCount) {
        if (loadedContainers.containsKey(id)) {
            SContainer existing = loadedContainers.get(id);
            if (type == Type.ECHEST) {
                existing.locked = specialFlag;
            } else {
                existing.empty = specialFlag;
                existing.renderH = renderH;
                existing.slotCount = slotCount;
            }
            return existing;
        } else {
            return new SContainer(new java.util.HashMap<>(), id, page, type, specialFlag, 307, renderH, slotCount, false);
        }
    }

    private static int extractBackpackRenderHeight(ItemStack stack) {
        String internalName = ItemUtils.getInternalName(stack);
        if (internalName != null && !internalName.isEmpty()) {
            String[] parts = internalName.split("_");
            if (parts.length > 0) {
                return getBackpackRenderHeight(parts[0]);
            }
        }
        return 200;
    }
}