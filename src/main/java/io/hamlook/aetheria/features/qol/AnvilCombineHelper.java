package io.hamlook.aetheria.features.qol;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.events.SlotClickEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.ContainerUtils;
import io.hamlook.aetheria.utils.item.ItemUtils;
import io.hamlook.aetheria.utils.render.HighlightUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
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
            if (targetId.equals(ItemUtils.getInternalName(stack))) {
                highlightedSlots.add(slot.slotNumber);
            }
        }
    }

    private static String idFromContainerSlot(ContainerChest container, int index) {
        for (Slot slot : container.inventorySlots) {
            if (slot.slotNumber == index) {
                ItemStack stack = slot.getStack();
                if (stack == null) return null;
                String id = ItemUtils.getInternalName(stack);
                return id.isEmpty() ? null : id;
            }
        }
        return null;
    }

    private static boolean equals(String a, String b) {
        return Objects.equals(a, b);
    }

    @SubscribeEvent
    public void onSlotClick(SlotClickEvent event) {
        if (isEnabled() || isAnvilGui(event.getGui())) return;
        pendingRefresh = true;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!pendingRefresh) return;
        pendingRefresh = false;

        Minecraft mc = Minecraft.getMinecraft();
        ContainerChest chest = ContainerUtils.getOpenChest();
        if (chest == null) return;
        if (isAnvilGui((GuiContainer) mc.currentScreen)) return;

        refreshSlots(chest);
    }

    @SubscribeEvent
    public void onGuiOpen(GuiScreenEvent.InitGuiEvent.Post event) {
        ContainerChest chest = ContainerUtils.getOpenChest(event.gui);
        if (chest == null) return;
        if (isAnvilGui((GuiContainer) event.gui)) return;
        refreshSlots(chest);
    }

    @SubscribeEvent
    public void onGuiClose(GuiScreenEvent.InitGuiEvent.Pre event) {
        if (!ContainerUtils.isChestOpen(event.gui)) return;
        leftId = null;
        rightId = null;
        highlightedSlots.clear();
    }
}
