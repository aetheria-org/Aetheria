package io.hamlook.aetheria.features.custommenu.editor;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.custommenu.CustomMMConfig;
import io.hamlook.aetheria.features.custommenu.Position;
import io.hamlook.aetheria.features.custommenu.ui.CMMElement;
import io.hamlook.aetheria.features.custommenu.ui.buttons.CMMButton;
import io.hamlook.aetheria.features.custommenu.ui.buttons.ButtonStyle;
import io.hamlook.aetheria.features.custommenu.ui.buttons.impl.GuiButton;
import io.hamlook.aetheria.features.custommenu.ui.text.Text;
import io.hamlook.aetheria.features.custommenu.ui.sprites.Sprite;
import io.hamlook.aetheria.features.custommenu.ui.dropdown.CMMDropdown;
import io.hamlook.aetheria.features.custommenu.util.CMMHelper;
import io.hamlook.aetheria.utils.compat.AetheriaBaseScreen;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.KeyboardCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.MouseCompat;
import io.hamlook.aetheria.features.custommenu.util.ScreenHelper;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** In-place CMM editor. It deliberately uses the legacy-screen compatibility hooks. */
public class CMMEditorGUI extends AetheriaBaseScreen {
    private final CustomMMConfig config;
    private final GuiScreen parent;
    private CMMElement selected;
    private final List<CMMElement> selectedElements = new ArrayList<>();
    private CMMElement dragging;
    private int dragOffsetX, dragOffsetY;
    private boolean resizing;
    private boolean snap;
    private boolean preview;
    private boolean help;
    private boolean sidebar;
    private int menuX, menuY;
    private boolean context;
    private GuiTextField xField, yField, widthField, heightField, opacityField;
    private GuiTextField nameField, textField, screenField;
    private CMMDropdown styleDropdown;
    private final Deque<String> undo = new ArrayDeque<>();
    private final Deque<String> redo = new ArrayDeque<>();

    public CMMEditorGUI(CustomMMConfig config, GuiScreen parent) {
        this.config = config;
        this.parent = parent;
        this.snap = ATHRConfig.feature != null && ATHRConfig.feature.cosmetics.customMenu.snapByDefault;
    }

    private int sidebarWidth() { return Math.max(220, ScreenHelper.getStaticWidth(220)); }
    private int sidebarLeft() { return width - sidebarWidth(); }
    private int sx(int value) { return ScreenHelper.getStaticWidth(value); }
    private int sy(int value) { return ScreenHelper.getStaticHeight(value); }

    @Override public void onResize(net.minecraft.client.Minecraft mcIn, int w, int h) {
        super.onResize(mcIn, w, h);
        ScreenHelper.updateScreenDimensions(w, h);
        if (config != null) for (CMMElement element : config.elements) element.updatePosition();
        if (sidebar && selected != null) openFields();
    }

    @Override protected void onInitGui() {
        ScreenHelper.updateScreenDimensions(width, height);
        if (config != null) for (CMMElement e : config.elements) e.updatePosition();
    }

    @Override protected void onDrawScreen(int mouseX, int mouseY, float partialTicks) {
        if (config == null) return;
        drawRect(0, 0, width, height, 0xCC101014);
        if (config.getBackground() != null && config.getBackground().getTextureToRender(true) != null) {
            GlStateManagerCompat.color(1f, 1f, 1f, 1f);
            MinecraftCompat.getMinecraft().getTextureManager().bindTexture(config.getBackground().getTextureToRender(true));
            GuiScreen.drawScaledCustomSizeModalRect(0, 0, 0, 0, width, height, width, height, width, height);
        }
        if (!preview && ATHRConfig.feature != null && ATHRConfig.feature.cosmetics.customMenu.showGrid) drawGrid();
        for (CMMElement e : config.elements) if (e.visible) e.draw(mouseX, mouseY, partialTicks);
        if (!preview) {
            for (CMMElement element : selectedElements) drawSelection(element);
            if (sidebar) drawSidebar(mouseX, mouseY);
            if (context) drawContextMenu(mouseX, mouseY);
            if (help) drawHelp();
        }
    }

    private void drawGrid() {
        int size = ATHRConfig.feature == null ? 10 : ATHRConfig.feature.cosmetics.customMenu.gridSize;
        for (int x = 0; x < width; x += size) drawRect(x, 0, x + 1, height, 0x183FFFFF);
        for (int y = 0; y < height; y += size) drawRect(0, y, width, y + 1, 0x183FFFFF);
    }

