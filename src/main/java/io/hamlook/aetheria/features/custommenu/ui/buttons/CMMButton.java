package io.hamlook.aetheria.features.custommenu.ui.buttons;

import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.features.custommenu.Position;
import io.hamlook.aetheria.features.custommenu.ui.CMMElement;
import io.hamlook.aetheria.utils.render.NineSliceUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;

public abstract class CMMButton extends CMMElement {

    public String displayString;
    public int style = 0;

    public CMMButton(int xPos, int yPos, String displayString) {
        this(xPos, yPos, 200, 20, displayString);
    }

    public CMMButton(int xPos, int yPos, int width, int height, String displayString) {
        super(new Position(),width,height,xPos,yPos);
        this.displayString = displayString;
    }

    public CMMButton(Position position, int width, int height, String displayString) {
        super(position,width,height);
        this.displayString = displayString;
    }

    public abstract void onClick(GuiScreen screen);

    @Override
    public void draw(int mouseX, int mouseY, float partialTicks) {
        boolean hovered = checkHover(mouseX, mouseY);
        int textColor = hovered ? 0xFFFFFFFF : 0xFFAAAAAA;

        GlStateManager.color(1f,1f,1f,1f);
        GlStateManager.pushMatrix();
        NineSliceUtils.draw(Resources.betterContainerNineSlice(style), xPos, yPos, width, height, 6, 18);
        GlStateManager.popMatrix();

        drawCenteredString(displayString, this.xPos, this.yPos, this.width, this.height, textColor, true);
    }

    public static void drawCenteredString(String displayString, int xPos, int yPos, int width, int height, int color, boolean shadow) {
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        float x = xPos + (width / 2f) - (fr.getStringWidth(displayString) / 2f);
        float y = (yPos + (height / 2f) - (fr.FONT_HEIGHT / 2f)) + 1;
        fr.drawString(displayString, x, y, color, shadow);
    }

    public boolean checkHover(int mouseX, int mouseY) {
        return mouseX >= this.xPos && mouseX <= this.xPos + this.width
                && mouseY >= this.yPos && mouseY <= this.yPos + this.height;
    }

}
