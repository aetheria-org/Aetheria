package io.hamlook.aetheria.utils.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class NineSliceUtils {

    public static void draw(ResourceLocation texture, int x, int y, int w, int h, int cornerSize, int texSize) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);

        float c = cornerSize / (float) texSize;
        float m = 1f - c;

        int x2 = x + cornerSize, x3 = x + w - cornerSize;
        int y2 = y + cornerSize, y3 = y + h - cornerSize;

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();

        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);

        // top-left corner
        drawQuad(wr, x,  y,  x2,  y2,  0, 0, c, c);
        // top-right corner
        drawQuad(wr, x3, y,  x+w, y2,  m, 0, 1, c);
        // bottom-left corner
        drawQuad(wr, x,  y3, x2,  y+h, 0, m, c, 1);
        // bottom-right corner
        drawQuad(wr, x3, y3, x+w, y+h, m, m, 1, 1);
        // top edge
        drawQuad(wr, x2, y,  x3,  y2,  c, 0, m, c);
        // bottom edge
        drawQuad(wr, x2, y3, x3,  y+h, c, m, m, 1);
        // left edge
        drawQuad(wr, x,  y2, x2,  y3,  0, c, c, m);
        // right edge
        drawQuad(wr, x3, y2, x+w, y3,  m, c, 1, m);
        // center
        drawQuad(wr, x2, y2, x3,  y3,  c, c, m, m);

        tess.draw();
        GlStateManager.disableBlend();
    }

    private static void drawQuad(WorldRenderer wr,
                                 int x1, int y1, int x2, int y2,
                                 float u1, float v1, float u2, float v2) {
        wr.pos(x1, y2, 0).tex(u1, v2).endVertex();
        wr.pos(x2, y2, 0).tex(u2, v2).endVertex();
        wr.pos(x2, y1, 0).tex(u2, v1).endVertex();
        wr.pos(x1, y1, 0).tex(u1, v1).endVertex();
    }
}