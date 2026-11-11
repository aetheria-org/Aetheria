package io.hamlook.aetheria.features.storage.utils;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.events.*;
import io.hamlook.aetheria.features.qol.TooltipHandler;
import io.hamlook.aetheria.features.storage.StorageManager;
import io.hamlook.aetheria.features.storage.data.StorageData;
import io.hamlook.aetheria.features.storage.render.StorageRenderer;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.ContainerUtils;
import io.hamlook.aetheria.utils.KeybindHelper;
import io.hamlook.aetheria.utils.compat.*;
import io.hamlook.aetheria.utils.render.ItemRenderUtils;
import io.hamlook.aetheria.utils.render.RenderUtils;
import lombok.Setter;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;

import java.util.HashMap;
import java.util.LinkedHashMap;

@RegisterEvents
public class StorageListener {

    private static final String STORAGE_MENU_TITLE = "Storage";

    @Setter
    private static boolean switchingContainer = false;
    private boolean shouldRenderOverlay = false;
    private boolean overlayInitialized = false;
    private int windowId = -1;
    private int menuWindowId = -1;
    private String containerId = null;

    @HandleEvent
    public void onChatMessage(ASMChatEvent event) {
        if (!ATHRConfig.feature.storage.enabled) return;
        if (!shouldRenderOverlay || !overlayInitialized) return;

        String message = TextCompat.getUnformattedText(event.message);
        if (message.contains("Slow down!") || message.contains("executing commands too fast")) {
            shouldRenderOverlay = false;
            overlayInitialized = false;
            StorageManager.closeOverlay();
        }
    }

    @HandleEvent
    public void onInventoryOpened(InventoryOpenEvent event) {
        // FullyOpened and Updated extend this event; only handle the plain open packet here
        if (event.getClass() != InventoryOpenEvent.class) return;
        if (!ATHRConfig.feature.storage.enabled) return;

        String title = event.getInventoryName();
        windowId = event.getInventoryId();

        if (STORAGE_MENU_TITLE.equals(title)) {
            menuWindowId = event.getInventoryId();
            containerId = null;
            return;
        }

        menuWindowId = -1;
        StorageParser.StorageTarget target = StorageParser.parseTarget(title);
        containerId = target == null ? null : target.id;
        if (target == null) return;

        StorageData.ensureLoaded();
        SContainer container = ensureContainer(target);
        applyWindowSize(container, event.getInventorySize());
        StorageManager.setActiveContainer(containerId);
    }

    @HandleEvent
    public void onInventoryFullyOpened(InventoryFullyOpenedEvent event) {
        if (!ATHRConfig.feature.storage.enabled) return;
        if (isStorageMenu(event)) {
            applyStorageMenu(event);
            return;
        }
        applyFullSnapshot(event);
    }

    @HandleEvent
    public void onInventoryUpdated(InventoryUpdatedEvent event) {
        if (!ATHRConfig.feature.storage.enabled) return;
        if (isStorageMenu(event)) {
            applyStorageMenu(event);
            return;
        }
        if (event.getInventoryId() != windowId || containerId == null) return;

        int changedSlot = event.getChangedSlot();
        if (changedSlot < 0) {
            applyFullSnapshot(event);
            return;
        }

        SContainer container = StorageData.containers.get(containerId);
        if (container == null) return;
        if (changedSlot < 9 || changedSlot >= 9 + container.slotCount) return;

        ItemStack stack = event.getInventoryItems().get(changedSlot);
        container.setStack(changedSlot - 9, stack == null ? null : stack.copy());
        StorageData.markDirty(container);
        StorageManager.markContainersDirty();
    }

    @HandleEvent
    public void onInventoryClosed(InventoryClosedEvent event) {
        if (event.getInventoryId() != windowId && event.getInventoryId() != menuWindowId) return;
        windowId = -1;
        menuWindowId = -1;
        containerId = null;
    }

    @HandleEvent(priority = HandleEvent.HIGHEST)
    public void onSend(PacketEvent.Send event) {
        if (!ATHRConfig.feature.storage.enabled || containerId == null) return;
        if (!ContainerUtils.isChestOpen()) return;

        ClickData click = PacketCompat.clickData(event.packet);
        if (click == null || click.getWindowId() != windowId) return;

        SContainer container = StorageData.containers.get(containerId);
        IInventory inventory = ContainerUtils.getLowerInventory();
        if (container == null || inventory == null) return;

        int maxSlot = Math.min(9 + container.slotCount, ContainerUtils.getInventorySize(inventory));
        for (int i = 9; i < maxSlot; i++) {
            ItemStack stack = ContainerUtils.getStackInSlot(inventory, i);
            container.setStack(i - 9, stack == null ? null : stack.copy());
        }

        StorageManager.markContainersDirty();
        StorageData.markDirty(container);
    }

    private boolean isStorageMenu(InventoryOpenEvent event) {
        return event.getInventoryId() == menuWindowId && STORAGE_MENU_TITLE.equals(event.getInventoryName());
    }

