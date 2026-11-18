package io.hamlook.aetheria.features.chat.chatfilters.ui;

import io.hamlook.aetheria.ThemeContext;

import io.hamlook.aetheria.features.chat.chatfilters.ChatFilter;
import io.hamlook.aetheria.features.chat.chatfilters.ChatFilterManager;
import io.hamlook.aetheria.features.chat.chatfilters.vars.FilterAction;
import io.hamlook.aetheria.features.chat.chatfilters.vars.FilterCase;
import io.hamlook.aetheria.features.chat.chatfilters.vars.FilterMode;
import io.hamlook.aetheria.utils.KeybindHelper;
import io.hamlook.aetheria.utils.LerpUtils;
import io.hamlook.aetheria.utils.compat.DrawContextUtils;
import io.hamlook.aetheria.utils.compat.KeyboardCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.MouseCompat;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.FontRenderer;

import java.util.ArrayList;
import java.util.List;
import io.hamlook.aetheria.utils.compat.GuiScreenUtils;

public class ChatFilterEditorGUI extends ChatFilterBaseGUI {

    // Layout constants (all in unscaled units)
    private static final double PAD = 14;
    private static final double HEADER_H = 22;
    private static final double LABEL_H = 16;
    private static final double FIELD_H = ChatFilterBaseGUI.FIELD_H;
    private static final double ROW_GAP = 8;
    private static final int TAB_SETUP = 0;
    private static final int TAB_PREVIEW = 1;
    private static final String[] TAB_LABELS = {"Setup", "Preview"};
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
    private int activeTab = TAB_SETUP;
    private LerpUtils.LerpingFloat tabHighlightX;
    private LerpUtils.LerpingFloat tabHighlightW;
    private int dragModeLeft = 0, dragStartYLeft = 0, dragStartScrollYLeft = 0;
    private int dragModeRight = 0, dragStartYRight = 0, dragStartScrollYRight = 0;
    private ListLayout cachedLeft;
    private ListLayout cachedRight;
    private List<String> cachedPreviewLines;
    private boolean previewDirty = true;
    private final LerpUtils.LerpingFloat matchSelection = new LerpUtils.LerpingFloat(0f, 150);
    private final LerpUtils.LerpingFloat caseSelection = new LerpUtils.LerpingFloat(0f, 150);
    private final LerpUtils.LerpingFloat actionSelection = new LerpUtils.LerpingFloat(0f, 150);

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

    private int lx() {
        return boxX + getScaledX(PAD);
    }

    private int rx() {
        return lx();
    }

    private int rw() {
        return boxW - getScaledX(PAD * 2);
    }

    private int tabsY() {
        return boxY + getScaledY(PAD + HEADER_H);
    }

    private int contentY() {
        return tabsY() + ThemeContext.TAB_H + getScaledY(8);
    }

    private int secMatchY() {
        int cardHeight = ThemeContext.TAB_H + getScaledY(LABEL_H + ROW_GAP + 8);
        return tipsY() - getScaledY(12) - cardHeight + getScaledY(4);
    }

    private int secCustomY() {
        return contentY() + getScaledY(4);
    }

    private int secPreviewY() {
        int resultSpace = customPreviewResult.isEmpty() ? 0 : MinecraftCompat.getFontRenderer().FONT_HEIGHT + getScaledY(4);
        return contentY() + getScaledY(22) + (int) FIELD_H + resultSpace + getScaledY(22);
    }

    private int wordsListY() {
        int fieldY = contentY() + getScaledY(16);
        return fieldY + (int) FIELD_H + getScaledY(8);
    }

    private int wordsListHeight() {
        return Math.max(0, secMatchY() - wordsListY() - getScaledY(28));
    }

    private int tipsHeight() {
        FontRenderer font = MinecraftCompat.getFontRenderer();
        return Math.max(getScaledY(48), font.FONT_HEIGHT * 2 + getScaledY(16));
    }

    private int tipsY() {
        return bottomBtnY() - getScaledY(6) - tipsHeight();
    }

    private int bottomBtnY() {
        return boxY + boxH - getScaledY(PAD) - smallButtonHeight();
    }

