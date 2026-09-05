package io.hamlook.aetheria.utils;

import io.hamlook.aetheria.utils.compat.InventoryCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.TextCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;

import javax.annotation.Nullable;

public class ContainerUtils {

    private static final Minecraft mc = MinecraftCompat.getMinecraft();

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
        return MinecraftCompat.getCurrentScreen() instanceof net.minecraft.client.gui.inventory.GuiContainer;
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

    public static int getWindowId() {
        if (!isChestOpen()) return -1;
        return InventoryCompat.getContainer((GuiChest) MinecraftCompat.getCurrentScreen()).windowId;
    }

    @Nullable
    public static String getContainerName() {
        GuiScreen screen = MinecraftCompat.getCurrentScreen();
        if (screen == cachedScreen) return cachedContainerName;
        cachedScreen = screen;
        IInventory inv = getLowerInventory(getOpenChest(screen));
        cachedContainerName = inv == null ? null : ColorUtils.stripColor(TextCompat.getUnformattedText(inv.getDisplayName())).trim();
        return cachedContainerName;
    }

    @Nullable
    public static String getContainerName(GuiScreen gui) {
        IInventory inv = getLowerInventory(getOpenChest(gui));
        return inv == null ? null : ColorUtils.stripColor(TextCompat.getUnformattedText(inv.getDisplayName())).trim();
    }

    @Nullable
    public static String getTitle(ContainerChest chest) {
        if (chest == null) return null;
        return ColorUtils.stripColor(TextCompat.getUnformattedText((chest.getLowerChestInventory()).getDisplayName())).trim();
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
