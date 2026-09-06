package io.hamlook.aetheria.features.custommenu.editor;

import io.hamlook.aetheria.features.custommenu.ui.buttons.impl.GuiButton;
import io.hamlook.aetheria.features.custommenu.util.GuiHelper;
import io.hamlook.aetheria.utils.compat.AetheriaBaseScreen;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import io.hamlook.aetheria.features.custommenu.util.ScreenHelper;
import io.hamlook.aetheria.features.custommenu.util.PercentageUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

import java.util.List;

/**
 * Searchable selector for the GUI names supported by GuiHelper.
 */
public class CMMGuiPickerGUI extends AetheriaBaseScreen {
    private final GuiButton target;
    private final GuiScreen parent;
    private GuiTextField search;

    public CMMGuiPickerGUI(GuiButton target, GuiScreen parent) {
        this.target = target;
        this.parent = parent;
    }

    private int left() {
        return PercentageUtils.centeredX(38f);
    }

    private int panelWidth() {
        return PercentageUtils.width(38f);
    }

    private int rowY(int row) {
        return PercentageUtils.y(17f) + row * PercentageUtils.height(5.2f);
    }

    @Override
    protected void onInitGui() {
        search = new GuiTextField(0, MinecraftCompat.getFontRenderer(), left(), PercentageUtils.y(9.4f), panelWidth(), PercentageUtils.height(4.2f));
    }

    @Override
    public void onResize(net.minecraft.client.Minecraft mc, int w, int h) {
        super.onResize(mc, w, h);
        ScreenHelper.updateScreenDimensions(w, h);
        if (search != null) {
            search.xPosition = left();
            search.yPosition = PercentageUtils.y(9.4f);
            search.width = panelWidth();
            search.height = PercentageUtils.height(4.2f);
        }
    }

    @Override
    protected void onDrawScreen(int mx, int my, float pt) {
        drawRect(0, 0, width, height, 0xF0121218);
        TextRenderUtils.drawCenteredStringScaleAware("Select GUI Screen", PercentageUtils.centerX(), PercentageUtils.y(5f), 0xFFFFFFFF, 1.8f, true);
        search.drawTextBox();
        String query = search.getText().toLowerCase();
        int row = 0;
        List<String> names = GuiHelper.getAvailableMenuNames();
        for (String name : names) {
            if (!query.isEmpty() && !name.toLowerCase().contains(query)) continue;
            int y = rowY(row++);
            boolean hover = mx >= left() && mx <= left() + panelWidth() && my >= y && my < y + PercentageUtils.height(4.4f);
            drawRect(left(), y, left() + panelWidth(), y + PercentageUtils.height(4.4f), hover ? 0xFF3B6982 : 0xFF292932);
            TextRenderUtils.drawStringScaleAware(name, left() + PercentageUtils.width(1.2f), y + PercentageUtils.height(1.2f), 0xFFFFFFFF, 1f, false);
        }
        TextRenderUtils.drawCenteredStringScaleAware("Select a supported GUI | Escape: return", PercentageUtils.centerX(), PercentageUtils.y(94f), 0xFFB8B8C8, 1f, false);
    }

    @Override
    protected void onMouseClicked(int mx, int my, int button) {
        search.mouseClicked(mx, my, button);
        if (button != 0) return;
        String query = search.getText().toLowerCase();
        int row = 0;
        for (String name : GuiHelper.getAvailableMenuNames()) {
            if (!query.isEmpty() && !name.toLowerCase().contains(query)) continue;
            int y = rowY(row++);
            if (mx >= left() && mx <= left() + panelWidth() && my >= y && my < y + PercentageUtils.height(4.4f)) {
                target.screen = name;
                MinecraftCompat.getMinecraft().displayGuiScreen(parent);
                return;
            }
        }
    }

    @Override
    protected void onKeyTyped(char c, int key) {
        if (search.textboxKeyTyped(c, key)) return;
        if (key == Keyboard.KEY_ESCAPE) MinecraftCompat.getMinecraft().displayGuiScreen(parent);
    }
}
