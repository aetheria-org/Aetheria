package io.hamlook.aetheria.features.storage.utils;

import io.hamlook.aetheria.DebugLogger;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.storage.StorageManager;
import io.hamlook.aetheria.features.storage.data.StorageData;
import io.hamlook.aetheria.features.storage.data.StorageSaving;
import io.hamlook.aetheria.utils.ContainerUtils;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.server.S2DPacketOpenWindow;
import net.minecraft.network.play.server.S2EPacketCloseWindow;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import net.minecraft.network.play.server.S30PacketWindowItems;

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

    public void handleOpenWindow(S2DPacketOpenWindow packet) {
        if (!ATHRConfig.feature.storage.enabled) return;

        String windowTitle = packet.getWindowTitle().getUnformattedText();
        windowTitle = windowTitle.replaceAll("§[0-9a-fk-or]", "");

        DebugLogger.log("Window opened: " + windowTitle);

        resetCurrentState();

        if (windowTitle.trim().equals("Storage")) {
            DebugLogger.log("Opened Storage menu");
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
                DebugLogger.log("Failed to parse backpack slot number: " + windowTitle);
            }
        }

        Matcher echestMatcher = StorageParser.ECHEST_PAGE_N.matcher(windowTitle);
        if (echestMatcher.matches()) {
            try {
                int page = Integer.parseInt(echestMatcher.group(1));
                return new ContainerInfo(Type.ECHEST, page, null);
            } catch (NumberFormatException e) {
                DebugLogger.log("Failed to parse ender chest page number: " + windowTitle);
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

            DebugLogger.log("Created new " + info.type.name() + " page " + info.page + " with " + slotCount + " slots");
        } else {
            SContainer existing = StorageData.containers.get(currentContainerId);
            if (info.type == Type.BAG && info.sizeType != null) {
                int renderH = StorageParser.getBackpackRenderHeight(info.sizeType);
                existing.slotCount = StorageUtils.getSlotCountFromRenderHeight(renderH);
                existing.renderH = renderH;
            }
            DebugLogger.log("Using existing " + info.type.name() + " page " + info.page);
        }
    }

    private void updateActiveContainer() {
        StorageManager.setActiveContainer(currentContainerId);
    }

    public void handleCloseWindow(S2EPacketCloseWindow packet) {
        if (currentContainerId != null) {
            DebugLogger.log("Closing container: " + currentContainerId);
        }

        currentContainerId = null;
    }

    public void handleSetSlot(S2FPacketSetSlot packet) {
        if (!ATHRConfig.feature.storage.enabled) return;
        if (currentContainerId == null) return;

        int windowId = getCurrentWindowId();
        if (windowId == -1 || windowId != packet.func_149175_c()) return;

        SContainer container = getCurrentContainer();
        if (container == null) return;

        int slot = packet.func_149173_d();
        ItemStack stack = packet.func_149174_e();

        if (slot >= 9 && slot < 9 + container.slotCount) {
            int storageSlot = slot - 9;

            if (stack == null) {
                container.slots.remove(storageSlot);
                DebugLogger.log("Removed item from slot " + storageSlot);
            } else {
                container.setStack(storageSlot, stack.copy());
                DebugLogger.log("Updated slot " + storageSlot + ": " + stack.getDisplayName());
            }

            StorageSaving.saveStorageData(StorageData.containers.values());
        }
    }

    public void handleWindowItems(S30PacketWindowItems packet) {
        if (!ATHRConfig.feature.storage.enabled) return;
        if (currentContainerId == null) return;

        int windowId = getCurrentWindowId();
        if (windowId == -1 || windowId != packet.func_148911_c()) return;

        SContainer container = getCurrentContainer();
        if (container == null) return;

        IInventory inv = ContainerUtils.getLowerInventory();
        if (inv != null) {
            int chestSize = inv.getSizeInventory();
            int rows = chestSize / 9;
            int storageSlots = (rows - 1) * 9;
            int renderH = StorageUtils.calculateRenderHeight(rows - 1);

            container.slotCount = storageSlots;
            container.renderH = renderH;

            DebugLogger.log("Chest has " + chestSize + " slots (" + rows + " rows), " + storageSlots + " storage slots");
        }

        ItemStack[] items = packet.getItemStacks();

        DebugLogger.log("Received full inventory update with " + items.length + " items");

        container.slots.clear();

        for (int i = 0; i < container.slotCount; i++) {
            int packetSlot = 9 + i;
            if (packetSlot >= items.length) break;

            ItemStack stack = items[packetSlot];
            if (stack != null) {
                container.setStack(i, stack.copy());
                DebugLogger.log("Updated slot " + i + ": " + stack.getDisplayName());
            }
        }

        DebugLogger.log("Updated " + container.slots.size() + " items in container " + currentContainerId);

        StorageSaving.saveStorageData(StorageData.containers.values());
    }

    public void handleClickWindow(C0EPacketClickWindow packet) {
        if (!ATHRConfig.feature.storage.enabled) return;
        if (currentContainerId == null) return;
        if (!ContainerUtils.isChestOpen()) return;

        int windowId = getCurrentWindowId();
        if (windowId == -1 || windowId != packet.getWindowId()) return;

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

        DebugLogger.log("Synced container after click");

        StorageSaving.saveStorageData(StorageData.containers.values());
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