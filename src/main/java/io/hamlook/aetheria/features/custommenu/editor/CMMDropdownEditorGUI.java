package io.hamlook.aetheria.features.custommenu.editor;

import io.hamlook.aetheria.features.custommenu.ui.dropdown.CMMDropdown;
import io.hamlook.aetheria.utils.compat.AetheriaBaseScreen;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import io.hamlook.aetheria.features.custommenu.util.ScreenHelper;
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

    @Override protected void onInitGui() { name = new GuiTextField(0, MinecraftCompat.getFontRenderer(), width / 2 - 120, 58, 240, 20); }
    @Override public void onResize(net.minecraft.client.Minecraft mc, int w, int h) { super.onResize(mc, w, h); ScreenHelper.updateScreenDimensions(w, h); if(name!=null){name.xPosition=w/2-ScreenHelper.getStaticWidth(120);name.yPosition=ScreenHelper.getStaticHeight(58);name.width=ScreenHelper.getStaticWidth(240);name.height=ScreenHelper.getStaticHeight(20);} }

    @Override protected void onDrawScreen(int mx, int my, float pt) {
        drawRect(0, 0, width, height, 0xF0121218);
        TextRenderUtils.drawCenteredStringScaleAware("Edit Dropdown Items", width / 2f, 25, 0xFFFFFFFF, 1.8f, true);
        name.drawTextBox();
        button(width / 2 - 120, 88, 75, 22, "Add", mx, my);
        button(width / 2 - 35, 88, 75, 22, "Apply", mx, my);
        button(width / 2 + 50, 88, 75, 22, "Remove", mx, my);
        for (int i = 0; i < dropdown.items.size(); i++) {
            int y = 125 + i * 25;
            boolean hover = mx >= width / 2 - 150 && mx <= width / 2 + 150 && my >= y && my < y + 21;
            drawRect(width / 2 - 150, y, width / 2 + 150, y + 21, i == selected ? 0xFF3B6982 : (hover ? 0xFF303B43 : 0xFF292932));
            TextRenderUtils.drawStringScaleAware(dropdown.items.get(i).name, width / 2 - 140, y + 6, 0xFFFFFFFF, 1f, false);
        }
        TextRenderUtils.drawCenteredStringScaleAware("Click an item to edit its name | Escape: return", width / 2f, height - 25, 0xFFB8B8C8, 1f, false);
    }

    private void button(int x, int y, int w, int h, String text, int mx, int my) {
        drawRect(x, y, x + w, y + h, mx >= x && mx <= x + w && my >= y && my < y + h ? 0xFF3B6982 : 0xFF292932);
        TextRenderUtils.drawCenteredStringScaleAware(text, x + w / 2f, y + h / 2f, 0xFFFFFFFF, .9f, false);
    }

    @Override protected void onMouseClicked(int mx, int my, int button) {
        name.mouseClicked(mx, my, button);
        if (button != 0) return;
        if (my >= 125) {
            int index = (my - 125) / 25;
            if (index >= 0 && index < dropdown.items.size()) { selected = index; name.setText(dropdown.items.get(index).name); }
        } else if (my >= 88 && my < 110) {
            if (mx >= width / 2 - 120 && mx < width / 2 - 45) { dropdown.items.add(new CMMDropdown.NameItem("New Item")); selected = dropdown.items.size() - 1; name.setText("New Item"); }
            else if (mx >= width / 2 - 35 && mx < width / 2 + 40) apply();
            else if (mx >= width / 2 + 50 && mx < width / 2 + 125 && selected >= 0 && selected < dropdown.items.size()) { dropdown.items.remove(selected); selected = -1; name.setText(""); }
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
