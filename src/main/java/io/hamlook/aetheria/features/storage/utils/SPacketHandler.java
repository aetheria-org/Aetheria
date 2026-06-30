package io.hamlook.aetheria.features.storage.utils;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.storage.StorageManager;
import io.hamlook.aetheria.features.storage.data.StorageData;
import io.hamlook.aetheria.features.storage.data.StorageSaving;
import io.hamlook.aetheria.utils.ContainerUtils;
import net.minecraft.client.Minecraft;
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
        } else {
            SContainer existing = StorageData.containers.get(currentContainerId);
            if (info.type == Type.BAG && info.sizeType != null) {
                int renderH = StorageParser.getBackpackRenderHeight(info.sizeType);
                existing.slotCount = StorageUtils.getSlotCountFromRenderHeight(renderH);
                existing.renderH = renderH;
            }
        }
    }

    private void updateActiveContainer() {
        StorageManager.setActiveContainer(currentContainerId);
    }

    public void handleCloseWindow(S2EPacketCloseWindow packet) {
        currentContainerId = null;
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

        StorageManager.markContainersDirty();
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
            } else {
                container.setStack(storageSlot, stack.copy());
            }

            StorageSaving.saveContainer(container);
        }
    }

    public void handleWindowItems(S30PacketWindowItems packet) {
        if (!ATHRConfig.feature.storage.enabled) return;
        if (currentContainerId == null) return;

        int windowId = getCurrentWindowId();
        if (windowId == -1 || windowId != packet.func_148911_c()) return;

        SContainer container = getCurrentContainer();
        if (container == null) return;

        ItemStack[] items = packet.getItemStacks();

        container.slots.clear();

        for (int i = 0; i < container.slotCount; i++) {
            int packetSlot = 9 + i;
            if (packetSlot >= items.length) break;

            ItemStack stack = items[packetSlot];
            if (stack != null) {
                container.setStack(i, stack.copy());
            }
        }

        StorageSaving.saveContainer(container);
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
