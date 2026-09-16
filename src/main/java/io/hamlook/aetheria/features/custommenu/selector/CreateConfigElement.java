package io.hamlook.aetheria.features.custommenu.selector;

import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.features.custommenu.ui.buttons.CMMButton;
import io.hamlook.aetheria.features.custommenu.util.ScreenHelper;
import io.hamlook.aetheria.utils.SoundUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.render.NineSliceUtils;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

import java.awt.*;
import io.hamlook.aetheria.utils.compat.GuiScreenUtils;

public class CreateConfigElement {

    public boolean enabled = false;
    public GuiTextField nameField = new GuiTextField(0, MinecraftCompat.getMinecraft().fontRendererObj, 0,0,0,0);
    public CMMButton createButton = new CMMButton(0, 0, 0, 0, "Create") {
        @Override
        public void onClick(GuiScreen screen) {
            //TODO: Create Preset & Open Editor
        }
    };
    public CMMButton cancelButton = new CMMButton(0, 0, 0, 0, "Cancel") {
        @Override
        public void onClick(GuiScreen screen) {
            enabled = false;
            nameField.setText("");
        }
    };
    public int GUI_WIDTH = 600;
    public int GUI_HEIGHT = 200;

    public void render(int x,int y,int mouseX,int mouseY) {
        if(!enabled) return;
        ResourceLocation location = Resources.betterContainerNineSlice(0);
        NineSliceUtils.draw(location,x,y,
                GUI_WIDTH,
                GUI_HEIGHT,6,18);

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        TextRenderUtils.drawStringScaleAware(
                "Create Custom Main Menu",
                x + ScreenHelper.getStaticWidth(20),
                y + ScreenHelper.getStaticHeight(30),
                3f
        );

        createButton.draw(mouseX,mouseY,0);
        cancelButton.draw(mouseX,mouseY,0);
        drawNameField();
    }

    private void drawNameField() {
        if(nameField.getEnableBackgroundDrawing()){
            nameField.setEnableBackgroundDrawing(false);
        }
        GuiScreen.drawRect(GuiScreenUtils.getX(nameField),GuiScreenUtils.getY(nameField),
                GuiScreenUtils.getX(nameField)+GuiScreenUtils.getW(nameField),
                GuiScreenUtils.getY(nameField)+GuiScreenUtils.getH(nameField),new Color(255,255,255).getRGB());
        GuiScreen.drawRect(GuiScreenUtils.getX(nameField)+1,GuiScreenUtils.getY(nameField)+1,GuiScreenUtils.getX(nameField)+GuiScreenUtils.getW(nameField)-1,
                GuiScreenUtils.getY(nameField)+GuiScreenUtils.getH(nameField)-1,new Color(28, 28, 28).getRGB());

        GlStateManager.pushMatrix();
        GlStateManager.translate(GuiScreenUtils.getX(nameField), GuiScreenUtils.getY(nameField)+ScreenHelper.getStaticHeight(5), 0);
        GlStateManager.scale(2*ScreenHelper.getScaleFactor(),2* ScreenHelper.getScaleFactor(), 1.0f);

        int originalX = GuiScreenUtils.getX(nameField);
        int originalY = GuiScreenUtils.getY(nameField);
        GuiScreenUtils.setX(nameField, 0);
        GuiScreenUtils.setY(nameField, 0);

        nameField.drawTextBox();
        GuiScreenUtils.setX(nameField, originalX);
        GuiScreenUtils.setY(nameField, originalY);
        GlStateManager.popMatrix();
    }

    public void updatePositions(int x, int y){
        GUI_WIDTH = ScreenHelper.getStaticWidth(600);
        GUI_HEIGHT = ScreenHelper.getStaticHeight(200);

        // Name Field
        GuiScreenUtils.setX(nameField, (int)(x + (GUI_WIDTH * 0.1)));
        GuiScreenUtils.setY(nameField, ScreenHelper.getAnchoredY(ScreenHelper.Anchor.CENTER,ScreenHelper.getStaticHeight(30)));
        GuiScreenUtils.setW(nameField, (int)(GUI_WIDTH * 0.8));
        GuiScreenUtils.setH(nameField, ScreenHelper.getStaticHeight(30));

        // Create Button
        createButton.xPos = ScreenHelper.getAnchoredX(ScreenHelper.Anchor.CENTER,-ScreenHelper.getStaticWidth(225));
        createButton.yPos = ScreenHelper.getAnchoredY(ScreenHelper.Anchor.CENTER,-ScreenHelper.getStaticHeight(30));
        createButton.width = ScreenHelper.getStaticWidth(200);
        createButton.height = ScreenHelper.getStaticHeight(20);

        // Cancel Button
        cancelButton.xPos = ScreenHelper.getAnchoredX(ScreenHelper.Anchor.CENTER,ScreenHelper.getStaticWidth(25));
        cancelButton.yPos = ScreenHelper.getAnchoredY(ScreenHelper.Anchor.CENTER,-ScreenHelper.getStaticHeight(30));
        cancelButton.width = ScreenHelper.getStaticWidth(200);
        cancelButton.height = ScreenHelper.getStaticHeight(20);
    }

    public boolean mouseInput(int mouseX, int mouseY,int mouseButton) {
        if(!enabled) return false;
        nameField.mouseClicked(mouseX,mouseY,mouseButton);
        if(isHovering(cancelButton,mouseX,mouseY)){
            SoundUtils.playSound("gui.button.press");
            cancelButton.onClick(MinecraftCompat.getMinecraft().currentScreen);
            return true;
        }
        if(isHovering(createButton,mouseX,mouseY)){
            SoundUtils.playSound("gui.button.press");
            createButton.onClick(MinecraftCompat.getMinecraft().currentScreen);
            return true;
        }
        return false;
    }

    private boolean isHovering(CMMButton button, int mouseX, int mouseY) {
        return mouseX >= button.xPos && mouseX <= button.xPos + button.width
                && mouseY >= button.yPos && mouseY <= button.yPos + button.height;
    }

    public boolean keyboardInput(char  typedChar, int keyCode){
        return nameField.textboxKeyTyped(typedChar,keyCode);
    }
}
