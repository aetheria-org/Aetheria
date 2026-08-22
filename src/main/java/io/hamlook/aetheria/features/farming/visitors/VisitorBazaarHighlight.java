package io.hamlook.aetheria.features.farming.visitors;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.farming.FarmingApi;
import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.item.ItemUtils;
import io.hamlook.aetheria.utils.render.HighlightUtils;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public final class VisitorBazaarHighlight {

    private static final int HIGHLIGHT_COLOR = 0x8055FF55;
    private static boolean registered = false;

    private VisitorBazaarHighlight() {
    }

    public static void register() {
        if (registered) return;
        registered = true;
        HighlightUtils.registerHighlighter(VisitorBazaarHighlight::colorFor);
    }

    private static boolean matches(GuiContainer gui, Slot slot) {
        if (ATHRConfig.feature == null) return false;
        if (!FarmingApi.isSearchFresh(60_000L)) return false;
        if (!(gui.inventorySlots instanceof ContainerChest)) return false;
        ContainerChest chest = (ContainerChest) gui.inventorySlots;

        IInventory lower = chest.getLowerChestInventory();
        if (slot.inventory != lower) return false;

        // Relevance gate: only chests that belong to the ordering flow
        // ("Search: <item>", product "<item>", "<item> ➜ Instant Buy").
        String title = ColorUtils.stripColor(lower.getDisplayName().getUnformattedText()).trim();
        if (!title.startsWith("Search:") && !VisitorShoppingList.nameMatchesFlow(title)) {
            return false;
        }

        ItemStack stack = slot.getStack();
        if (stack == null) return false;
        boolean searchChest = title.startsWith("Search:");
        return slotMatches(stack, searchChest);
    }

    private static boolean slotMatches(ItemStack stack, boolean searchChest) {
        String searched = FarmingApi.getSearchedItemName();
        if (searched.isEmpty()) return false;
        String name = ColorUtils.stripColor(stack.getDisplayName()).trim();

        if (searchChest && name.equals(searched)) return true;

        String lore0 = firstLoreLine(stack);
        if (name.equals("Buy Instantly") && searched.equals(lore0)) return true;

        return "Custom Amount".equals(name) && "Instant Buy Quantity".equals(lore0);
    }

    /** First non-empty color-stripped lore line, or null. */
    private static String firstLoreLine(ItemStack stack) {
        for (String raw : ItemUtils.getLoreLinesWithoutColor(stack)) {
            String line = raw.trim();
            if (!line.isEmpty()) return line;
        }
        return null;
    }

    private static Integer colorFor(GuiContainer gui, Slot slot) {
        return matches(gui, slot) ? HIGHLIGHT_COLOR : null;
    }
}
