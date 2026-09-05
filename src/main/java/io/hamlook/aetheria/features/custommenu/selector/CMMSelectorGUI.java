package io.hamlook.aetheria.features.custommenu.selector;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.features.custommenu.CustomMMConfig;
import io.hamlook.aetheria.features.custommenu.Position;
import io.hamlook.aetheria.features.custommenu.ui.buttons.CMMButton;
import io.hamlook.aetheria.features.custommenu.util.CMMHelper;
import io.hamlook.aetheria.features.custommenu.util.ScreenHelper;
import io.hamlook.aetheria.utils.SoundUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
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
import java.util.HashMap;

public class CMMSelectorGUI extends GuiScreen {

    public HashMap<CustomMMConfig, Position> cmmList = new HashMap<>();
    public CustomMMConfig active;
    public CMMButton addButton;
    public CreateConfigElement createElement = new CreateConfigElement();

    public int CONFIG_BOX_WIDTH = 150;
    public int CONFIG_BOX_HEIGHT = 40;
    public int PADDING = 5;

    public int CREATE_ELEMENT_X = 0;
    public int CREATE_ELEMENT_Y = 0;
    public GuiTextField searchBar;


    @Override
    public void initGui() {
        ScreenHelper.updateScreenDimensions(this.width, this.height);
        Keyboard.enableRepeatEvents(true);
        searchBar = new GuiTextField(0,MinecraftCompat.getFontRenderer(), 0,0,0,0);
        addButton = new CMMButton( 0, 0, 0, 0, "Add") {
            @Override
            public void onClick(GuiScreen screen) {
                createElement.enabled = true;
            }
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
        createElement.updatePositions(CREATE_ELEMENT_X,CREATE_ELEMENT_Y);
    }

    private void updateButtons() {
        addButton.xPos = ScreenHelper.getAnchoredX(ScreenHelper.Anchor.LEFT,(ScreenHelper.getStaticWidth(35)+searchBar.width));
        addButton.yPos = ScreenHelper.getAnchoredY(ScreenHelper.Anchor.TOP,-ScreenHelper.getStaticHeight(35));
        addButton.width = ScreenHelper.getStaticWidth(70);
        addButton.height = ScreenHelper.getStaticHeight(30);
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
        createElement.updatePositions(CREATE_ELEMENT_X,CREATE_ELEMENT_Y);
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
        int xOff = 0;
        int yOff = 0;
        for(CustomMMConfig config : CMMHelper.configList.values()){
            Position position = new Position("LEFT","TOP",(ScreenHelper.getStaticWidth(10)+xOff),-(ScreenHelper.getStaticHeight(80)+yOff));
            cmmList.put(config,position);
            xOff += CONFIG_BOX_WIDTH + PADDING;
            if(xOff + CONFIG_BOX_WIDTH > this.width){
                yOff += CONFIG_BOX_HEIGHT + PADDING;
                xOff = 0;
            }
            Aetheria.logger.info(config.configName + " is set at " + position.getX() + " | " + position.getY());
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawBG();
        drawSearchBar();
        if(cmmList.isEmpty()){
            updateCMMList();
        }
        createElement.render(CREATE_ELEMENT_X,CREATE_ELEMENT_Y,mouseX,mouseY);
        addButton.draw(mouseX,mouseY,partialTicks);
        drawCMMList(mouseX,mouseY);
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
        NineSliceUtils.draw(getBGTex(),x,y,CONFIG_BOX_WIDTH,CONFIG_BOX_HEIGHT,6,18,isHovering(position,mouseX,mouseY));

        float xOff = ScreenHelper.getStaticWidth(20);
        float uiScale = Math.max(1,((CONFIG_BOX_WIDTH - xOff)/MinecraftCompat.getFontRenderer().getStringWidth(config.configName)));
        float yPos = y + (CONFIG_BOX_HEIGHT / 2f);
        TextRenderUtils.drawCenteredStringScaleAware(config.configName,x+(CONFIG_BOX_WIDTH/2f),yPos,
                isHovering(position,mouseX,mouseY) ? new Color(214, 214, 214).getRGB() : new Color(168, 168, 168).getRGB(),
                uiScale,true);
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
        if(searchBar.textboxKeyTyped(typedChar,keyCode)) return;
        createElement.keyboardInput(typedChar,keyCode);
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        searchBar.mouseClicked(mouseX, mouseY, mouseButton);
        if (createElement.mouseInput(mouseX, mouseY, mouseButton)) {
            return;
        }

        if (addButton.checkHover(mouseX, mouseY)) {
            SoundUtils.playSound("gui.button.press");
            addButton.onClick(this);
            return;
        }
        if(mouseButton == 0) {
            for (CustomMMConfig config : cmmList.keySet()) {
                Position position = cmmList.get(config);
                if (isHovering(position, mouseX, mouseY)) {
                    CMMHelper.selectPreset(config.configName);
                    SoundUtils.playSound("gui.button.press");
                    updateCMMList();
                }
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
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
