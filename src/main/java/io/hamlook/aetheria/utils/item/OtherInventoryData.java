package io.hamlook.aetheria.utils.item;

import io.hamlook.aetheria.api.event.AetheriaEventBus;
import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.events.ASMGuiOpenEvent;
import io.hamlook.aetheria.events.ASMWorldUnloadEvent;
import io.hamlook.aetheria.events.InventoryClosedEvent;
import io.hamlook.aetheria.events.InventoryFullyOpenedEvent;
import io.hamlook.aetheria.events.InventoryOpenEvent;
import io.hamlook.aetheria.events.InventoryUpdatedEvent;
import io.hamlook.aetheria.events.PacketEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.compat.ItemCompatKt;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.PacketCompat;
import io.hamlook.aetheria.utils.compat.TextCompat;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S2DPacketOpenWindow;
import net.minecraft.network.play.server.S2EPacketCloseWindow;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import net.minecraft.network.play.server.S30PacketWindowItems;
import net.minecraft.util.IChatComponent;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tracks the server-opened container (the "other" inventory, not the player's own).
 * Fires InventoryOpenEvent when the window opens, InventoryFullyOpenedEvent when the
 * bulk contents packet arrives, InventoryUpdatedEvent for later slot changes and
 * InventoryClosedEvent when the container closes.
 */
@RegisterEvents
public final class OtherInventoryData {

    public static final OtherInventoryData INSTANCE = new OtherInventoryData();

    // a server-opened container always carries the 36 player-inventory slots last
    private static final int PLAYER_INVENTORY_SLOTS = 36;

    private int windowId = -1;
    private String name = "";
    private int size = -1;
    private boolean fullyOpened;
    private final Map<Integer, ItemStack> items = new LinkedHashMap<>();

    @HandleEvent()
    public synchronized void onPacket(PacketEvent.Receive event) {
        Object packet = event.packet;

        if (packet instanceof S2DPacketOpenWindow) {
            S2DPacketOpenWindow p = (S2DPacketOpenWindow) packet;
            //#if MC < 1.21
            open(p.getWindowId(), title(p.getWindowTitle()), p.getSlotCount());
            //#else
            //$$ open(p.getSyncId(), title(p.getName()), -1);
            //#endif
        } else if (packet instanceof S30PacketWindowItems) {
            S30PacketWindowItems p = (S30PacketWindowItems) packet;
            contents(PacketCompat.windowItemsId(p), PacketCompat.windowItemsStacks(p));
        } else if (packet instanceof S2FPacketSetSlot) {
            S2FPacketSetSlot p = (S2FPacketSetSlot) packet;
            //#if MC < 1.21
            updateSlot(p.func_149175_c(), p.func_149173_d(), p.func_149174_e());
            //#else
            //$$ updateSlot(p.getSyncId(), p.getSlot(), p.getStack());
            //#endif
        } else if (packet instanceof S2EPacketCloseWindow) {
            close();
        }
    }

    @HandleEvent
    public synchronized void onWorldUnload(ASMWorldUnloadEvent event) {
        close();
    }

    @HandleEvent
    public void onGuiOpen(ASMGuiOpenEvent event) {
        if (event.gui == null) close();
    }

    public synchronized String getInventoryName() {
        return name;
    }

    public synchronized boolean isFullyOpened() {
        return fullyOpened;
    }

    private void open(int windowId, String name, int size) {
        close();

        this.windowId = windowId;
        this.name = name;
        this.size = size;
        fullyOpened = false;
        items.clear();

        if (!AetheriaEventBus.INSTANCE.hasListeners(InventoryOpenEvent.class)) return;
        MinecraftCompat.getMinecraft().addScheduledTask(
                () -> new InventoryOpenEvent(windowId, name, size, new LinkedHashMap<>()).post());
    }

    private void contents(int windowId, ItemStack[] stacks) {
        if (windowId != this.windowId || stacks == null) return;

        if (size <= 0) size = stacks.length - PLAYER_INVENTORY_SLOTS;
        int limit = size > 0 ? Math.min(size, stacks.length) : stacks.length;

        items.clear();
        for (int i = 0; i < limit; i++) {
            ItemStack stack = stacks[i];
            if (!ItemCompatKt.isEmptyCompat(stack)) items.put(i, stack);
        }

        boolean first = !fullyOpened;
        fullyOpened = true;
        dispatch(first, -1);
    }

    private void updateSlot(int windowId, int index, ItemStack stack) {
        if (windowId != this.windowId) return;

        // a slot past the container is the player inventory, never part of the map
        if (size > 0 && index >= size) {
            if (!fullyOpened) {
                fullyOpened = true;
                dispatch(true, -1);
            }
            return;
        }

        if (ItemCompatKt.isEmptyCompat(stack)) items.remove(index);
        else items.put(index, stack);

        if (fullyOpened) dispatch(false, index);
        else if (size > 0 && items.size() == size) {
            fullyOpened = true;
            dispatch(true, -1);
        }
    }

    private void dispatch(boolean first, int changedSlot) {
        boolean postFully = first && AetheriaEventBus.INSTANCE.hasListeners(InventoryFullyOpenedEvent.class);
        boolean postUpdated = AetheriaEventBus.INSTANCE.hasListeners(InventoryUpdatedEvent.class) && !postFully;
        if (!postFully && !postUpdated) return;

        int windowId = this.windowId;
        String name = this.name;
        int size = this.size;
        Map<Integer, ItemStack> snapshot = new LinkedHashMap<>(items);

        // bulk packets arrive on the netty thread on modern; consumers expect the client thread
        MinecraftCompat.getMinecraft().addScheduledTask(() -> {
            if (postFully) new InventoryFullyOpenedEvent(windowId, name, size, snapshot).post();
            if (postUpdated) new InventoryUpdatedEvent(windowId, name, size, snapshot, changedSlot).post();
        });
    }

    private synchronized void close() {
        if (windowId == -1) return;

        int id = windowId;
        String title = name;
        reset();

        if (!AetheriaEventBus.INSTANCE.hasListeners(InventoryClosedEvent.class)) return;
        MinecraftCompat.getMinecraft().addScheduledTask(() -> new InventoryClosedEvent(id, title).post());
    }

    private synchronized void reset() {
        windowId = -1;
        name = "";
        size = -1;
        fullyOpened = false;
        items.clear();
    }

    private static String title(IChatComponent component) {
        return component == null ? "" : ColorUtils.stripColor(TextCompat.getUnformattedText(component)).trim();
    }
}
