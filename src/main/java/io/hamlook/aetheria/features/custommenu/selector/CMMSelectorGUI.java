package io.hamlook.aetheria.features.custommenu.selector;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.features.custommenu.CustomMMConfig;
import io.hamlook.aetheria.features.custommenu.Position;
import io.hamlook.aetheria.features.custommenu.ui.buttons.CMMButton;
import io.hamlook.aetheria.features.custommenu.util.CMMHelper;
import io.hamlook.aetheria.features.custommenu.editor.CMMEditorGUI;
import io.hamlook.aetheria.features.custommenu.editor.CMMClipboard;
import io.hamlook.aetheria.features.custommenu.util.ScreenHelper;
import io.hamlook.aetheria.utils.SoundUtils;
import io.hamlook.aetheria.utils.render.NineSliceUtils;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class CMMSelectorGUI extends GuiScreen {

    public Map<CustomMMConfig, Position> cmmList = new LinkedHashMap<>();
    public CustomMMConfig active;
    public CMMButton addButton;
    public CMMButton importButton;
    public CreateConfigElement createElement = new CreateConfigElement();

    public int CONFIG_BOX_WIDTH = 150;
    public int CONFIG_BOX_HEIGHT = 40;
    public int PADDING = 5;

    public int CREATE_ELEMENT_X = 0;
    public int CREATE_ELEMENT_Y = 0;
    public GuiTextField searchBar;
    private CustomMMConfig actionConfig;
    private boolean actionMenu;
    private boolean deleteConfirm;


    @Override
    public void initGui() {
        ScreenHelper.updateScreenDimensions(this.width, this.height);
        Keyboard.enableRepeatEvents(true);
        searchBar = new GuiTextField(0,mc.fontRendererObj, 0,0,0,0);
        addButton = new CMMButton( 0, 0, 0, 0, "Add") {
            @Override
            public void onClick(GuiScreen screen) {
                createElement.enabled = true;
            }
        };
        importButton = new CMMButton(0, 0, 0, 0, "Import") {
            @Override public void onClick(GuiScreen screen) { CustomMMConfig imported=CMMClipboard.pastePreset(); if(imported!=null){CMMHelper.importPreset(imported);updateCMMList();} }
        };
        updateSearchBar();
        updateButtons();
        Aetheria.logger.info("SearchBar Coords: " + searchBar.xPosition + " | "  + searchBar.yPosition);
        Aetheria.logger.info("SearchBar Size: " + searchBar.width + " | " + searchBar.height);
        CONFIG_BOX_WIDTH = ScreenHelper.getStaticWidth(150);
        CONFIG_BOX_HEIGHT = ScreenHelper.getStaticHeight(40);
        PADDING = ScreenHelper.getStaticWidth(5);
        CREATE_ELEMENT_X = ScreenHelper.getAnchoredX(ScreenHelper.Anchor.CENTER,-ScreenHelper.getStaticWidth(300));
        CREATE_ELEMENT_Y = ScreenHelper.getAnchoredY(ScreenHelper.Anchor.CENTER,ScreenHelper.getStaticHeight(100));
        createElement.updatePositions(CREATE_ELEMENT_X, CREATE_ELEMENT_Y);
    }

    private void updateButtons() {
        addButton.xPos = ScreenHelper.getAnchoredX(ScreenHelper.Anchor.LEFT,(ScreenHelper.getStaticWidth(35)+searchBar.width));
        addButton.yPos = ScreenHelper.getAnchoredY(ScreenHelper.Anchor.TOP,-ScreenHelper.getStaticHeight(35));
        addButton.width = ScreenHelper.getStaticWidth(70);
        addButton.height = ScreenHelper.getStaticHeight(30);
        importButton.xPos = addButton.xPos + addButton.width + ScreenHelper.getStaticWidth(8);
        importButton.yPos = addButton.yPos; importButton.width=ScreenHelper.getStaticWidth(90); importButton.height=addButton.height;
    }

    @Override
    public void onResize(Minecraft mcIn, int w, int h) {
        super.onResize(mcIn, w, h);
        ScreenHelper.updateScreenDimensions(this.width, this.height);
        updateCMMList();
        updateSearchBar();
        CONFIG_BOX_WIDTH = ScreenHelper.getStaticWidth(150);
        CONFIG_BOX_HEIGHT = ScreenHelper.getStaticHeight(40);
        PADDING = ScreenHelper.getStaticWidth(5);
        CREATE_ELEMENT_X = ScreenHelper.getAnchoredX(ScreenHelper.Anchor.CENTER,-ScreenHelper.getStaticWidth(300));
        CREATE_ELEMENT_Y = ScreenHelper.getAnchoredY(ScreenHelper.Anchor.CENTER, ScreenHelper.getStaticHeight(100));
        createElement.updatePositions(CREATE_ELEMENT_X, CREATE_ELEMENT_Y);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        super.actionPerformed(button);
    }

    private void updateSearchBar() {
        searchBar.xPosition = ScreenHelper.getAnchoredX(ScreenHelper.Anchor.LEFT,ScreenHelper.getStaticWidth(10));
        searchBar.yPosition = ScreenHelper.getAnchoredY(ScreenHelper.Anchor.TOP,-ScreenHelper.getStaticHeight(35));
        searchBar.width = ScreenHelper.getStaticWidth(300);
        searchBar.height = ScreenHelper.getStaticHeight(30);
    }

    private void updateCMMList() {
        active = CMMHelper.getCMMConfig();
        cmmList = CMMSelectorLayout.layout(CMMHelper.configList, searchBar == null ? "" : searchBar.getText(),
                this.width, CONFIG_BOX_WIDTH, CONFIG_BOX_HEIGHT, PADDING);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawBG();
        if (!CMMHelper.incompatiblePresets.isEmpty()) {
            String warning = "Unsupported preset version: " + CMMHelper.incompatiblePresets.get(0);
            TextRenderUtils.drawCenteredStringScaleAware(warning, this.width / 2f, ScreenHelper.getStaticHeight(12), new Color(255, 170, 85).getRGB(), 1f, true);
        }
        drawSearchBar();
        updateCMMList();
        createElement.render(CREATE_ELEMENT_X,CREATE_ELEMENT_Y,mouseX,mouseY);
        addButton.draw(mouseX,mouseY,partialTicks);
        importButton.draw(mouseX,mouseY,partialTicks);
        drawCMMList(mouseX,mouseY);
        if (actionMenu && actionConfig != null) drawActionMenu(mouseX, mouseY);
        if (deleteConfirm && actionConfig != null) drawDeleteConfirmation(mouseX, mouseY);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawCMMList(int mouseX,int mouseY) {
        for(CustomMMConfig config : cmmList.keySet()){
            Position position = cmmList.get(config);
            drawCMM(position,config,mouseX,mouseY);
        }
    }

    private void drawCMM(Position position, CustomMMConfig config,int mouseX, int mouseY) {
        int x = position.getX();
        int y = position.getY();
        boolean hovered = isHovering(position,mouseX,mouseY);
        boolean selected = CMMHelper.selectedConfig.equals(config.configName);
        NineSliceUtils.draw(getBGTex(),x,y,CONFIG_BOX_WIDTH,CONFIG_BOX_HEIGHT,6,18,hovered || selected);

        float xOff = ScreenHelper.getStaticWidth(20);
        float fontHeight = CONFIG_BOX_HEIGHT * 0.8f;
        float uiScale = Math.min(fontHeight / Math.max(1, mc.fontRendererObj.FONT_HEIGHT),
                (CONFIG_BOX_WIDTH - xOff) / Math.max(1f, mc.fontRendererObj.getStringWidth(config.configName)));
        float yPos = y + (CONFIG_BOX_HEIGHT / 2f);
        TextRenderUtils.drawCenteredStringScaleAware(config.configName,x+(CONFIG_BOX_WIDTH/2f),yPos,
                hovered || selected ? new Color(235, 235, 235).getRGB() : new Color(168, 168, 168).getRGB(),
                uiScale,true);
    }

    private void drawActionMenu(int mx, int my) {
        int x = width / 2 - 190, y = height / 2 - 62;
        NineSliceUtils.draw(getBGTex(), x, y, 380, 125, 8, 18, true);
        TextRenderUtils.drawCenteredStringScaleAware("Menu Actions", width / 2f, y + 17, 0xFFFFFFFF, 1.35f, true);
        TextRenderUtils.drawCenteredStringScaleAware(actionConfig.configName, width / 2f, y + 34, 0xFF8FD7F0, .9f, false);
        String[] actions = {"Use this Menu", "Edit this Menu", "Delete this Menu", "Export to Clipboard"};
        for (int i=0;i<4;i++) { int bx=x+10+i*91; boolean h=mx>=bx&&mx<=bx+84&&my>=y+58&&my<y+88; drawRect(bx,y+58,bx+84,y+88,h?0xFF3B6982:0xFF292932); TextRenderUtils.drawCenteredStringScaleAware(actions[i],bx+42,y+73, i==2?0xFFFF9999:0xFFFFFFFF,.61f,false); }
    }

    private void drawDeleteConfirmation(int mx, int my) {
        int x=width/2-150,y=height/2-50; NineSliceUtils.draw(getBGTex(),x,y,300,100,8,18,true);
        TextRenderUtils.drawCenteredStringScaleAware("Delete " + actionConfig.configName + "?",width/2f,y+22,0xFFFFFFFF,1.2f,true);
        drawRect(x+25,y+55,x+125,y+80,mx>=x+25&&mx<=x+125&&my>=y+55&&my<y+80?0xFF7A3D4A:0xFF3B252D);
        drawRect(x+175,y+55,x+275,y+80,mx>=x+175&&mx<=x+275&&my>=y+55&&my<y+80?0xFF3B6982:0xFF292932);
        TextRenderUtils.drawCenteredStringScaleAware("Delete",x+75,y+67,0xFFFFFFFF,.9f,false); TextRenderUtils.drawCenteredStringScaleAware("Cancel",x+225,y+67,0xFFFFFFFF,.9f,false);
    }

    public ResourceLocation getBGTex(){
        return Resources.betterContainerNineSlice(0);
    }

    public void drawSearchBar() {
        if(searchBar.getEnableBackgroundDrawing()){
            searchBar.setEnableBackgroundDrawing(false);
        }
        GuiScreen.drawRect(searchBar.xPosition,searchBar.yPosition,
                searchBar.xPosition+searchBar.width,
                searchBar.yPosition+searchBar.height,new Color(255,255,255).getRGB());
        GuiScreen.drawRect(searchBar.xPosition+1,searchBar.yPosition+1,searchBar.xPosition+searchBar.width-1,
                searchBar.yPosition+searchBar.height-1,new Color(28, 28, 28).getRGB());

        GlStateManager.pushMatrix();
        GlStateManager.translate(searchBar.xPosition, searchBar.yPosition+ScreenHelper.getStaticHeight(5), 0);
        GlStateManager.scale(2*ScreenHelper.getScaleFactor(), 2*ScreenHelper.getScaleFactor(), 1.0f);

        int originalX = searchBar.xPosition;
        int originalY = searchBar.yPosition;
        searchBar.xPosition = 0;
        searchBar.yPosition = 0;

        searchBar.drawTextBox();
        // GuiTextField in 1.8.9 has no native placeholder API. Draw it in the same
        // local coordinate space and scale as the field so it cannot drift outside.
        if (searchBar.getText().isEmpty() && !searchBar.isFocused())
            Minecraft.getMinecraft().fontRendererObj.drawString("Search for a Preset", 4, (searchBar.height - 8) / 2, 0xFF888888);
        searchBar.xPosition = originalX;
        searchBar.yPosition = originalY;
        GlStateManager.popMatrix();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if(searchBar.textboxKeyTyped(typedChar,keyCode)) { updateCMMList(); return; }
        createElement.keyboardInput(typedChar,keyCode);
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (deleteConfirm && actionConfig != null) { handleDeleteConfirmation(mouseX, mouseY); return; }
        if (actionMenu && actionConfig != null) { handleActionMenu(mouseX, mouseY); return; }
        searchBar.mouseClicked(mouseX, mouseY, mouseButton);
        if (createElement.mouseInput(mouseX, mouseY, mouseButton)) {
            return;
        }

        if (addButton.checkHover(mouseX, mouseY)) {
            SoundUtils.playSound("gui.button.press");
            addButton.onClick(this);
            return;
        }
        if (importButton.checkHover(mouseX, mouseY)) { SoundUtils.playSound("gui.button.press"); importButton.onClick(this); return; }
        if(mouseButton == 0) {
            for (CustomMMConfig config : cmmList.keySet()) {
                Position position = cmmList.get(config);
                if (isHovering(position, mouseX, mouseY)) {
                    actionConfig = config;
                    actionMenu = true;
                    SoundUtils.playSound("gui.button.press");
                    return;
                }
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void handleActionMenu(int mx, int my) {
        int x=width/2-190,y=height/2-62;
        if (my<y+58||my>=y+88) { actionMenu=false; return; }
        int option=(mx-x-10)/91; if(option<0||option>3||mx>x+374){actionMenu=false;return;}
        if(option==0){CMMHelper.selectPreset(actionConfig.configName);actionMenu=false;updateCMMList();}
        else if(option==1){actionMenu=false;Minecraft.getMinecraft().displayGuiScreen(new CMMEditorGUI(actionConfig,this));}
        else if(option==2){actionMenu=false;if(!CMMHelper.isModPreset(actionConfig.configName))deleteConfirm=true;}
        else { CMMClipboard.copyPreset(actionConfig); actionMenu=false; }
    }
    private void handleDeleteConfirmation(int mx,int my) {
        int x=width/2-150,y=height/2-50;
        if(my<y+55||my>=y+80){deleteConfirm=false;return;}
        if(mx>=x+25&&mx<=x+125){CMMHelper.deletePreset(actionConfig.configName);deleteConfirm=false;actionConfig=null;updateCMMList();}
        else if(mx>=x+175&&mx<=x+275){deleteConfirm=false;}
    }

    private boolean isHovering(Position position, int mouseX, int mouseY) {
        int x = position.getX();
        int y = position.getY();
        return mouseX > x && mouseX < x + CONFIG_BOX_WIDTH && mouseY > y && mouseY < y + CONFIG_BOX_HEIGHT;
    }

    private void drawBG() {
        drawRect(0,0,this.width,this.height,0);
        ResourceLocation location = getBGTex();
        NineSliceUtils.draw(location,1,1,this.width-2,this.height-2,6,18);
        TextRenderUtils.drawStringScaleAware("Select Menu Configuration",
                ScreenHelper.getAnchoredX(ScreenHelper.Anchor.LEFT,ScreenHelper.getStaticWidth(10)),
                ScreenHelper.getAnchoredY(ScreenHelper.Anchor.TOP,-ScreenHelper.getStaticHeight(10)),
                new Color(1, 91, 117).getRGB(),3f,true);
    }
}
