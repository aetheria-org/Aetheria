package io.hamlook.aetheria.features.qol;

import io.hamlook.aetheria.DebugLogger;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.config.gui.GuiTextures;
import io.hamlook.aetheria.utils.data.SkyblockData;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;


public final class BetterContainers {

    private static final BetterContainers INSTANCE = new BetterContainers();

    private DynamicTexture dynamicTexture = null;
    @Getter
    private boolean loaded = false;
    private int lastInvIdentity = -1;
    private int lastLoadedStyle  = -1;
    private BufferedImage imgBase = null;
    private BufferedImage imgSlot = null;
    private boolean imagesLoaded = false;

    private BetterContainers() {}

    public static BetterContainers getInstance() { return INSTANCE; }

    public static boolean isEnabled() {
        return ATHRConfig.feature != null
                && ATHRConfig.feature.qol.betterContainers.enabled
                && SkyblockData.isOnSkyblock();
    }

    private static boolean isChestOpen() {
        return Minecraft.getMinecraft().currentScreen instanceof GuiChest;
    }

    private static int currentStyle() {
        if (ATHRConfig.feature == null) return 0;
        int s = ATHRConfig.feature.qol.betterContainers.style;
        return Math.max(0, Math.min(GuiTextures.BETTER_CONTAINERS_STYLE_COUNT - 1, s));
    }

    public boolean tryBindTexture(TextureManager tm, ResourceLocation original) {
        if (!isEnabled() || !isChestOpen()) return false;

        Minecraft mc = Minecraft.getMinecraft();
        ContainerChest cc = (ContainerChest) ((GuiChest) mc.currentScreen).inventorySlots;
        IInventory lower = cc.getLowerChestInventory();
        int identity = System.identityHashCode(lower);
        int style    = currentStyle();

        if (!loaded || identity != lastInvIdentity || style != lastLoadedStyle) {
            lastInvIdentity = identity;
            regenerate(lower);
        }

        if (!loaded || dynamicTexture == null) {
            return false;
        }

        mc.getTextureManager().loadTexture(GuiTextures.BETTER_CONTAINERS_DYNAMIC, dynamicTexture);
        mc.getTextureManager().bindTexture(GuiTextures.BETTER_CONTAINERS_DYNAMIC);
        return true;
    }

    public void reset() {
        loaded          = false;
        lastInvIdentity = -1;
        dynamicTexture  = null;
    }


    public static boolean isBlankPane(ItemStack stack) {
        if (stack == null) return false;
        if (stack.getItem() != Item.getItemFromBlock(Blocks.stained_glass_pane)) return false;
        if (stack.getItemDamage() != 15) return false;
        String name = stack.getDisplayName();
        if (name == null) return true;
        return StringUtils.stripControlCodes(name).trim().isEmpty();
    }


    private void loadImages() {
        int style = currentStyle();
        if (imagesLoaded && style == lastLoadedStyle) return;
        imagesLoaded = false;
        try {
            imgBase = read(GuiTextures.betterContainersBg(style));
            imgSlot = read(GuiTextures.betterContainersSlot(style));
            imagesLoaded = imgBase != null && imgSlot != null;
            lastLoadedStyle = style;
        } catch (Exception e) {
            DebugLogger.log("[BetterContainers] Failed to load images for style "
                    + (style + 1) + ": " + e.getMessage());
        }
    }

    private static BufferedImage read(ResourceLocation rl) throws IOException {
        try (InputStream is = Minecraft.getMinecraft().getResourceManager().getResource(rl).getInputStream()) {
            return ImageIO.read(is);
        }
    }

