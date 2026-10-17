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
import io.hamlook.aetheria.utils.KeybindHelper;
import io.hamlook.aetheria.utils.compat.AetheriaBaseScreen;
import io.hamlook.aetheria.utils.compat.DrawContextUtils;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.GuiScreenUtils;
import io.hamlook.aetheria.utils.compat.KeyboardCompat;

public class CMMSelectorGUI extends AetheriaBaseScreen {

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
    protected void onInitGui() {
        ScreenHelper.updateScreenDimensions(this.width, this.height);
        KeyboardCompat.enableRepeatEvents(true);
        searchBar = GuiScreenUtils.createTextField(MinecraftCompat.getFontRenderer(), 0,0,0,0);
        addButton = new CMMButton( 0, 0, 0, 0, "Add") {
            @Override
            public void onClick(GuiScreen screen) {
                createElement.enabled = true;
            }
        };
        updateSearchBar();
        updateButtons();
        Aetheria.logger.info("SearchBar Coords: " + GuiScreenUtils.getX(searchBar) + " | "  + GuiScreenUtils.getY(searchBar));
        Aetheria.logger.info("SearchBar Size: " + GuiScreenUtils.getW(searchBar) + " | " + GuiScreenUtils.getH(searchBar));
        CONFIG_BOX_WIDTH = ScreenHelper.getStaticWidth(150);
        CONFIG_BOX_HEIGHT = ScreenHelper.getStaticHeight(40);
        PADDING = ScreenHelper.getStaticWidth(5);
        CREATE_ELEMENT_X = ScreenHelper.getAnchoredX(ScreenHelper.Anchor.CENTER,-ScreenHelper.getStaticWidth(300));
        CREATE_ELEMENT_Y = ScreenHelper.getAnchoredY(ScreenHelper.Anchor.CENTER,ScreenHelper.getStaticHeight(100));
        createElement.updatePositions(CREATE_ELEMENT_X,CREATE_ELEMENT_Y);
    }

    private void updateButtons() {
        addButton.xPos = ScreenHelper.getAnchoredX(ScreenHelper.Anchor.LEFT,(ScreenHelper.getStaticWidth(35)+GuiScreenUtils.getW(searchBar)));
        addButton.yPos = ScreenHelper.getAnchoredY(ScreenHelper.Anchor.TOP,-ScreenHelper.getStaticHeight(35));
        addButton.width = ScreenHelper.getStaticWidth(70);
        addButton.height = ScreenHelper.getStaticHeight(30);
    }

    @Override
    protected void onResizeScreen(int w, int h) {
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

    private void updateSearchBar() {
        GuiScreenUtils.setX(searchBar, ScreenHelper.getAnchoredX(ScreenHelper.Anchor.LEFT,ScreenHelper.getStaticWidth(10)));
        GuiScreenUtils.setY(searchBar, ScreenHelper.getAnchoredY(ScreenHelper.Anchor.TOP,-ScreenHelper.getStaticHeight(35)));
        GuiScreenUtils.setW(searchBar, ScreenHelper.getStaticWidth(300));
        GuiScreenUtils.setH(searchBar, ScreenHelper.getStaticHeight(30));
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
    protected void onDrawScreen(int mouseX, int mouseY, float partialTicks) {
        drawBG();
        drawSearchBar();
        if(cmmList.isEmpty()){
            updateCMMList();
        }
        createElement.render(CREATE_ELEMENT_X,CREATE_ELEMENT_Y,mouseX,mouseY);
        addButton.draw(mouseX,mouseY,partialTicks);
        drawCMMList(mouseX,mouseY);
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
        if(GuiScreenUtils.drawsTextFieldBackground(searchBar)){
            GuiScreenUtils.setTextFieldBackground(searchBar, false);
        }
        DrawContextUtils.fill(GuiScreenUtils.getX(searchBar),GuiScreenUtils.getY(searchBar),
                GuiScreenUtils.getX(searchBar)+GuiScreenUtils.getW(searchBar),
                GuiScreenUtils.getY(searchBar)+GuiScreenUtils.getH(searchBar),new Color(255,255,255).getRGB());
        DrawContextUtils.fill(GuiScreenUtils.getX(searchBar)+1,GuiScreenUtils.getY(searchBar)+1,GuiScreenUtils.getX(searchBar)+GuiScreenUtils.getW(searchBar)-1,
                GuiScreenUtils.getY(searchBar)+GuiScreenUtils.getH(searchBar)-1,new Color(28, 28, 28).getRGB());

        GlStateManagerCompat.pushMatrix();
        GlStateManagerCompat.translate(GuiScreenUtils.getX(searchBar), GuiScreenUtils.getY(searchBar)+ScreenHelper.getStaticHeight(5), 0);
        GlStateManagerCompat.scale(2*ScreenHelper.getScaleFactor(), 2*ScreenHelper.getScaleFactor(), 1.0f);

        int originalX = GuiScreenUtils.getX(searchBar);
        int originalY = GuiScreenUtils.getY(searchBar);
        GuiScreenUtils.setX(searchBar, 0);
        GuiScreenUtils.setY(searchBar, 0);

        int[] mouse = KeybindHelper.getMouseCoords(GuiScreenUtils.getScaledWindowWidth(), GuiScreenUtils.getScaledWindowHeight());
        GuiScreenUtils.renderTextField(searchBar, mouse[0], mouse[1]);
        GuiScreenUtils.setX(searchBar, originalX);
        GuiScreenUtils.setY(searchBar, originalY);
        GlStateManagerCompat.popMatrix();
    }

    @Override
    protected void guiClosed() {
        KeyboardCompat.enableRepeatEvents(false);
    }

    @Override
    protected void onKeyTyped(char typedChar, int keyCode) {
        if(GuiScreenUtils.typeTextField(searchBar, typedChar,keyCode)) return;
        createElement.keyboardInput(typedChar,keyCode);
    }

    //#if MC < 1.21
    //#else
    //$$ @Override
    //$$ protected void onCharTyped(char typedChar, int modifiers) {
    //$$     if(GuiScreenUtils.typeTextField(searchBar, typedChar,0)) return;
    //$$     createElement.keyboardInput(typedChar,0);
    //$$ }
    //#endif

    @Override
    protected void onMouseClicked(int mouseX, int mouseY, int mouseButton) {
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
    }

    private boolean isHovering(Position position, int mouseX, int mouseY) {
        int x = position.getX();
        int y = position.getY();
        return mouseX > x && mouseX < x + CONFIG_BOX_WIDTH && mouseY > y && mouseY < y + CONFIG_BOX_HEIGHT;
    }

    private void drawBG() {
        DrawContextUtils.fill(0,0,this.width,this.height,0);
        ResourceLocation location = getBGTex();
        NineSliceUtils.draw(location,1,1,this.width-2,this.height-2,6,18);
        TextRenderUtils.drawStringScaleAware("Select Menu Configuration",
                ScreenHelper.getAnchoredX(ScreenHelper.Anchor.LEFT,ScreenHelper.getStaticWidth(10)),
                ScreenHelper.getAnchoredY(ScreenHelper.Anchor.TOP,-ScreenHelper.getStaticHeight(10)),
                new Color(1, 91, 117).getRGB(),3f,true);
    }
}