    private void applyStorageMenu(InventoryOpenEvent event) {
        StorageData.ensureLoaded();
        LinkedHashMap<String, SContainer> detected = StorageParser.parseOverlay(event.getInventoryItems(), StorageData.containers);
        if (detected.isEmpty()) return;

        StorageData.replaceContainers(detected);
        StorageManager.markContainersDirty();
        overlayInitialized = true;
    }

    private void applyFullSnapshot(InventoryOpenEvent event) {
        if (event.getInventoryId() != windowId || containerId == null) return;

        SContainer container = StorageData.containers.get(containerId);
        if (container == null) return;

        applyWindowSize(container, event.getInventorySize());

        container.slots.clear();
        for (int i = 0; i < container.slotCount; i++) {
            ItemStack stack = event.getInventoryItems().get(9 + i);
            if (stack != null) container.setStack(i, stack.copy());
        }

        StorageData.markDirty(container);
        StorageManager.markContainersDirty();
    }

    private void applyWindowSize(SContainer container, int windowSize) {
        if (container == null) return;

        int contentSlots = StorageUtils.contentSlots(windowSize);
        if (contentSlots <= 0 || contentSlots == container.slotCount) return;

        container.slotCount = contentSlots;
        container.renderH = StorageUtils.renderHeightForSlotCount(contentSlots);
        container.slots.keySet().removeIf(index -> index >= contentSlots);

        StorageData.markDirty(container);
        StorageManager.markContainersDirty();
        StorageManager.markLayoutDirty();
    }

    private SContainer ensureContainer(StorageParser.StorageTarget target) {
        SContainer existing = StorageData.containers.get(target.id);
        if (existing != null) return existing;

        int slotCount = target.slotCount > 0
                ? target.slotCount
                : StorageUtils.initialSlotCount(target.type, StorageData.containers.values());
        SContainer container = new SContainer(new HashMap<>(), target.id, target.page, target.type, false, 307,
                StorageUtils.renderHeightForSlotCount(slotCount), slotCount, false);
        StorageData.containers.put(target.id, container);
        StorageData.markDirty(container);
        return container;
    }

    @HandleEvent
    public void onGuiOpen(ASMGuiOpenEvent event) {
        if (!ATHRConfig.feature.storage.enabled) return;

        if (event.gui == null) {
            handleGuiClose();
            return;
        }

        if (!ContainerUtils.isChestOpen(event.gui)) {
            if (!switchingContainer) {
                resetOverlayState();
                StorageManager.closeOverlay();
            }
            return;
        }

        String title = ContainerUtils.getContainerName(event.gui);
        handleStorageGuiOpen(title);
    }

    private void handleGuiClose() {
        if (!switchingContainer) {
            resetOverlayState();
            StorageManager.closeOverlay();
        }
    }

    private void resetOverlayState() {
        shouldRenderOverlay = false;
        overlayInitialized = false;
    }

    private void handleStorageGuiOpen(String title) {
        if (title == null) return;

        switch (getStorageGuiType(title)) {
            case STORAGE_MENU:
                shouldRenderOverlay = true;
                switchingContainer = false;
                break;
            case STORAGE_CONTAINER:
                StorageData.ensureLoaded();
                shouldRenderOverlay = true;
                overlayInitialized = true;
                switchingContainer = false;
                break;
            case OTHER:
                if (!switchingContainer) {
                    resetOverlayState();
                    StorageManager.closeOverlay();
                }
                break;
        }
    }

    private StorageGuiType getStorageGuiType(String title) {
        if (title.equals(STORAGE_MENU_TITLE)) {
            return StorageGuiType.STORAGE_MENU;
        } else if (StorageParser.isStorageContainer(title)) {
            return StorageGuiType.STORAGE_CONTAINER;
        }
        return StorageGuiType.OTHER;
    }

    @HandleEvent
    public void onMouseInput(ASMMouseEvent event) {
        if (!shouldRenderOverlay || !overlayInitialized) return;
        if (!ATHRConfig.feature.storage.enabled) return;
        if (!ContainerUtils.isChestOpen(event.gui)) return;
        if (!StorageManager.isStorageChest()) return;
        if (StorageManager.isTransitioning()) return;

        GuiChest guiChest = (GuiChest) event.gui;
        int mouseX = event.mouseX, mouseY = event.mouseY;

        if (handleScrollInput(event)) {
            event.cancel();
            return;
        }

        if (handleClickInput(event, mouseX, mouseY, guiChest)) {
            event.cancel();
        }
    }

