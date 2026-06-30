package io.hamlook.aetheria.features.storage.utils;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.storage.StorageManager;
import io.hamlook.aetheria.utils.KeybindHelper;
import io.hamlook.aetheria.utils.ContainerUtils;
import io.hamlook.aetheria.utils.render.RenderUtils;
import io.hamlook.aetheria.features.storage.data.StorageData;
import io.hamlook.aetheria.features.storage.render.StorageRenderer;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.render.ItemRenderUtils;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ContainerChest;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

@RegisterEvents
public class StorageListener {

    @Setter
    private static boolean switchingContainer = false;
    private boolean shouldRenderOverlay = false;
    private boolean overlayInitialized = false;

    @SubscribeEvent
    public void onChatMessage(ClientChatReceivedEvent event) {
        if (!ATHRConfig.feature.storage.enabled) return;
        if (!shouldRenderOverlay || !overlayInitialized) return;

        String message = event.message.getUnformattedText();
        if (message.contains("Slow down!") || message.contains("executing commands too fast")) {
            shouldRenderOverlay = false;
            overlayInitialized = false;
            StorageManager.closeOverlay();
        }
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
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

    @SubscribeEvent
    public void onBackgroundDrawn(GuiScreenEvent.BackgroundDrawnEvent event) {
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

    @SubscribeEvent
    public void onMouseInput(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!shouldRenderOverlay || !overlayInitialized) return;
        if (!ATHRConfig.feature.storage.enabled) return;
        if (!ContainerUtils.isChestOpen(event.gui)) return;
        if (StorageManager.isTransitioning()) return;

        GuiChest guiChest = (GuiChest) event.gui;
        int[] mouse = KeybindHelper.getMouseCoords(guiChest.width, guiChest.height);
        int mouseX = mouse[0], mouseY = mouse[1];
  
         if (handleScrollInput()) {
            event.setCanceled(true);
            return;
        }

        if (handleClickInput(mouseX, mouseY, guiChest)) {
            event.setCanceled(true);
        }
    }

    private boolean handleScrollInput() {
        int dWheel = Mouse.getEventDWheel();
        if (dWheel != 0) {
            // Don't scroll overlay if shift is held (for item moving)
            if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LSHIFT) ||
                    org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RSHIFT)) {
                return false;
            }

            // Only scroll if mouse is over the storage overlay area
            GuiChest guiChest = (GuiChest) Minecraft.getMinecraft().currentScreen;
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
        int button = Mouse.getEventButton();
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
            if (slot.inventory == Minecraft.getMinecraft().thePlayer.inventory) continue;
            if (r.isMouseOverActiveContainerSlot(slot, mouseX, mouseY)) return true;
        }
        return false;
    }

    @SubscribeEvent
    public void onKeyboardInput(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (!shouldRenderOverlay || !overlayInitialized) return;
        if (!ATHRConfig.feature.storage.enabled) return;
        if (!ContainerUtils.isChestOpen(event.gui)) return;

        int keyCode = org.lwjgl.input.Keyboard.getEventKey();
        if (keyCode == org.lwjgl.input.Keyboard.KEY_ESCAPE) return;
        if (StorageManager.isTransitioning()) return;
        if (!org.lwjgl.input.Keyboard.getEventKeyState()) return;

        char typedChar = org.lwjgl.input.Keyboard.getEventCharacter();

        if (StorageManager.handleKeyTyped(typedChar, keyCode)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!shouldRenderOverlay || !overlayInitialized) return;
        if (!ATHRConfig.feature.storage.enabled) return;
        if (!ContainerUtils.isChestOpen(event.gui)) return;

        StorageManager.renderOverlay(event.mouseX, event.mouseY);
        ItemRenderUtils.renderHeldCursorItem();
    }

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (!ATHRConfig.feature.storage.enabled) return;
        if (!switchingContainer || !overlayInitialized || !StorageManager.isOverlayActive()) return;
        if (Minecraft.getMinecraft().currentScreen != null) return;

        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        int width = sr.getScaledWidth();
        int height = sr.getScaledHeight();

        // Keep the background dim during container switch so the screen never flashes un-dimmed
        net.minecraft.client.renderer.GlStateManager.disableLighting();
        net.minecraft.client.renderer.GlStateManager.disableFog();
        net.minecraft.client.renderer.GlStateManager.enableBlend();
        net.minecraft.client.renderer.GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        RenderUtils.drawGradientRect(0, 0, 0, width, height, -1072689136, -804253680);
        net.minecraft.client.renderer.GlStateManager.disableBlend();

        int[] mouse = KeybindHelper.getMouseCoords(width, height);
        int mouseX = mouse[0], mouseY = mouse[1];
        StorageManager.renderOverlay(mouseX, mouseY);
    }

    private enum StorageGuiType {
        STORAGE_MENU, STORAGE_CONTAINER, OTHER
    }
}