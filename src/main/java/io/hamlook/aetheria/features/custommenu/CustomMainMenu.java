package io.hamlook.aetheria.features.custommenu;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.features.chat.globalchat.image.GCImage;
import io.hamlook.aetheria.features.custommenu.ui.CMMElement;
import io.hamlook.aetheria.features.custommenu.ui.buttons.CMMButton;
import io.hamlook.aetheria.features.custommenu.ui.dropdown.CMMDropdown;
import io.hamlook.aetheria.features.custommenu.util.ScreenHelper;
import io.hamlook.aetheria.features.custommenu.animation.AnimationController;
import io.hamlook.aetheria.utils.SoundUtils;
import io.hamlook.aetheria.utils.compat.AetheriaBaseScreen;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

public class CustomMainMenu extends AetheriaBaseScreen {

    public CustomMMConfig configuration;
    private final AnimationController openController = new AnimationController();

    public CustomMainMenu(CustomMMConfig config){
        configuration = config;
        if (config != null) openController.start(config.openAnimation);
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
    public void onInitGui() {
        ScreenHelper.updateScreenDimensions(this.width, this.height);
        if (configuration != null) {
            for(CMMElement element : configuration.elements) {
                element.updatePosition();
                element.triggerAnimation(element.openAnimation);
            }
        }
    }

    @Override
    public void onDrawScreen(int mouseX, int mouseY, float partialTicks) {
        if(configuration == null) return;
        drawRect(0,0, this.width, this.height, 0xBB000000);
        drawBackground();

        int rendered = 0;
        for(CMMElement element : configuration.elements) {
            if (++rendered > 256 || !element.visible) continue;
            int[] bounds = element.getEditorBounds();
            boolean hovered = mouseX >= bounds[0] && mouseX <= bounds[2] && mouseY >= bounds[1] && mouseY <= bounds[3];
            if (hovered && !element.wasHovered && element.hoverAnimation != null) element.triggerAnimation(element.hoverAnimation);
            element.wasHovered = hovered;
            element.draw(mouseX,mouseY,partialTicks);
        }
        if (configuration.animateBackground) {
            int alpha = Math.max(0, Math.min(255, (int) ((1f - openController.value()) * 180f)));
            if (alpha > 0) drawRect(0, 0, this.width, this.height, (alpha << 24));
        }
    }

    @Override
    protected void onMouseClicked(int mouseX, int mouseY, int mButton) {
        if(mButton != 0) return;
        for(CMMDropdown dropdown : configuration.getDropdowns()){
            if(dropdown.onMouseClick(mouseX,mouseY)){
                return;
            }
        }

        for(CMMButton button : configuration.getButtons()){
            if(button.checkHover(mouseX,mouseY)){
                button.onClick(this);
                button.triggerAnimation(button.clickAnimation);
                SoundUtils.playSound("gui.button.press");
            }
        }
    }

    public void drawBackground() {
        GCImage image = configuration.getBackground();
        if(image == null || image.getTextureToRender(true) == null) {
            Aetheria.logger.info("[CMM] CustomMainMenu image is null");
            return;
        }
        if (image.width <= 0 || image.height <= 0) {
            drawDefaultBackground();
            return;
        }
        GlStateManagerCompat.pushMatrix();
        MinecraftCompat.getMinecraft().getTextureManager().bindTexture(image.getTextureToRender(true));
        GlStateManagerCompat.color(1.0F, 1.0F, 1.0F, 1.0F);
        GuiScreen.drawScaledCustomSizeModalRect(0,0,0,0,width,height,width,height,width,height);
        GlStateManagerCompat.popMatrix();
    }
}
