package io.hamlook.aetheria.utils.compat;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.events.PacketEvent;
import io.hamlook.aetheria.events.SlotClickEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.ContainerUtils;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.network.play.client.C0EPacketClickWindow;

/**
 * Observational fallback for raw {@code C0EPacketClickWindow}s that bypass
 * {@code GuiContainer.handleMouseClick} (currently triggered by NEF's
 * "Middle Click Chests" in whitelisted menus such as the Pets chest).
 * The GUI path in {@code MixinGuiContainer} remains the authoritative,
 * cancellable {@code SlotClickEvent} source; this handler posts only when
 * the GUI path did not fire for the same click, deduplicated via a
 * single-slot marker consumed on exact match within 1.5 s.
 */
@RegisterEvents
public class NefSlotClickCompat {

    private static final long MARKER_TTL_MS = 1500L;

    private static int lastWindowId = -1;
    private static int lastSlotId = -1;
    private static int lastButton;
    private static int lastClickType;
    private static long lastRecordMs;

    private static boolean posting;

    public static void recordGuiPosted(int slotId, int clickedButton, int clickType) {
        if (slotId < 0) return;
        int windowId = ContainerUtils.getWindowId();
        if (windowId == -1) return;
        lastWindowId = windowId;
        lastSlotId = slotId;
        lastButton = clickedButton;
        lastClickType = clickType;
        lastRecordMs = System.currentTimeMillis();
    }

    private static boolean consumeMatchingGuiClick(C0EPacketClickWindow packet) {
        if (System.currentTimeMillis() - lastRecordMs > MARKER_TTL_MS) return false;
        if (packet.getWindowId() == lastWindowId && packet.getSlotId() == lastSlotId && packet.getUsedButton() == lastButton && packet.getMode() == lastClickType) {
            lastRecordMs = 0;
            return true;
        }
        return false;
    }

    @HandleEvent
    public void onPacketSend(PacketEvent.Send event) {
        if (posting) return;
        if (!(event.packet instanceof C0EPacketClickWindow)) return;
        C0EPacketClickWindow packet = (C0EPacketClickWindow) event.packet;
        if (consumeMatchingGuiClick(packet)) return;
        if (!ContainerUtils.isGuiContainerOpen()) return;
        int guiWindowId = ContainerUtils.getWindowId();
        if (guiWindowId == -1 || guiWindowId != packet.getWindowId()) return;
        int slotId = packet.getSlotId();
        if (slotId < 0) return;
        ContainerChest chest = ContainerUtils.getOpenChest();
        if (chest == null) return;
        Slot slot;
        try {
            slot = chest.getSlot(slotId);
        } catch (IndexOutOfBoundsException e) {
            return;
        }
        if (slot == null) return;
        GuiContainer gui = (GuiContainer) MinecraftCompat.getMinecraft().currentScreen;
        posting = true;
        try {
            new SlotClickEvent(gui, slot, slotId, packet.getUsedButton(), SlotClickEvent.ClickType.fromId(packet.getMode())).post();
        } finally {
            posting = false;
        }
    }
}