    private void drawSelection(CMMElement e) {
        int[] b = e.getEditorBounds();
        drawRect(b[0], b[1], b[2], b[1] + 1, 0xFF55CCFF);
        drawRect(b[0], b[3] - 1, b[2], b[3], 0xFF55CCFF);
        drawRect(b[0], b[1], b[0] + 1, b[3], 0xFF55CCFF);
        drawRect(b[2] - 1, b[1], b[2], b[3], 0xFF55CCFF);
        drawRect(b[2] - 6, b[3] - 6, b[2] + 6, b[3] + 6, 0xFFFFFFFF);
    }

    private void drawToolbar() {
        drawRect(0, 0, width, 24, 0xEE17171D);
        TextRenderUtils.drawStringScaleAware("CMM Editor: " + config.configName, 8, 7, 0xFFFFFFFF, 1f, false);
        TextRenderUtils.drawStringScaleAware("Right-click: menu | S: save | P: preview | G: grid | H: help | A: add", 220, 7, 0xFFB8B8C8, 1f, false);
    }

    private void drawSidebar(int mouseX, int mouseY) {
        int left = sidebarLeft();
        drawRect(left, sy(24), width, height, 0xF018181F);
        TextRenderUtils.drawStringScaleAware("Element Properties", left + sx(12), sy(38), 0xFFFFFFFF, 1.4f, false);
        if (selected == null) {
            TextRenderUtils.drawStringScaleAware("Select an element", left + sx(12), sy(65), 0xFFAAAAAA, 1f, false);
            return;
        }
        String name = selected.displayName == null || selected.displayName.isEmpty() ? selected.getClass().getSimpleName() : selected.displayName;
        TextRenderUtils.drawStringScaleAware(name, left + sx(12), sy(65), 0xFF55CCFF, 1f, false);
        TextRenderUtils.drawStringScaleAware("Locked: " + selected.locked, left + sx(12), sy(292), 0xFFE0E0E0, 1f, false);
        TextRenderUtils.drawStringScaleAware("Animation: " + (selected.animation == null ? "None" : selected.animation.type), left + sx(12), sy(316), 0xFFE0E0E0, 1f, false);
        smallButton(left + sx(12), sy(260), sx(92), sy(20), "Lock", mouseX, mouseY);
        smallButton(left + sx(112), sy(260), sx(92), sy(20), "Reset Pos", mouseX, mouseY);
        smallButton(left + sx(12), sy(312), sx(192), sy(20), "Reset Element", mouseX, mouseY);
        drawField(xField, left + sx(72), sy(72)); drawField(yField, left + sx(72), sy(98)); drawField(widthField, left + sx(72), sy(124)); drawField(heightField, left + sx(72), sy(150)); drawField(opacityField, left + sx(72), sy(176));
        TextRenderUtils.drawStringScaleAware("X", left + sx(12), sy(78), 0xFFE0E0E0, 1f, false); TextRenderUtils.drawStringScaleAware("Y", left + sx(12), sy(104), 0xFFE0E0E0, 1f, false); TextRenderUtils.drawStringScaleAware("Width", left + sx(12), sy(130), 0xFFE0E0E0, 1f, false); TextRenderUtils.drawStringScaleAware("Height", left + sx(12), sy(156), 0xFFE0E0E0, 1f, false); TextRenderUtils.drawStringScaleAware("Opacity", left + sx(12), sy(182), 0xFFE0E0E0, 1f, false);
        if (nameField != null) { TextRenderUtils.drawStringScaleAware(selected instanceof CMMButton ? "Text" : "Name", left + sx(12), sy(214), 0xFFE0E0E0, 1f, false); drawField(nameField, left + sx(72), sy(208)); }
        if (selected instanceof Text && textField != null) { TextRenderUtils.drawStringScaleAware("Text", left + sx(12), sy(240), 0xFFE0E0E0, 1f, false); drawField(textField, left + sx(72), sy(234)); }
        if (selected instanceof GuiButton) { TextRenderUtils.drawStringScaleAware("Screen", left + sx(12), sy(240), 0xFFE0E0E0, 1f, false); smallButton(left + sx(72), sy(234), sx(130), sy(18), ((GuiButton) selected).screen == null ? "Select GUI" : ((GuiButton) selected).screen, mouseX, mouseY); }
        if (selected instanceof Text) { Text text = (Text) selected; TextRenderUtils.drawStringScaleAware("Font Size: " + String.format("%.2f", text.scale), left + sx(12), sy(270), 0xFFE0E0E0, 1f, false); drawRect(left + sx(12), sy(286), left + sx(204), sy(291), 0xFF4A4A55); drawRect(left + sx(12), sy(286), left + sx(12) + Math.round(Math.max(0f, Math.min(1f, (text.scale - .25f) / 3.75f)) * sx(192)), sy(291), 0xFF55A8CC); }
        if (selected instanceof CMMDropdown) smallButton(left + sx(12), sy(340), sx(192), sy(20), "Edit Items", mouseX, mouseY);
        if (selected instanceof Sprite) smallButton(left + sx(12), sy(340), sx(192), sy(20), "Change Image", mouseX, mouseY);
        if (selected instanceof CMMButton && styleDropdown != null) { TextRenderUtils.drawStringScaleAware("Button Style", left + sx(12), sy(346), 0xFFE0E0E0, 1f, false); styleDropdown.draw(mouseX, mouseY, 0); }
    }

