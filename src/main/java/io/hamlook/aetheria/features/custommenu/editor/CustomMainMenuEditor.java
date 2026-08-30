package io.hamlook.aetheria.features.custommenu.editor;

import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.features.chat.globalchat.image.GCImage;
import io.hamlook.aetheria.features.chat.globalchat.image.ImageManager;
import io.hamlook.aetheria.features.custommenu.CustomMMConfig;
import io.hamlook.aetheria.features.custommenu.Position;
import io.hamlook.aetheria.features.custommenu.ui.CMMElement;
import io.hamlook.aetheria.features.custommenu.util.CMMHelper;
import io.hamlook.aetheria.features.custommenu.util.ScreenHelper;
import io.hamlook.aetheria.utils.render.NineSliceUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.GuiButton;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Full-screen preset picker and visual editor for Custom Main Menu layouts.
 */
public class CustomMainMenuEditor extends GuiScreen {
    private CustomMMConfig config;
    private CMMElement selected;
    private AddElementGui addGui;
    private ContextMenuGui contextMenu;
    private ElementEditorGui elementEditor;
    private GuiTextField newPreset, backgroundPath;
    private boolean editing, grid = false, dragging, resizing;
    private boolean showHistory;
    private long lastClick;
    private final EditorHistory history = new EditorHistory(CMMHelper.GSON, 30);
    private static final int ID_CREATE = 10, ID_DELETE_PRESET = 11, ID_DONE = 12;
    private static final int ID_BACK = 20, ID_SAVE = 21, ID_ADD = 22, ID_GRID = 23, ID_HISTORY = 24, ID_BACKGROUND = 25;
    private int dragX, dragY, resizeCorner, resizeStartX, resizeStartY, resizeStartW, resizeStartH;

