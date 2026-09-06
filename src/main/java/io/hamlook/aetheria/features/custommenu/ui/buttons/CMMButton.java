package io.hamlook.aetheria.features.custommenu.ui.buttons;

import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.features.custommenu.Position;
import io.hamlook.aetheria.features.custommenu.ui.CMMElement;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.render.NineSliceUtils;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import io.hamlook.aetheria.features.custommenu.util.ScreenHelper;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;

public abstract class CMMButton extends CMMElement {

    public String displayString;
    public ButtonStyle style = ButtonStyle.DEFAULT;

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

        GlStateManagerCompat.color(1f, 1f, 1f, 1f);
        GlStateManagerCompat.pushMatrix();
        NineSliceUtils.draw(Resources.betterContainerNineSlice(style.index), xPos, yPos, width, height, 6, 18, hovered);
        GlStateManagerCompat.popMatrix();

        drawCenteredString(displayString, this.xPos, this.yPos, this.width, this.height, textColor, true);
    }

    public static void drawCenteredString(String displayString, int xPos, int yPos, int width, int height, int color, boolean shadow) {
        FontRenderer fr = MinecraftCompat.getMinecraft().fontRendererObj;
        if (displayString == null) displayString = "";
        float availableWidth = Math.max(1f, width - Math.max(4, ScreenHelper.getStaticWidth(10)));
        float widthScale = availableWidth / Math.max(1f, fr.getStringWidth(displayString));
        float heightScale = Math.max(0.25f, (height * 0.72f) / Math.max(1f, fr.FONT_HEIGHT));
        float scale = Math.max(0.25f, Math.min(2.0f, Math.min(widthScale, heightScale)));
        TextRenderUtils.drawCenteredStringScaleAware(displayString, xPos + width / 2f, yPos + height / 2f, color, scale, false);
    }

    public boolean checkHover(int mouseX, int mouseY) {
        return mouseX >= this.xPos && mouseX <= this.xPos + this.width
                && mouseY >= this.yPos && mouseY <= this.yPos + this.height;
    }

}