    private void drawField(GuiTextField field, int x, int y) { if (field == null) return; field.xPosition=x; field.yPosition=y; field.width=ScreenHelper.getStaticWidth(130); field.height=ScreenHelper.getStaticHeight(18); field.drawTextBox(); }
    private void openFields() { int l=sidebarLeft(); int sw=ScreenHelper.getStaticWidth(130); xField=new GuiTextField(10,MinecraftCompat.getFontRenderer(),l+ScreenHelper.getStaticWidth(72),ScreenHelper.getStaticHeight(72),sw,ScreenHelper.getStaticHeight(18)); yField=new GuiTextField(11,MinecraftCompat.getFontRenderer(),l+ScreenHelper.getStaticWidth(72),ScreenHelper.getStaticHeight(98),sw,ScreenHelper.getStaticHeight(18)); widthField=new GuiTextField(12,MinecraftCompat.getFontRenderer(),l+ScreenHelper.getStaticWidth(72),ScreenHelper.getStaticHeight(124),sw,ScreenHelper.getStaticHeight(18)); heightField=new GuiTextField(13,MinecraftCompat.getFontRenderer(),l+ScreenHelper.getStaticWidth(72),ScreenHelper.getStaticHeight(150),sw,ScreenHelper.getStaticHeight(18)); opacityField=new GuiTextField(14,MinecraftCompat.getFontRenderer(),l+ScreenHelper.getStaticWidth(72),ScreenHelper.getStaticHeight(176),sw,ScreenHelper.getStaticHeight(18)); nameField=new GuiTextField(15,MinecraftCompat.getFontRenderer(),l+ScreenHelper.getStaticWidth(72),ScreenHelper.getStaticHeight(208),sw,ScreenHelper.getStaticHeight(18)); textField=new GuiTextField(16,MinecraftCompat.getFontRenderer(),l+ScreenHelper.getStaticWidth(72),ScreenHelper.getStaticHeight(234),sw,ScreenHelper.getStaticHeight(18)); screenField=null; styleDropdown=new CMMDropdown(Position.absolute(l+ScreenHelper.getStaticWidth(72),ScreenHelper.getStaticHeight(368)),sw,ScreenHelper.getStaticHeight(18),new ArrayList<CMMDropdown.Item>()); styleDropdown.xPos=l+ScreenHelper.getStaticWidth(72); styleDropdown.yPos=ScreenHelper.getStaticHeight(368); ArrayList<CMMDropdown.Item> styles=new ArrayList<>(); for(ButtonStyle style:ButtonStyle.values()){CMMDropdown.NameItem item=new CMMDropdown.NameItem(style.label);item.id=style.name();styles.add(item);} styleDropdown.setItems(styles); syncFields(); }
    private int textWidth(Text text) { return text.getEditorBounds()[2] - text.getEditorBounds()[0]; }
    private int textHeight(Text text) { return text.getEditorBounds()[3] - text.getEditorBounds()[1]; }
    private void syncFields() { if(selected==null)return; xField.setText(""+selected.xPos); yField.setText(""+selected.yPos); widthField.setText(""+(selected instanceof Text ? textWidth((Text)selected) : Math.max(1,selected.width))); heightField.setText(""+(selected instanceof Text ? textHeight((Text)selected) : (selected instanceof CMMDropdown ? ((CMMDropdown)selected).itemHeight : Math.max(1,selected.height)))); opacityField.setText(""+selected.opacity); nameField.setText(selected instanceof CMMButton ? (((CMMButton)selected).displayString == null ? "" : ((CMMButton)selected).displayString) : (selected.displayName == null ? "" : selected.displayName)); if(selected instanceof Text) textField.setText(((Text)selected).text == null ? "" : ((Text)selected).text); if(selected instanceof CMMButton && styleDropdown != null) styleDropdown.setSelectedItem(((CMMButton)selected).style == null ? ButtonStyle.DEFAULT.name() : ((CMMButton)selected).style.name()); }
    private boolean fieldFocused() { return xField != null && (xField.isFocused() || yField.isFocused() || widthField.isFocused() || heightField.isFocused() || opacityField.isFocused() || nameField.isFocused() || textField.isFocused()); }
    private void commitFields() { if(selected==null)return; try{selected.xPos=Integer.parseInt(xField.getText());selected.yPos=Integer.parseInt(yField.getText());if(selected instanceof Text){Text text=(Text)selected;int rawWidth=Math.max(1,MinecraftCompat.getMinecraft().fontRendererObj.getStringWidth(text.text==null?"":text.text));int rawHeight=Math.max(1,MinecraftCompat.getMinecraft().fontRendererObj.FONT_HEIGHT);text.scale=Math.max(.25f,Math.min(4f,Integer.parseInt(widthField.getText())/(float)rawWidth));if(Integer.parseInt(heightField.getText())>0)text.scale=Math.max(.25f,Math.min(4f,Integer.parseInt(heightField.getText())/(float)rawHeight));}else{selected.width=Math.max(1,Integer.parseInt(widthField.getText()));if(selected instanceof CMMDropdown)((CMMDropdown)selected).setEditorItemHeight(Integer.parseInt(heightField.getText()));else selected.height=Math.max(1,Integer.parseInt(heightField.getText()));}selected.opacity=Math.max(0f,Math.min(1f,Float.parseFloat(opacityField.getText())));}catch(Exception ignored){} if(selected instanceof CMMButton){CMMButton button=(CMMButton)selected;button.displayString=nameField.getText();if(styleDropdown!=null)try{button.style=ButtonStyle.valueOf(styleDropdown.getSelectedItem());}catch(Exception ignored){}}else selected.displayName=nameField.getText(); if(selected instanceof Text)((Text)selected).text=textField.getText(); selected.position=Position.absolute(selected.xPos,selected.yPos); }