    private void regenerate(IInventory lower) {
        loadImages();
        if (!imagesLoaded) { loaded = false; return; }

        int size = lower.getSizeInventory();
        int cols = 9, rows = size / cols;

        boolean[][] real  = new boolean[cols][rows];
        boolean anyBlank  = false;
        for (int i = 0; i < size; i++) {
            ItemStack stack = lower.getStackInSlot(i);
            boolean blank   = isBlankPane(stack);
            real[i % cols][i / cols] = !blank;
            if (blank) anyBlank = true;
        }

        if (!anyBlank) {
            loaded = false;
            return;
        }

        try {
            int w     = imgBase.getWidth();
            int h     = imgBase.getHeight();
            int hMult = w / 256;
            int vMult = h / 256;

            BufferedImage out = new BufferedImage(
                    imgBase.getColorModel(),
                    imgBase.copyData(null),
                    imgBase.isAlphaPremultiplied(),
                    null);

            for (int i = 0; i < size; i++) {
                int xi = i % cols, yi = i / cols;
                if (!real[xi][yi]) continue; // blank pane, keep background

                boolean up    = yi > 0           && real[xi][yi - 1];
                boolean right = xi < cols - 1    && real[xi + 1][yi];
                boolean down  = yi < rows - 1    && real[xi][yi + 1];
                boolean left  = xi > 0           && real[xi - 1][yi];
                boolean ul    = xi > 0           && yi > 0           && real[xi - 1][yi - 1];
                boolean ur    = xi < cols - 1    && yi > 0           && real[xi + 1][yi - 1];
                boolean dr    = xi < cols - 1    && yi < rows - 1    && real[xi + 1][yi + 1];
                boolean dl    = xi > 0           && yi < rows - 1    && real[xi - 1][yi + 1];

                int ctm  = getCTMIndex(up, right, down, left, ul, ur, dr, dl);
                int srcX = (ctm % 12) * 19 * hMult;
                int srcY = (ctm / 12) * 19 * vMult;
                int dstX = 7 * hMult  + xi * 18 * hMult;
                int dstY = 17 * vMult + yi * 18 * vMult;

                int[] pixels = imgSlot.getRGB(srcX, srcY, 18 * hMult, 18 * vMult, null, 0, 18 * hMult);
                out.setRGB(dstX, dstY, 18 * hMult, 18 * vMult, pixels, 0, 18 * hMult);
            }

            if (dynamicTexture != null) {
                out.getRGB(0, 0, w, h, dynamicTexture.getTextureData(), 0, w);
                dynamicTexture.updateDynamicTexture();
            } else {
                dynamicTexture = new DynamicTexture(out);
            }
            loaded = true;

        } catch (Exception e) {
            DebugLogger.log("[BetterContainers] Texture generation failed: " + e.getMessage());
            loaded = false;
        }
    }


    public static int getCTMIndex(boolean up, boolean right, boolean down, boolean left,
                                  boolean upleft, boolean upright, boolean downright, boolean downleft) {
        if (up && right && down && left) {
            if (upleft && upright && downright && downleft) return 26;
            if (upleft && upright && downright)             return 33;
            if (upleft && upright && downleft)              return 32;
            if (upleft && upright)                          return 11;
            if (upleft && downright && downleft)            return 44;
            if (upleft && downright)                        return 35;
            if (upleft && downleft)                         return 10;
            if (upleft)                                     return 20;
            if (upright && downright && downleft)           return 45;
            if (upright && downright)                       return 23;
            if (upright && downleft)                        return 34;
            if (upright)                                    return 8;
            if (downright && downleft)                      return 22;
            if (downright)                                  return 9;
            if (downleft)                                   return 21;
            return 46;
        }
        if (up && right && down)  return (!upright && !downright) ? 6  : !upright ? 28 : !downright ? 30 : 25;
        if (up && right && left)  return (!upleft  && !upright)  ? 18 : !upleft  ? 40 : !upright   ? 42 : 38;
        if (up && right)          return upright   ? 37 : 16;
        if (up && down  && left)  return (!upleft  && !downleft) ? 19 : !upleft  ? 43 : !downleft  ? 41 : 27;
        if (up && down)           return 24;
        if (up && left)           return upleft    ? 39 : 17;
        if (up)                   return 36;
        if (right && down && left) return (!downleft && !downright) ? 7 : !downleft ? 31 : !downright ? 29 : 14;
        if (right && down)        return downright ? 13 : 4;
        if (right && left)        return 2;
        if (right)                return 1;
        if (down && left)         return downleft  ? 15 : 5;
        if (down)                 return 12;
        if (left)                 return 3;
        return 0;
    }

    public static boolean shouldRenderStack(int slotNumber, ItemStack stack) {
        return !isBlankPane(stack);
    }
}