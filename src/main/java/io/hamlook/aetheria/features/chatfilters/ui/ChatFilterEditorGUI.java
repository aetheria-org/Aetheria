package io.hamlook.aetheria.features.chatfilters.ui;

import io.hamlook.aetheria.core.config.gui.GuiTextures;
import io.hamlook.aetheria.features.chatfilters.ChatFilter;
import io.hamlook.aetheria.features.chatfilters.ChatFilterManager;
import io.hamlook.aetheria.features.chatfilters.vars.FilterCase;
import io.hamlook.aetheria.features.chatfilters.vars.FilterMode;
import io.hamlook.aetheria.features.chatfilters.vars.FilterAction;
import io.hamlook.aetheria.utils.render.NineSliceUtils;
import io.hamlook.aetheria.utils.render.RenderUtils;
import io.hamlook.aetheria.utils.render.ResolutionUtils;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ChatFilterEditorGUI extends GuiScreen {

    private final ChatFilterGUI parent;
    private ChatFilter filter;
    private final boolean isNew;

    public int boxX, boxY, boxW, boxH;

    private GuiTextField addWordField;
    private final List<String> words;
    private FilterMode mode;
    private FilterCase filterCase;
    private FilterAction action;

    private GuiTextField customPreviewField;
    private String customPreviewResult = "";

    private int scrollY = 0;
    private int previewsScrollY = 0;
    private float textScale;

    private int dragModeLeft = 0;
    private int dragStartYLeft = 0;
    private int dragStartScrollYLeft = 0;

    private int dragModeRight = 0;
    private int dragStartYRight = 0;
    private int dragStartScrollYRight = 0;

    private final String[] commonWords = {"quick", "brown", "fox", "lazy", "dog", "apple", "banana", "cherry"};

    public ChatFilterEditorGUI(ChatFilterGUI parent, ChatFilter filter) {
        this.parent = parent;
        this.isNew = (filter == null);
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

    @Override
    public void initGui() {
        boxW = getScaledX(700);
        boxH = getScaledY(500);
        boxX = (width - boxW) / 2;
        boxY = (height - boxH) / 2;
        float configScale = io.hamlook.aetheria.core.ATHRConfig.feature.chat.chatFilterConfig.uiScale;
        textScale = ResolutionUtils.getXStatic(1) * 1.5f * configScale;

        String prevText = addWordField != null ? addWordField.getText() : "";
        addWordField = new GuiTextField(0, fontRendererObj, boxX + getScaledX(20), boxY + getScaledY(50), getScaledX(200), getScaledY(20));
        addWordField.setMaxStringLength(50);
        addWordField.setText(prevText);

        int settingsX = boxX + getScaledX(320);
        int customY = boxY + getScaledY(185);
        String prevCustom = customPreviewField != null ? customPreviewField.getText() : "Type here...";
        customPreviewField = new GuiTextField(1, fontRendererObj, settingsX + getScaledX(55), customY, getScaledX(135), getScaledY(20));
        customPreviewField.setMaxStringLength(100);
        customPreviewField.setText(prevCustom);

        buttonList.clear();
        buttonList.add(new CFButton(0, boxX + getScaledX(230), boxY + getScaledY(50), getScaledX(60), getScaledY(20), "Add", 0.2f, 0.4f, 0.8f));
        buttonList.add(new CFButton(8, settingsX + getScaledX(195), customY, getScaledX(60), getScaledY(20), "Test", 0.2f, 0.4f, 0.8f));

        int settingsY = boxY + getScaledY(40);
        int btnH = getScaledY(20);
        
        buttonList.add(new CFButton(1, settingsX, settingsY + getScaledY(15), getScaledX(60), btnH, "STARTS", true, mode == FilterMode.STARTS));
        buttonList.add(new CFButton(2, settingsX + getScaledX(65), settingsY + getScaledY(15), getScaledX(60), btnH, "ENDS", true, mode == FilterMode.ENDS));
        buttonList.add(new CFButton(3, settingsX + getScaledX(130), settingsY + getScaledY(15), getScaledX(75), btnH, "CONTAINS", true, mode == FilterMode.CONTAINS));

        settingsY += getScaledY(45);
        buttonList.add(new CFButton(4, settingsX, settingsY + getScaledY(15), getScaledX(80), btnH, "SENSITIVE", true, filterCase == FilterCase.SENSITIVE));
        buttonList.add(new CFButton(5, settingsX + getScaledX(85), settingsY + getScaledY(15), getScaledX(90), btnH, "INSENSITIVE", true, filterCase == FilterCase.INSENSITIVE));

        settingsY += getScaledY(45);
        buttonList.add(new CFButton(6, settingsX, settingsY + getScaledY(15), getScaledX(70), btnH, "CANCEL", true, action == FilterAction.CANCEL));
        buttonList.add(new CFButton(7, settingsX + getScaledX(75), settingsY + getScaledY(15), getScaledX(70), btnH, "REPLACE", true, action == FilterAction.REPLACE));
        buttonList.add(new CFButton(9, settingsX + getScaledX(150), settingsY + getScaledY(15), getScaledX(70), btnH, "CENSOR", true, action == FilterAction.CENSOR));

        int btnY = boxY + boxH - getScaledY(40);
        buttonList.add(new CFButton(100, boxX + boxW - getScaledX(170), btnY, getScaledX(70), getScaledY(25), "Save", 0.2f, 0.7f, 0.2f));
        buttonList.add(new CFButton(101, boxX + boxW - getScaledX(90), btnY, getScaledX(70), getScaledY(25), "Cancel", 0.8f, 0.2f, 0.2f));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) { addWord(); }
        else if (button.id == 1) { mode = FilterMode.STARTS; }
        else if (button.id == 2) { mode = FilterMode.ENDS; }
        else if (button.id == 3) { mode = FilterMode.CONTAINS; }
        else if (button.id == 4) { filterCase = FilterCase.SENSITIVE; }
        else if (button.id == 5) { filterCase = FilterCase.INSENSITIVE; }
        else if (button.id == 6) { action = FilterAction.CANCEL; }
        else if (button.id == 7) { action = FilterAction.REPLACE; }
        else if (button.id == 9) { action = FilterAction.CENSOR; }
        else if (button.id == 8) { testCustomSentence(); }
        else if (button.id == 100) {
            if (isNew) {
                ChatFilter newFilter = new ChatFilter(words, mode, filterCase, action);
                ChatFilterManager.chatFilters.add(newFilter);
            } else {
                filter.filterWords = words;
                filter.filterType = mode;
                filter.filterCase = filterCase;
                filter.action = action;
                filter.replace = (action == FilterAction.REPLACE);
            }
            ChatFilterManager.saveToFile();
            mc.displayGuiScreen(parent);
            return;
        }
        else if (button.id == 101) {
            mc.displayGuiScreen(parent);
            return;
        }

        initGui();
    }

    @Override
    public void updateScreen() {
        addWordField.updateCursorCounter();
        if (customPreviewField != null) customPreviewField.updateCursorCounter();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
            boolean unfocused = false;
            if (addWordField.isFocused()) {
                addWordField.setFocused(false);
                unfocused = true;
            }
            if (customPreviewField != null && customPreviewField.isFocused()) {
                customPreviewField.setFocused(false);
                unfocused = true;
            }
            if (unfocused) return;
        }

        if (addWordField.isFocused()) {
            addWordField.textboxKeyTyped(typedChar, keyCode);
            if (keyCode == 28) {
                addWord();
            }
        } else if (customPreviewField != null && customPreviewField.isFocused()) {
            customPreviewField.textboxKeyTyped(typedChar, keyCode);
            if (keyCode == 28) {
                testCustomSentence();
            }
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    private void testCustomSentence() {
        if (customPreviewField != null) {
            String text = customPreviewField.getText();
            if (text.isEmpty() || text.equals("Type here...")) {
                customPreviewResult = "";
                return;
            }
            ChatFilter tempFilter = new ChatFilter(words, mode, filterCase, action);
            String result = tempFilter.applyFilter(text);
            customPreviewResult = result == null ? "§c[CANCELLED]" : result;
        }
    }

    private void addWord() {
        String w = addWordField.getText().trim();
        if (!w.isEmpty() && !words.contains(w)) {
            words.add(w);
            addWordField.setText("");
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        
        GlStateManager.color(0.2f, 0.2f, 0.2f, 1f);
        NineSliceUtils.draw(GuiTextures.storageBackground(1), boxX, boxY, boxW, boxH, 6, 18);
        GlStateManager.color(1f, 1f, 1f, 1f);

        TextRenderUtils.drawStringScaleAware(isNew ? "Create Chat Filter" : "Edit Chat Filter", boxX + getScaledX(20), boxY + getScaledY(20), textScale * 1.5f, false);
        GlStateManager.color(1f, 0.7f, 0f, 1f);

        RenderUtils.drawSearchBar(addWordField, false);
        
        TextRenderUtils.drawStringScaleAware("§7Tip: Use & for colors (e.g. &c). Use \\& for normal &.", boxX + getScaledX(20), boxY + getScaledY(38), textScale, false);

        int settingsX = boxX + getScaledX(320);
        int settingsY = boxY + getScaledY(40);
        
        GlStateManager.color(1f, 0.7f, 0f, 1f);
        TextRenderUtils.drawStringScaleAware("Match Type:", settingsX, settingsY, textScale * 1.2f, false);
        
        settingsY += getScaledY(45);
        GlStateManager.color(1f, 0.7f, 0f, 1f);
        TextRenderUtils.drawStringScaleAware("Case Sensitivity:", settingsX, settingsY, textScale * 1.2f, false);
        
        settingsY += getScaledY(45);
        GlStateManager.color(1f, 0.7f, 0f, 1f);
        TextRenderUtils.drawStringScaleAware("Action:", settingsX, settingsY, textScale * 1.2f, false);
        
        settingsY += getScaledY(35);
        GlStateManager.color(1f, 0.7f, 0f, 1f);
        TextRenderUtils.drawStringScaleAware("Previews:", settingsX, settingsY, textScale * 1.2f, false);
        
        int customY = boxY + getScaledY(185);
        GlStateManager.color(1f, 0.7f, 0f, 1f);
        TextRenderUtils.drawStringScaleAware("Custom:", settingsX, customY + getScaledY(5), textScale * 1.1f, false);
        RenderUtils.drawSearchBar(customPreviewField, false);
        
        int resultYOffset = 0;
        if (customPreviewResult != null && !customPreviewResult.isEmpty()) {
            GlStateManager.color(0.7f, 0.7f, 0.7f, 1f);
            TextRenderUtils.drawStringScaleAware("Result: " + customPreviewResult, settingsX, customY + getScaledY(22), textScale * 1.1f, false);
            resultYOffset = getScaledY(15);
        }

        int rightListStartY = customY + getScaledY(25) + resultYOffset;
        int rightListH = (boxY + boxH - getScaledY(50)) - rightListStartY;
        int rightItemHeight = getScaledY(25);
        int rightTotalHeight = words.size() * rightItemHeight;
        int rightMaxScroll = Math.max(0, rightTotalHeight - rightListH);

        int leftListStartY = boxY + getScaledY(80);
        int leftListH = boxH - getScaledY(150);
        int leftItemHeight = getScaledY(30);
        int leftTotalHeight = words.size() * leftItemHeight;
        int leftMaxScroll = Math.max(0, leftTotalHeight - leftListH);

        if (org.lwjgl.input.Mouse.isButtonDown(0)) {
            if (dragModeRight == 1) {
                int thumbH = Math.max(getScaledY(20), (int)((float)rightListH * rightListH / Math.max(rightListH, rightTotalHeight)));
                float ratio = rightMaxScroll == 0 ? 0 : rightMaxScroll / (float)(rightListH - thumbH);
                previewsScrollY = dragStartScrollYRight + (int)((mouseY - dragStartYRight) * ratio);
            } else if (dragModeRight == 2) {
                previewsScrollY = dragStartScrollYRight - (mouseY - dragStartYRight);
            }
            if (dragModeLeft == 1) {
                int thumbH = Math.max(getScaledY(20), (int)((float)leftListH * leftListH / Math.max(leftListH, leftTotalHeight)));
                float ratio = leftMaxScroll == 0 ? 0 : leftMaxScroll / (float)(leftListH - thumbH);
                scrollY = dragStartScrollYLeft + (int)((mouseY - dragStartYLeft) * ratio);
            } else if (dragModeLeft == 2) {
                scrollY = dragStartScrollYLeft - (mouseY - dragStartYLeft);
            }
        } else {
            dragModeRight = 0;
            dragModeLeft = 0;
        }

        int dWheel = org.lwjgl.input.Mouse.getDWheel();
        if (dWheel != 0) {
            if (mouseX < boxX + getScaledX(310)) {
                scrollY -= Integer.signum(dWheel) * getScaledY(20);
            } else {
                previewsScrollY -= Integer.signum(dWheel) * getScaledY(20);
            }
        }

        if (previewsScrollY > rightMaxScroll) previewsScrollY = rightMaxScroll;
        if (previewsScrollY < 0) previewsScrollY = 0;
        
        if (scrollY > leftMaxScroll) scrollY = leftMaxScroll;
        if (scrollY < 0) scrollY = 0;

        if (scrollY > leftMaxScroll) scrollY = leftMaxScroll;
        if (scrollY < 0) scrollY = 0;

        if (!words.isEmpty()) {
            int dynamicWidth = getRightListWidth();
            ChatFilter tempFilter = new ChatFilter(words, mode, filterCase, action);
            startScissor(settingsX, rightListStartY, dynamicWidth, rightListH);
            int pCurrentY = rightListStartY - previewsScrollY;

            for (int i = 0; i < words.size(); i++) {
                String word = words.get(i);
                int caseType = i % 3;
                if (caseType == 1) word = word.toUpperCase();
                else if (caseType == 2) word = word.toLowerCase();

                String ex = "";
                if (mode == FilterMode.STARTS) ex = "§e" + word + "§r quick brown fox";
                else if (mode == FilterMode.CONTAINS) ex = "quick §e" + word + "§r brown fox";
                else if (mode == FilterMode.ENDS) ex = "quick brown fox §e" + word + "§r";

                String res = tempFilter.applyFilter(ex);
                
                GlStateManager.color(0.7f, 0.7f, 0.7f, 1f);
                TextRenderUtils.drawStringScaleAware("§7[§e" + words.get(i) + "§7]§f: " + (i+1) + ") " + ex + " -> " + (res == null ? "§c[CANCELLED]" : res), settingsX, pCurrentY, textScale, false);
                pCurrentY += rightItemHeight;
            }
            stopScissor();

            if (rightMaxScroll > 0) {
                int trackX = settingsX + dynamicWidth - getScaledX(10);
                net.minecraft.client.gui.Gui.drawRect(trackX, rightListStartY, trackX + getScaledX(4), rightListStartY + rightListH, 0x55000000);
                int thumbH = Math.max(getScaledY(20), (int)((float)rightListH * rightListH / Math.max(rightListH, rightTotalHeight)));
                int thumbY = rightListStartY + (int)(((float)previewsScrollY / rightMaxScroll) * (rightListH - thumbH));
                net.minecraft.client.gui.Gui.drawRect(trackX, thumbY, trackX + getScaledX(4), thumbY + thumbH, 0xFF888888);
            }
        } else {
            GlStateManager.color(0.7f, 0.7f, 0.7f, 1f);
            TextRenderUtils.drawStringScaleAware("Add a word to see previews!", settingsX, rightListStartY + getScaledY(5), textScale * 1.1f, false);
        }

        startScissor(boxX + getScaledX(20), leftListStartY, getScaledX(280), leftListH);
        int currentY = leftListStartY - scrollY;
        for (String word : words) {
            GlStateManager.color(0.15f, 0.15f, 0.15f, 1f);
            NineSliceUtils.draw(GuiTextures.storageBackground(1), boxX + getScaledX(20), currentY, getScaledX(270), getScaledY(26), 6, 18);
            GlStateManager.color(1f, 1f, 1f, 1f);

            TextRenderUtils.drawCenteredStringScaleAware(word, boxX + getScaledX(20) + (getScaledX(240) / 2f), currentY + (getScaledY(26) / 2f), textScale * 1.2f, false);

            CFButton delBtn = new CFButton(-1, boxX + getScaledX(265), currentY + getScaledY(3), getScaledX(20), getScaledY(20), "X", 0.8f, 0.2f, 0.2f);
            delBtn.drawButton(mc, mouseX, mouseY);

            currentY += leftItemHeight;
        }
        stopScissor();

        if (leftMaxScroll > 0) {
            int trackX = boxX + getScaledX(290);
            net.minecraft.client.gui.Gui.drawRect(trackX, leftListStartY, trackX + getScaledX(4), leftListStartY + leftListH, 0x55000000);
            int thumbH = Math.max(getScaledY(20), (int)((float)leftListH * leftListH / Math.max(leftListH, leftTotalHeight)));
            int thumbY = leftListStartY + (int)(((float)scrollY / leftMaxScroll) * (leftListH - thumbH));
            net.minecraft.client.gui.Gui.drawRect(trackX, thumbY, trackX + getScaledX(4), thumbY + thumbH, 0xFF888888);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        addWordField.mouseClicked(mouseX, mouseY, mouseButton);
        if (customPreviewField != null) customPreviewField.mouseClicked(mouseX, mouseY, mouseButton);

        int settingsX = boxX + getScaledX(320);
        int customY = boxY + getScaledY(185);
        int resultYOffset = (customPreviewResult != null && !customPreviewResult.isEmpty()) ? getScaledY(15) : 0;
        int rightListStartY = customY + getScaledY(25) + resultYOffset;
        int rightListH = (boxY + boxH - getScaledY(50)) - rightListStartY;
        int rightItemHeight = getScaledY(25);
        int rightTotalHeight = words.size() * rightItemHeight;
        int rightMaxScroll = Math.max(0, rightTotalHeight - rightListH);

        int leftListStartY = boxY + getScaledY(80);
        int leftListH = boxH - getScaledY(150);
        int leftItemHeight = getScaledY(30);
        int leftTotalHeight = words.size() * leftItemHeight;
        int leftMaxScroll = Math.max(0, leftTotalHeight - leftListH);

        if (rightMaxScroll > 0) {
            int dynamicWidth = getRightListWidth();
            int trackX = settingsX + dynamicWidth - getScaledX(10);
            int thumbH = Math.max(getScaledY(20), (int)((float)rightListH * rightListH / Math.max(rightListH, rightTotalHeight)));
            int thumbY = rightListStartY + (int)(((float)previewsScrollY / rightMaxScroll) * (rightListH - thumbH));
            if (mouseX >= trackX - getScaledX(5) && mouseX <= trackX + getScaledX(10) && mouseY >= thumbY && mouseY <= thumbY + thumbH) {
                dragModeRight = 1;
                dragStartYRight = mouseY;
                dragStartScrollYRight = previewsScrollY;
                return;
            }
        }
        if (mouseX >= settingsX && mouseX <= settingsX + getRightListWidth() && mouseY >= rightListStartY && mouseY <= rightListStartY + rightListH && rightMaxScroll > 0) {
            dragModeRight = 2;
            dragStartYRight = mouseY;
            dragStartScrollYRight = previewsScrollY;
            return;
        }

        if (leftMaxScroll > 0) {
            int trackX = boxX + getScaledX(290);
            int thumbH = Math.max(getScaledY(20), (int)((float)leftListH * leftListH / Math.max(leftListH, leftTotalHeight)));
            int thumbY = leftListStartY + (int)(((float)scrollY / leftMaxScroll) * (leftListH - thumbH));
            if (mouseX >= trackX - getScaledX(5) && mouseX <= trackX + getScaledX(10) && mouseY >= thumbY && mouseY <= thumbY + thumbH) {
                dragModeLeft = 1;
                dragStartYLeft = mouseY;
                dragStartScrollYLeft = scrollY;
                return;
            }
        }
        
        if (mouseX >= boxX + getScaledX(20) && mouseX <= boxX + getScaledX(290) && mouseY >= leftListStartY && mouseY <= leftListStartY + leftListH) {
            boolean clickedButton = false;
            int currentY = leftListStartY - scrollY;
            for (int i = 0; i < words.size(); i++) {
                int itemY = currentY + i * leftItemHeight;
                if (mouseY >= itemY && mouseY <= itemY + getScaledY(26)) {
                    CFButton delBtn = new CFButton(-1, boxX + getScaledX(265), itemY + getScaledY(3), getScaledX(20), getScaledY(20), "X", 0.8f, 0.2f, 0.2f);
                    if (delBtn.mousePressed(mc, mouseX, mouseY)) {
                        words.remove(i);
                        return;
                    }
                }
            }
            if (!clickedButton && leftMaxScroll > 0) {
                dragModeLeft = 2;
                dragStartYLeft = mouseY;
                dragStartScrollYLeft = scrollY;
                return;
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private int getRightListWidth() {
        int maxW = getScaledX(260);
        if (!words.isEmpty()) {
            ChatFilter tempFilter = new ChatFilter(words, mode, filterCase, action);
            for (int i = 0; i < words.size(); i++) {
                String word = words.get(i);
                int caseType = i % 3;
                if (caseType == 1) word = word.toUpperCase();
                else if (caseType == 2) word = word.toLowerCase();

                String ex = "";
                if (mode == FilterMode.STARTS) ex = "§e" + word + "§r quick brown fox";
                else if (mode == FilterMode.CONTAINS) ex = "quick §e" + word + "§r brown fox";
                else if (mode == FilterMode.ENDS) ex = "quick brown fox §e" + word + "§r";

                String res = tempFilter.applyFilter(ex);
                String fullStr = "§7[§e" + words.get(i) + "§7]§f: " + (i+1) + ") " + ex + " -> " + (res == null ? "§c[CANCELLED]" : res);
                int w = (int)(mc.fontRendererObj.getStringWidth(fullStr) * textScale);
                if (w > maxW) maxW = w;
            }
        }
        return Math.min(maxW + getScaledX(15), getScaledX(370));
    }

    private void startScissor(int x, int y, int width, int height) {
        ScaledResolution res = new ScaledResolution(mc);
        int scale = res.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * scale, mc.displayHeight - (y + height) * scale, width * scale, height * scale);
    }

    private void stopScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    public int getScaledX(double entry) {
        float configScale = io.hamlook.aetheria.core.ATHRConfig.feature.chat.chatFilterConfig.uiScale;
        return (int) (ResolutionUtils.getXStatic(1) * entry * 2.0 * configScale);
    }

    public int getScaledY(double entry) {
        float configScale = io.hamlook.aetheria.core.ATHRConfig.feature.chat.chatFilterConfig.uiScale;
        return (int) (ResolutionUtils.getYStatic(1) * entry * 2.0 * configScale);
    }
}