    private void smallButton(int x, int y, int w, int h, String label, int mx, int my) {
        drawRect(x, y, x + w, y + h, mx >= x && mx <= x + w && my >= y && my <= y + h ? 0xFF3B6982 : 0xFF292932);
        TextRenderUtils.drawCenteredStringScaleAware(label, x + w / 2f, y + h / 2f, 0xFFFFFFFF, 1f, false);
    }

    private void drawContextMenu(int mouseX, int mouseY) {
        int h = 12 * 18;
        drawRect(menuX, menuY, menuX + 150, menuY + h, 0xF025252D);
        String[] items = {"Edit", "Add Element", "Duplicate", "Copy", "Paste", "Delete", snap ? "Disable Snap" : "Enable Snap", "Edit Background", "Animation Presets", "Timeline", "Layers", "Help"};
        for (int i = 0; i < items.length; i++) {
            boolean hovered = mouseX >= menuX && mouseX <= menuX + 150 && mouseY >= menuY + i * 18 && mouseY < menuY + (i + 1) * 18;
            if (hovered) drawRect(menuX, menuY + i * 18, menuX + 150, menuY + (i + 1) * 18, 0xFF3A6680);
            TextRenderUtils.drawStringScaleAware(items[i], menuX + 8, menuY + i * 18 + 5, 0xFFFFFFFF, 1f, false);
        }
    }

