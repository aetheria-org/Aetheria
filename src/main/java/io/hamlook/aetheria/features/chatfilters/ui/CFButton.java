package io.hamlook.aetheria.features.chatfilters.ui;

import io.hamlook.aetheria.core.config.gui.GuiTextures;
import io.hamlook.aetheria.utils.render.NineSliceUtils;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import io.hamlook.aetheria.utils.render.ResolutionUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;

public class CFButton extends GuiButton {

    public float defaultR, defaultG, defaultB;
    public boolean toggled;
    public boolean isToggleButton;

    public CFButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText, float r, float g, float b) {
        super(buttonId, x, y, widthIn, heightIn, buttonText);
        this.defaultR = r;
        this.defaultG = g;
        this.defaultB = b;
        this.isToggleButton = false;
    }

    public CFButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText, boolean isToggleButton, boolean defaultState) {
        super(buttonId, x, y, widthIn, heightIn, buttonText);
        this.isToggleButton = isToggleButton;
        this.toggled = defaultState;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (this.visible) {
            FontRenderer fontrenderer = mc.fontRendererObj;
            this.hovered = mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;
            
            float r, g, b;
            
            if (isToggleButton) {
                if (toggled) {
                    this.displayString = "§a" + this.displayString;
                    r = 0.2f; g = 0.8f;
                } else {
                    r = 0.5f; g = 0.2f;
                }
                b = 0.2f;
            } else {
                r = defaultR; g = defaultG; b = defaultB;
            }

            if (!this.enabled) {
                r *= 0.5f; g *= 0.5f; b *= 0.5f;
            } else if (this.hovered) {
                r = Math.min(1f, r * 1.3f);
                g = Math.min(1f, g * 1.3f);
                b = Math.min(1f, b * 1.3f);
            }

            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.color(r, g, b, 1.0F);
            NineSliceUtils.draw(GuiTextures.storageBackground(1), xPosition, yPosition, width, height, 6, 18);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            
            this.mouseDragged(mc, mouseX, mouseY);
            
            float scale = ResolutionUtils.getXStatic(1);
            float centerX = this.xPosition + (this.width / 2.0f);
            float centerY = this.yPosition + (this.height / 2.0f);
            
            if (!this.enabled) {
                GlStateManager.color(0.6f, 0.6f, 0.6f, 1f);
            }
            TextRenderUtils.drawCenteredStringScaleAware(this.displayString, centerX, centerY, scale * 1.2f, false);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