    @Override
    public void initGui() {
        ScreenHelper.updateScreenDimensions(width, height);
        buttonList.clear();
        newPreset = new GuiTextField(1, fontRendererObj, 22, 45, 145, 20);
        newPreset.setMaxStringLength(40);
        backgroundPath = new GuiTextField(2, fontRendererObj, Math.max(370, width - 245), 8, 205, 20);
        backgroundPath.setMaxStringLength(300);
        if (config != null) backgroundPath.setText(config.background == null ? "" : config.background.url);
        if (editing) {
            buttonList.add(new GuiButton(ID_BACK, 8, 8, 55, 20, "Back"));
            buttonList.add(new GuiButton(ID_SAVE, 66, 8, 55, 20, "Save"));
            buttonList.add(new GuiButton(ID_ADD, 124, 8, 55, 20, "Add"));
            buttonList.add(new GuiButton(ID_GRID, 182, 8, 76, 20, grid ? "Grid: ON" : "Grid: OFF"));
            buttonList.add(new GuiButton(ID_HISTORY, 261, 8, 88, 20, showHistory ? "History: ON" : "History: OFF"));
            buttonList.add(new GuiButton(ID_BACKGROUND, width - 36, 8, 28, 20, "BG"));
        } else {
            buttonList.add(new GuiButton(ID_CREATE, 178, 45, 95, 20, "Create"));
            buttonList.add(new GuiButton(ID_DELETE_PRESET, 278, 45, 82, 20, "Delete"));
            buttonList.add(new GuiButton(ID_DONE, width - 104, height - 38, 82, 20, "Done"));
            int y = 72;
            for (String name : names()) {
                buttonList.add(new GuiButton(1000 + y, 22, y, 338, 24, name));
                y += 28;
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(0, 0, width, height, 0xff10141b);
        if (!editing) drawPresetSelector(mouseX, mouseY);
        else drawEditor(mouseX, mouseY, partialTicks);
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (addGui != null) addGui.render();
        if (elementEditor != null) elementEditor.render();
        if (contextMenu != null) contextMenu.render();
    }

    private void drawPresetSelector(int mouseX, int mouseY) {
        title("Custom Main Menu Presets");
        panel(16, 28, width - 16, height - 18);
        fontRendererObj.drawString("Select a preset to edit", 22, 33, 0xffdddddd);
        int y = 75;
        List<String> names = names();
        for (String name : names) {
            boolean hover = mouseX >= 22 && mouseX <= 210 && mouseY >= y - 3 && mouseY <= y + 18;
            drawRect(22, y - 3, 210, y + 18, hover ? 0xff31536d : 0xff202a35);
            fontRendererObj.drawString(name + (name.equals(CMMHelper.selectedConfig) ? "  (selected)" : ""), 30, y + 3, 0xffffffff);
            y += 28;
        }
        newPreset.drawTextBox();
        fontRendererObj.drawString("New preset name", 22, 31, 0xffaaaaaa);
    }

    private void drawEditor(int mouseX, int mouseY, float partialTicks) {
        fontRendererObj.drawString("Editing: " + config.configName, 370, 14, 0xffdbeeff);
        drawRect(0, 0, width, EditorLayout.TOOLBAR_HEIGHT, 0xff18222d);
        backgroundPath.drawTextBox();
        // The actual toolbar controls are rendered by GuiScreen.super.drawScreen.
        int canvasX = 0, canvasY = EditorLayout.canvasTop(), canvasW = EditorLayout.canvasWidth(width), canvasH = EditorLayout.canvasHeight(height);
        drawRect(canvasX, canvasY, canvasX + canvasW, canvasY + canvasH, 0xff080b10);
        if (grid) drawGrid(canvasX, canvasY, canvasW, canvasH);
        ScreenHelper.updateScreenDimensions(width, height);
        for (CMMElement element : config.elements) {
            element.updatePosition();
            element.draw(mouseX, mouseY, partialTicks);
            int[] hitbox = element.getCorners();
            if (element == selected || (mouseX >= hitbox[0] - 5 && mouseX <= hitbox[2] + 5 && mouseY >= hitbox[1] - 5 && mouseY <= hitbox[3] + 5)) {
                drawRect(hitbox[0] - 5, hitbox[1] - 5, hitbox[2] + 5, hitbox[1] - 4, element == selected ? 0xff71d7ff : 0x555f91a8);
                drawRect(hitbox[0] - 5, hitbox[3] + 4, hitbox[2] + 5, hitbox[3] + 5, element == selected ? 0xff71d7ff : 0x555f91a8);
            }
        }
        if (selected != null) drawSelection(selected);
        if (showHistory) drawHistoryOverlay();
        CMMElement hovered = elementAt(mouseX, mouseY);
        String hint = hovered == null ? "Right-click background: upload image | Shift: grid snap | Ctrl: alignment" : (hovered instanceof io.hamlook.aetheria.features.custommenu.ui.text.Text ? "Double-click text to edit | Right-click for parameters" : "Drag to move | Drag corners to resize | Right-click for parameters");
        fontRendererObj.drawString(hint, 10, height - 12, 0xffc7d9e6);
        String cursorState = hovered == null ? "Right-click" : cornerAt(hovered, mouseX, mouseY) != 0 ? "Resize" : hovered instanceof io.hamlook.aetheria.features.custommenu.ui.text.Text ? "Edit text" : "Drag";
        drawRect(mouseX + 10, mouseY + 10, mouseX + 18 + fontRendererObj.getStringWidth(cursorState), mouseY + 25, 0xd018222c);
        fontRendererObj.drawString(cursorState, mouseX + 14, mouseY + 14, 0xffdbeeff);
    }

    private void drawGrid(int x, int y, int w, int h) {
        for (int gx = x; gx < x + w; gx += EditorLayout.GRID_SIZE) drawRect(gx, y, gx + 1, y + h, 0x181f5b76);
        for (int gy = y; gy < y + h; gy += EditorLayout.GRID_SIZE) drawRect(x, gy, x + w, gy + 1, 0x181f5b76);
    }

    private void drawSelection(CMMElement e) {
        int[] c = e.getCorners();
        drawRect(c[0] - 2, c[1] - 2, c[2] + 2, c[1], 0xff71d7ff);
        drawRect(c[0] - 2, c[3], c[2] + 2, c[3] + 2, 0xff71d7ff);
        drawRect(c[0] - 2, c[1], c[0], c[3], 0xff71d7ff);
        drawRect(c[2], c[1], c[2] + 2, c[3], 0xff71d7ff);
        drawRect(c[0] - 4, c[1] - 4, c[0] + 4, c[1] + 4, 0xffdbeeff);
        drawRect(c[2] - 4, c[1] - 4, c[2] + 4, c[1] + 4, 0xffdbeeff);
        drawRect(c[0] - 4, c[3] - 4, c[0] + 4, c[3] + 4, 0xffdbeeff);
        drawRect(c[2] - 4, c[3] - 4, c[2] + 4, c[3] + 4, 0xffdbeeff);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        if (addGui != null) {
            addGui.mouseInput(mouseX, mouseY);
            return;
        }
        if (elementEditor != null) {
            elementEditor.mouseInput(mouseX, mouseY);
            return;
        }
        if (contextMenu != null) {
            contextMenu.mouseInput(mouseX, mouseY);
            return;
        }
        if (!editing) {
            super.mouseClicked(mouseX, mouseY, button);
            selectorClick(mouseX, mouseY, button);
            return;
        }
        super.mouseClicked(mouseX, mouseY, button);
        if (button == 1) {
            CMMElement target = elementAt(mouseX, mouseY);
            int menuX = Math.min(mouseX, Math.max(0, width - 175));
            int menuY = Math.min(mouseY, Math.max(70, height - (target == null ? 48 : 68)));
            contextMenu = new ContextMenuGui(menuX, menuY, target, action -> contextAction(action, target));
            return;
        }
        if (button != 0) return;
        if (mouseY < EditorLayout.TOOLBAR_HEIGHT) {
            if (backgroundPath.getVisible() && mouseX >= backgroundPath.xPosition && mouseX <= backgroundPath.xPosition + backgroundPath.width) backgroundPath.mouseClicked(mouseX, mouseY, button);
            return;
        }
        if (backgroundPath.getVisible() && mouseX >= backgroundPath.xPosition && mouseX <= backgroundPath.xPosition + backgroundPath.width && mouseY >= backgroundPath.yPosition && mouseY <= backgroundPath.yPosition + backgroundPath.height) {
            backgroundPath.mouseClicked(mouseX, mouseY, button);
            return;
        }
        CMMElement clicked = elementAt(mouseX, mouseY);
        if (clicked instanceof io.hamlook.aetheria.features.custommenu.ui.text.Text && clicked == selected && System.currentTimeMillis() - lastClick < 350) {
            pushHistory("Edit text");
            elementEditor = new ElementEditorGui(clicked, saved -> elementEditor = null);
            lastClick = 0;
            return;
        }
        lastClick = System.currentTimeMillis();
        selected = clicked;
        if (selected != null) {
            pushHistory("Move / resize");
            dragging = true;
            dragX = mouseX - selected.xPos;
            dragY = mouseY - selected.yPos;
            resizeCorner = cornerAt(selected, mouseX, mouseY);
            if (resizeCorner != 0) {
                dragging = false;
                resizing = true;
                resizeStartX = selected.xPos;
                resizeStartY = selected.yPos;
                resizeStartW = Math.max(1, selected.width);
                resizeStartH = Math.max(1, selected.height);
            }
        }
    }

    private void contextAction(String action, CMMElement target) {
        contextMenu = null;
        if ("edit".equals(action) && target != null) {
            pushHistory("Edit element");
            elementEditor = new ElementEditorGui(target, saved -> elementEditor = null);
        }
        else if ("delete".equals(action) && target != null) {
            pushHistory("Delete element");
            config.removeElement(target);
            if (selected == target) selected = null;
        } else if ("background".equals(action)) chooseBackground();
    }

    private void chooseBackground() {
        String path = CMMImagePicker.pick();
        if (path != null) { pushHistory("Change background"); config.setBackground(ImageManager.images.get(GCImage.createGCImageFromFile(path))); }
    }

    private void selectorClick(int mx, int my, int button) {
        if (button != 0) return;
        newPreset.mouseClicked(mx, my, button);
    }

    @Override protected void actionPerformed(GuiButton button) {
        if (button.id == ID_CREATE) {
            if (CMMHelper.createPreset(newPreset.getText())) { config = CMMHelper.getCMMConfig(); editing = true; initGui(); }
        } else if (button.id == ID_DELETE_PRESET) {
            if (!"default".equals(CMMHelper.selectedConfig)) { CMMHelper.deletePreset(CMMHelper.selectedConfig); config = null; initGui(); }
        } else if (button.id == ID_DONE) mc.displayGuiScreen(null);
        else if (button.id >= 1000) {
            int indexY = button.id - 1000;
            List<String> presetNames = names();
            int index = Math.max(0, (indexY - 72) / 28);
            if (index < presetNames.size()) { CMMHelper.selectPreset(presetNames.get(index)); config = CMMHelper.getCMMConfig(); editing = true; initGui(); }
        } else if (button.id == ID_BACK) { editing = false; selected = null; initGui(); }
        else if (button.id == ID_SAVE) save();
        else if (button.id == ID_ADD) openAdd();
        else if (button.id == ID_GRID) { grid = !grid; button.displayString = grid ? "Grid: ON" : "Grid: OFF"; }
        else if (button.id == ID_HISTORY) { showHistory = !showHistory; button.displayString = showHistory ? "History: ON" : "History: OFF"; }
        else if (button.id == ID_BACKGROUND) chooseBackground();
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        dragging = false;
        resizing = false;
    }

    @Override
    protected void mouseClickMove(int mx, int my, int button, long time) {
        if (selected == null || button != 0) return;
        boolean shift = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        if (resizing) {
            resizeSelected(mx, my, shift);
            return;
        }
        if (!dragging) return;
        int nx = mx - dragX, ny = my - dragY;
        if (shift) {
            nx = snap(nx);
            ny = snap(ny);
        }
        selected.xPos = nx;
        selected.yPos = ny;
        selected.position = Position.absolute(nx, ny);
        if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) align(selected);
    }

    @Override
    protected void keyTyped(char c, int code) throws IOException {
        if (addGui != null) {
            addGui.keyTyped(c, code);
            return;
        }
        if (elementEditor != null) {
            elementEditor.keyTyped(c, code);
            return;
        }
        if (!editing) {
            newPreset.textboxKeyTyped(c, code);
            return;
        }
        boolean ctrl = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
        if (ctrl && code == Keyboard.KEY_Z) { undo(); return; }
        if (ctrl && code == Keyboard.KEY_Y) { redo(); return; }
        if (code == Keyboard.KEY_DELETE && selected != null) {
            pushHistory("Delete element");
            config.removeElement(selected);
            selected = null;
        } else if (code == Keyboard.KEY_ESCAPE) {
            editing = false;
            selected = null;
        } else backgroundPath.textboxKeyTyped(c, code);
    }

    private void openAdd() {
        addGui = new AddElementGui(e -> { pushHistory("Add element"); config.addElement(e); }, () -> addGui = null);
    }

    private void loadBackground() {
        String path = backgroundPath.getText().trim();
        if (!path.isEmpty()) { pushHistory("Change background"); config.setBackground(ImageManager.images.get(GCImage.createGCImageFromFile(path))); }
    }

    private void save() {
        loadBackground();
        CMMHelper.savePreset(config);
    }

    private void pushHistory(String label) {
        if (config == null) return;
        history.checkpoint(config, label);
    }

    private void undo() {
        config = history.undo(config);
        selected = null;
    }

    private void redo() {
        config = history.redo(config);
        selected = null;
    }

    private void drawHistoryOverlay() {
        int x = EditorLayout.historyX(), y = EditorLayout.historyY(height, 140), w = 170, h = 140;
        NineSliceUtils.draw(Resources.storageBackground(1), x, y, w, h, 6, 18);
        fontRendererObj.drawString("History", x + 10, y + 10, 0xffffffff);
        fontRendererObj.drawString("Ctrl+Z undo | Ctrl+Y redo", x + 10, y + 25, 0xff9fb5c5);
        int line = y + 43, index = 0;
        for (String label : history.labels()) {
            if (index++ >= 5) break;
            fontRendererObj.drawString("- " + label, x + 10, line, 0xffdbeeff);
            line += 16;
        }
        if (history.labels().isEmpty()) fontRendererObj.drawString("No changes yet", x + 10, line, 0xff888888);
    }

    private int snap(int value) {
        return Math.round(value / (float) EditorLayout.GRID_SIZE) * EditorLayout.GRID_SIZE;
    }

    private CMMElement elementAt(int x, int y) {
        for (int i = config.elements.size() - 1; i >= 0; i--) {
            CMMElement e = config.elements.get(i);
            int[] c = e.getCorners();
            if (x >= c[0] && x <= c[2] && y >= c[1] && y <= c[3]) return e;
        }
        return null;
    }

    private int cornerAt(CMMElement element, int x, int y) {
        int[] c = element.getCorners();
        int hit = 7;
        if (Math.abs(x - c[0]) <= hit && Math.abs(y - c[1]) <= hit) return 1;
        if (Math.abs(x - c[2]) <= hit && Math.abs(y - c[1]) <= hit) return 2;
        if (Math.abs(x - c[0]) <= hit && Math.abs(y - c[3]) <= hit) return 3;
        if (Math.abs(x - c[2]) <= hit && Math.abs(y - c[3]) <= hit) return 4;
        return 0;
    }

    private void resizeSelected(int mouseX, int mouseY, boolean keepAspect) {
        if (selected instanceof io.hamlook.aetheria.features.custommenu.ui.text.Text) {
            float factor = (mouseX - resizeStartX) / (float) Math.max(1, resizeStartW);
            if (resizeCorner == 1 || resizeCorner == 3)
                factor = (resizeStartW - (mouseX - resizeStartX)) / (float) resizeStartW;
            ((io.hamlook.aetheria.features.custommenu.ui.text.Text) selected).scale = Math.max(.25f, factor);
            return;
        }
        int left = resizeStartX, top = resizeStartY, right = resizeStartX + resizeStartW, bottom = resizeStartY + resizeStartH;
        if (resizeCorner == 1 || resizeCorner == 3) left = mouseX;
        else right = mouseX;
        if (resizeCorner == 1 || resizeCorner == 2) top = mouseY;
        else bottom = mouseY;
        int nw = Math.max(8, right - left), nh = Math.max(8, bottom - top);
        if (keepAspect) {
            float aspect = resizeStartW / (float) resizeStartH;
            if (nw / (float) nh > aspect) nw = Math.max(8, Math.round(nh * aspect));
            else nh = Math.max(8, Math.round(nw / aspect));
            if (resizeCorner == 1 || resizeCorner == 3) left = right - nw;
            if (resizeCorner == 1 || resizeCorner == 2) top = bottom - nh;
        }
        selected.xPos = left;
        selected.yPos = top;
        selected.width = nw;
        selected.height = nh;
        selected.position = Position.absolute(left, top);
    }

    private void align(CMMElement moving) {
        int[] best = moving.getCorners(), mX = {best[0], best[2], (best[0] + best[2]) / 2}, mY = {best[1], best[3], (best[1] + best[3]) / 2};
        for (CMMElement other : config.elements)
            if (other != moving) {
                int[] c = other.getCorners(), x = {c[0], c[2], (c[0] + c[2]) / 2}, y = {c[1], c[3], (c[1] + c[3]) / 2};
                for (int i = 0; i < 3; i++) {
                    if (Math.abs(mX[i] - x[i]) <= 6) {
                        moving.xPos += x[i] - mX[i];
                        best = moving.getCorners();
                        mX = new int[]{best[0], best[2], (best[0] + best[2]) / 2};
                    }
                    if (Math.abs(mY[i] - y[i]) <= 6) {
                        moving.yPos += y[i] - mY[i];
                        best = moving.getCorners();
                        mY = new int[]{best[1], best[3], (best[1] + best[3]) / 2};
                    }
                }
            }
    }

    private List<String> names() {
        List<String> n = new ArrayList<>(CMMHelper.configList.keySet());
        Collections.sort(n);
        return n;
    }

    private void title(String text) {
        fontRendererObj.drawString(text, width / 2 - fontRendererObj.getStringWidth(text) / 2, 12, 0xffffffff);
    }

    private void panel(int x, int y, int r, int b) {
        NineSliceUtils.draw(Resources.storageBackground(1), x, y, r - x, b - y, 6, 18);
    }

    private void button(int x, int y, int w, int h, String text) {
        drawRect(x, y, x + w, y + h, 0xff263b4d);
        fontRendererObj.drawString(text, x + (w - fontRendererObj.getStringWidth(text)) / 2, y + 6, 0xffffffff);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        if (addGui == null) Mouse.getEventDWheel();
    }
}
