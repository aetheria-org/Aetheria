package io.hamlook.aetheria.features.chat.chatfilters.ui;

import io.hamlook.aetheria.ThemeContext;

import io.hamlook.aetheria.features.chat.chatfilters.ChatFilter;
import io.hamlook.aetheria.features.chat.chatfilters.ChatFilterManager;
import io.hamlook.aetheria.features.chat.chatfilters.vars.FilterCase;
import io.hamlook.aetheria.features.chat.chatfilters.vars.FilterMode;
import io.hamlook.aetheria.utils.KeybindHelper;
import io.hamlook.aetheria.utils.compat.DrawContextUtils;
import io.hamlook.aetheria.utils.compat.GuiScreenUtils;
import io.hamlook.aetheria.utils.compat.KeyboardCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.MouseCompat;
import io.hamlook.aetheria.utils.LerpUtils;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ChatFilterGUI extends ChatFilterBaseGUI {

    public List<ChatFilter> chatFilters;
    public int boxX, boxY, boxW, boxH;
    private GuiTextField searchField;
    private boolean showStarts = true;
    private boolean showEnds = true;
    private boolean showContains = true;
    private boolean showSensitive = true;
    private boolean showInsensitive = true;
    private int scrollY = 0;
    private int dragMode = 0;
    private int dragStartY = 0;
    private int dragStartScrollY = 0;

    private ListLayout cachedLayout;
    private final Map<ChatFilter, LerpUtils.LerpingFloat> rowHover = new IdentityHashMap<>();

    @Override
    protected void onInitGui() {
        chatFilters = new ArrayList<>(ChatFilterManager.chatFilters);

        boxW = Math.min(getScaledX(1280), width - getScaledX(32));
        boxH = Math.min(getScaledY(840), height - getScaledY(32));
        boxX = (width - boxW) / 2;
        boxY = (height - boxH) / 2;

        cachedLayout = null;
        KeyboardCompat.enableRepeatEvents(true);
        String prevSearch = searchField != null ? searchField.getText() : "";

        FontRenderer font = MinecraftCompat.getFontRenderer();
        int addW = buttonWidth(font, "Add Filter");
        int fieldX = boxX + getScaledX(20);
        int searchY = boxY + getScaledY(32);
        int addX = boxX + boxW - getScaledX(20) - addW;
        int fieldH = (int) FIELD_H;
        searchField = GuiScreenUtils.createTextField(font, fieldX, searchY, addX - fieldX - getScaledX(8), fieldH);

        GuiScreenUtils.setTextFieldMaxLength(searchField, 100);
        searchField.setText(prevSearch);
        searchField.setFocused(true);
        GuiScreenUtils.clearButtons(this);
    }

    @Override
    protected void guiClosed() {
        KeyboardCompat.enableRepeatEvents(false);
        ChatFilterManager.saveToFile();
    }

    //#if MC < 1.21
    @Override
    public void updateScreen() {
        GuiScreenUtils.tickTextField(searchField);
    }
    //#endif

    @Override
    protected void onKeyTyped(char typedChar, int keyCode) {
        if (KeybindHelper.isEscapeKey(keyCode) && searchField.isFocused()) {
            searchField.setFocused(false);
            return;
        }

        if (searchField.isFocused()) {
            GuiScreenUtils.typeTextField(searchField, typedChar, keyCode);
        }
    }

    private List<ChatFilter> getDisplayedFilters() {
        String search = searchField.getText().toLowerCase();

        return chatFilters.stream().filter(cf -> {
            if (cf.filterType == FilterMode.STARTS && !showStarts) return false;
            if (cf.filterType == FilterMode.ENDS && !showEnds) return false;
            if (cf.filterType == FilterMode.CONTAINS && !showContains) return false;
            if (cf.filterCase == FilterCase.SENSITIVE && !showSensitive) return false;
            if (cf.filterCase == FilterCase.INSENSITIVE && !showInsensitive) return false;
            if (search.isEmpty()) return true;

            return cf.filterWords.stream().anyMatch(w -> w.toLowerCase().contains(search));
        }).collect(Collectors.toList());
    }

    private ListLayout buildLayout(List<ChatFilter> displayed) {
        ListLayout l = new ListLayout();

        l.itemH = getScaledY(52);
        int searchAndFilters = getScaledY(32) + (int) FIELD_H + getScaledY(14)
                + MinecraftCompat.getFontRenderer().FONT_HEIGHT + getScaledY(4) + smallButtonHeight() + getScaledY(30);
        l.listY = boxY + searchAndFilters;
        l.listH = boxH - searchAndFilters - getScaledY(14);
        l.totalH = displayed.size() * l.itemH;
        l.maxScroll = Math.max(0, l.totalH - l.listH);

        return l;
    }

    private void updateScroll(ListLayout layout, int mouseY) {
        if (MouseCompat.isButtonDown(0)) {
            scrollY = applyDraggedScroll(mouseY, dragMode, dragStartY, dragStartScrollY, scrollY, layout.listH, layout.totalH, layout.maxScroll);
        } else {
            dragMode = 0;
        }

        int dWheel = MouseCompat.getDWheel();
        if (dWheel != 0) {
            scrollY -= Integer.signum(dWheel) * getScaledY(20);
        }
        scrollY = clampScroll(scrollY, layout.maxScroll);
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
        FontRenderer font = MinecraftCompat.getFontRenderer();
        ThemeContext.text(font, "Chat Filters", boxX + getScaledX(20), boxY + getScaledY(12), ThemeContext.TEXT);
        List<ChatFilter> displayed = getDisplayedFilters();
        cachedLayout = buildLayout(displayed);
        ListLayout l = cachedLayout;
        updateScroll(l, mouseY);

        ThemeContext.inset(boxX + getScaledX(10), l.listY - getScaledY(6), boxW - getScaledX(20), l.listH + getScaledY(12), fade);
        drawSearchField();
        drawToolbarButtons(font);

        if (displayed.isEmpty()) {
            drawEmptyState(l);
        } else {
            drawFilterList(mouseX, mouseY, displayed, l);
        }

        drawScrollbar(boxX + boxW - getScaledX(18), l.listY, l.listH, l.totalH, scrollY, l.maxScroll);
    }

    private void drawToolbarButtons(FontRenderer font) {
        int buttonH = buttonHeight();
        int searchY = boxY + getScaledY(32);
        int fieldH = (int) FIELD_H;
        int addW = buttonWidth(font, "Add Filter");
        int addX = boxX + boxW - getScaledX(20) - addW;
        drawThemeButton(font, "Add Filter", addX, searchY + (fieldH - buttonH) / 2, addW, buttonH, true, false,
                () -> mc.displayGuiScreen(new ChatFilterEditorGUI(this, null)));

        int groupY = searchY + fieldH + getScaledY(14);
        int x = boxX + getScaledX(20);
        int matchW = drawFilterGroup(font, "Match type", new String[]{"Starts", "Ends", "Contains"},
                new boolean[]{showStarts, showEnds, showContains}, 0, x, groupY);
        x += matchW + 12;
        drawFilterGroup(font, "Case sensitivity", new String[]{"Sensitive", "Insensitive"},
                new boolean[]{showSensitive, showInsensitive}, 3, x, groupY);
    }

    private int drawFilterGroup(FontRenderer font, String title, String[] labels, boolean[] selected, int firstIndex,
                                int x, int y) {
        int gap = 6;
        int buttonH = smallButtonHeight();
        int groupW = 0;
        for (String label : labels) groupW += buttonWidth(font, label);
        groupW += gap * (labels.length - 1);
        int buttonY = y + font.FONT_HEIGHT + getScaledY(4);
        int groupH = font.FONT_HEIGHT + getScaledY(4) + buttonH + getScaledY(8);
        ThemeContext.panel(x, y - getScaledY(4), groupW + 12, groupH);
        ThemeContext.text(font, title, x + 6, y, ThemeContext.ACCENT);
        int cursor = x + 6;
        for (int i = 0; i < labels.length; i++) {
            final int index = firstIndex + i;
            int w = buttonWidth(font, labels[i]);
            drawThemeButton(font, labels[i], cursor, buttonY, w, buttonH, selected[i], false, () -> toggleFilter(index));
            cursor += w + gap;
        }
        return groupW + 12;
    }

    private void toggleFilter(int index) {
        switch (index) {
            case 0: showStarts = !showStarts; break;
            case 1: showEnds = !showEnds; break;
            case 2: showContains = !showContains; break;
            case 3: showSensitive = !showSensitive; break;
            case 4: showInsensitive = !showInsensitive; break;
        }
        cachedLayout = null;
    }

    private void drawThemeButton(FontRenderer font, String label, int x, int y, int w, int h,
                                 boolean primary, boolean danger, Runnable action) {
        ThemeContext.button(buttonHits, font, label, x, y, w, h, primary, danger, action, null);
    }

    private void drawSearchField() {
        int x = GuiScreenUtils.getX(searchField), y = GuiScreenUtils.getY(searchField);
        int w = GuiScreenUtils.getW(searchField), h = GuiScreenUtils.getH(searchField);
        ThemeContext.inset(x, y, w, h);
        FontRenderer font = MinecraftCompat.getFontRenderer();
        String text = searchField.getText();
        int textY = y + (h - font.FONT_HEIGHT) / 2;
        if (text.isEmpty()) ThemeContext.text(font, "Search filters...", x + 8, textY, ThemeContext.MUTED);
        else ThemeContext.text(font, font.trimStringToWidth(text, w - 16), x + 8, textY, ThemeContext.TEXT);
        if (searchField.isFocused() && System.currentTimeMillis() % 1000 > 500) {
            int cursor = Math.min(GuiScreenUtils.getTextFieldCursor(searchField), text.length());
            String before = font.trimStringToWidth(text.substring(0, cursor), w - 16);
            int cx = x + 8 + DrawContextUtils.getStringWidth(font, before);
            ThemeContext.rect(cx, textY - 1, 1, font.FONT_HEIGHT + 2, ThemeContext.TEXT);
        }
    }

    private void drawEmptyState(ListLayout l) {
        FontRenderer font = MinecraftCompat.getFontRenderer();
        int centerX = boxX + boxW / 2;
        int centerY = l.listY + l.listH / 2;
        if (chatFilters.isEmpty()) {
            ThemeContext.textCenter(font, "No filters yet", centerX, centerY - font.FONT_HEIGHT - getScaledY(8), ThemeContext.TEXT);
            ThemeContext.textCenter(font, "Choose which messages to match and how to handle them.",
                    centerX, centerY - font.FONT_HEIGHT / 2, ThemeContext.MUTED);
            ThemeContext.textCenter(font, "Select Add Filter above to create your first one.",
                    centerX, centerY + getScaledY(8), ThemeContext.MUTED);
        } else {
            ThemeContext.textCenter(font, "No filters match the current search.", centerX,
                    centerY - font.FONT_HEIGHT / 2, ThemeContext.MUTED);
        }
    }

    private void drawFilterList(int mouseX, int mouseY, List<ChatFilter> displayed, ListLayout l) {
        startScissor(boxX + getScaledX(10), l.listY, boxW - getScaledX(20), l.listH);
        buttonHits.clip(boxX + getScaledX(10), l.listY, boxW - getScaledX(20), l.listH);

        int curY = l.listY - scrollY;
        FontRenderer font = MinecraftCompat.getFontRenderer();
        int editW = buttonWidth(font, "Edit");
        int delW = buttonWidth(font, "Delete");
        int btnH = smallButtonHeight();
        int rightPad = 12;
        int gap = 6;

        for (ChatFilter cf : displayed) {
            int rowX = boxX + getScaledX(18), rowY = curY + getScaledY(4);
            int rowW = boxW - getScaledX(36), rowH = getScaledY(44);
            boolean hovered = mouseX >= rowX && mouseX < rowX + rowW && mouseY >= rowY && mouseY < rowY + rowH;
            LerpUtils.LerpingFloat hover = rowHover.get(cf);
            if (hover == null) {
                hover = new LerpUtils.LerpingFloat(0f, 120);
                hover.resetTimer();
                rowHover.put(cf, hover);
            }
            float hoverTarget = hovered ? 1f : 0f;
            if (hover.getTarget() != hoverTarget) {
                hover.resetTimer();
                hover.setTarget(hoverTarget);
            }
            hover.tick();
            int rowColor = ThemeContext.mixArgb(ThemeContext.PANEL, ThemeContext.BTN_HOVER, LerpUtils.sigmoidZeroOne(hover.getValue()));
            ThemeContext.rect(rowX, rowY, rowW, rowH, rowColor);
            ThemeContext.border(rowX, rowY, rowW, rowH, ThemeContext.BTN_BORDER);
            ThemeContext.rect(rowX, rowY, getScaledX(2), rowH, hovered ? ThemeContext.ACCENT : ThemeContext.LINE);
            String preview = String.join(", ", cf.filterWords);

            if (preview.length() > 55) {
                preview = preview.substring(0, 55) + "...";
            }

            String action = cf.action != null ? cf.action.name() : (cf.replace ? "REPLACE" : "CANCEL");
            action = action.substring(0, 1) + action.substring(1).toLowerCase();
            String info = cf.filterType.name + "  ·  " + (cf.filterCase == FilterCase.SENSITIVE ? "Case sensitive" : "Ignore case") + "  ·  " + action;
            int buttonStart = rowX + rowW - rightPad - editW - gap - delW;
            int textX = rowX + getScaledX(12);
            int textRight = buttonStart - getScaledX(10);
            ThemeContext.text(font, ThemeContext.fit(font, preview, textRight - textX), textX, rowY + getScaledY(9), ThemeContext.TEXT);
            ThemeContext.text(font, ThemeContext.fit(font, info, textRight - textX), textX, rowY + getScaledY(26), ThemeContext.MUTED);
            int btnY = rowY + (rowH - btnH) / 2;
            ThemeContext.button(buttonHits, font, "Edit", buttonStart, btnY, editW, btnH, false, false,
                    () -> mc.displayGuiScreen(new ChatFilterEditorGUI(this, cf)), null);
            ThemeContext.button(buttonHits, font, "Delete", buttonStart + editW + gap, btnY, delW, btnH, false, true, () -> {
                chatFilters.remove(cf);
                ChatFilterManager.chatFilters.remove(cf);
                ChatFilterManager.saveToFile();
                cachedLayout = null;
            }, null);
            curY += l.itemH;
        }
        buttonHits.unclip();
        stopScissor();
    }

    @Override
    protected void onMouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && clickThemeButton(mouseX, mouseY)) return;
        searchField.mouseClicked(mouseX, mouseY, mouseButton);

        List<ChatFilter> displayed = getDisplayedFilters();
        ListLayout l = cachedLayout != null ? cachedLayout : buildLayout(displayed);
        if (tryStartScrollbarDrag(mouseX, mouseY, boxX + boxW - getScaledX(18), l.listY, l.listH, l.totalH, scrollY, l.maxScroll)) {
            dragMode = 1;
            dragStartY = mouseY;
            dragStartScrollY = scrollY;
            return;
        }

        if (mouseX >= boxX && mouseX <= boxX + boxW && mouseY >= l.listY && mouseY <= l.listY + l.listH) {
            if (l.maxScroll > 0) {
                dragMode = 2;
                dragStartY = mouseY;
                dragStartScrollY = scrollY;
            }
        }
    }

    private static class ListLayout {
        int itemH;
        int listY;
        int listH;
        int totalH;
        int maxScroll;
    }
}
