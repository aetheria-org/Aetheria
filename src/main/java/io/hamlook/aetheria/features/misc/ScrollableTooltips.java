package io.hamlook.aetheria.features.misc;

import io.hamlook.aetheria.features.storage.StorageManager;
import io.hamlook.aetheria.init.RegisterEvents;
import net.minecraft.item.ItemStack;
import io.hamlook.aetheria.api.event.HandleEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import io.hamlook.aetheria.events.ASMTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@RegisterEvents
public class ScrollableTooltips {

    public static int scrollOffset = 0;
    private static long lastScrollTime = 0;
    private static ItemStack lastHoveredItem = null;

    private static void checkHoveredItemChange() {
        if (!StorageManager.isOverlayActive() || StorageManager.getRenderer() == null) {
            if (lastHoveredItem != null) {
                scrollOffset = 0;
                lastHoveredItem = null;
            }
            return;
        }

        ItemStack currentHovered = StorageManager.getRenderer().getHoveredItem();

        if (currentHovered != lastHoveredItem) {
            if (currentHovered == null || lastHoveredItem == null || !ItemStack.areItemStacksEqual(currentHovered, lastHoveredItem)) {
                scrollOffset = 0;
            }
            lastHoveredItem = currentHovered;
        }
    }

    private static void handleScroll() {
        if (!shouldAllowScrolling()) {
            return;
        }

        int wheel = Mouse.getDWheel();
        if (wheel != 0) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastScrollTime > 50) { // Prevent too rapid scrolling
                scrollOffset += wheel > 0 ? -10 : 10;

                lastScrollTime = currentTime;
            }
        }
    }

    public static void resetScroll() {
        scrollOffset = 0;
        lastHoveredItem = null;
    }

    private static boolean shouldAllowScrolling() {
        if (!StorageManager.isOverlayActive() || StorageManager.getRenderer() == null) {
            return false;
        }

        // Only allow scrolling if hovering over an item
        if (lastHoveredItem == null) {
            return false;
        }

        // Storage inventory tooltips scroll without shift
        // Storage overlay container tooltips require shift
        if (StorageManager.getRenderer().isHoveredItemFromInventory()) {
            return true;
        } else {
            return Keyboard.isKeyDown(Keyboard.KEY_LSHIFT);
        }
    }

    @HandleEvent
    public void onClientTick(ASMTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        handleScroll();
        checkHoveredItemChange();
    }
}
