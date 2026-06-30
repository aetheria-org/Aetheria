package io.hamlook.aetheria.features.storage;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.farming.mouse.LockMouse;
import io.hamlook.aetheria.features.storage.data.StorageData;
import io.hamlook.aetheria.features.storage.render.StorageRenderer;
import io.hamlook.aetheria.features.storage.utils.SContainer;
import io.hamlook.aetheria.features.storage.utils.StorageListener;
import io.hamlook.aetheria.features.storage.utils.StorageParser;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.KeybindHelper;
import io.hamlook.aetheria.utils.ContainerUtils;
import io.hamlook.aetheria.utils.SoundUtils;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.ContainerChest;

import java.util.LinkedHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


@RegisterEvents
public class StorageManager {

    private static final long TRANSITION_TIMEOUT = 5000;
    private static final long SWITCH_COOLDOWN_MS = 1500;
    private static long lastSuccessfulOpenTime = 0;
    @Getter
    private static String activeContainerId = null;
    @Getter
    private static StorageRenderer renderer = null;
    private static boolean overlayActive = false;
    private static long transitionStartTime = 0;
    @Getter
    private static boolean isTransitioning = false;
    private static boolean overlayLockedMouse = false;
    private static boolean switchInitiatedFromOverlay = false;
    private static long lastValidChestTime = 0;
    private static final ScheduledExecutorService COMMAND_EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "Storage-Cmd"));

    public static void setActiveContainer(String containerId) {
        // If this was an overlay-initiated switch, record successful open time
        if (switchInitiatedFromOverlay) {
            lastSuccessfulOpenTime = System.currentTimeMillis();
        }

        activeContainerId = containerId;

        // Only auto-scroll if enabled in config and switch was NOT initiated from overlay
        if (ATHRConfig.feature.storage.autoScrollToActive && renderer != null && containerId != null && !switchInitiatedFromOverlay) {
            renderer.requestScrollToActive();
        }

        // Reset the flag
        switchInitiatedFromOverlay = false;

        endTransition();
    }

    private static void endTransition() {
        if (overlayLockedMouse) {
            setMouseLockedSilent(false);
            overlayLockedMouse = false;
        }
        isTransitioning = false;
    }

    /**
     * Sets the mouse lock state without printing the "Mouse locked/unlocked" chat message.
     */
    private static void setMouseLockedSilent(boolean locked) {
        if (ATHRConfig.feature == null) return;
        ATHRConfig.feature.farming.lockMouseConfig.lockMouse = locked;
        ATHRConfig.saveConfig();
    }

    public static boolean isOverlayActive() {
        return overlayActive;
    }

    public static boolean initializeOverlay(ContainerChest parser) {
        if (StorageData.containers.isEmpty()) {
            StorageData.loadContainers();
        }

        LinkedHashMap<String, SContainer> containers = StorageParser.parseOverlay(parser, StorageData.containers);

        if (containers.isEmpty()) {
            return false;
        }

        StorageData.containers = containers;

        lastValidChestTime = System.currentTimeMillis();
        renderer = new StorageRenderer(containers);
        overlayActive = true;

        // Request scroll to active container
        if (ATHRConfig.feature.storage.autoScrollToActive && activeContainerId != null) {
            renderer.requestScrollToActive();
        }

        return true;
    }

    public static void renderOverlay(int mouseX, int mouseY) {
        if (ContainerUtils.isChestOpen()) {
            lastValidChestTime = System.currentTimeMillis();
        }
        if (renderer == null && !StorageData.containers.isEmpty()) {
            renderer = new StorageRenderer(StorageData.containers);
            overlayActive = true;
            lastValidChestTime = System.currentTimeMillis();


        }

        // Check if overlay has been active without a storage container for too long
        if (overlayActive && !ContainerUtils.isChestOpen()) {
            long elapsed = System.currentTimeMillis() - lastValidChestTime;

            if (elapsed > TRANSITION_TIMEOUT) {
                System.out.println("[ATHR DEBUG] No valid chest GUI for " + TRANSITION_TIMEOUT / 1000 + "s - closing overlay");
                closeOverlay();
                return;
            }
        }

        if (isTransitioning) {
            long elapsed = System.currentTimeMillis() - transitionStartTime;
            if (elapsed > TRANSITION_TIMEOUT) {
                System.out.println("[ATHR DEBUG] Transition timeout - closing overlay");
                closeOverlay();
                return;
            }
        }

        if (renderer != null && overlayActive) {
            renderer.render(mouseX, mouseY);
        }
    }


    public static boolean isMouseOverStorageArea(int mouseX, int mouseY) {
        if (renderer == null) return false;
        return renderer.isMouseOverStorageArea(mouseX, mouseY);
    }

    public static void handleMouseInput() {
        if (renderer == null) return;

        int dWheel = org.lwjgl.input.Mouse.getEventDWheel();
        if (dWheel != 0) {
            renderer.handleScroll(dWheel);
        }

        if (org.lwjgl.input.Mouse.getEventButtonState()) {
            int mouseButton = org.lwjgl.input.Mouse.getEventButton();
            if (mouseButton == 0) {
                net.minecraft.client.gui.ScaledResolution sr = new net.minecraft.client.gui.ScaledResolution(Minecraft.getMinecraft());
                int[] mouse = KeybindHelper.getMouseCoords(sr);
                int mouseX = mouse[0], mouseY = mouse[1];

                renderer.handleClick(mouseX, mouseY);
            }
        }
    }


    public static boolean handleKeyTyped(char typedChar, int keyCode) {
        if (renderer == null) return false;
        return renderer.handleKeyTyped(typedChar, keyCode);
    }

    public static void switchToContainer(String containerId) {
        SContainer container = StorageData.containers.get(containerId);
        if (container == null) return;

        if (isSwitchOnCooldown()) {
            SoundUtils.playSound("note.pling");
            return;
        }


        // Mark that this switch was initiated from the overlay
        switchInitiatedFromOverlay = true;

        isTransitioning = true;
        transitionStartTime = System.currentTimeMillis();
        if (!LockMouse.isLocked()) {
            setMouseLockedSilent(true);
            overlayLockedMouse = true;
        }

        StorageListener.setSwitchingContainer(true);
        Minecraft.getMinecraft().thePlayer.closeScreen();

        String command = buildContainerCommand(container);
        executeCommandDelayed(command);
    }

    private static String buildContainerCommand(SContainer container) {
        switch (container.type) {
            case ECHEST:
                return "/echest " + container.page;
            case BAG:
                return "/storage " + container.page;
            default:
                throw new IllegalArgumentException("Unknown container type: " + container.type);
        }
    }

    private static void executeCommandDelayed(String command) {
        COMMAND_EXECUTOR.schedule(() -> {
            Minecraft.getMinecraft().addScheduledTask(() -> Minecraft.getMinecraft().thePlayer.sendChatMessage(command));
        }, 100, TimeUnit.MILLISECONDS);
    }

    public static boolean isClickingPlayerInventory(int mouseX, int mouseY) {
        if (renderer == null) return false;
        return renderer.isClickingPlayerInventory(mouseX, mouseY);
    }

    public static boolean isSwitchOnCooldown() {
        return System.currentTimeMillis() - lastSuccessfulOpenTime < SWITCH_COOLDOWN_MS;
    }

    public static void closeOverlay() {
        StorageData.saveContainers();
        activeContainerId = null;
        renderer = null;
        overlayActive = false;
        lastValidChestTime = 0;
        if (overlayLockedMouse) {
            setMouseLockedSilent(false);
            overlayLockedMouse = false;
        }
        isTransitioning = false;
    }

    public static void markContainersDirty() {
        if (renderer != null) {
            renderer.markDirty();
        }
    }

    public static void overrideIsMouseOverSlot(net.minecraft.inventory.Slot slotIn, int mouseX, int mouseY, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if (!isOverlayActive() || renderer == null) return;
        if (!ContainerUtils.isChestOpen()) return;

        boolean isPlayerSlot = slotIn.inventory == Minecraft.getMinecraft().thePlayer.inventory;

        if (isPlayerSlot) {
            cir.setReturnValue(renderer.isMouseOverPlayerInventorySlot(slotIn, mouseX, mouseY));
        } else {
            cir.setReturnValue(renderer.isMouseOverActiveContainerSlot(slotIn, mouseX, mouseY));
        }
    }
}