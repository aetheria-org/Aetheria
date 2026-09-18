package io.hamlook.aetheria.features.storage.utils;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.storage.StorageManager;
import io.hamlook.aetheria.features.storage.data.StorageData;
import io.hamlook.aetheria.utils.ContainerUtils;
import io.hamlook.aetheria.utils.compat.ClickData;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import java.util.regex.Matcher;

public class SPacketHandler {

    private String currentContainerId = null;

    public int getCurrentWindowId() {
        return ContainerUtils.getWindowId();
    }

    public SContainer getCurrentContainer() {
        if (currentContainerId == null) return null;
        return StorageData.containers.get(currentContainerId);
    }

    public void handleOpenWindow(String windowTitle) {
        if (!ATHRConfig.feature.storage.enabled) return;

        windowTitle = windowTitle.replaceAll("§[0-9a-fk-or]", "");

        resetCurrentState();

        if (windowTitle.trim().equals("Storage")) {
            return;
        }

        handleContainerWindow(windowTitle);
    }

    private void resetCurrentState() {
        currentContainerId = null;
    }

    private void handleContainerWindow(String windowTitle) {
        ContainerInfo info = parseContainerInfo(windowTitle);
        if (info == null) return;

        currentContainerId = info.type.prefix + "-" + info.page;

        ensureContainerExists(info);
        updateActiveContainer();
    }

    private ContainerInfo parseContainerInfo(String windowTitle) {
        Matcher backpackMatcher = StorageParser.BACKPACK_N_N.matcher(windowTitle);
        if (backpackMatcher.matches()) {
            try {
                int page = Integer.parseInt(backpackMatcher.group(2));
                String sizeType = backpackMatcher.group(1);
                return new ContainerInfo(Type.BAG, page, sizeType);
            } catch (NumberFormatException e) {
                // Failed to parse backpack slot number
            }
        }

        Matcher echestMatcher = StorageParser.ECHEST_PAGE_N.matcher(windowTitle);
        if (echestMatcher.matches()) {
            try {
                int page = Integer.parseInt(echestMatcher.group(1));
                return new ContainerInfo(Type.ECHEST, page, null);
            } catch (NumberFormatException e) {
                // Failed to parse ender chest page number
            }
        }

        return null;
    }

    private void ensureContainerExists(ContainerInfo info) {
        if (!StorageData.containers.containsKey(currentContainerId)) {
            int renderH = info.type == Type.ECHEST ? 200 : StorageParser.getBackpackRenderHeight(info.sizeType);
            int slotCount = StorageUtils.getSlotCountFromRenderHeight(renderH);

            SContainer container = new SContainer(new java.util.HashMap<>(), info.page, info.type, renderH, false);
            container.slotCount = slotCount;
            StorageData.containers.put(currentContainerId, container);
            StorageData.markDirty(container);
        } else {
            SContainer existing = StorageData.containers.get(currentContainerId);
            if (info.type == Type.BAG && info.sizeType != null) {
                int renderH = StorageParser.getBackpackRenderHeight(info.sizeType);
                if (existing.renderH != renderH) {
                    existing.slotCount = StorageUtils.getSlotCountFromRenderHeight(renderH);
                    existing.renderH = renderH;
                    StorageData.markDirty(existing);
                }
            }
        }
    }

    private void updateActiveContainer() {
        StorageManager.setActiveContainer(currentContainerId);
    }

    public void handleCloseWindow() {
        currentContainerId = null;
    }

    public void handleClickWindow(ClickData click) {
        if (!ATHRConfig.feature.storage.enabled) return;
        if (currentContainerId == null) return;
        if (!ContainerUtils.isChestOpen()) return;

        int windowId = getCurrentWindowId();
        if (windowId == -1 || windowId != click.getWindowId()) return;

        SContainer container = getCurrentContainer();
        if (container == null) return;

        IInventory inv = ContainerUtils.getLowerInventory();
        if (inv == null) return;

        int maxSlot = Math.min(9 + container.slotCount, inv.getSizeInventory());
        for (int i = 9; i < maxSlot; i++) {
            ItemStack stack = inv.getStackInSlot(i);
            int storageSlot = i - 9;
            container.setStack(storageSlot, stack != null ? stack.copy() : null);
        }

        StorageManager.markContainersDirty();
        StorageData.markDirty(container);
    }

    public void handleSetSlot(int windowId, int slot, ItemStack stack) {
        if (!ATHRConfig.feature.storage.enabled) return;
        if (currentContainerId == null) return;

        int currentId = getCurrentWindowId();
        if (currentId == -1 || currentId != windowId) return;

        SContainer container = getCurrentContainer();
        if (container == null) return;

        if (slot >= 9 && slot < 9 + container.slotCount) {
            int storageSlot = slot - 9;

            if (stack == null) {
                container.slots.remove(storageSlot);
            } else {
                container.setStack(storageSlot, stack.copy());
            }

            StorageData.markDirty(container);
        }
    }

    public void handleWindowItems(int windowId, ItemStack[] items) {
        if (!ATHRConfig.feature.storage.enabled) return;
        if (currentContainerId == null) return;

        int currentId = getCurrentWindowId();
        if (currentId == -1 || currentId != windowId) return;

        SContainer container = getCurrentContainer();
        if (container == null) return;

        container.slots.clear();

        for (int i = 0; i < container.slotCount; i++) {
            int packetSlot = 9 + i;
            if (packetSlot >= items.length) break;

            ItemStack stack = items[packetSlot];
            if (stack != null) {
                container.setStack(i, stack.copy());
            }
        }

        StorageData.markDirty(container);
    }

    public void reset() {
        currentContainerId = null;
    }

    private static class ContainerInfo {
        final Type type;
        final int page;
        final String sizeType;

        ContainerInfo(Type type, int page, String sizeType) {
            this.type = type;
            this.page = page;
            this.sizeType = sizeType;
        }
    }
}
