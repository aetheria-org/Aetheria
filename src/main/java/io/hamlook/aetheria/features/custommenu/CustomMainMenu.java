package io.hamlook.aetheria.features.custommenu;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.features.chat.globalchat.image.GCImage;
import io.hamlook.aetheria.features.custommenu.ui.CMMElement;
import io.hamlook.aetheria.features.custommenu.ui.buttons.CMMButton;
import io.hamlook.aetheria.features.custommenu.util.ScreenHelper;
import io.hamlook.aetheria.utils.SoundUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;

public class CustomMainMenu extends GuiScreen {

    public CustomMMConfig configuration;

    public CustomMainMenu(CustomMMConfig config){
        configuration = config;
        if(config == null) {
            Aetheria.logger.warning("CustomMainMenu config is null");
        }
    }

    @Override
    public void onResize(Minecraft mcIn, int w, int h) {
        super.onResize(mcIn, w, h);
        ScreenHelper.updateScreenDimensions(this.width, this.height);
        if (configuration != null) {
            for(CMMElement element : configuration.elements) {
                element.updatePosition();
            }
        }
    }

    @Override
    public void initGui() {
        ScreenHelper.updateScreenDimensions(this.width, this.height);
        if (configuration != null) {
            for(CMMElement element : configuration.elements) {
                element.updatePosition();
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if(configuration == null) return;
        drawRect(0,0, this.width, this.height, 0xBB000000);
        drawBackground();

        for(CMMElement element : configuration.elements) {
            element.draw(mouseX,mouseY,partialTicks);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mButton) {
        if(mButton != 0) return;
        for(CMMButton button : configuration.getButtons()){
            if(button.checkHover(mouseX,mouseY)){
                button.onClick(this);
                SoundUtils.playSound("gui.button.press");
            }
        }
    }

    public void drawBackground() {
        GCImage image = configuration.background;
        if(image == null || image.getTextureToRender(true) == null) {
            Aetheria.logger.info("[CMM] CustomMainMenu image is null");
            return;
        }
        if (image.width <= 0 || image.height <= 0) {
            drawDefaultBackground();
            return;
        }
        GlStateManager.pushMatrix();
        Minecraft.getMinecraft().getTextureManager().bindTexture(image.getTextureToRender(true));
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GuiScreen.drawScaledCustomSizeModalRect(0,0,0,0,width,height,width,height,width,height);
        GlStateManager.popMatrix();
    }
}
