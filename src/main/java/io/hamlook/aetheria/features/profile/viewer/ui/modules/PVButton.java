package io.hamlook.aetheria.features.profile.viewer.ui.modules;

import io.hamlook.aetheria.features.profile.viewer.ui.ProfileViewerGUI;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.render.NineSliceUtils;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;

public class PVButton extends GuiButton {


    public PVButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText) {
        super(buttonId, x, y, widthIn, heightIn, buttonText);
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (this.visible) {
            FontRenderer fontrenderer = mc.fontRendererObj;
            GlStateManagerCompat.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.hovered = mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;
            GlStateManagerCompat.enableBlend();
            GlStateManagerCompat.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManagerCompat.blendFunc(770, 771);
            NineSliceUtils.draw(ProfileViewerGUI.CONTAINER_BG,xPosition,yPosition,width,height,6,18);
            this.mouseDragged(mc, mouseX, mouseY);
            float centerX = this.xPosition + (this.width / 2.0f);
            float centerY = this.yPosition + (this.height / 2.0f);

            TextRenderUtils.drawCenteredStringScaleAware(this.displayString, centerX, centerY, (ProfileViewerGUI.uiScale * 2f), false);
        }
    }
}