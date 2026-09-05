package io.hamlook.aetheria.features.custommenu.editor;

import io.hamlook.aetheria.features.custommenu.ui.buttons.impl.GuiButton;
import io.hamlook.aetheria.features.custommenu.util.GuiHelper;
import io.hamlook.aetheria.utils.compat.AetheriaBaseScreen;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import io.hamlook.aetheria.features.custommenu.util.ScreenHelper;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

import java.util.List;

/** Searchable selector for the GUI names supported by GuiHelper. */
public class CMMGuiPickerGUI extends AetheriaBaseScreen {
    private final GuiButton target;
    private final GuiScreen parent;
    private GuiTextField search;

    public CMMGuiPickerGUI(GuiButton target, GuiScreen parent) { this.target = target; this.parent = parent; }
    @Override protected void onInitGui() { search = new GuiTextField(0, MinecraftCompat.getFontRenderer(), width / 2 - 160, 45, 320, 20); }
    @Override public void onResize(net.minecraft.client.Minecraft mc, int w, int h) { super.onResize(mc, w, h); ScreenHelper.updateScreenDimensions(w, h); if(search!=null){search.xPosition=w/2-ScreenHelper.getStaticWidth(160);search.yPosition=ScreenHelper.getStaticHeight(45);search.width=ScreenHelper.getStaticWidth(320);search.height=ScreenHelper.getStaticHeight(20);} }
    @Override protected void onDrawScreen(int mx, int my, float pt) {
        drawRect(0, 0, width, height, 0xF0121218);
        TextRenderUtils.drawCenteredStringScaleAware("Select GUI Screen", width / 2f, 25, 0xFFFFFFFF, 1.8f, true);
        search.drawTextBox();
        String query = search.getText().toLowerCase(); int row = 0;
        List<String> names = GuiHelper.getAvailableMenuNames();
        for (String name : names) {
            if (!query.isEmpty() && !name.toLowerCase().contains(query)) continue;
            int y = 80 + row++ * 25; boolean hover = mx >= width / 2 - 160 && mx <= width / 2 + 160 && my >= y && my < y + 21;
            drawRect(width / 2 - 160, y, width / 2 + 160, y + 21, hover ? 0xFF3B6982 : 0xFF292932);
            TextRenderUtils.drawStringScaleAware(name, width / 2 - 150, y + 6, 0xFFFFFFFF, 1f, false);
        }
        TextRenderUtils.drawCenteredStringScaleAware("Select a supported GUI | Escape: return", width / 2f, height - 25, 0xFFB8B8C8, 1f, false);
    }
    @Override protected void onMouseClicked(int mx, int my, int button) {
        search.mouseClicked(mx, my, button); if (button != 0) return;
        String query = search.getText().toLowerCase(); int row = 0;
        for (String name : GuiHelper.getAvailableMenuNames()) {
            if (!query.isEmpty() && !name.toLowerCase().contains(query)) continue;
            int y = 80 + row++ * 25;
            if (mx >= width / 2 - 160 && mx <= width / 2 + 160 && my >= y && my < y + 21) { target.screen = name; MinecraftCompat.getMinecraft().displayGuiScreen(parent); return; }
        }
    }
    @Override protected void onKeyTyped(char c, int key) { if (search.textboxKeyTyped(c, key)) return; if (key == Keyboard.KEY_ESCAPE) MinecraftCompat.getMinecraft().displayGuiScreen(parent); }
}
