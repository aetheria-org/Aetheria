package io.hamlook.aetheria.utils;

import io.hamlook.aetheria.utils.compat.InventoryCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.TextCompat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IChatComponent;

import javax.annotation.Nullable;

public class ContainerUtils {

    private static GuiScreen cachedScreen;
    private static String cachedContainerName;

    private ContainerUtils() {
    }

    public static boolean isChestOpen() {
        return MinecraftCompat.getCurrentScreen() instanceof GuiChest;
    }

    public static boolean isChestOpen(GuiScreen gui) {
        return gui instanceof GuiChest;
    }

    public static boolean isInventoryOpen() {
        return MinecraftCompat.getCurrentScreen() instanceof GuiInventory;
    }

    public static boolean isInventoryOpen(GuiScreen gui) {
        return gui instanceof GuiInventory;
    }

    public static boolean isGuiContainerOpen() {
        return MinecraftCompat.getCurrentScreen() instanceof GuiContainer;
    }

    @Nullable
    public static ContainerChest getOpenChest() {
        if (!isChestOpen()) return null;
        if (!(InventoryCompat.getContainer((GuiChest) MinecraftCompat.getCurrentScreen()) instanceof ContainerChest)) return null;
        return (ContainerChest) InventoryCompat.getContainer((GuiChest) MinecraftCompat.getCurrentScreen());
    }

    @Nullable
    public static ContainerChest getOpenChest(GuiScreen gui) {
        if (!isChestOpen(gui)) return null;
        if (!(InventoryCompat.getContainer((GuiChest) gui) instanceof ContainerChest)) return null;
        return (ContainerChest) InventoryCompat.getContainer((GuiChest) gui);
    }

    @Nullable
    public static IInventory getLowerInventory() {
        ContainerChest chest = getOpenChest();
        return chest == null ? null : chest.getLowerChestInventory();
    }

    @Nullable
    public static IInventory getLowerInventory(ContainerChest chest) {
        return chest == null ? null : chest.getLowerChestInventory();
    }

    public static int getInventorySize(IInventory inventory) {
        //#if MC < 1.16
        return inventory.getSizeInventory();
        //#else
        //$$ return inventory.size();
        //#endif
    }

    public static ItemStack getStackInSlot(IInventory inventory, int slot) {
        //#if MC < 1.16
        return inventory.getStackInSlot(slot);
        //#else
        //$$ return inventory.getStack(slot);
        //#endif
    }

    public static boolean isPlayerInventory(IInventory inventory) {
        EntityPlayer player = MinecraftCompat.getLocalPlayer();
        //#if MC < 1.16
        return player != null && inventory == player.inventory;
        //#else
        //$$ return player != null && inventory == player.getInventory();
        //#endif
    }

    public static int getWindowId() {
        if (!isChestOpen()) return -1;
        return InventoryCompat.getWindowId(InventoryCompat.getContainer((GuiChest) MinecraftCompat.getCurrentScreen()));
    }

    @Nullable
    public static String getContainerName() {
        GuiScreen screen = MinecraftCompat.getCurrentScreen();
        if (screen == null) return null;
        if (screen == cachedScreen) return cachedContainerName;
        cachedScreen = screen;
        IChatComponent title = InventoryCompat.getContainerTitle(screen);
        cachedContainerName = title == null ? null : ColorUtils.stripColor(TextCompat.getUnformattedText(title)).trim();
        return cachedContainerName;
    }

    @Nullable
    public static String getContainerName(GuiScreen gui) {
        IChatComponent title = InventoryCompat.getContainerTitle(gui);
        return title == null ? null : ColorUtils.stripColor(TextCompat.getUnformattedText(title)).trim();
    }

    @Nullable
    public static String getTitle(ContainerChest chest) {
        if (chest == null) return null;
        //#if MC < 1.16
        return ColorUtils.stripColor(TextCompat.getUnformattedText((chest.getLowerChestInventory()).getDisplayName())).trim();
        //#else
        //$$ Screen screen = MinecraftCompat.getCurrentScreen();
        //$$ if (!(screen instanceof HandledScreen)) return null;
        //$$ if (InventoryCompat.getContainer((HandledScreen) screen) != (Object) chest) return null;
        //$$ Text title = InventoryCompat.getContainerTitle(screen);
        //$$ return title == null ? null : ColorUtils.stripColor(TextCompat.getUnformattedText(title)).trim();
        //#endif
    }

    public static boolean isInContainer(String name) {
        String container = getContainerName();
        return name.equals(container);
    }

    public static boolean isInContainer(GuiScreen gui, String name) {
        String container = getContainerName(gui);
        return name.equals(container);
    }

    public static boolean containerNameStartsWith(String prefix) {
        String container = getContainerName();
        return container != null && container.startsWith(prefix);
    }

    public static boolean containerNameContains(String infix) {
        String container = getContainerName();
        return container != null && container.contains(infix);
    }
}
