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
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ChatFilterGUI extends GuiScreen {

    public List<ChatFilter> chatFilters;
    public int boxX, boxY, boxW, boxH;

    private GuiTextField searchField;
    private boolean showStarts = true, showEnds = true, showContains = true;
    private boolean showSensitive = true, showInsensitive = true;

    private int scrollY = 0;
    private float textScale;

    private int dragMode = 0;
    private int dragStartY = 0;
    private int dragStartScrollY = 0;

    @Override
    public void initGui() {
        chatFilters = new ArrayList<>(ChatFilterManager.chatFilters);
        boxW = getScaledX(700);
        boxH = getScaledY(500);
        boxX = (width - boxW) / 2;
        boxY = (height - boxH) / 2;
        float configScale = io.hamlook.aetheria.core.ATHRConfig.feature.chat.chatFilterConfig.uiScale;
        textScale = ResolutionUtils.getXStatic(1) * 1.5f * configScale;

        String prevSearch = searchField != null ? searchField.getText() : "";
        searchField = new GuiTextField(0, fontRendererObj, boxX + getScaledX(20), boxY + getScaledY(20), boxW - getScaledX(140), getScaledY(20));
        searchField.setMaxStringLength(100);
        searchField.setText(prevSearch);
        searchField.setFocused(true);

        buttonList.clear();
        int addX = boxX + boxW - getScaledX(110);
        int addY = boxY + getScaledY(20);
        buttonList.add(new CFButton(0, addX, addY, getScaledX(90), getScaledY(20), "Add Filter", 0.2f, 0.4f, 0.8f));

        int ty = boxY + getScaledY(50);
        int th = getScaledY(16);
        buttonList.add(new CFButton(1, boxX + getScaledX(20), ty, getScaledX(60), th, "STARTS", true, showStarts));
        buttonList.add(new CFButton(2, boxX + getScaledX(85), ty, getScaledX(60), th, "ENDS", true, showEnds));
        buttonList.add(new CFButton(3, boxX + getScaledX(150), ty, getScaledX(65), th, "CONTAINS", true, showContains));
        buttonList.add(new CFButton(4, boxX + getScaledX(220), ty, getScaledX(70), th, "SENSITIVE", true, showSensitive));
        buttonList.add(new CFButton(5, boxX + getScaledX(295), ty, getScaledX(85), th, "INSENSITIVE", true, showInsensitive));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            mc.displayGuiScreen(new ChatFilterEditorGUI(this, null));
            return;
        } else if (button.id == 1) { showStarts = !showStarts; }
        else if (button.id == 2) { showEnds = !showEnds; }
        else if (button.id == 3) { showContains = !showContains; }
        else if (button.id == 4) { showSensitive = !showSensitive; }
        else if (button.id == 5) { showInsensitive = !showInsensitive; }

        initGui();
    }

    @Override
    public void onGuiClosed() {
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
            if (!search.isEmpty()) {
                boolean match = false;
                for (String w : cf.filterWords) {
                    if (w.toLowerCase().contains(search)) {
                        match = true;
                        break;
                    }
                }
                return match;
            }
            return true;
        }).collect(Collectors.toList());
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        
        GlStateManager.color(0.2f, 0.2f, 0.2f, 1f);
        NineSliceUtils.draw(GuiTextures.storageBackground(1), boxX, boxY, boxW, boxH, 6, 18);
        GlStateManager.color(1f, 1f, 1f, 1f);

        RenderUtils.drawSearchBar(searchField, false);

        List<ChatFilter> displayed = getDisplayedFilters();

        int itemHeight = getScaledY(40);
        int listStartY = boxY + getScaledY(80);
        int listHeight = boxH - getScaledY(90);
        int totalHeight = displayed.size() * itemHeight;
        int maxScroll = Math.max(0, totalHeight - listHeight);

        if (Mouse.isButtonDown(0)) {
            if (dragMode == 1) {
                int deltaY = mouseY - dragStartY;
                int thumbHeight = Math.max(getScaledY(20), (int)((float)listHeight * listHeight / Math.max(listHeight, totalHeight)));
                float scrollRatio = maxScroll == 0 ? 0 : maxScroll / (float)(listHeight - thumbHeight);
                scrollY = dragStartScrollY + (int)(deltaY * scrollRatio);
            } else if (dragMode == 2) {
                int deltaY = mouseY - dragStartY;
                scrollY = dragStartScrollY - deltaY;
            }
        } else {
            dragMode = 0;
        }

        int dWheel = Mouse.getDWheel();
        if (dWheel != 0) {
            scrollY -= Integer.signum(dWheel) * getScaledY(20);
        }
        
        if (scrollY > maxScroll) scrollY = maxScroll;
        if (scrollY < 0) scrollY = 0;

        startScissor(boxX, listStartY, boxW, listHeight);
        int currentY = listStartY - scrollY;

        for (ChatFilter cf : displayed) {
            GlStateManager.color(0.15f, 0.15f, 0.15f, 1f);
            NineSliceUtils.draw(GuiTextures.storageBackground(1), boxX + getScaledX(20), currentY, boxW - getScaledX(40), getScaledY(36), 6, 18);
            GlStateManager.color(1f, 1f, 1f, 1f);
            
            String wordsPreview = String.join(", ", cf.filterWords);
            if (wordsPreview.length() > 50) wordsPreview = wordsPreview.substring(0, 50) + "...";
            
            String info = String.format("§7[%s] [%s] [%s]", cf.filterType.name, cf.filterCase.name(), cf.replace ? "REPLACE" : "CANCEL");
            
            TextRenderUtils.drawStringScaleAware(wordsPreview, boxX + getScaledX(30), currentY + getScaledY(8), textScale * 1.2f, false);
            TextRenderUtils.drawStringScaleAware(info, boxX + getScaledX(30), currentY + getScaledY(22), textScale, false);

            CFButton editBtn = new CFButton(-1, boxX + boxW - getScaledX(120), currentY + getScaledY(8), getScaledX(45), getScaledY(20), "Edit", 0.2f, 0.7f, 0.2f);
            CFButton delBtn = new CFButton(-2, boxX + boxW - getScaledX(70), currentY + getScaledY(8), getScaledX(45), getScaledY(20), "Delete", 0.8f, 0.2f, 0.2f);
            editBtn.drawButton(mc, mouseX, mouseY);
            delBtn.drawButton(mc, mouseX, mouseY);

            currentY += itemHeight;
        }
        stopScissor();

        if (maxScroll > 0) {
            int trackX = boxX + boxW - getScaledX(10);
            net.minecraft.client.gui.Gui.drawRect(trackX, listStartY, trackX + getScaledX(4), listStartY + listHeight, 0x55000000);
            
            int thumbHeight = Math.max(getScaledY(20), (int)((float)listHeight * listHeight / Math.max(listHeight, totalHeight)));
            float scrollProgress = (float)scrollY / maxScroll;
            int thumbY = listStartY + (int)(scrollProgress * (listHeight - thumbHeight));
            net.minecraft.client.gui.Gui.drawRect(trackX, thumbY, trackX + getScaledX(4), thumbY + thumbHeight, 0xFF888888);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        searchField.mouseClicked(mouseX, mouseY, mouseButton);

        List<ChatFilter> displayed = getDisplayedFilters();
        int itemHeight = getScaledY(40);
        int listStartY = boxY + getScaledY(80);
        int listHeight = boxH - getScaledY(90);
        int totalHeight = displayed.size() * itemHeight;
        int maxScroll = Math.max(0, totalHeight - listHeight);
        int currentY = listStartY - scrollY;

        if (maxScroll > 0) {
            int trackX = boxX + boxW - getScaledX(10);
            int thumbHeight = Math.max(getScaledY(20), (int)((float)listHeight * listHeight / Math.max(listHeight, totalHeight)));
            float scrollProgress = (float)scrollY / maxScroll;
            int thumbY = listStartY + (int)(scrollProgress * (listHeight - thumbHeight));
            
            if (mouseX >= trackX - getScaledX(5) && mouseX <= trackX + getScaledX(10) && mouseY >= thumbY && mouseY <= thumbY + thumbHeight) {
                dragMode = 1;
                dragStartY = mouseY;
                dragStartScrollY = scrollY;
                return;
            }
        }

        if (mouseX >= boxX && mouseX <= boxX + boxW && mouseY >= listStartY && mouseY <= listStartY + listHeight) {
            boolean clickedButton = false;
            for (int i = 0; i < displayed.size(); i++) {
                ChatFilter cf = displayed.get(i);
                int itemY = currentY + i * itemHeight;
                if (mouseY >= itemY && mouseY <= itemY + getScaledY(36)) {
                    CFButton editBtn = new CFButton(-1, boxX + boxW - getScaledX(120), itemY + getScaledY(8), getScaledX(45), getScaledY(20), "Edit", 0.2f, 0.7f, 0.2f);
                    CFButton delBtn = new CFButton(-2, boxX + boxW - getScaledX(70), itemY + getScaledY(8), getScaledX(45), getScaledY(20), "Delete", 0.8f, 0.2f, 0.2f);
                    
                    if (editBtn.mousePressed(mc, mouseX, mouseY)) {
                        mc.displayGuiScreen(new ChatFilterEditorGUI(this, cf));
                        return;
                    }
                    if (delBtn.mousePressed(mc, mouseX, mouseY)) {
                        chatFilters.remove(cf);
                        ChatFilterManager.chatFilters.remove(cf);
                        ChatFilterManager.saveToFile();
                        return;
                    }
                }
            }
            if (!clickedButton && maxScroll > 0) {
                dragMode = 2;
                dragStartY = mouseY;
                dragStartScrollY = scrollY;
                return;
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
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
