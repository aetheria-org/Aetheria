package io.hamlook.aetheria.features.chatfilters.ui;

import io.hamlook.aetheria.core.config.gui.GuiTextures;
import io.hamlook.aetheria.features.chatfilters.ChatFilter;
import io.hamlook.aetheria.features.chatfilters.ChatFilterManager;
import io.hamlook.aetheria.features.chatfilters.vars.FilterAction;
import io.hamlook.aetheria.features.chatfilters.vars.FilterCase;
import io.hamlook.aetheria.features.chatfilters.vars.FilterMode;
import io.hamlook.aetheria.utils.render.NineSliceUtils;
import io.hamlook.aetheria.utils.render.ResolutionUtils;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ChatFilterEditorGUI extends ChatFilterBaseGUI {

    private final ChatFilterGUI parent;
    private final boolean isNew;
    private final List<String> words;

    public int boxX, boxY, boxW, boxH;

    private ChatFilter filter;
    private GuiTextField addWordField;
    private FilterMode mode;
    private FilterCase filterCase;
    private FilterAction action;
    private GuiTextField customPreviewField;
    private String customPreviewResult = "";

    private int scrollY = 0;
    private int previewsScrollY = 0;
    private float textScale;

    private int dragModeLeft = 0, dragStartYLeft = 0, dragStartScrollYLeft = 0;
    private int dragModeRight = 0, dragStartYRight = 0, dragStartScrollYRight = 0;

    private ListLayout cachedLeft;
    private ListLayout cachedRight;
    private List<String> cachedPreviewLines;
    private boolean previewDirty = true;

    // Layout constants (all in unscaled units)
    private static final double PAD        = 14;
    private static final double HEADER_H   = 22;
    private static final double LABEL_H    = 16;
    private static final double FIELD_H    = BTN_H;
    private static final double ROW_GAP    = 8;
    private static final double SEC_GAP    = 14;
    private static final double LEFT_W     = 280;
    private static final double SPLIT      = LEFT_W + PAD * 2 + 10;

    public ChatFilterEditorGUI(ChatFilterGUI parent, ChatFilter filter) {
        this.parent = parent;
        this.isNew = filter == null;
        if (this.isNew) {
            this.words = new ArrayList<>();
            this.mode = FilterMode.STARTS;
            this.filterCase = FilterCase.SENSITIVE;
            this.action = FilterAction.CANCEL;
        } else {
            this.filter = filter;
            this.words = new ArrayList<>(filter.filterWords);
            this.mode = filter.filterType;
            this.filterCase = filter.filterCase;
            this.action = filter.action != null ? filter.action : (filter.replace ? FilterAction.REPLACE : FilterAction.CANCEL);
        }
    }


    private int lx()  { return boxX + getScaledX(PAD); }
    private int rx()  { return boxX + getScaledX(SPLIT); }
    private int rw()  { return boxW - getScaledX(SPLIT) - getScaledX(PAD); }

    private int secMatchY()   { return boxY + getScaledY(PAD + HEADER_H + ROW_GAP); }
    private int secCaseY()    { return secMatchY()  + getScaledY(LABEL_H + ROW_GAP + TOGGLE_H + SEC_GAP); }
    private int secActionY()  { return secCaseY()   + getScaledY(LABEL_H + ROW_GAP + TOGGLE_H + SEC_GAP); }
    private int secCustomY()  { return secActionY() + getScaledY(LABEL_H + ROW_GAP + TOGGLE_H + SEC_GAP); }
    private int secPreviewY() { return secCustomY() + getScaledY(LABEL_H + ROW_GAP + FIELD_H  + (customPreviewResult.isEmpty() ? 0 : LABEL_H + 4) + ROW_GAP); }

    private int bottomBtnY()  { return boxY + boxH - getScaledY(PAD + BTN_H); }


    @Override
    public void initGui() {
        boxW = getScaledX(760);
        boxH = getScaledY(540);
        boxX = (width  - boxW) / 2;
        boxY = (height - boxH) / 2;
        textScale = ResolutionUtils.getXStatic(1) * 2.6f * configScale();
        cachedLeft = null;
        cachedRight = null;
        previewDirty = true;

        org.lwjgl.input.Keyboard.enableRepeatEvents(true);
        initTextFields();
        initButtons();
    }

    private void initTextFields() {
        int fieldH = getScaledY(FIELD_H);

        String prevAdd = addWordField != null ? addWordField.getText() : "";
        int addFieldW  = getScaledX(LEFT_W - BTN_W - PAD);
        addWordField = new GuiTextField(0, fontRendererObj, lx(), boxY + getScaledY(PAD + HEADER_H + ROW_GAP), addFieldW, fieldH);
        addWordField.setMaxStringLength(50);
        addWordField.setText(prevAdd);

        String prevCustom = customPreviewField != null ? customPreviewField.getText() : "";
        int customFieldW  = getScaledX(rw() - BTN_W - PAD - 4);
        customPreviewField = new GuiTextField(1, fontRendererObj, rx(), secCustomY() + getScaledY(LABEL_H + ROW_GAP), customFieldW, fieldH);
        customPreviewField.setMaxStringLength(100);
        customPreviewField.setText(prevCustom);
    }

    private void initButtons() {
        buttonList.clear();

        int fieldH    = getScaledY(FIELD_H);
        int addBtnX   = lx() + getScaledX(LEFT_W - BTN_W);
        int addBtnY   = boxY + getScaledY(PAD + HEADER_H + ROW_GAP);
        buttonList.add(new CFButton(0, addBtnX, addBtnY, getScaledX(BTN_W), fieldH, "Add", 0.2f, 0.4f, 0.8f));

        int testBtnX  = rx() + getScaledX(rw() - BTN_W + 4);
        int testBtnY  = secCustomY() + getScaledY(LABEL_H + ROW_GAP);
        buttonList.add(new CFButton(8, testBtnX, testBtnY, getScaledX(BTN_W), fieldH, "Test", 0.2f, 0.4f, 0.8f));

        int th = getScaledY(TOGGLE_H);
        int tw = getScaledX(TOGGLE_W);

        int matchBtnY = secMatchY() + getScaledY(LABEL_H + ROW_GAP);
        buttonList.add(new CFButton(1, rx(),                      matchBtnY, tw, th, "STARTS",   true, mode == FilterMode.STARTS));
        buttonList.add(new CFButton(2, rx() + getScaledX(82),    matchBtnY, tw, th, "ENDS",     true, mode == FilterMode.ENDS));
        buttonList.add(new CFButton(3, rx() + getScaledX(164),   matchBtnY, getScaledX(TOGGLE_W + 10), th, "CONTAINS", true, mode == FilterMode.CONTAINS));

        int caseBtnY = secCaseY() + getScaledY(LABEL_H + ROW_GAP);
        buttonList.add(new CFButton(4, rx(),                      caseBtnY, getScaledX(TOGGLE_W + 10), th, "SENSITIVE",   true, filterCase == FilterCase.SENSITIVE));
        buttonList.add(new CFButton(5, rx() + getScaledX(92),    caseBtnY, getScaledX(TOGGLE_W + 18), th, "INSENSITIVE", true, filterCase == FilterCase.INSENSITIVE));

        int actionBtnY = secActionY() + getScaledY(LABEL_H + ROW_GAP);
        buttonList.add(new CFButton(6, rx(),                      actionBtnY, tw, th, "CANCEL",  true, action == FilterAction.CANCEL));
        buttonList.add(new CFButton(7, rx() + getScaledX(82),    actionBtnY, tw, th, "REPLACE", true, action == FilterAction.REPLACE));
        buttonList.add(new CFButton(9, rx() + getScaledX(164),   actionBtnY, tw, th, "CENSOR",  true, action == FilterAction.CENSOR));

        int saveX = boxX + boxW - getScaledX(PAD + BTN_W * 2 + PAD);
        buttonList.add(new CFButton(100, saveX,                     bottomBtnY(), getScaledX(BTN_W), getScaledY(BTN_H), "Save",   0.2f, 0.7f, 0.2f));
        buttonList.add(new CFButton(101, saveX + getScaledX(BTN_W + PAD), bottomBtnY(), getScaledX(BTN_W), getScaledY(BTN_H), "Cancel", 0.8f, 0.2f, 0.2f));
    }


    private ListLayout buildLeftLayout() {
        ListLayout l = new ListLayout();
        l.x          = lx();
        l.y          = boxY + getScaledY(PAD + HEADER_H + ROW_GAP + FIELD_H + ROW_GAP + LABEL_H + ROW_GAP);
        l.width      = getScaledX(LEFT_W);
        l.height     = bottomBtnY() - getScaledY(ROW_GAP) - l.y;
        l.itemHeight = getScaledY(38);
        l.totalHeight = words.size() * l.itemHeight;
        l.maxScroll  = Math.max(0, l.totalHeight - l.height);
        return l;
    }

    private ListLayout buildRightLayout() {
        ListLayout l  = new ListLayout();
        l.x           = rx();
        l.y           = secPreviewY() + getScaledY(LABEL_H + ROW_GAP);
        l.width       = rw();
        l.height      = bottomBtnY() - getScaledY(ROW_GAP) - l.y;
        l.itemHeight  = getScaledY(28);
        l.totalHeight = words.size() * l.itemHeight;
        l.maxScroll   = Math.max(0, l.totalHeight - l.height);
        return l;
    }


    private List<String> buildPreviewLines() {
        if (words.isEmpty()) return new ArrayList<>();
        ChatFilter tempFilter = new ChatFilter(words, mode, filterCase, action);
        List<String> lines = new ArrayList<>(words.size());
        for (int i = 0; i < words.size(); i++) {
            lines.add(buildPreviewLine(tempFilter, words.get(i), i));
        }
        return lines;
    }

    private String buildPreviewExample(String word, int index) {
        int caseType = index % 3;
        if (caseType == 1) word = word.toUpperCase();
        else if (caseType == 2) word = word.toLowerCase();
        switch (mode) {
            case STARTS: return "§e" + word + "§r quick brown fox";
            case ENDS:   return "quick brown fox §e" + word + "§r";
            default:     return "quick §e" + word + "§r brown fox";
        }
    }

    private String buildPreviewLine(ChatFilter f, String word, int index) {
        String ex  = buildPreviewExample(word, index);
        String res = f.applyFilter(ex);
        return "§7[§e" + word + "§7]§f: " + ex + " §8->§f " + (res == null ? "§c[CANCELLED]" : res);
    }


    @Override
    public void onGuiClosed() {
        org.lwjgl.input.Keyboard.enableRepeatEvents(false);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 0:   addWord(); break;
            case 1:   mode = FilterMode.STARTS; break;
            case 2:   mode = FilterMode.ENDS; break;
            case 3:   mode = FilterMode.CONTAINS; break;
            case 4:   filterCase = FilterCase.SENSITIVE; break;
            case 5:   filterCase = FilterCase.INSENSITIVE; break;
            case 6:   action = FilterAction.CANCEL; break;
            case 7:   action = FilterAction.REPLACE; break;
            case 9:   action = FilterAction.CENSOR; break;
            case 8:   testCustomSentence(); break;
            case 100: saveFilter(); mc.displayGuiScreen(parent); return;
            case 101: mc.displayGuiScreen(parent); return;
        }
        previewDirty = true;
        initGui();
    }

    private void saveFilter() {
        if (isNew) {
            ChatFilterManager.chatFilters.add(new ChatFilter(words, mode, filterCase, action));
        } else {
            filter.filterWords = words;
            filter.filterType  = mode;
            filter.filterCase  = filterCase;
            filter.action      = action;
            filter.replace     = action == FilterAction.REPLACE;
        }
        ChatFilterManager.saveToFile();
    }

    @Override
    public void updateScreen() {
        addWordField.updateCursorCounter();
        customPreviewField.updateCursorCounter();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
            if (addWordField.isFocused())       { addWordField.setFocused(false);       return; }
            if (customPreviewField.isFocused()) { customPreviewField.setFocused(false); return; }
        }
        if (addWordField.isFocused()) {
            addWordField.textboxKeyTyped(typedChar, keyCode);
            if (keyCode == 28) addWord();
        } else if (customPreviewField.isFocused()) {
            customPreviewField.textboxKeyTyped(typedChar, keyCode);
            if (keyCode == 28) testCustomSentence();
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    private void testCustomSentence() {
        String text = customPreviewField.getText();
        if (text.isEmpty()) { customPreviewResult = ""; return; }
        String result = new ChatFilter(words, mode, filterCase, action).applyFilter(text);
        customPreviewResult = result == null ? "§c[CANCELLED]" : result;
        cachedRight = null;
    }

    private void addWord() {
        String w = addWordField.getText().trim();
        if (!w.isEmpty() && !words.contains(w)) {
            words.add(w);
            addWordField.setText("");
            previewDirty = true;
            cachedLeft  = null;
            cachedRight = null;
        }
    }


    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        GlStateManager.color(0.18f, 0.18f, 0.18f, 1f);
        NineSliceUtils.draw(GuiTextures.storageBackground(1), boxX, boxY, boxW, boxH, 6, 18);
        GlStateManager.color(1f, 1f, 1f, 1f);

        if (previewDirty) {
            cachedPreviewLines = buildPreviewLines();
            previewDirty = false;
        }
        if (cachedLeft  == null) cachedLeft  = buildLeftLayout();
        if (cachedRight == null) cachedRight = buildRightLayout();

        updateScrollDragging(mouseY, cachedLeft, cachedRight);
        handleMouseWheel(mouseX, cachedLeft, cachedRight);

        drawSectionBackgrounds(cachedLeft, cachedRight);
        drawHeader();
        drawSettingsPanel();
        drawRightPreviewList(cachedRight);
        drawLeftWordList(mouseX, mouseY, cachedLeft);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawSectionBackgrounds(ListLayout left, ListLayout right) {
        int pad = getScaledX(6);

        GlStateManager.color(0.12f, 0.12f, 0.12f, 1f);

        // Left: word list area
        NineSliceUtils.draw(GuiTextures.storageBackground(1),
                left.x - pad, left.y - pad,
                left.width + pad * 2, left.height + pad * 2,
                6, 18);

        // Right: settings section (match/case/action/custom)
        int settingsTop = secMatchY() - getScaledY(4);
        int settingsBot = secPreviewY() - getScaledY(4);
        NineSliceUtils.draw(GuiTextures.storageBackground(1),
                right.x - pad, settingsTop,
                right.width + pad * 2, settingsBot - settingsTop,
                6, 18);

        // Right: preview section
        NineSliceUtils.draw(GuiTextures.storageBackground(1),
                right.x - pad, settingsBot,
                right.width + pad * 2, (bottomBtnY() - getScaledY(4)) - settingsBot,
                6, 18);

        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private void drawHeader() {
        GlStateManager.color(1f, 1f, 1f, 1f);
        TextRenderUtils.drawStringScaleAware(
                isNew ? "§lCreate Chat Filter" : "§lEdit Chat Filter",
                lx(), boxY + getScaledY(PAD), textScale * 1.4f, false);

        int fieldY = boxY + getScaledY(PAD + HEADER_H + ROW_GAP);
        TextRenderUtils.drawStringScaleAware("§7Filter Word:", lx(), fieldY - getScaledY(LABEL_H), textScale, false);
        drawTextField(addWordField, "e.g. badword, spam...");

        int tipY = addWordField.yPosition + addWordField.height + getScaledY(ROW_GAP / 2);
        TextRenderUtils.drawStringScaleAware("§8Use & for colors (&c), \\& for literal &. Press ENTER to add.", lx(), tipY, textScale * 0.85f, false);
    }

    private void drawSettingsPanel() {
        int sx = rx();
        GlStateManager.color(1f, 0.8f, 0.2f, 1f);
        TextRenderUtils.drawStringScaleAware("Match Type:",     sx, secMatchY(),  textScale, false);
        TextRenderUtils.drawStringScaleAware("Case:",           sx, secCaseY(),   textScale, false);
        TextRenderUtils.drawStringScaleAware("Action:",         sx, secActionY(), textScale, false);
        GlStateManager.color(0.75f, 0.75f, 0.75f, 1f);
        TextRenderUtils.drawStringScaleAware("Custom Test:",    sx, secCustomY(), textScale, false);
        TextRenderUtils.drawStringScaleAware("§7Previews:",     sx, secPreviewY(), textScale, false);
        GlStateManager.color(1f, 1f, 1f, 1f);

        drawTextField(customPreviewField, "Type a sentence to test...");

        if (!customPreviewResult.isEmpty()) {
            int resultY = customPreviewField.yPosition + customPreviewField.height + getScaledY(4);
            GlStateManager.color(0.75f, 0.75f, 0.75f, 1f);
            TextRenderUtils.drawStringScaleAware("Result: " + customPreviewResult, sx, resultY, textScale, false);
            GlStateManager.color(1f, 1f, 1f, 1f);
        }
    }

    private void drawTextField(GuiTextField field, String placeholder) {
        int x = field.xPosition, y = field.yPosition, w = field.width, h = field.height;
        Gui.drawRect(x, y, x + w, y + h, 0xFF2C2C2C);
        Gui.drawRect(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF111111);

        int textY = y + (h - fontRendererObj.FONT_HEIGHT) / 2;
        int maxW  = w - 10;
        String text    = field.getText();
        String display = fontRendererObj.trimStringToWidth(text, maxW);

        if (text.isEmpty()) {
            fontRendererObj.drawStringWithShadow("§7" + placeholder, x + 5, textY, 0x888888);
        } else {
            fontRendererObj.drawStringWithShadow(display, x + 5, textY, field.isFocused() ? 0xFFFFFF : 0xAAAAAA);
        }

        if (field.isFocused() && System.currentTimeMillis() % 1000 > 500) {
            int cursor    = Math.min(field.getCursorPosition(), text.length());
            String before = fontRendererObj.trimStringToWidth(text.substring(0, cursor), maxW);
            int cx = x + 5 + fontRendererObj.getStringWidth(before);
            Gui.drawRect(cx, textY - 1, cx + 1, textY + fontRendererObj.FONT_HEIGHT + 1, 0xFFFFFFFF);
        }
    }


    private void updateScrollDragging(int mouseY, ListLayout left, ListLayout right) {
        if (Mouse.isButtonDown(0)) {
            scrollY       = applyDraggedScroll(mouseY, dragModeLeft,  dragStartYLeft,  dragStartScrollYLeft,  scrollY,       left.height,  left.totalHeight,  left.maxScroll);
            previewsScrollY = applyDraggedScroll(mouseY, dragModeRight, dragStartYRight, dragStartScrollYRight, previewsScrollY, right.height, right.totalHeight, right.maxScroll);
        } else {
            dragModeLeft  = 0;
            dragModeRight = 0;
        }
        scrollY       = clampScroll(scrollY,       left.maxScroll);
        previewsScrollY = clampScroll(previewsScrollY, right.maxScroll);
    }

    private void handleMouseWheel(int mouseX, ListLayout left, ListLayout right) {
        int dWheel = Mouse.getDWheel();
        if (dWheel == 0) return;
        if (mouseX < rx()) {
            scrollY = clampScroll(scrollY - Integer.signum(dWheel) * getScaledY(20), left.maxScroll);
        } else {
            previewsScrollY = clampScroll(previewsScrollY - Integer.signum(dWheel) * getScaledY(20), right.maxScroll);
        }
    }


    private void drawRightPreviewList(ListLayout layout) {
        if (words.isEmpty()) {
            GlStateManager.color(0.5f, 0.5f, 0.5f, 1f);
            TextRenderUtils.drawStringScaleAware("Add a filter word to see live previews.", layout.x, layout.y + getScaledY(6), textScale, false);
            GlStateManager.color(1f, 1f, 1f, 1f);
            return;
        }
        startScissor(layout.x, layout.y, layout.width, layout.height);
        int pY = layout.y - previewsScrollY;
        for (String line : cachedPreviewLines) {
            GlStateManager.color(0.75f, 0.75f, 0.75f, 1f);
            TextRenderUtils.drawStringScaleAware(line, layout.x, pY, textScale * 0.85f, false);
            pY += layout.itemHeight;
        }
        stopScissor();
        GlStateManager.color(1f, 1f, 1f, 1f);
        drawScrollbar(layout.x + layout.width - getScaledX(6), layout.y, layout.height, layout.totalHeight, previewsScrollY, layout.maxScroll);
    }

    private void drawLeftWordList(int mouseX, int mouseY, ListLayout layout) {
        if (words.isEmpty()) {
            GlStateManager.color(0.45f, 0.45f, 0.45f, 1f);
            TextRenderUtils.drawStringScaleAware("No filter words added.", layout.x + getScaledX(8), layout.y + getScaledY(8), textScale, false);
            GlStateManager.color(1f, 1f, 1f, 1f);
            return;
        }
        startScissor(layout.x, layout.y, layout.width, layout.height);
        int curY   = layout.y - scrollY;
        int itemW  = layout.width - getScaledX(10);
        int delW   = getScaledX(26);
        int delH   = getScaledY(24);

        for (String word : words) {
            GlStateManager.color(0.22f, 0.22f, 0.22f, 1f);
            NineSliceUtils.draw(GuiTextures.storageBackground(1), layout.x, curY + getScaledY(3), itemW, getScaledY(32), 6, 18);
            GlStateManager.color(1f, 1f, 1f, 1f);
            TextRenderUtils.drawCenteredStringScaleAware(word, layout.x + (itemW - delW - getScaledX(4)) / 2f, curY + getScaledY(19), textScale, false);
            new CFButton(-1, layout.x + itemW - delW - getScaledX(2), curY + getScaledY(7), delW, delH, "X", 0.8f, 0.2f, 0.2f).drawButton(mc, mouseX, mouseY);
            curY += layout.itemHeight;
        }
        stopScissor();
        drawScrollbar(layout.x + layout.width - getScaledX(6), layout.y, layout.height, layout.totalHeight, scrollY, layout.maxScroll);
    }


    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        addWordField.mouseClicked(mouseX, mouseY, mouseButton);
        customPreviewField.mouseClicked(mouseX, mouseY, mouseButton);

        if (cachedLeft  == null) cachedLeft  = buildLeftLayout();
        if (cachedRight == null) cachedRight = buildRightLayout();

        handleRightClick(mouseX, mouseY, cachedRight);
        handleLeftClick(mouseX, mouseY, cachedLeft);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void handleRightClick(int mouseX, int mouseY, ListLayout layout) {
        if (tryStartScrollbarDrag(mouseX, mouseY, layout.x + layout.width - getScaledX(6), layout.y, layout.height, layout.totalHeight, previewsScrollY, layout.maxScroll)) {
            dragModeRight = 1; dragStartYRight = mouseY; dragStartScrollYRight = previewsScrollY;
            return;
        }
        if (mouseX >= layout.x && mouseX <= layout.x + layout.width && mouseY >= layout.y && mouseY <= layout.y + layout.height && layout.maxScroll > 0) {
            dragModeRight = 2; dragStartYRight = mouseY; dragStartScrollYRight = previewsScrollY;
        }
    }

    private void handleLeftClick(int mouseX, int mouseY, ListLayout layout) {
        if (tryStartScrollbarDrag(mouseX, mouseY, layout.x + layout.width - getScaledX(6), layout.y, layout.height, layout.totalHeight, scrollY, layout.maxScroll)) {
            dragModeLeft = 1; dragStartYLeft = mouseY; dragStartScrollYLeft = scrollY;
            return;
        }
        if (mouseX >= layout.x && mouseX <= layout.x + layout.width && mouseY >= layout.y && mouseY <= layout.y + layout.height) {
            int curY  = layout.y - scrollY;
            int itemW = layout.width - getScaledX(10);
            int delW  = getScaledX(26);
            int delH  = getScaledY(24);
            for (int i = 0; i < words.size(); i++) {
                int itemY = curY + i * layout.itemHeight;
                CFButton del = new CFButton(-1, layout.x + itemW - delW - getScaledX(2), itemY + getScaledY(7), delW, delH, "X", 0.8f, 0.2f, 0.2f);
                if (mouseY >= itemY && mouseY <= itemY + getScaledY(35) && del.mousePressed(mc, mouseX, mouseY)) {
                    words.remove(i);
                    previewDirty = true;
                    cachedLeft = cachedRight = null;
                    return;
                }
            }
            if (layout.maxScroll > 0) {
                dragModeLeft = 2; dragStartYLeft = mouseY; dragStartScrollYLeft = scrollY;
            }
        }
    }

    private static class ListLayout {
        int x, y, width, height, itemHeight, totalHeight, maxScroll;
    }
}