package io.hamlook.aetheria.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;

import javax.annotation.Nullable;

public class ContainerUtils {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private ContainerUtils() {
    }

    public static boolean isChestOpen() {
        return mc.currentScreen instanceof GuiChest;
    }

    public static boolean isChestOpen(GuiScreen gui) {
        return gui instanceof GuiChest;
    }

    public static boolean isInventoryOpen() {
        return mc.currentScreen instanceof GuiInventory;
    }

    public static boolean isInventoryOpen(GuiScreen gui) {
        return gui instanceof GuiInventory;
    }

    public static boolean isGuiContainerOpen() {
        return mc.currentScreen instanceof net.minecraft.client.gui.inventory.GuiContainer;
    }

    @Nullable
    public static ContainerChest getOpenChest() {
        if (!isChestOpen()) return null;
        if (!(((GuiChest) mc.currentScreen).inventorySlots instanceof ContainerChest)) return null;
        return (ContainerChest) ((GuiChest) mc.currentScreen).inventorySlots;
    }

    @Nullable
    public static ContainerChest getOpenChest(GuiScreen gui) {
        if (!isChestOpen(gui)) return null;
        if (!(((GuiChest) gui).inventorySlots instanceof ContainerChest)) return null;
        return (ContainerChest) ((GuiChest) gui).inventorySlots;
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
        return ((GuiChest) mc.currentScreen).inventorySlots.windowId;
    }

    @Nullable
    public static String getContainerName() {
        IInventory inv = getLowerInventory();
        return inv == null ? null : ColorUtils.stripColor(inv.getDisplayName().getUnformattedText()).trim();
    }

    @Nullable
    public static String getContainerName(GuiScreen gui) {
        IInventory inv = getLowerInventory(getOpenChest(gui));
        return inv == null ? null : ColorUtils.stripColor(inv.getDisplayName().getUnformattedText()).trim();
    }

    @Nullable
    public static String getTitle(ContainerChest chest) {
        if (chest == null) return null;
        return ColorUtils.stripColor(chest.getLowerChestInventory().getDisplayName().getUnformattedText()).trim();
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
