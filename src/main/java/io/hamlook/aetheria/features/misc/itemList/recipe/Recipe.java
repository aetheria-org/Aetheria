package io.hamlook.aetheria.features.misc.itemList.recipe;

import io.hamlook.aetheria.features.misc.itemList.SkyblockItem;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;

import java.util.List;

public abstract class Recipe {
    protected SkyblockItem targetItem;

    public Recipe(SkyblockItem targetItem) {
        this.targetItem = targetItem;
    }

    public abstract void draw(int mouseX, int mouseY, int x, int y, int width, int height,
                              int scrollY, FontRenderer fontRenderer, GuiScreen gui);

    public abstract List<String> getTooltip(int mouseX, int mouseY, int x, int y,
                                            int width, int height, int scrollY);

    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton, int x, int y, int width, int height, int scrollY) {
        return false;
    }

    public SkyblockItem getSkyblockItemAt(int mouseX, int mouseY, int x, int y, int width, int height, int scrollY) {
        return null;
    }

    public int[] preferredSize() { return new int[]{220, 100}; }

    public String typeLabel()      { return "Recipe"; }
    public int    typeLabelColor() { return 0xFFFFFF; }
}