    @Override
    protected void onInitGui() {
        boxW = Math.min(getScaledX(1360), width - getScaledX(32));
        boxH = Math.min(getScaledY(1040), height - getScaledY(32));
        boxX = (width - boxW) / 2;
        boxY = (height - boxH) / 2;
        cachedLeft = null;
        cachedRight = null;
        previewDirty = true;

        KeyboardCompat.enableRepeatEvents(true);
        initTextFields();
        GuiScreenUtils.clearButtons(this);
    }

    private void initTextFields() {
        int fieldH = (int) FIELD_H;

        String prevAdd = addWordField != null ? addWordField.getText() : "";
        int addFieldW = Math.max(0, rw() - buttonWidth(MinecraftCompat.getFontRenderer(), "Add") - getScaledX(PAD));
        addWordField = GuiScreenUtils.createTextField(MinecraftCompat.getFontRenderer(), lx(), contentY() + getScaledY(16), addFieldW, fieldH);
        GuiScreenUtils.setTextFieldMaxLength(addWordField, 50);
        addWordField.setText(prevAdd);

        String prevCustom = customPreviewField != null ? customPreviewField.getText() : "";
        int customFieldW = Math.max(0, rw() - buttonWidth(MinecraftCompat.getFontRenderer(), "Test") - getScaledX(PAD + 4));
        customPreviewField = GuiScreenUtils.createTextField(MinecraftCompat.getFontRenderer(), rx(), secCustomY() + getScaledY(LABEL_H + ROW_GAP), customFieldW, fieldH);
        GuiScreenUtils.setTextFieldMaxLength(customPreviewField, 100);
        customPreviewField.setText(prevCustom);
    }

    private ListLayout buildLeftLayout() {
        ListLayout l = new ListLayout();
        l.x = lx();
        l.y = wordsListY();
        l.width = rw();
        l.height = wordsListHeight();
        l.itemHeight = getScaledY(38);
        l.totalHeight = words.size() * l.itemHeight;
        l.maxScroll = Math.max(0, l.totalHeight - l.height);
        return l;
    }

