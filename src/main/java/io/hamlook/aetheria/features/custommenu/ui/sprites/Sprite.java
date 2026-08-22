package io.hamlook.aetheria.features.custommenu.ui.sprites;

import io.hamlook.aetheria.features.chat.globalchat.image.GCImage;
import io.hamlook.aetheria.features.custommenu.Position;
import io.hamlook.aetheria.features.custommenu.ui.CMMElement;
import io.hamlook.aetheria.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

public class Sprite extends CMMElement {

    public GCImage image;
    public ResourceLocation imageLocal;

    public Sprite(Position position, int width, int height, @Nullable GCImage image, @Nullable ResourceLocation imageLocal) {
        super(position,width,height);
        this.image = image;
        this.imageLocal = imageLocal;
    }

    public Sprite(int xPos, int yPos, int width, int height, @Nullable GCImage image, @Nullable ResourceLocation imageLocal) {
        super(new Position(),width,height,xPos,yPos);
        this.image = image;
        this.imageLocal = imageLocal;
    }

    public ResourceLocation getImage() {
        if (image == null && imageLocal == null) {
            return null;
        }
        if (image == null) return imageLocal;
        return image.getTextureToRender(true);
    }

    @Override
    public void draw(int mouseX, int mouseY,float partialTicks) {
        ResourceLocation resource = getImage();
        if (resource == null) return;
        
        GlStateManager.pushMatrix();
        Minecraft.getMinecraft().getTextureManager().bindTexture(resource);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        RenderUtils.drawTexturedRect(xPos, yPos, width, height);
        GlStateManager.popMatrix();
    }
}
