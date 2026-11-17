package io.hamlook.aetheria.utils.hooks;

import io.hamlook.aetheria.features.farming.visitors.VisitorTooltips;
import io.hamlook.aetheria.features.storage.StorageManager;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import net.minecraft.item.ItemStack;

import java.util.List;

public final class GuiScreenHook {

    private GuiScreenHook() {
    }

    /**
     * Takes over the vanilla item tooltip for the stack under the cursor.
     * Returns true when the caller must cancel the vanilla draw, either
     * because an overlay replaces it or because a replacement was drawn.
     */
    public static boolean handleTooltip(ItemStack stack, int x, int y) {
        if (shouldCancelStorageTooltip()) {
            return true;
        }
        return stack != null && visitorTooltip(stack, x, y);
    }

    public static boolean shouldCancelStorageTooltip() {
        return StorageManager.isOverlayActive() && StorageManager.isStorageChest();
    }

    public static boolean visitorTooltip(ItemStack stack, int x, int y) {
        List<String> lines = VisitorTooltips.replaceToolTip(stack);
        if (lines == null) return false;
        TextRenderUtils.drawHoveringText(lines, x, y, MinecraftCompat.getFontRenderer());
        return true;
    }
}
