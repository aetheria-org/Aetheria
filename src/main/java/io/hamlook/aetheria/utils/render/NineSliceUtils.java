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
    private static void drawQuad(WorldRenderer wr,
                                 int x1, int y1, int x2, int y2,
                                 float u1, float v1, float u2, float v2,
                                 float r, float g, float b, float a) {
        wr.pos(x1, y2, 0).tex(u1, v2).color(r, g, b, a).endVertex();
        wr.pos(x2, y2, 0).tex(u2, v2).color(r, g, b, a).endVertex();
        wr.pos(x2, y1, 0).tex(u2, v1).color(r, g, b, a).endVertex();
        wr.pos(x1, y1, 0).tex(u1, v1).color(r, g, b, a).endVertex();
    }

    public static void draw(ResourceLocation texture, int x, int y, int w, int h, int cornerSize, int texSize, boolean hovered) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);

        // Calculate hover color components
        float r = hovered ? 0.7f : 1.0f;
        float g = hovered ? 0.7f : 1.0f;
        float b = hovered ? 0.7f : 1.0f;
        float a = hovered ? 0.7f : 1.0f;

        GlStateManager.color(r, g, b, a);

        float c = cornerSize / (float) texSize;
        float m = 1f - c;

        int x2 = x + cornerSize, x3 = x + w - cornerSize;
        int y2 = y + cornerSize, y3 = y + h - cornerSize;

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();

        // Use POSITION_TEX_COLOR so the RGBA tint explicitly affects the texture vertices
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

        // top-left corner
        drawQuad(wr, x,  y,  x2,  y2,  0, 0, c, c, r, g, b, a);
        // top-right corner
        drawQuad(wr, x3, y,  x+w, y2,  m, 0, 1, c, r, g, b, a);
        // bottom-left corner
        drawQuad(wr, x,  y3, x2,  y+h, 0, m, c, 1, r, g, b, a);
        // bottom-right corner
        drawQuad(wr, x3, y3, x+w, y+h, m, m, 1, 1, r, g, b, a);
        // top edge
        drawQuad(wr, x2, y,  x3,  y2,  c, 0, m, c, r, g, b, a);
        // bottom edge
        drawQuad(wr, x2, y3, x3,  y+h, c, m, m, 1, r, g, b, a);
        // left edge
        drawQuad(wr, x,  y2, x2,  y3,  0, c, c, m, r, g, b, a);
        // right edge
        drawQuad(wr, x3, y2, x+w, y3,  m, c, 1, m, r, g, b, a);
        // center
        drawQuad(wr, x2, y2, x3,  y3,  c, c, m, m, r, g, b, a);

        tess.draw();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.disableBlend();
    }
}