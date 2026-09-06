package io.hamlook.aetheria.features.custommenu.editor;

import io.hamlook.aetheria.features.custommenu.ui.dropdown.CMMDropdown;
import io.hamlook.aetheria.utils.compat.AetheriaBaseScreen;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import io.hamlook.aetheria.features.custommenu.util.ScreenHelper;
import io.hamlook.aetheria.features.custommenu.util.PercentageUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

/** Small serializable item editor for dropdowns. */
public class CMMDropdownEditorGUI extends AetheriaBaseScreen {
    private final CMMDropdown dropdown;
    private final GuiScreen parent;
    private GuiTextField name;
    private int selected = -1;

    public CMMDropdownEditorGUI(CMMDropdown dropdown, GuiScreen parent) { this.dropdown = dropdown; this.parent = parent; }

    private int left() { return PercentageUtils.centeredX(28f); }
    private int panelWidth() { return PercentageUtils.width(28f); }
    private int fieldY() { return PercentageUtils.y(12f); }
    private int buttonY() { return PercentageUtils.y(18.3f); }
    private int rowY(int i) { return PercentageUtils.y(26f) + i * PercentageUtils.height(5.2f); }
    @Override protected void onInitGui() { name = new GuiTextField(0, MinecraftCompat.getFontRenderer(), left(), fieldY(), panelWidth(), PercentageUtils.height(4.2f)); }
    @Override public void onResize(net.minecraft.client.Minecraft mc, int w, int h) { super.onResize(mc, w, h); ScreenHelper.updateScreenDimensions(w, h); if(name!=null){name.xPosition=left();name.yPosition=fieldY();name.width=panelWidth();name.height=PercentageUtils.height(4.2f);} }

    @Override protected void onDrawScreen(int mx, int my, float pt) {
        drawRect(0, 0, width, height, 0xF0121218);
        TextRenderUtils.drawCenteredStringScaleAware("Edit Dropdown Items", PercentageUtils.centerX(), PercentageUtils.y(5f), 0xFFFFFFFF, 1.8f, true);
        name.drawTextBox();
        button(left(), buttonY(), PercentageUtils.width(8.8f), PercentageUtils.height(4.6f), "Add", mx, my);
        button(PercentageUtils.x(45.6f), buttonY(), PercentageUtils.width(8.8f), PercentageUtils.height(4.6f), "Apply", mx, my);
        button(PercentageUtils.x(55.6f), buttonY(), PercentageUtils.width(8.8f), PercentageUtils.height(4.6f), "Remove", mx, my);
        for (int i = 0; i < dropdown.items.size(); i++) {
            int y = rowY(i);
            boolean hover = mx >= PercentageUtils.centeredX(35f) && mx <= PercentageUtils.centeredX(35f)+PercentageUtils.width(35f) && my >= y && my < y + PercentageUtils.height(4.4f);
            drawRect(PercentageUtils.centeredX(35f), y, PercentageUtils.centeredX(35f)+PercentageUtils.width(35f), y + PercentageUtils.height(4.4f), i == selected ? 0xFF3B6982 : (hover ? 0xFF303B43 : 0xFF292932));
            TextRenderUtils.drawStringScaleAware(dropdown.items.get(i).name, PercentageUtils.centeredX(35f)+PercentageUtils.width(1.2f), y + PercentageUtils.height(1.2f), 0xFFFFFFFF, 1f, false);
        }
        TextRenderUtils.drawCenteredStringScaleAware("Click an item to edit its name | Escape: return", PercentageUtils.centerX(), PercentageUtils.y(94f), 0xFFB8B8C8, 1f, false);
    }

    private void button(int x, int y, int w, int h, String text, int mx, int my) {
        drawRect(x, y, x + w, y + h, mx >= x && mx <= x + w && my >= y && my < y + h ? 0xFF3B6982 : 0xFF292932);
        TextRenderUtils.drawCenteredStringScaleAware(text, x + w / 2f, y + h / 2f, 0xFFFFFFFF, .9f, false);
    }

    @Override protected void onMouseClicked(int mx, int my, int button) {
        name.mouseClicked(mx, my, button);
        if (button != 0) return;
        if (my >= PercentageUtils.y(26f)) {
            int index = (my - PercentageUtils.y(26f)) / PercentageUtils.height(5.2f);
            if (index >= 0 && index < dropdown.items.size()) { selected = index; name.setText(dropdown.items.get(index).name); }
        } else if (my >= buttonY() && my < buttonY()+PercentageUtils.height(4.6f)) {
            if (mx >= left() && mx < left()+PercentageUtils.width(8.8f)) { dropdown.items.add(new CMMDropdown.NameItem("New Item")); selected = dropdown.items.size() - 1; name.setText("New Item"); }
            else if (mx >= PercentageUtils.x(45.6f) && mx < PercentageUtils.x(54.4f)) apply();
            else if (mx >= PercentageUtils.x(55.6f) && mx < PercentageUtils.x(64.4f) && selected >= 0 && selected < dropdown.items.size()) { dropdown.items.remove(selected); selected = -1; name.setText(""); }
        }
    }

    private void apply() {
        if (selected < 0 || selected >= dropdown.items.size()) return;
        String value = name.getText() == null ? "" : name.getText().trim();
        if (value.isEmpty()) return;
        CMMDropdown.Item item = dropdown.items.get(selected);
        item.name = value;
        item.id = value.toLowerCase().replace(" ", "-");
        if (dropdown.selectedItem == null || dropdown.selectedItem.isEmpty()) dropdown.selectedItem = item.id;
    }

    @Override protected void onKeyTyped(char c, int key) {
        if (name.textboxKeyTyped(c, key)) return;
        if (key == Keyboard.KEY_ESCAPE) MinecraftCompat.getMinecraft().displayGuiScreen(parent);
    }
}
