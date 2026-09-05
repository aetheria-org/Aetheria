package io.hamlook.aetheria.features.capes.ui;

import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.features.capes.Cape;
import io.hamlook.aetheria.features.capes.CapeManager;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.render.NineSliceUtils;
import io.hamlook.aetheria.utils.render.ResolutionUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.util.ResourceLocation;

public class CapeDisplay {

    private static final float FIXED_ANGLE = 310f;
    private static final ResourceLocation SLOT_BG = Resources.CAPES_UI;
    private final ModelRenderer capeModel;
    public String capeID;
    public int width, height;
    public int xPos = -1, yPos = -1;

    public CapeDisplay(Cape cape) {
        this.capeID = cape.id;
        this.width = (int) ResolutionUtils.getXStatic(150);
        this.height = (int) ResolutionUtils.getYStatic(250);

        this.capeModel = new ModelRenderer(new ModelBase() {
        }, 0, 0);
        this.capeModel.setTextureSize(64, 32);
        this.capeModel.addBox(-5.0F, 0.0F, -1.0F, 10, 16, 1);
    }

    public void draw(int xPos, int yPos, boolean hovering, boolean selected, Minecraft mc) {
        this.xPos = xPos;
        this.yPos = yPos;

        GlStateManagerCompat.pushMatrix();
        GlStateManagerCompat.disableLighting();
        GlStateManagerCompat.disableDepth();

        NineSliceUtils.draw(SLOT_BG, xPos, yPos, width, height, 6, 18);

        if (hovering || selected) {
            GlStateManagerCompat.enableBlend();
            GlStateManagerCompat.color(1f, 1f, 1f, selected ? 0.25f : 0.10f);
            NineSliceUtils.draw(SLOT_BG, xPos, yPos, width, height, 6, 18);
            GlStateManagerCompat.color(1f, 1f, 1f, 1f);
        }

        GlStateManagerCompat.enableDepth();
        GlStateManagerCompat.enableLighting();
        GlStateManagerCompat.popMatrix();

        int modelX = xPos + width / 2;
        // Move the render origin exactly to the vertical center of the card
        int modelY = yPos + (height / 2);

        // Scale it up significantly (From 90 to 140)
        int modelScale = (int) ResolutionUtils.getYStatic(140);

        drawStaticCape(mc, modelX, modelY, modelScale);

        GlStateManagerCompat.pushMatrix();
        GlStateManagerCompat.disableDepth();
        GlStateManagerCompat.disableLighting();
        String label = CapeManager.getCape(capeID) != null ? CapeManager.getCape(capeID).name : capeID;
        int labelX = xPos + (width / 2) - (MinecraftCompat.getFontRenderer().getStringWidth(label) / 2);
        MinecraftCompat.getFontRenderer().drawStringWithShadow(label, labelX, yPos + height - 14, selected ? 0xFFD700 : 0xFFFFFF);
        GlStateManagerCompat.enableDepth();
        GlStateManagerCompat.enableLighting();
        GlStateManagerCompat.popMatrix();
    }

    private void drawStaticCape(Minecraft mc, int x, int y, int scale) {
        Cape cape = CapeManager.getCape(capeID);
        if (cape == null || cape.resourceLocation == null) return;

        GlStateManagerCompat.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManagerCompat.pushMatrix();

        GlStateManagerCompat.translate(x, y, 50f);

        GlStateManagerCompat.scale(scale, scale, scale);

        GlStateManagerCompat.rotate(0f, 0f, 1f, 0f);
        GlStateManagerCompat.rotate(CapeDisplay.FIXED_ANGLE, 0f, 1f, 0f);

        GlStateManagerCompat.translate(0f, -8f * 0.0625f, 0f);

        mc.getTextureManager().bindTexture(cape.resourceLocation);
        capeModel.render(0.0625F);

        GlStateManagerCompat.popMatrix();
    }

    public boolean isOver(int mouseX, int mouseY) {
        return xPos >= 0 && yPos >= 0 && mouseX >= xPos && mouseX <= xPos + width && mouseY >= yPos && mouseY <= yPos + height;
    }

    public boolean isOverClamped(int mouseX, int mouseY, int clipX1, int clipY1, int clipX2, int clipY2) {
        return isOver(mouseX, mouseY) && mouseX >= clipX1 && mouseX <= clipX2 && mouseY >= clipY1 && mouseY <= clipY2;
    }
}