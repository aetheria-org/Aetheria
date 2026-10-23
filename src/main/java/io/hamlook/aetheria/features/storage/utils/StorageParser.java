package io.hamlook.aetheria.features.storage.utils;

import io.hamlook.aetheria.features.storage.StorageManager;
import io.hamlook.aetheria.features.storage.data.StorageData;
import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.compat.TextCompat;
import io.hamlook.aetheria.utils.item.ItemUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockStainedGlassPane;
import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StorageParser {

    public static final Pattern BACKPACK_N_N = Pattern.compile("(.+) Backpack (\\d+)/(\\d+)");
    public static final Pattern ECHEST_PAGE_N = Pattern.compile("Ender Chest \\(Page (\\d+)\\)");

    public static final class StorageTarget {
        public final String id;
        public final Type type;
        public final int page;
        public final int renderH;
        public final int slotCount;

        StorageTarget(String id, Type type, int page, int renderH, int slotCount) {
            this.id = id;
            this.type = type;
            this.page = page;
            this.renderH = renderH;
            this.slotCount = slotCount;
        }
    }

    public static boolean isStorageContainer(String title) {
        return ECHEST_PAGE_N.matcher(title).matches() || BACKPACK_N_N.matcher(title).matches();
    }

    public static StorageTarget parseTarget(String title) {
        if (title == null || title.isEmpty()) return null;

        Matcher backpack = BACKPACK_N_N.matcher(title);
        if (backpack.matches()) {
            int page = parseInt(backpack.group(2));
            if (page <= 0) return null;
            int renderH = StorageUtils.backpackRenderHeightOrZero(backpack.group(1));
            int slotCount = renderH > 0 ? StorageUtils.getSlotCountFromRenderHeight(renderH) : 0;
            return new StorageTarget(Type.BAG.prefix + "-" + page, Type.BAG, page, renderH, slotCount);
        }

        Matcher echest = ECHEST_PAGE_N.matcher(title);
        if (echest.matches()) {
            int page = parseInt(echest.group(1));
            if (page <= 0) return null;
            return new StorageTarget(Type.ECHEST.prefix + "-" + page, Type.ECHEST, page, 0, 0);
        }

        return null;
    }

    public static LinkedHashMap<String, SContainer> parseOverlay(Map<Integer, ItemStack> items, LinkedHashMap<String, SContainer> loadedContainers) {
        LinkedHashMap<String, SContainer> detectedContainers = new LinkedHashMap<>();

        parseEchestSlots(items, loadedContainers, detectedContainers);
        parseBackpackSlots(items, loadedContainers, detectedContainers);

        return detectedContainers;
    }

    private static void parseEchestSlots(Map<Integer, ItemStack> items, LinkedHashMap<String, SContainer> loadedContainers, LinkedHashMap<String, SContainer> detectedContainers) {
        for (int j = 0; j < 9; j++) {
            ItemStack stack = items.get(9 + j);
            if (stack == null) continue;

            String title = ColorUtils.stripColor(TextCompat.getUnformattedText(stack.getDisplayName()));
            if (!title.contains("Ender") || !(Block.getBlockFromItem(stack.getItem()) instanceof BlockStainedGlassPane)) {
                continue;
            }

            int page = j + 1;
            String id = Type.ECHEST.prefix + "-" + page;
            boolean locked = title.contains("Locked");
            SContainer container = getOrCreateContainer(loadedContainers, id, page, Type.ECHEST, locked, 0, 0);
            detectedContainers.put(id, container);
        }
    }

    private static void parseBackpackSlots(Map<Integer, ItemStack> items, LinkedHashMap<String, SContainer> loadedContainers, LinkedHashMap<String, SContainer> detectedContainers) {
        for (int j = 0; j < 18; j++) {
            ItemStack stack = items.get(27 + j);
            if (stack == null) continue;

            // Skip empty backpack placeholder slots (stained glass pane named "§cEmpty Backpack Slot N")
            if (Block.getBlockFromItem(stack.getItem()) instanceof BlockStainedGlassPane) {
                String rawName = TextCompat.plainString(stack.getDisplayName());
                if (rawName.startsWith("Empty Backpack Slot")) continue;
            }

            int page = j + 1;
            String id = Type.BAG.prefix + "-" + page;
            int renderH = extractBackpackRenderHeight(stack);
            int slotCount = renderH > 0 ? StorageUtils.getSlotCountFromRenderHeight(renderH) : 0;

            SContainer container = getOrCreateContainer(loadedContainers, id, page, Type.BAG, false, renderH, slotCount);
            detectedContainers.put(id, container);
        }
    }

    private static SContainer getOrCreateContainer(LinkedHashMap<String, SContainer> loadedContainers, String id, int page, Type type, boolean specialFlag, int renderH, int slotCount) {
        SContainer existing = loadedContainers.get(id);
        if (existing != null) {
            boolean changed = false;
            if (type == Type.ECHEST) {
                if (existing.locked != specialFlag) {
                    existing.locked = specialFlag;
                    changed = true;
                }
            } else if (existing.empty != specialFlag) {
                existing.empty = specialFlag;
                changed = true;
            }
            if (slotCount > 0 && existing.slotCount != slotCount) {
                existing.slotCount = slotCount;
                existing.renderH = renderH;
                StorageManager.markLayoutDirty();
                changed = true;
            }
            if (changed) StorageData.markDirty(existing);
            return existing;
        }

        if (slotCount <= 0) {
            slotCount = StorageUtils.initialSlotCount(type, loadedContainers.values());
            renderH = StorageUtils.renderHeightForSlotCount(slotCount);
        }

        boolean locked = type == Type.ECHEST && specialFlag;
        boolean empty = type == Type.BAG && specialFlag;
        SContainer created = new SContainer(new HashMap<>(), id, page, type, locked, 307, renderH, slotCount, empty);
        StorageData.markDirty(created);
        return created;
    }

    private static int extractBackpackRenderHeight(ItemStack stack) {
        String internalName = ItemUtils.getInternalName(stack);
        if (internalName == null || internalName.isEmpty()) return 0;
        String[] parts = internalName.split("_");
        if (parts.length == 0) return 0;
        return StorageUtils.backpackRenderHeightOrZero(parts[0]);
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
