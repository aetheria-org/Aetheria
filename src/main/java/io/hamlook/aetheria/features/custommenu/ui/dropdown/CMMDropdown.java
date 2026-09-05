package io.hamlook.aetheria.features.custommenu.ui.dropdown;

import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.features.custommenu.Position;
import io.hamlook.aetheria.features.custommenu.ui.CMMElement;
import io.hamlook.aetheria.utils.SoundUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.render.NineSliceUtils;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import net.minecraft.util.ResourceLocation;
import java.util.ArrayList;
import java.util.List;

public class CMMDropdown extends CMMElement {
    public int itemHeight;
    public List<Item> items;
    public String selectedItem = "";
    public boolean focused;
    public int maxVisibleItems = 8;

    public CMMDropdown(Position pos, int width, int itemHeight, List<Item> items) {
        super(pos, width, Math.max(12, itemHeight));
        this.itemHeight = Math.max(12, itemHeight);
        setItems(items);
    }
    public abstract static class Item {
        public String name, id;
        public Item(String name) { this.name = name == null ? "" : name; this.id = this.name.toLowerCase().replace(" ", "-"); }
        public abstract void onClick();
    }
    public static class NameItem extends Item { public NameItem(String name) { super(name); } public void onClick() { } }

    @Override public void draw(int mouseX, int mouseY, float partialTicks) {
        drawRow(xPos, yPos, width, itemHeight, getText(selectedItem), focused, inside(mouseX, mouseY, xPos, yPos, width, itemHeight));
        if (!focused) return;
        int visible = Math.min(maxVisibleItems, items.size());
        for (int i = 0; i < visible; i++) {
            int y = yPos + itemHeight + i * itemHeight;
            drawRow(xPos, y, width, itemHeight, items.get(i).name, selectedItem.equals(items.get(i).id), inside(mouseX, mouseY, xPos, y, width, itemHeight));
        }
    }
    private void drawRow(int x, int y, int w, int h, String text, boolean selected, boolean hover) {
        ResourceLocation bg = Resources.betterContainerNineSlice(selected || hover ? 1 : 0);
        NineSliceUtils.draw(bg, x, y, w, h, 6, 18, selected || hover);
        int textWidth = Math.max(1, MinecraftCompat.getFontRenderer().getStringWidth(text));
        float scale = Math.min(1.25f, Math.min((w - 20f) / textWidth, (h - 2f) / MinecraftCompat.getFontRenderer().FONT_HEIGHT));
        TextRenderUtils.drawCenteredStringScaleAware(text, x + w / 2f - 6, y + h / 2f, hover ? 0xFFFFFFFF : 0xFFE0E0E0, Math.max(.25f, scale), true);
        TextRenderUtils.drawStringScaleAware(focused ? "▲" : "▼", x + w - 12, y + h / 2f - 4, 0xFFB8B8B8, .8f, false);
    }
    public boolean onMouseClick(int mouseX, int mouseY) {
        if (!focused) { if (!inside(mouseX, mouseY, xPos, yPos, width, itemHeight)) return false; focused = true; updateHeight(); SoundUtils.playSound("gui.button.press"); return true; }
        if (inside(mouseX, mouseY, xPos, yPos + itemHeight, width, Math.min(maxVisibleItems, items.size()) * itemHeight)) {
            int index = (mouseY - yPos - itemHeight) / itemHeight;
            if (index >= 0 && index < items.size()) { Item item = items.get(index); selectedItem = item.id; item.onClick(); onItemClick(item); SoundUtils.playSound("gui.button.press"); }
            focused = false; updateHeight(); return true;
        }
        if (inside(mouseX, mouseY, xPos, yPos, width, itemHeight)) return true;
        focused = false; updateHeight(); return false;
    }
    private void updateHeight() { height = itemHeight + (focused ? Math.min(maxVisibleItems, items.size()) * itemHeight : 0); }
    public void onItemClick(Item clickedItem) { }
    private boolean inside(int mx, int my, int x, int y, int w, int h) { return mx >= x && mx <= x + w && my >= y && my < y + h; }
    private String getText(String id) { if (id != null) for (Item item : items) if (id.equals(item.id)) return item.name; return "Select an option"; }
    public String getSelectedItem() { return selectedItem == null ? "" : selectedItem; }
    public void setSelectedItem(String id) { selectedItem = id == null ? "" : id; }
    public void setItems(List<Item> values) { items = new ArrayList<>(values == null ? new ArrayList<Item>() : values); updateHeight(); }

    /** Dropdown geometry is defined by its row height; the editor should not serialize a second height. */
    @Override public int[] getEditorBounds() {
        int rows = Math.max(1, items == null ? 0 : items.size() + 1);
        return new int[]{xPos, yPos, xPos + width, yPos + itemHeight * rows};
    }

    public void setEditorItemHeight(int value) {
        itemHeight = Math.max(12, value);
        updateHeight();
    }
}