    private void drawHelp() {
        drawRect(width / 2 - 210, height / 2 - 90, width / 2 + 210, height / 2 + 90, 0xEF15151B);
        String[] lines = {"CMM Editor Help", "Left click: select and drag", "Right click: context menu", "Shift: free movement / keep aspect ratio while resizing", "Delete: remove | D: duplicate | Ctrl+Z/Y: undo/redo", "S: save | P: preview | G: grid | Escape: back"};
        for (int i = 0; i < lines.length; i++) TextRenderUtils.drawCenteredStringScaleAware(lines[i], width / 2f, height / 2f - 65 + i * 24, 0xFFE6E6E6, 1f, false);
    }

    @Override protected void onMouseClicked(int mouseX, int mouseY, int button) {
        if (help) { help = false; return; }
        if (preview) return;
        if (button == 1) { context = true; menuX = Math.min(mouseX, Math.max(0, width - 150)); menuY = Math.min(mouseY, Math.max(0, height - 216)); selected = elementAt(mouseX, mouseY); return; }
        if (sidebar && selected != null && xField != null) {
            xField.mouseClicked(mouseX,mouseY,button); yField.mouseClicked(mouseX,mouseY,button); widthField.mouseClicked(mouseX,mouseY,button); heightField.mouseClicked(mouseX,mouseY,button); opacityField.mouseClicked(mouseX,mouseY,button);
            nameField.mouseClicked(mouseX,mouseY,button); textField.mouseClicked(mouseX,mouseY,button);
            if (selected instanceof CMMButton && styleDropdown != null && styleDropdown.onMouseClick(mouseX, mouseY)) { commitFields(); return; }
            if (selected instanceof GuiButton && mouseX >= sidebarLeft() + sx(72) && mouseX <= sidebarLeft() + sx(202) && mouseY >= sy(234) && mouseY < sy(252)) { commitFields(); MinecraftCompat.getMinecraft().displayGuiScreen(new CMMGuiPickerGUI((GuiButton) selected, this)); return; }
            if (selected instanceof Text && mouseX >= sidebarLeft() + sx(12) && mouseX <= sidebarLeft() + sx(204) && mouseY >= sy(282) && mouseY < sy(296)) { setTextScale(mouseX); return; }
            if (handleSidebar(mouseX, mouseY)) return;
            if (mouseX >= sidebarLeft()) return;
        }
        if (context) { if (handleContext(mouseX, mouseY)) return; context = false; }
        if (button != 0) return;
        if (mouseY < 24) return;
        CMMElement clicked = elementAt(mouseX, mouseY);
        if (KeyboardCompat.isKeyDown(Keyboard.KEY_LCONTROL)) {
            if (clicked != null && selectedElements.contains(clicked)) selectedElements.remove(clicked);
            else if (clicked != null) selectedElements.add(clicked);
            selected = clicked;
        } else {
            selectedElements.clear();
            selected = clicked;
            if (selected != null) selectedElements.add(selected);
        }
        if (selected != null && !selected.locked) {
            pushUndo();
            int[] b = selected.getEditorBounds();
            resizing = mouseX >= b[2] - 14 && mouseY >= b[3] - 14 && mouseX <= b[2] + 14 && mouseY <= b[3] + 14;
            if (resizing) {
            dragging = selected;
                dragOffsetX = mouseX;
                dragOffsetY = mouseY;
            } else {
                dragging = selected;
                dragOffsetX = mouseX - selected.xPos;
                dragOffsetY = mouseY - selected.yPos;
            }
        }
    }

    private boolean handleContext(int x, int y) {
        if (x < menuX || x > menuX + 150 || y < menuY || y >= menuY + 216) return false;
        int action = (y - menuY) / 18;
        switch (action) {
            case 0: sidebar = selected != null; if(sidebar) openFields(); break;
            case 1: MinecraftCompat.getMinecraft().displayGuiScreen(new CMMAddElementGUI(config, this)); break;
            case 2: duplicateSelected(); break;
            case 3: CMMClipboard.copy(selected); break;
            case 4: { CMMElement pasted = CMMClipboard.paste(); if (pasted != null) { pushUndo(); pasted.xPos += 10; pasted.yPos += 10; pasted.position = Position.absolute(pasted.xPos, pasted.yPos); config.addElement(pasted); selected = pasted; } break; }
            case 5: if (selected != null) { pushUndo(); config.removeElement(selected); selected = null; } break;
            case 6: snap = !snap; break;
            case 7: MinecraftCompat.getMinecraft().displayGuiScreen(new CMMBackgroundEditor(config, this)); break;
            case 8: if (selected != null) MinecraftCompat.getMinecraft().displayGuiScreen(new CMMAnimationGraphGUI(selected, this)); break;
            case 9: if (selected != null) MinecraftCompat.getMinecraft().displayGuiScreen(new CMMTimelineGUI(selected, this)); break;
            case 10: MinecraftCompat.getMinecraft().displayGuiScreen(new CMMLayersGUI(config, this)); break;
            case 11: help = true; break;
        }
        context = false;
        return true;
    }

