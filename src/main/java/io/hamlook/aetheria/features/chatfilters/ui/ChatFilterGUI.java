package io.hamlook.aetheria.features.chatfilters.ui;

import io.hamlook.aetheria.core.config.gui.GuiTextures;
import io.hamlook.aetheria.features.chatfilters.ChatFilter;
import io.hamlook.aetheria.features.chatfilters.ChatFilterManager;
import io.hamlook.aetheria.features.chatfilters.vars.FilterCase;
import io.hamlook.aetheria.features.chatfilters.vars.FilterMode;
import io.hamlook.aetheria.utils.render.NineSliceUtils;
import io.hamlook.aetheria.utils.render.RenderUtils;
import io.hamlook.aetheria.utils.render.ResolutionUtils;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
    private float textScale;
    private int dragMode = 0;
    private int dragStartY = 0;
    private int dragStartScrollY = 0;

    private ListLayout cachedLayout;

    @Override
    public void initGui() {
        chatFilters = new ArrayList<>(ChatFilterManager.chatFilters);

        boxW = getScaledX(700);
        boxH = getScaledY(520);
        boxX = (width - boxW) / 2;
        boxY = (height - boxH) / 2;

        textScale = ResolutionUtils.getXStatic(1) * 2.6f * configScale();
        cachedLayout = null;
        org.lwjgl.input.Keyboard.enableRepeatEvents(true);
        String prevSearch = searchField != null ? searchField.getText() : "";

        int fieldH = getScaledY(BTN_H);

        searchField = new GuiTextField(0, fontRendererObj, boxX + getScaledX(20), boxY + getScaledY(24), boxW - getScaledX(160), fieldH);

        searchField.setMaxStringLength(100);
        searchField.setText(prevSearch);
        searchField.setFocused(true);
        buttonList.clear();

        buttonList.add(new CFButton(0, boxX + boxW - getScaledX(135), boxY + getScaledY(24), getScaledX(BTN_W + 20), getScaledY(BTN_H), "Add Filter", 0.2f, 0.4f, 0.8f));

        int ty = boxY + getScaledY(68);
        int th = getScaledY(TOGGLE_H);
        int tw = getScaledX(TOGGLE_W);

        buttonList.add(new CFButton(1, boxX + getScaledX(20), ty, tw, th, "STARTS", true, showStarts));
        buttonList.add(new CFButton(2, boxX + getScaledX(100), ty, tw, th, "ENDS", true, showEnds));
        buttonList.add(new CFButton(3, boxX + getScaledX(180), ty, getScaledX(TOGGLE_W + 10), th, "CONTAINS", true, showContains));

        buttonList.add(new CFButton(4, boxX + getScaledX(275), ty, getScaledX(TOGGLE_W + 10), th, "SENSITIVE", true, showSensitive));
        buttonList.add(new CFButton(5, boxX + getScaledX(370), ty, getScaledX(TOGGLE_W + 20), th, "INSENSITIVE", true, showInsensitive));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 0:
                mc.displayGuiScreen(new ChatFilterEditorGUI(this, null));
                return;
            case 1:
                showStarts = !showStarts;
                break;
            case 2:
                showEnds = !showEnds;
                break;
            case 3:
                showContains = !showContains;
                break;
            case 4:
                showSensitive = !showSensitive;
                break;
            case 5:
                showInsensitive = !showInsensitive;
                break;
        }

        initGui();
    }

    @Override
    public void onGuiClosed() {
        org.lwjgl.input.Keyboard.enableRepeatEvents(false);
        ChatFilterManager.saveToFile();
    }

    @Override
    public void updateScreen() {
        searchField.updateCursorCounter();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1 && searchField.isFocused()) {
            searchField.setFocused(false);
            return;
        }

        if (searchField.isFocused()) {
            searchField.textboxKeyTyped(typedChar, keyCode);
        } else {
            super.keyTyped(typedChar, keyCode);
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
        l.listY = boxY + getScaledY(106);
        l.listH = boxH - getScaledY(116);
        l.totalH = displayed.size() * l.itemH;
        l.maxScroll = Math.max(0, l.totalH - l.listH);

        return l;
    }

    private void updateScroll(ListLayout layout, int mouseY) {
        if (Mouse.isButtonDown(0)) {
            scrollY = applyDraggedScroll(mouseY, dragMode, dragStartY, dragStartScrollY, scrollY, layout.listH, layout.totalH, layout.maxScroll);
        } else {
            dragMode = 0;
        }

        int dWheel = Mouse.getDWheel();
        if (dWheel != 0) {
            scrollY -= Integer.signum(dWheel) * getScaledY(20);
        }
        scrollY = clampScroll(scrollY, layout.maxScroll);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        GlStateManager.color(0.18f, 0.18f, 0.18f, 1f);
        NineSliceUtils.draw(GuiTextures.storageBackground(1), boxX, boxY, boxW, boxH, 6, 18);
        GlStateManager.color(1f, 1f, 1f, 1f);
        List<ChatFilter> displayed = getDisplayedFilters();
        cachedLayout = buildLayout(displayed);
        ListLayout l = cachedLayout;
        updateScroll(l, mouseY);

        GlStateManager.color(0.12f, 0.12f, 0.12f, 1f);
        NineSliceUtils.draw(GuiTextures.storageBackground(1), boxX + getScaledX(10), l.listY - getScaledY(6), boxW - getScaledX(20), l.listH + getScaledY(12), 6, 18);
        GlStateManager.color(1f, 1f, 1f, 1f);
        RenderUtils.drawSearchBar(searchField, false);

        if (displayed.isEmpty()) {
            drawEmptyState(l);
        } else {
            drawFilterList(mouseX, mouseY, displayed, l);
        }

        drawScrollbar(boxX + boxW - getScaledX(18), l.listY, l.listH, l.totalH, scrollY, l.maxScroll);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawEmptyState(ListLayout l) {
        GlStateManager.color(0.5f, 0.5f, 0.5f, 1f);
        String msg = chatFilters.isEmpty() ? "No filters yet. Click \"Add Filter\" to create one." : "No filters match the current search.";

        float msgScale = textScale * 1.1f;

        int msgW = (int) (mc.fontRendererObj.getStringWidth(msg) * msgScale);
        int centerX = boxX + (boxW - msgW) / 2;
        int centerY = l.listY + l.listH / 2 - getScaledY(6);

        TextRenderUtils.drawStringScaleAware(msg, centerX, centerY, msgScale, false);
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private void drawFilterList(int mouseX, int mouseY, List<ChatFilter> displayed, ListLayout l) {
        startScissor(boxX + getScaledX(10), l.listY, boxW - getScaledX(20), l.listH);

        int curY = l.listY - scrollY;
        int editW = getScaledX(BTN_W - 20);
        int delW = getScaledX(BTN_W - 20);
        int btnH = getScaledY(BTN_H - 4);
        int rightPad = getScaledX(30);

        for (ChatFilter cf : displayed) {
            GlStateManager.color(0.22f, 0.22f, 0.22f, 1f);
            NineSliceUtils.draw(GuiTextures.storageBackground(1), boxX + getScaledX(18), curY + getScaledY(4), boxW - getScaledX(36), getScaledY(44), 6, 18);
            GlStateManager.color(1f, 1f, 1f, 1f);
            String preview = String.join(", ", cf.filterWords);

            if (preview.length() > 55) {
                preview = preview.substring(0, 55) + "...";
            }

            String action = cf.action != null ? cf.action.name() : (cf.replace ? "REPLACE" : "CANCEL");
            String info = String.format("§7[%s] [%s] [%s]", cf.filterType.name, cf.filterCase.name(), action);
            TextRenderUtils.drawStringScaleAware(preview, boxX + getScaledX(32), curY + getScaledY(11), textScale * 1.1f, false);
            TextRenderUtils.drawStringScaleAware(info, boxX + getScaledX(32), curY + getScaledY(29), textScale * 0.85f, false);
            int btnY = curY + getScaledY(14);

            new CFButton(-1, boxX + boxW - rightPad - editW - delW - getScaledX(8), btnY, editW, btnH, "Edit", 0.2f, 0.7f, 0.2f).drawButton(mc, mouseX, mouseY);
            new CFButton(-2, boxX + boxW - rightPad - delW, btnY, delW, btnH, "Delete", 0.8f, 0.2f, 0.2f).drawButton(mc, mouseX, mouseY);
            curY += l.itemH;
        }
        stopScissor();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
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
            int editW = getScaledX(BTN_W - 20);
            int delW = getScaledX(BTN_W - 20);
            int btnH = getScaledY(BTN_H - 4);
            int rightPad = getScaledX(30);
            int curY = l.listY - scrollY;
            for (int i = 0; i < displayed.size(); i++) {
                ChatFilter cf = displayed.get(i);
                int itemY = curY + i * l.itemH;
                if (mouseY >= itemY && mouseY <= itemY + getScaledY(44)) {
                    int btnY = itemY + getScaledY(14);
                    if (new CFButton(-1, boxX + boxW - rightPad - editW - delW - getScaledX(8), btnY, editW, btnH, "Edit", 0.2f, 0.7f, 0.2f).mousePressed(mc, mouseX, mouseY)) {
                        mc.displayGuiScreen(new ChatFilterEditorGUI(this, cf));
                        return;
                    }

                    if (new CFButton(-2, boxX + boxW - rightPad - delW, btnY, delW, btnH, "Delete", 0.8f, 0.2f, 0.2f).mousePressed(mc, mouseX, mouseY)) {

                        chatFilters.remove(cf);
                        ChatFilterManager.chatFilters.remove(cf);
                        ChatFilterManager.saveToFile();
                        cachedLayout = null;
                        return;
                    }
                }
            }

            if (l.maxScroll > 0) {
                dragMode = 2;
                dragStartY = mouseY;
                dragStartScrollY = scrollY;
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private static class ListLayout {
        int itemH;
        int listY;
        int listH;
        int totalH;
        int maxScroll;
    }
}