    private boolean handleScrollInput(ASMMouseEvent event) {
        int dWheel = event.dWheel;
        if (dWheel != 0) {
            if (TooltipHandler.isScaleKeyHeld()) return false;

            // Don't scroll overlay if shift is held (for item moving)
            if (KeyboardCompat.isKeyDown(Keyboard.KEY_LSHIFT) ||
                    KeyboardCompat.isKeyDown(Keyboard.KEY_RSHIFT)) {
                return false;
            }

            // Only scroll if mouse is over the storage overlay area
            if (StorageManager.isMouseOverStorageArea(event.mouseX, event.mouseY)) {
                StorageManager.handleMouseInput(event.mouseX, event.mouseY, event.button, event.buttonState, event.dWheel);
                return true;
            }
        }
        return false;
    }

    private boolean handleClickInput(ASMMouseEvent event, int mouseX, int mouseY, GuiChest guiChest) {
        int button = event.button;
        if (button != 0 && button != 1) return false;

        if (isClickingPlayerInventory(mouseX, mouseY) || isClickingActiveContainerSlots(mouseX, mouseY, guiChest)) {
            return false;
        }

        StorageManager.handleMouseInput(mouseX, mouseY, button, event.buttonState, event.dWheel);
        return true;
    }

    private boolean isClickingPlayerInventory(int mouseX, int mouseY) {
        return StorageManager.isClickingPlayerInventory(mouseX, mouseY);
    }

    private boolean isClickingActiveContainerSlots(int mouseX, int mouseY, GuiChest guiChest) {
        StorageRenderer r = StorageManager.getRenderer();
        if (r == null) return false;
        // Early exit if mouse isn't even over the storage area
        if (!StorageManager.isMouseOverStorageArea(mouseX, mouseY)) return false;
        ContainerChest chest = ContainerUtils.getOpenChest(guiChest);
        if (chest == null) return false;

        int slotCount = InventoryCompat.getContainerSize(chest);
        for (int i = 0; i < slotCount; i++) {
            Slot slot = InventoryCompat.getSlot(chest, i);
            if (ContainerUtils.isPlayerInventory(slot.inventory)) continue;
            if (r.isMouseOverActiveContainerSlot(slot, mouseX, mouseY)) return true;
        }
        return false;
    }

    @HandleEvent
    public void onKeyboardInput(ASMKeyEvent event) {
        if (!shouldRenderOverlay || !overlayInitialized) return;
        if (!ATHRConfig.feature.storage.enabled) return;
        if (!ContainerUtils.isChestOpen(event.gui)) return;
        if (!StorageManager.isStorageChest()) return;

        int keyCode = KeyboardCompat.getEventKey();
        if (KeybindCompat.consumeInventoryKey(keyCode, StorageManager.isSearchFocused(),
                () -> StorageManager.handleKeyTyped(KeyboardCompat.getEventCharacter(), keyCode))) {
            event.cancel();
            return;
        }
        if (keyCode == Keyboard.KEY_ESCAPE) return;
        if (StorageManager.isTransitioning()) return;
        if (!KeyboardCompat.getEventKeyState()) return;

        char typedChar = KeyboardCompat.getEventCharacter();

        if (StorageManager.handleKeyTyped(typedChar, keyCode)) {
            event.cancel();
        }
    }

    @HandleEvent
    public void onDrawScreen(ASMGuiDrawEvent event) {
        if (!shouldRenderOverlay || !overlayInitialized) return;
        if (!ATHRConfig.feature.storage.enabled) return;
        if (!ContainerUtils.isChestOpen(event.gui)) return;
        if (!StorageManager.isStorageChest()) return;

        StorageManager.renderOverlay(event.mouseX, event.mouseY);
        ItemRenderUtils.renderHeldCursorItem();
    }

    @HandleEvent
    public void onRenderGameOverlay(ASMRenderOverlayEvent event) {
        if (event.type != 0) return;
        if (!ATHRConfig.feature.storage.enabled) return;
        if (!switchingContainer || !overlayInitialized || !StorageManager.isOverlayActive()) return;
        if (MinecraftCompat.getCurrentScreen() != null) return;

        int width = GuiScreenUtils.getScaledWindowWidth();
        int height = GuiScreenUtils.getScaledWindowHeight();

        // Keep the background dim during container switch so the screen never flashes un-dimmed
        GlStateManagerCompat.disableLighting();
        GlStateManagerCompat.disableFog();
        GlStateManagerCompat.enableBlend();
        GlStateManagerCompat.tryBlendFuncSeparate(770, 771, 1, 0);
        //#if MC < 1.21
        RenderUtils.drawGradientRect(0, 0, 0, width, height, -1072689136, -804253680);
        //#else
        //$$ RenderUtils.drawGradientRect(0, 0, 0, width, height, 0x40000000, 0x40000000);
        //#endif
        GlStateManagerCompat.disableBlend();

        int[] mouse = KeybindHelper.getMouseCoords(width, height);
        int mouseX = mouse[0], mouseY = mouse[1];
        StorageManager.renderOverlay(mouseX, mouseY);
    }

    private enum StorageGuiType {
        STORAGE_MENU, STORAGE_CONTAINER, OTHER
    }
}