    @Override protected void onMouseClickMove(int mouseX, int mouseY, int button, long timeSinceLastClick) {
        if (dragging == null || button != 0) return;
        if (resizing) {
            int newWidth = Math.max(8, mouseX - dragging.xPos);
            int newHeight = Math.max(8, mouseY - dragging.yPos);
            if (KeyboardCompat.isKeyDown(Keyboard.KEY_LSHIFT) && dragging.width > 0 && dragging.height > 0) {
                float ratio = dragging.width / (float) dragging.height;
                newHeight = Math.max(8, Math.round(newWidth / ratio));
            }
            if (dragging instanceof Text) { Text text = (Text) dragging; int raw = Math.max(1, MinecraftCompat.getMinecraft().fontRendererObj.getStringWidth(text.text == null ? "" : text.text)); text.scale = Math.max(.25f, Math.min(4f, newWidth / (float) raw)); }
            else { dragging.width = newWidth; dragging.height = newHeight; }
            if (sidebar && !fieldFocused()) syncFields();
            return;
        }
        int x = mouseX - dragOffsetX, y = mouseY - dragOffsetY;
        if (snap && !KeyboardCompat.isKeyDown(Keyboard.KEY_LSHIFT) && ATHRConfig.feature != null) {
            int size = Math.max(1, ATHRConfig.feature.cosmetics.customMenu.gridSize);
            x = Math.round(x / (float) size) * size;
            y = Math.round(y / (float) size) * size;
        }
        int dx = x - dragging.xPos, dy = y - dragging.yPos;
        for (CMMElement element : selectedElements) { element.xPos += dx; element.yPos += dy; element.position = Position.absolute(element.xPos, element.yPos); }
    }

    @Override protected void onMouseReleased(int mouseX, int mouseY, int state) { dragging = null; if (sidebar && selected != null) syncFields(); }

    private void setTextScale(int mouseX) { if (!(selected instanceof Text)) return; Text text = (Text) selected; int left = sidebarLeft(); text.scale = .25f + 3.75f * Math.max(0f, Math.min(1f, (mouseX - (left + sx(12))) / (float) sx(192))); syncFields(); }

    private boolean handleSidebar(int x, int y) {
        int left = sidebarLeft();
        if (x < left || selected == null) return false;
        if (y >= sy(260) && y < sy(280) && x < left + sx(104)) { selected.locked = !selected.locked; return true; }
        if (y >= sy(260) && y < sy(280) && x >= left + sx(104)) { selected.xPos = width / 2 - selected.width / 2; selected.yPos = height / 2 - selected.height / 2; selected.position = Position.absolute(selected.xPos, selected.yPos); syncFields(); return true; }
        if (y >= sy(312) && y < sy(336)) { pushUndo(); selected.position = Position.absolute(0, 0); selected.xPos = 0; selected.yPos = 0; selected.opacity = 1f; selected.rotation = 0f; selected.scaleX = 1f; selected.scaleY = 1f; syncFields(); return true; }
        if (y >= sy(340) && y < sy(364) && selected instanceof Sprite) { commitFields(); MinecraftCompat.getMinecraft().displayGuiScreen(new CMMBackgroundEditor((Sprite) selected, this)); return true; }
        if (y >= sy(340) && y < sy(364) && selected instanceof CMMDropdown) { commitFields(); MinecraftCompat.getMinecraft().displayGuiScreen(new CMMDropdownEditorGUI((CMMDropdown) selected, this)); return true; }
        return false;
    }