    private ListLayout buildRightLayout() {
        ListLayout l = new ListLayout();
        l.x = rx();
        l.y = secPreviewY() + getScaledY(LABEL_H + ROW_GAP);
        l.width = rw();
        l.height = bottomBtnY() - getScaledY(ROW_GAP) - l.y;
        l.itemHeight = getScaledY(28);
        l.totalHeight = words.size() * l.itemHeight;
        l.maxScroll = Math.max(0, l.totalHeight - l.height);
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
            case STARTS:
                return "§e" + word + "§r quick brown fox";
            case ENDS:
                return "quick brown fox §e" + word + "§r";
            default:
                return "quick §e" + word + "§r brown fox";
        }
    }

    private String buildPreviewLine(ChatFilter f, String word, int index) {
        String ex = buildPreviewExample(word, index);
        String res = f.applyFilter(ex);
        return "§7[§e" + word + "§7]§f: " + ex + " §8->§f " + (res == null ? "§c[CANCELLED]" : res);
    }

    @Override
    protected void guiClosed() {
        KeyboardCompat.enableRepeatEvents(false);
    }

    private void saveFilter() {
        if (isNew) {
            ChatFilterManager.chatFilters.add(new ChatFilter(words, mode, filterCase, action));
        } else {
            filter.filterWords = words;
            filter.filterType = mode;
            filter.filterCase = filterCase;
            filter.action = action;
            filter.replace = action == FilterAction.REPLACE;
        }
        ChatFilterManager.saveToFile();
    }

    //#if MC < 1.21
    @Override
    public void updateScreen() {
        addWordField.updateCursorCounter();
        customPreviewField.updateCursorCounter();
    }
    //#endif

    @Override
    protected void onKeyTyped(char typedChar, int keyCode) {
        if (KeybindHelper.isEscapeKey(keyCode)) {
            if (addWordField.isFocused()) {
                addWordField.setFocused(false);
                return;
            }
            if (customPreviewField.isFocused()) {
                customPreviewField.setFocused(false);
                return;
            }
        }
        if (addWordField.isFocused()) {
            GuiScreenUtils.typeTextField(addWordField, typedChar, keyCode);
            if (KeybindHelper.isEnterKey(keyCode)) addWord();
        } else if (customPreviewField.isFocused()) {
            GuiScreenUtils.typeTextField(customPreviewField, typedChar, keyCode);
            if (KeybindHelper.isEnterKey(keyCode)) testCustomSentence();
        }
    }

    private void testCustomSentence() {
        String text = customPreviewField.getText();
        if (text.isEmpty()) {
            customPreviewResult = "";
            return;
        }
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
            cachedLeft = null;
            cachedRight = null;
        }
    }

    @Override
    protected void onDrawScreen(int mouseX, int mouseY, float partialTicks) {
        //#if MC < 1.21
        drawDefaultBackground();
        //#else
        //$$ // modern background handled via Screen.renderBackground
        //#endif

        beginThemeButtons(mouseX, mouseY);
        float fade = openProgress();
        ThemeContext.panel(boxX, boxY, boxW, boxH, fade);
        ThemeContext.rect(boxX, boxY, boxW, getScaledY(3), ThemeContext.withAlpha(ThemeContext.ACCENT, fade));

        if (previewDirty) {
            cachedPreviewLines = buildPreviewLines();
            previewDirty = false;
        }
        if (cachedLeft == null) cachedLeft = buildLeftLayout();
        if (cachedRight == null) cachedRight = buildRightLayout();

        updateScrollDragging(mouseY, cachedLeft, cachedRight);
        handleMouseWheel(mouseX, cachedLeft, cachedRight);

        drawSectionBackgrounds(cachedLeft, cachedRight, fade);
        drawHeader();
        drawTabs(mouseX, mouseY);
        if (activeTab == TAB_SETUP) {
            drawSetupPage();
            drawLeftWordList(mouseX, mouseY, cachedLeft);
            drawSettingsPanel();
            drawSetupTips();
        } else if (activeTab == TAB_PREVIEW) {
            drawPreviewPage();
            drawRightPreviewList(cachedRight);
        }
        drawActionButtons();
    }

    private void drawSectionBackgrounds(ListLayout left, ListLayout right, float fade) {
        if (activeTab == TAB_SETUP) {
            int pad = getScaledX(6);
            ThemeContext.inset(left.x - pad, left.y - pad, left.width + pad * 2, left.height + pad * 2, fade);
        } else {
            int top = contentY() - getScaledY(4);
            ThemeContext.panel(lx() - getScaledX(6), top, rw() + getScaledX(12),
                    bottomBtnY() - top - getScaledY(8), fade);
            if (activeTab == TAB_PREVIEW) {
                int pad = getScaledX(6);
                ThemeContext.inset(right.x - pad, right.y - pad, right.width + pad * 2, right.height + pad * 2, fade);
            }
        }
    }

    private void drawHeader() {
        FontRenderer font = MinecraftCompat.getFontRenderer();
        ThemeContext.text(font, isNew ? "Create Chat Filter" : "Edit Chat Filter", lx(), boxY + getScaledY(PAD), ThemeContext.TEXT);
    }

    private void drawTabs(int mouseX, int mouseY) {
        FontRenderer font = MinecraftCompat.getFontRenderer();
        int y = tabsY();
        int x = lx();
        ThemeContext.divider(x, y + ThemeContext.TAB_H - 1, rw());

        int activeX = x;
        int activeW = 0;
        int cursor = x;
        for (int i = 0; i < TAB_LABELS.length; i++) {
            int w = Math.max(48, ThemeContext.width(font, TAB_LABELS[i]) + 24);
            if (i == activeTab) {
                activeX = cursor;
                activeW = w;
            }
            cursor += w;
        }
        if (tabHighlightX == null) {
            tabHighlightX = new LerpUtils.LerpingFloat(activeX, 120);
            tabHighlightW = new LerpUtils.LerpingFloat(activeW, 120);
        }
        if ((int) tabHighlightX.getTarget() != activeX) {
            tabHighlightX.setTarget(activeX);
            tabHighlightX.resetTimer();
        }
        if ((int) tabHighlightW.getTarget() != activeW) {
            tabHighlightW.setTarget(activeW);
            tabHighlightW.resetTimer();
        }
        tabHighlightX.tick();
        tabHighlightW.tick();
        ThemeContext.rect(Math.round(tabHighlightX.getValue()), y, Math.round(tabHighlightW.getValue()),
                ThemeContext.TAB_H - 1, ThemeContext.ACCENT);

        cursor = x;
        for (int i = 0; i < TAB_LABELS.length; i++) {
            int w = Math.max(48, ThemeContext.width(font, TAB_LABELS[i]) + 24);
            boolean hover = mouseX >= cursor && mouseX < cursor + w && mouseY >= y && mouseY < y + ThemeContext.TAB_H - 1;
            if (i != activeTab && hover) ThemeContext.rect(cursor, y, w, 1, ThemeContext.LINE);
            ThemeContext.textCenter(font, TAB_LABELS[i], cursor + w / 2, y + 7,
                    i == activeTab ? ThemeContext.ACCENT_ON : ThemeContext.MUTED);
            final int tab = i;
            buttonHits.add(cursor, y, w, ThemeContext.TAB_H - 1, null, () -> setTab(tab));
            cursor += w;
        }
    }

    private void setTab(int tab) {
        if (tab == activeTab) return;
        activeTab = tab;
        dragModeLeft = dragModeRight = 0;
        cachedLeft = cachedRight = null;
        refreshGui();
    }

    private void drawSetupPage() {
        FontRenderer font = MinecraftCompat.getFontRenderer();
        ThemeContext.text(font, "Words to match", lx(), contentY(), ThemeContext.MUTED);
        drawTextField(addWordField, "e.g. badword, spam...");
    }

    private void drawPreviewPage() {
        FontRenderer font = MinecraftCompat.getFontRenderer();
        ThemeContext.text(font, "Try a message", lx(), secCustomY(), ThemeContext.MUTED);
        drawTextField(customPreviewField, "Type a sentence to test...");
        if (!customPreviewResult.isEmpty()) {
            int resultY = GuiScreenUtils.getY(customPreviewField) + GuiScreenUtils.getH(customPreviewField) + getScaledY(6);
            ThemeContext.text(font, "Result: " + customPreviewResult, lx(), resultY, ThemeContext.TEXT);
        }
        ThemeContext.text(font, "Filter examples", lx(), secPreviewY(), ThemeContext.ACCENT);
    }

    private void drawSettingsPanel() {
        FontRenderer font = MinecraftCompat.getFontRenderer();
        int gap = 12;
        int groupW = (rw() - gap * 2) / 3;
        int groupY = secMatchY();
        int controlY = groupY + getScaledY(LABEL_H + ROW_GAP);
        int controlH = ThemeContext.TAB_H;
        drawRuleGroup(font, "Match type", new String[]{"Starts", "Ends", "Contains"},
                selectionIndex(mode), matchSelection, lx(), groupY, groupW, controlY, controlH);
        drawRuleGroup(font, "Case sensitivity", new String[]{"Sensitive", "Insensitive"},
                selectionIndex(filterCase), caseSelection, lx() + groupW + gap, groupY, groupW, controlY, controlH);
        drawRuleGroup(font, "Action", new String[]{"Cancel", "Replace", "Censor"},
                selectionIndex(action), actionSelection, lx() + (groupW + gap) * 2, groupY, groupW, controlY, controlH);

        int headingY = groupY - font.FONT_HEIGHT - getScaledY(8);
        ThemeContext.text(font, "Filter rules", lx(), headingY, ThemeContext.ACCENT);
        int dividerX = lx() + ThemeContext.width(font, "Filter rules") + getScaledX(10);
        ThemeContext.divider(dividerX, headingY + font.FONT_HEIGHT / 2, lx() + rw() - dividerX);
    }

    private void drawSetupTips() {
        FontRenderer font = MinecraftCompat.getFontRenderer();
        int tipH = tipsHeight();
        int tipY = tipsY();
        ThemeContext.panel(lx(), tipY, rw(), tipH);
        ThemeContext.rect(lx(), tipY, getScaledX(2), tipH, ThemeContext.ACCENT);
        int badgeX = lx() + getScaledX(10);
        int badgeW = ThemeContext.width(font, "TIPS") + getScaledX(14);
        int badgeY = tipY + (tipH - getScaledY(20)) / 2;
        ThemeContext.rect(badgeX, badgeY, badgeW, getScaledY(20), ThemeContext.BTN);
        ThemeContext.border(badgeX, badgeY, badgeW, getScaledY(20), ThemeContext.BTN_BORDER);
        ThemeContext.textCenter(font, "TIPS", badgeX + badgeW / 2, badgeY + (getScaledY(20) - font.FONT_HEIGHT) / 2,
                ThemeContext.ACCENT);
        int dividerX = badgeX + badgeW + getScaledX(12);
        ThemeContext.rect(dividerX, tipY + getScaledY(5), 1, tipH - getScaledY(10), ThemeContext.LINE);

        int textX = dividerX + getScaledX(12);
        int textW = lx() + rw() - textX - getScaledX(8);
        int firstY = tipY + getScaledY(5);
        int secondY = firstY + font.FONT_HEIGHT + getScaledY(2);
        String colorLabel = "Colors";
        String literalLabel = "Literal &";
        ThemeContext.text(font, colorLabel, textX, firstY, ThemeContext.ACCENT);
        ThemeContext.text(font, ThemeContext.fit(font, "Use codes like &c or &a to add formatting.",
                textW - ThemeContext.width(font, colorLabel) - getScaledX(8)),
                textX + ThemeContext.width(font, colorLabel) + getScaledX(8), firstY, ThemeContext.MUTED);
        ThemeContext.text(font, literalLabel, textX, secondY, ThemeContext.ACCENT);
        ThemeContext.text(font, ThemeContext.fit(font, "Type \\& to match it. Press Enter or Add to add a word.",
                textW - ThemeContext.width(font, literalLabel) - getScaledX(8)),
                textX + ThemeContext.width(font, literalLabel) + getScaledX(8), secondY, ThemeContext.MUTED);
    }

    private boolean selectRuleSegment(int mouseX, int mouseY, int group) {
        int gap = 12;
        int groupW = (rw() - gap * 2) / 3;
        int groupY = secMatchY();
        int controlY = groupY + getScaledY(LABEL_H + ROW_GAP);
        int controlH = ThemeContext.TAB_H;
        int groupX = lx() + (groupW + gap) * group;
        int x = groupX + getScaledX(4);
        int w = groupW - getScaledX(8);
        String[] labels = group == 0 ? new String[]{"Starts", "Ends", "Contains"}
                : group == 1 ? new String[]{"Sensitive", "Insensitive"} : new String[]{"Cancel", "Replace", "Censor"};
        return selectSegment(mouseX, mouseY, x, controlY, w, controlH, labels, group);
    }

    private void drawRuleGroup(FontRenderer font, String title, String[] labels, int selected,
                               LerpUtils.LerpingFloat animation, int x, int titleY, int w, int y, int h) {
        int cardY = titleY - getScaledY(4);
        ThemeContext.panel(x, cardY, w, h + getScaledY(LABEL_H + ROW_GAP + 8));
        ThemeContext.text(font, title, x + getScaledX(6), titleY, ThemeContext.ACCENT);
        drawSegments(labels, selected, animation, x + getScaledX(4), y, w - getScaledX(8), h);
    }

    private void drawSegments(String[] labels, int selected, LerpUtils.LerpingFloat animation, int x, int y, int w, int h) {
        FontRenderer font = MinecraftCompat.getFontRenderer();
        float bounded = Math.max(0, Math.min(labels.length - 1, selected));
        if (animation.getTarget() != bounded) {
            animation.resetTimer();
            animation.setTarget(bounded);
        }
        animation.tick();

        ThemeContext.rect(x, y, w, h, ThemeContext.BTN);
        ThemeContext.border(x, y, w, h, ThemeContext.BTN_BORDER);
        int segmentW = w / labels.length;
        int activeX = x + Math.round(animation.getValue() * segmentW);
        int activeW = animation.getValue() >= labels.length - 1 ? w - segmentW * (labels.length - 1) : segmentW;
        ThemeContext.rect(activeX + 1, y + 1, activeW - 2, h - 2, ThemeContext.ACCENT);
        ThemeContext.border(activeX + 1, y + 1, activeW - 2, h - 2, ThemeContext.ACCENT_HOVER);
        for (int i = 1; i < labels.length; i++) {
            ThemeContext.rect(x + i * segmentW, y + 4, 1, h - 8, ThemeContext.BTN_BORDER);
        }
        for (int i = 0; i < labels.length; i++) {
            int center = x + i * segmentW + (i == labels.length - 1 ? w - segmentW * i : segmentW) / 2;
            int color = i == selected ? ThemeContext.ACCENT_ON : ThemeContext.TEXT;
            ThemeContext.textCenter(font, labels[i], center, y + (h - font.FONT_HEIGHT) / 2, color);
        }
    }

    private int selectionIndex(FilterMode value) {
        return value == FilterMode.ENDS ? 1 : value == FilterMode.CONTAINS ? 2 : 0;
    }

    private int selectionIndex(FilterCase value) {
        return value == FilterCase.INSENSITIVE ? 1 : 0;
    }

    private int selectionIndex(FilterAction value) {
        return value == FilterAction.REPLACE ? 1 : value == FilterAction.CENSOR ? 2 : 0;
    }

    private boolean selectSegment(int mouseX, int mouseY, int x, int y, int w, int h, String[] labels, int group) {
        if (mouseX < x || mouseX >= x + w || mouseY < y || mouseY >= y + h) return false;
        int index = Math.min(labels.length - 1, (mouseX - x) * labels.length / Math.max(1, w));
        if (group == 0) mode = index == 1 ? FilterMode.ENDS : index == 2 ? FilterMode.CONTAINS : FilterMode.STARTS;
        else if (group == 1) filterCase = index == 1 ? FilterCase.INSENSITIVE : FilterCase.SENSITIVE;
        else action = index == 1 ? FilterAction.REPLACE : index == 2 ? FilterAction.CENSOR : FilterAction.CANCEL;
        previewDirty = true;
        cachedRight = null;
        return true;
    }

    private void drawTextField(GuiTextField field, String placeholder) {
        int x = GuiScreenUtils.getX(field), y = GuiScreenUtils.getY(field), w = GuiScreenUtils.getW(field), h = GuiScreenUtils.getH(field);
        DrawContextUtils.fill(x, y, x + w, y + h, ThemeContext.BTN_BORDER);
        DrawContextUtils.fill(x + 1, y + 1, x + w - 1, y + h - 1, ThemeContext.INSET);

        int textY = y + (h - MinecraftCompat.getFontRenderer().FONT_HEIGHT) / 2;
        int maxW = w - 16;
        String text = field.getText();
        String display = MinecraftCompat.getFontRenderer().trimStringToWidth(text, maxW);

        if (text.isEmpty()) {
            ThemeContext.text(MinecraftCompat.getFontRenderer(), placeholder, x + 8, textY, ThemeContext.MUTED);
        } else {
            ThemeContext.text(MinecraftCompat.getFontRenderer(), display, x + 8, textY, field.isFocused() ? ThemeContext.TEXT : ThemeContext.MUTED);
        }

        if (field.isFocused() && System.currentTimeMillis() % 1000 > 500) {
            int cursor = Math.min(GuiScreenUtils.getTextFieldCursor(field), text.length());
            String before = MinecraftCompat.getFontRenderer().trimStringToWidth(text.substring(0, cursor), maxW);
            int cx = x + 8 + DrawContextUtils.getStringWidth(MinecraftCompat.getFontRenderer(), before);
            DrawContextUtils.fill(cx, textY - 1, cx + 1, textY + MinecraftCompat.getFontRenderer().FONT_HEIGHT + 1, 0xFFFFFFFF);
        }
    }

    private void updateScrollDragging(int mouseY, ListLayout left, ListLayout right) {
        if (MouseCompat.isButtonDown(0)) {
            scrollY = applyDraggedScroll(mouseY, dragModeLeft, dragStartYLeft, dragStartScrollYLeft, scrollY, left.height, left.totalHeight, left.maxScroll);
            previewsScrollY = applyDraggedScroll(mouseY, dragModeRight, dragStartYRight, dragStartScrollYRight, previewsScrollY, right.height, right.totalHeight, right.maxScroll);
        } else {
            dragModeLeft = 0;
            dragModeRight = 0;
        }
        scrollY = clampScroll(scrollY, left.maxScroll);
        previewsScrollY = clampScroll(previewsScrollY, right.maxScroll);
    }

    private void handleMouseWheel(int mouseX, ListLayout left, ListLayout right) {
        int dWheel = MouseCompat.getDWheel();
        if (dWheel == 0) return;
        if (activeTab == TAB_SETUP) {
            scrollY = clampScroll(scrollY - Integer.signum(dWheel) * getScaledY(20), left.maxScroll);
        } else if (activeTab == TAB_PREVIEW) {
            previewsScrollY = clampScroll(previewsScrollY - Integer.signum(dWheel) * getScaledY(20), right.maxScroll);
        }
    }

    private void drawRightPreviewList(ListLayout layout) {
        FontRenderer font = MinecraftCompat.getFontRenderer();
        if (words.isEmpty()) {
            ThemeContext.textCenter(font, "Preview appears here", layout.x + layout.width / 2,
                    layout.y + layout.height / 2 - font.FONT_HEIGHT, ThemeContext.TEXT);
            ThemeContext.textCenter(font, "Add a word to see how it matches and changes a message.",
                    layout.x + layout.width / 2, layout.y + layout.height / 2 + getScaledY(4), ThemeContext.MUTED);
            return;
        }
        startScissor(layout.x, layout.y, layout.width, layout.height);
        int pY = layout.y - previewsScrollY;
        for (String line : cachedPreviewLines) {
            ThemeContext.text(font, line, layout.x, pY, ThemeContext.MUTED);
            pY += layout.itemHeight;
        }
        stopScissor();
        drawScrollbar(layout.x + layout.width - getScaledX(6), layout.y, layout.height, layout.totalHeight, previewsScrollY, layout.maxScroll);
    }

    private void drawLeftWordList(int mouseX, int mouseY, ListLayout layout) {
        FontRenderer font = MinecraftCompat.getFontRenderer();
        if (words.isEmpty()) {
            ThemeContext.textCenter(font, "No words yet", layout.x + layout.width / 2,
                    layout.y + layout.height / 2 - font.FONT_HEIGHT, ThemeContext.TEXT);
            ThemeContext.textCenter(font, "Add a filter word above to get started.",
                    layout.x + layout.width / 2, layout.y + layout.height / 2 + getScaledY(4), ThemeContext.MUTED);
            return;
        }
        startScissor(layout.x, layout.y, layout.width, layout.height);
        int curY = layout.y - scrollY;
        int itemW = layout.width - getScaledX(10);
        int delW = buttonWidth(font, "Remove");
        int delH = smallButtonHeight();
        buttonHits.clip(layout.x, layout.y, layout.width, layout.height);

        for (int i = 0; i < words.size(); i++) {
            final int index = i;
            String word = words.get(i);
            int rowY = curY + getScaledY(3);
            int rowH = getScaledY(32);
            boolean hovered = mouseX >= layout.x && mouseX < layout.x + itemW && mouseY >= rowY && mouseY < rowY + rowH;
            int rowColor = hovered ? ThemeContext.BTN_HOVER : ThemeContext.PANEL;
            ThemeContext.rect(layout.x, rowY, itemW, rowH, rowColor);
            ThemeContext.border(layout.x, rowY, itemW, rowH, ThemeContext.BTN_BORDER);
            ThemeContext.rect(layout.x, rowY, getScaledX(2), rowH, hovered ? ThemeContext.ACCENT : ThemeContext.LINE);
            int removeX = layout.x + itemW - delW - getScaledX(6);
            ThemeContext.text(font, ThemeContext.fit(font, word, removeX - layout.x - getScaledX(16)),
                    layout.x + getScaledX(8), rowY + (rowH - font.FONT_HEIGHT) / 2, ThemeContext.TEXT);
            ThemeContext.button(buttonHits, font, "Remove", removeX, rowY + (rowH - delH) / 2, delW, delH,
                    false, true, () -> {
                        words.remove(index);
                        previewDirty = true;
                        cachedLeft = cachedRight = null;
                    }, null);
            curY += layout.itemHeight;
        }
        buttonHits.unclip();
        stopScissor();
        drawScrollbar(layout.x + layout.width - getScaledX(6), layout.y, layout.height, layout.totalHeight, scrollY, layout.maxScroll);
    }

    private void drawActionButtons() {
        FontRenderer font = MinecraftCompat.getFontRenderer();
        int buttonH = buttonHeight();
        int footerH = compactButtonHeight();
        int fieldH = (int) FIELD_H;
        if (activeTab == TAB_SETUP) {
            int addW = buttonWidth(font, "Add");
            int addX = lx() + rw() - addW;
            int addY = GuiScreenUtils.getY(addWordField) + (fieldH - buttonH) / 2;
            ThemeContext.button(buttonHits, font, "Add", addX, addY, addW, buttonH, true, false, () -> {
                addWord();
                previewDirty = true;
                cachedLeft = null;
            }, null);
        } else if (activeTab == TAB_PREVIEW) {
            int testW = buttonWidth(font, "Test");
            int testX = rx() + rw() - testW;
            int testY = GuiScreenUtils.getY(customPreviewField) + (fieldH - buttonH) / 2;
            ThemeContext.button(buttonHits, font, "Test", testX, testY, testW, buttonH, true, false, () -> {
                testCustomSentence();
                previewDirty = true;
                cachedRight = null;
            }, null);
        }

        int saveW = buttonWidth(font, "Save");
        int cancelW = buttonWidth(font, "Cancel");
        int gap = 6;
        int right = boxX + boxW - getScaledX(PAD);
        int y = bottomBtnY();
        ThemeContext.button(buttonHits, font, "Cancel", right - cancelW, y, cancelW, footerH, false, false,
                () -> mc.displayGuiScreen(parent), null);
        ThemeContext.button(buttonHits, font, "Save", right - cancelW - gap - saveW, y, saveW, footerH,
                true, false, () -> {
                    saveFilter();
                    mc.displayGuiScreen(parent);
                }, null);
    }

    @Override
    protected void onMouseClicked(int mouseX, int mouseY, int mouseButton) {
        addWordField.mouseClicked(mouseX, mouseY, mouseButton);
        customPreviewField.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseButton == 0 && clickThemeButton(mouseX, mouseY)) return;

        if (mouseButton == 0 && activeTab == TAB_SETUP && (selectRuleSegment(mouseX, mouseY, 0)
                || selectRuleSegment(mouseX, mouseY, 1)
                || selectRuleSegment(mouseX, mouseY, 2))) {
            return;
        }

        if (cachedLeft == null) cachedLeft = buildLeftLayout();
        if (cachedRight == null) cachedRight = buildRightLayout();

        if (activeTab == TAB_PREVIEW) handleRightClick(mouseX, mouseY, cachedRight);
        else if (activeTab == TAB_SETUP) handleLeftClick(mouseX, mouseY, cachedLeft);
    }

    private void handleRightClick(int mouseX, int mouseY, ListLayout layout) {
        if (tryStartScrollbarDrag(mouseX, mouseY, layout.x + layout.width - getScaledX(6), layout.y, layout.height, layout.totalHeight, previewsScrollY, layout.maxScroll)) {
            dragModeRight = 1;
            dragStartYRight = mouseY;
            dragStartScrollYRight = previewsScrollY;
            return;
        }
        if (mouseX >= layout.x && mouseX <= layout.x + layout.width && mouseY >= layout.y && mouseY <= layout.y + layout.height && layout.maxScroll > 0) {
            dragModeRight = 2;
            dragStartYRight = mouseY;
            dragStartScrollYRight = previewsScrollY;
        }
    }

    private void handleLeftClick(int mouseX, int mouseY, ListLayout layout) {
        if (tryStartScrollbarDrag(mouseX, mouseY, layout.x + layout.width - getScaledX(6), layout.y, layout.height, layout.totalHeight, scrollY, layout.maxScroll)) {
            dragModeLeft = 1;
            dragStartYLeft = mouseY;
            dragStartScrollYLeft = scrollY;
            return;
        }
        if (mouseX >= layout.x && mouseX <= layout.x + layout.width && mouseY >= layout.y && mouseY <= layout.y + layout.height) {
            if (layout.maxScroll > 0) {
                dragModeLeft = 2;
                dragStartYLeft = mouseY;
                dragStartScrollYLeft = scrollY;
            }
        }
    }

    private static class ListLayout {
        int x, y, width, height, itemHeight, totalHeight, maxScroll;
    }
}
