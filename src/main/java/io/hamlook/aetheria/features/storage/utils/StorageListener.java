package io.hamlook.aetheria.features.storage.utils;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.events.*;
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
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ContainerChest;

@RegisterEvents
public class StorageListener {

    @Setter
    private static boolean switchingContainer = false;
    private boolean shouldRenderOverlay = false;
    private boolean overlayInitialized = false;

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
                overlayInitialized = false;
                switchingContainer = false;
                break;
            case STORAGE_CONTAINER:
                if (StorageData.containers.isEmpty()) {
                    StorageData.loadContainers();
                }
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
        if (title.equals("Storage")) {
            return StorageGuiType.STORAGE_MENU;
        } else if (StorageParser.isStorageContainer(title)) {
            return StorageGuiType.STORAGE_CONTAINER;
        }
        return StorageGuiType.OTHER;
    }

    @HandleEvent
    public void onBackgroundDrawn(ASMGuiBackgroundDrawEvent event) {
        if (!shouldRenderOverlay) return;
        if (!ATHRConfig.feature.storage.enabled) return;

        if (!ContainerUtils.isInContainer(event.gui, "Storage")) return;

        ContainerChest chest = ContainerUtils.getOpenChest(event.gui);
        if (chest == null) return;

        if (!overlayInitialized) {
            boolean success = StorageManager.initializeOverlay(chest);
            if (success) {
                overlayInitialized = true;
            }
        }
    }

    @HandleEvent
    public void onMouseInput(ASMMouseEvent event) {
        if (!shouldRenderOverlay || !overlayInitialized) return;
        if (!ATHRConfig.feature.storage.enabled) return;
        if (!ContainerUtils.isChestOpen(event.gui)) return;
        if (!StorageManager.isStorageChest()) return;
        if (StorageManager.isTransitioning()) return;

        GuiChest guiChest = (GuiChest) event.gui;
        int[] mouse = KeybindHelper.getMouseCoords(guiChest.width, guiChest.height);
        int mouseX = mouse[0], mouseY = mouse[1];
  
         if (handleScrollInput()) {
            event.cancel();
            return;
        }

        if (handleClickInput(mouseX, mouseY, guiChest)) {
            event.cancel();
        }
    }

    private boolean handleScrollInput() {
        int dWheel = MouseCompat.getEventDWheel();
        if (dWheel != 0) {
            // Don't scroll overlay if shift is held (for item moving)
            if (KeyboardCompat.isKeyDown(org.lwjgl.input.Keyboard.KEY_LSHIFT) ||
                    KeyboardCompat.isKeyDown(org.lwjgl.input.Keyboard.KEY_RSHIFT)) {
                return false;
            }

            // Only scroll if mouse is over the storage overlay area
            GuiChest guiChest = (GuiChest) MinecraftCompat.getMinecraft().currentScreen;
            int[] mouse = KeybindHelper.getMouseCoords(guiChest.width, guiChest.height);
            int mouseX = mouse[0], mouseY = mouse[1];

            if (StorageManager.isMouseOverStorageArea(mouseX, mouseY)) {
                StorageManager.handleMouseInput();
                return true;
            }
        }
        return false;
    }

    private boolean handleClickInput(int mouseX, int mouseY, GuiChest guiChest) {
        int button = MouseCompat.getEventButton();
        if (button != 0 && button != 1) return false;

        if (isClickingPlayerInventory(mouseX, mouseY) || isClickingActiveContainerSlots(mouseX, mouseY, guiChest)) {
            return false;
        }

        StorageManager.handleMouseInput();
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
        for (net.minecraft.inventory.Slot slot : chest.inventorySlots) {
            if (slot == null) continue;
            if (slot.inventory == MinecraftCompat.getMinecraft().thePlayer.inventory) continue;
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
        if (keyCode == org.lwjgl.input.Keyboard.KEY_ESCAPE) return;
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
        if (MinecraftCompat.getMinecraft().currentScreen != null) return;

        ScaledResolution sr = GuiScreenUtils.getScaledResolution();
        int width = sr.getScaledWidth();
        int height = sr.getScaledHeight();

        // Keep the background dim during container switch so the screen never flashes un-dimmed
        GlStateManagerCompat.disableLighting();
        GlStateManagerCompat.disableFog();
        GlStateManagerCompat.enableBlend();
        GlStateManagerCompat.tryBlendFuncSeparate(770, 771, 1, 0);
        RenderUtils.drawGradientRect(0, 0, 0, width, height, -1072689136, -804253680);
        GlStateManagerCompat.disableBlend();

        int[] mouse = KeybindHelper.getMouseCoords(width, height);
        int mouseX = mouse[0], mouseY = mouse[1];
        StorageManager.renderOverlay(mouseX, mouseY);
    }

    private enum StorageGuiType {
        STORAGE_MENU, STORAGE_CONTAINER, OTHER
    }
}