    @Override protected void onKeyTyped(char typedChar, int keyCode) {
        if (sidebar && xField != null) { if(xField.isFocused()){xField.textboxKeyTyped(typedChar,keyCode);commitFields();return;} if(yField.isFocused()){yField.textboxKeyTyped(typedChar,keyCode);commitFields();return;} if(widthField.isFocused()){widthField.textboxKeyTyped(typedChar,keyCode);commitFields();return;} if(heightField.isFocused()){heightField.textboxKeyTyped(typedChar,keyCode);commitFields();return;} if(opacityField.isFocused()){opacityField.textboxKeyTyped(typedChar,keyCode);commitFields();return;} if(nameField.isFocused()){nameField.textboxKeyTyped(typedChar,keyCode);commitFields();return;} if(textField.isFocused()){textField.textboxKeyTyped(typedChar,keyCode);commitFields();return;} }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (context || help || preview || sidebar) { context = false; help = false; preview = false; sidebar = false; return; }
            save(); MinecraftCompat.getMinecraft().displayGuiScreen(new io.hamlook.aetheria.features.custommenu.selector.CMMSelectorGUI()); return;
        }
        int saveKey = ATHRConfig.feature == null ? Keyboard.KEY_C : ATHRConfig.feature.cosmetics.customMenu.keySave;
        int previewKey = ATHRConfig.feature == null ? Keyboard.KEY_P : ATHRConfig.feature.cosmetics.customMenu.keyPreview;
        int gridKey = ATHRConfig.feature == null ? Keyboard.KEY_G : ATHRConfig.feature.cosmetics.customMenu.keyGrid;
        if (keyCode == saveKey) save();
        else if (keyCode == previewKey) preview = !preview;
        else if (keyCode == gridKey) ATHRConfig.feature.cosmetics.customMenu.showGrid = !ATHRConfig.feature.cosmetics.customMenu.showGrid;
        else if (keyCode == (ATHRConfig.feature == null ? Keyboard.KEY_DELETE : ATHRConfig.feature.cosmetics.customMenu.keyDelete) && selected != null) { pushUndo(); config.removeElement(selected); selected = null; }
        else if (keyCode == (ATHRConfig.feature == null ? Keyboard.KEY_D : ATHRConfig.feature.cosmetics.customMenu.keyDuplicate)) duplicateSelected();
        else if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) && keyCode == (ATHRConfig.feature == null ? Keyboard.KEY_Z : ATHRConfig.feature.cosmetics.customMenu.keyUndo)) undo();
        else if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) && keyCode == (ATHRConfig.feature == null ? Keyboard.KEY_Y : ATHRConfig.feature.cosmetics.customMenu.keyRedo)) redo();
        else if (keyCode == (ATHRConfig.feature == null ? Keyboard.KEY_A : ATHRConfig.feature.cosmetics.customMenu.keySelectAll)) { selectedElements.clear(); selectedElements.addAll(config.elements); selected = config.elements.isEmpty() ? null : config.elements.get(0); }
        else if (keyCode == (ATHRConfig.feature == null ? Keyboard.KEY_H : ATHRConfig.feature.cosmetics.customMenu.keyHelp)) help = true;
        else if (keyCode == Keyboard.KEY_N) MinecraftCompat.getMinecraft().displayGuiScreen(new CMMAddElementGUI(config, this));
    }

    private CMMElement elementAt(int x, int y) {
        ArrayList<CMMElement> copy = new ArrayList<>(config.elements);
        for (int i = copy.size() - 1; i >= 0; i--) { int[] b = copy.get(i).getEditorBounds(); if (x >= b[0] && x <= b[2] && y >= b[1] && y <= b[3]) return copy.get(i); }
        return null;
    }
    private void duplicateSelected() { if (selected == null) return; pushUndo(); CMMElement copy = CMMHelper.copyElement(selected); if (copy == null) return; copy.xPos += 10; copy.yPos += 10; copy.position = Position.absolute(copy.xPos, copy.yPos); config.addElement(copy); selected = copy; }
    private void pushUndo() { undo.push(CMMHelper.GSON.toJson(config)); redo.clear(); while (undo.size() > 30) undo.removeLast(); }
    private void undo() { if (undo.isEmpty()) return; redo.push(CMMHelper.GSON.toJson(config)); restore(undo.pop()); }
    private void redo() { if (redo.isEmpty()) return; undo.push(CMMHelper.GSON.toJson(config)); restore(redo.pop()); }
    private void restore(String json) { CustomMMConfig restored = CMMHelper.GSON.fromJson(json, CustomMMConfig.class); config.elements = restored.elements; config.background = restored.background; for (CMMElement e : config.elements) e.updatePosition(); selected = null; }
    private void save() { CMMHelper.savePreset(config); }
}
