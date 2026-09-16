package io.hamlook.aetheria.utils.hooks;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import io.hamlook.aetheria.features.qol.BetterContainers;
import io.hamlook.aetheria.features.storage.StorageManager;
import io.hamlook.aetheria.utils.compat.DrawContextUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

public final class GuiChestHook {

    private GuiChestHook() {
    }

    public static boolean redirectBindTexture(TextureManager tm, ResourceLocation location) {
        return BetterContainers.getInstance().tryBindTexture(tm, location);
    }

    public static int modifyContainerTitleColor(int original) {
        if (BetterContainers.isEnabled() && BetterContainers.getInstance().isLoaded() && ATHRConfig.feature.qol.betterContainers.style <= 1) {
            return 0;
        }
        return original;
    }

    public static void drawWatermark(GuiChest gui) {
        if (!BetterContainers.isEnabled() || !BetterContainers.getInstance().isLoaded() || ATHRConfig.feature == null)
            return;
        String label = "ASM";
        int textW = DrawContextUtils.getStringWidth(MinecraftCompat.getFontRenderer(), label);
        int x = gui.xSize - textW - 10;
        int y = 6;
        int baseColor = ChromaColour.specialToChromaRGB(ATHRConfig.feature.qol.betterContainers.watermarkColor);
        int color = ChromaColour.applyChromaShift(baseColor, x, y, ATHRConfig.feature.qol.betterContainers.watermarkChromaMode, ATHRConfig.feature.qol.betterContainers.watermarkChromaSize);
        MinecraftCompat.getFontRenderer().drawStringWithShadow(label, x, y, color);
    }

    public static boolean shouldCancelDraw() {
        return StorageManager.isOverlayActive() && StorageManager.isStorageChest();
    }
}
