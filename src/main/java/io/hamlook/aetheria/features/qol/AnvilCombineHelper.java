package io.hamlook.aetheria.features.qol;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.events.ASMGuiInitEvent;
import io.hamlook.aetheria.events.ASMGuiInitPreEvent;
import io.hamlook.aetheria.events.ASMTickEvent;
import io.hamlook.aetheria.events.SlotClickEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.ContainerUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.item.ItemUtils;
import io.hamlook.aetheria.utils.render.HighlightUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@RegisterEvents
public class AnvilCombineHelper {

    private static final int SLOT_LEFT = 29;
    private static final int SLOT_RIGHT = 33;
    private static final int HIGHLIGHT_COLOR = 0x8000FF00;

    private static final String ANVIL_TITLE = "Anvil";
    private static final Set<Integer> highlightedSlots = Collections.synchronizedSet(new HashSet<>());
    private static String leftId = null;
    private static String rightId = null;
    private static boolean pendingRefresh = false;

    static {
        HighlightUtils.registerHighlighter((gui, slot) -> {
            if (isEnabled()) return null;
            if (isAnvilGui(gui)) return null;
            if (!highlightedSlots.contains(slot.slotNumber)) return null;
            return HIGHLIGHT_COLOR;
        });
    }

    private static boolean isEnabled() {
        return ATHRConfig.feature == null || !ATHRConfig.feature.qol.anvilCombineHelper;
    }

    private static boolean isAnvilGui(GuiContainer gui) {
        ContainerChest cc = ContainerUtils.getOpenChest(gui);
        if (cc == null) return true;
        String title = ContainerUtils.getTitle(cc);
        return !ANVIL_TITLE.equals(title);
    }

    private static void refreshSlots(ContainerChest container) {
        String newLeft = idFromContainerSlot(container, SLOT_LEFT);
        String newRight = idFromContainerSlot(container, SLOT_RIGHT);

        if (equals(newLeft, leftId) && equals(newRight, rightId)) return;

        leftId = newLeft;
        rightId = newRight;

        updateHighlights(container);
    }

    private static void updateHighlights(ContainerChest container) {
        highlightedSlots.clear();

        boolean hasLeft = leftId != null;
        boolean hasRight = rightId != null;
        if (hasLeft == hasRight) return;

        String targetId = hasLeft ? leftId : rightId;

        int chestSize = ContainerUtils.getLowerInventory(container).getSizeInventory();
        for (Slot slot : container.inventorySlots) {
            if (slot.slotNumber < chestSize) continue;
            ItemStack stack = slot.getStack();
            if (stack == null) continue;
            if (targetId.equals(idFromStack(stack))) {
                highlightedSlots.add(slot.slotNumber);
            }
        }
    }

    private static String idFromStack(ItemStack stack) {
        if (stack == null) return null;
        String internal = ItemUtils.getInternalName(stack);
        String id = internal.equals("ENCHANTED_BOOK")
                ? ItemUtils.getEffectiveItemId(stack)
                : internal;
        return id.isEmpty() ? null : id;
    }

    private static String idFromContainerSlot(ContainerChest container, int index) {
        for (Slot slot : container.inventorySlots) {
            if (slot.slotNumber == index) {
                ItemStack stack = slot.getStack();
                if (stack == null) return null;
                return idFromStack(stack);
            }
        }
        return null;
    }

    private static boolean equals(String a, String b) {
        return Objects.equals(a, b);
    }

    @HandleEvent
    public void onSlotClick(SlotClickEvent event) {
        if (isEnabled() || isAnvilGui(event.getGui())) return;
        pendingRefresh = true;
    }

    @HandleEvent
    public void onTick(ASMTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!pendingRefresh) return;
        pendingRefresh = false;

        Minecraft mc = MinecraftCompat.getMinecraft();
        ContainerChest chest = ContainerUtils.getOpenChest();
        if (chest == null) return;
        if (isAnvilGui((GuiContainer) MinecraftCompat.getCurrentScreen())) return;

        refreshSlots(chest);
    }

    @HandleEvent
    public void onGuiOpen(ASMGuiInitEvent event) {
        ContainerChest chest = ContainerUtils.getOpenChest(event.gui);
        if (chest == null) return;
        if (isAnvilGui((GuiContainer) event.gui)) return;
        refreshSlots(chest);
    }

    @HandleEvent
    public void onGuiClose(ASMGuiInitPreEvent event) {
        if (!ContainerUtils.isChestOpen(event.gui)) return;
        leftId = null;
        rightId = null;
        highlightedSlots.clear();
    }
}