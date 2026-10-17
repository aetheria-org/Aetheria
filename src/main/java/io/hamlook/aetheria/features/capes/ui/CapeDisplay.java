package io.hamlook.aetheria.features.capes.ui;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.features.capes.Cape;
import io.hamlook.aetheria.features.capes.CapeManager;
import io.hamlook.aetheria.utils.compat.DrawContext;
import io.hamlook.aetheria.utils.compat.DrawContextUtils;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.render.NineSliceUtils;
import io.hamlook.aetheria.utils.render.ResolutionUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.util.ResourceLocation;
//#if MC > 1.21
//$$ import net.minecraft.client.model.ModelData;
//$$ import net.minecraft.client.model.ModelPartBuilder;
//$$ import net.minecraft.client.model.ModelTransform;
//$$ import net.minecraft.client.model.TexturedModelData;
//$$ import net.minecraft.client.render.LightmapTextureManager;
//$$ import net.minecraft.client.render.OverlayTexture;
//$$ import net.minecraft.client.render.RenderLayer;
//#endif

public class CapeDisplay {

    private static final float FIXED_ANGLE = 310f;

    /**
     * GUI depth for the cape model. 1.8.9 flushed capes immediately with GUI
     * depth testing effectively inactive, so the old value of 50 was inert.
     * 1.21+ buffers the geometry and flushes it with the entity layer's depth
     * test active, where 50 lands behind the card chrome emitted at z 0.
     */
    //#if MC < 1.21
    private static final float GUI_DEPTH = 50f;
    //#else
    //$$ private static final float GUI_DEPTH = 1f;
    //#endif

    private static final ResourceLocation SLOT_BG = Resources.CAPES_UI;
    private static long lastNullTextureWarnMs = 0L;
    private static int nullTextureSkipped = 0;
    private final ModelRenderer capeModel;
    public String capeID;
    public int width, height;
    public int xPos = -1, yPos = -1;

    public CapeDisplay(Cape cape) {
        this.capeID = cape.id;
        this.width = (int) ResolutionUtils.getXStatic(150);
        this.height = (int) ResolutionUtils.getYStatic(250);

        //#if MC < 1.21
        this.capeModel = new ModelRenderer(new ModelBase() {
        }, 0, 0);
        this.capeModel.setTextureSize(64, 32);
        this.capeModel.addBox(-5.0F, 0.0F, -1.0F, 10, 16, 1);
        //#else
        //$$ ModelData capeData = new ModelData();
        //$$ capeData.getRoot().addChild("cape", ModelPartBuilder.create().uv(0, 0).cuboid(-5.0F, 0.0F, -1.0F, 10.0F, 16.0F, 1.0F), ModelTransform.NONE);
        //$$ this.capeModel = TexturedModelData.of(capeData, 64, 32).createModel();
        //#endif
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
        DrawContextUtils.drawTextWithShadow(MinecraftCompat.getFontRenderer(), label, labelX, yPos + height - 14, selected ? 0xFFD700 : 0xFFFFFF);
        GlStateManagerCompat.enableDepth();
        GlStateManagerCompat.enableLighting();
        GlStateManagerCompat.popMatrix();
    }

    private void drawStaticCape(Minecraft mc, int x, int y, int scale) {
        Cape cape = CapeManager.getCape(capeID);
        if (cape == null || cape.resourceLocation == null) {
            nullTextureSkipped++;
            long now = System.currentTimeMillis();
            if (now - lastNullTextureWarnMs > 30000L) {
                lastNullTextureWarnMs = now;
                Aetheria.logger.warning(
                        "CapeDisplay: skipped " + nullTextureSkipped + " card(s) with no uploaded texture (fetch pending or failed).");
                nullTextureSkipped = 0;
            }
            int pw = width / 3;
            int ph = height / 2;
            DrawContextUtils.fill(x - pw / 2, y - ph / 2, x + pw / 2, y + ph / 2, 0xFF2A2A2A);
            DrawContextUtils.drawTextWithShadow(MinecraftCompat.getFontRenderer(), "?",
                    x - MinecraftCompat.getFontRenderer().getStringWidth("?") / 2, y - 4, 0xFF888888);
            return;
        }

        GlStateManagerCompat.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManagerCompat.pushMatrix();

        // Both renderers apply their own 1/16 internally (legacy
        // ModelRenderer.render(0.0625F), modern ModelPart's cuboid and pivot
        // division), so the matrix scale is the raw model scale on either
        // version. Scaling by 0.0625 here as well shrinks the cape 16x.
        GlStateManagerCompat.translate(x, y, GUI_DEPTH);
        GlStateManagerCompat.scale(scale, scale, scale);

        //#if MC < 1.21
        GlStateManagerCompat.rotate(CapeDisplay.FIXED_ANGLE, 0f, 1f, 0f);
        //#else
        //$$ GlStateManagerCompat.rotate(180f, 0f, 1f, 0f);
        //#endif

        // The cape therefore hangs the same eight model units below the pivot on
        // both versions, so the lift back to centre applies to both.
        GlStateManagerCompat.translate(0f, -8f * 0.0625f, 0f);

        //#if MC < 1.21
        mc.getTextureManager().bindTexture(cape.resourceLocation);
        capeModel.render(0.0625F);
        //#else
        //$$ DrawContext ctx;
        //$$ try {
        //$$     ctx = DrawContextUtils.getDrawContext();
        //$$ } catch (IllegalStateException unbound) {
        //$$     GlStateManagerCompat.popMatrix();
        //$$     return;
        //$$ }
        //$$ capeModel.render(ctx.getMatrices(),
        //$$         ctx.getVertexConsumers().getBuffer(RenderLayer.getEntityCutoutNoCull(cape.resourceLocation)),
        //$$         LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV);
        //$$ ctx.draw();
        //#endif

        GlStateManagerCompat.popMatrix();
    }

    public boolean isOver(int mouseX, int mouseY) {
        return xPos >= 0 && yPos >= 0 && mouseX >= xPos && mouseX <= xPos + width && mouseY >= yPos && mouseY <= yPos + height;
    }

    public boolean isOverClamped(int mouseX, int mouseY, int clipX1, int clipY1, int clipX2, int clipY2) {
        return isOver(mouseX, mouseY) && mouseX >= clipX1 && mouseX <= clipX2 && mouseY >= clipY1 && mouseY <= clipY2;
    }
}