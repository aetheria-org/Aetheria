package io.hamlook.aetheria.utils.render;

import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.TessellatorCompat;
import io.hamlook.aetheria.utils.compat.VertexBuilder;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import io.hamlook.aetheria.utils.compat.DrawContextUtils;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.TessellatorCompat;
import io.hamlook.aetheria.utils.compat.VertexBuilder;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class NineSliceUtils {

    public static void draw(ResourceLocation texture, int x, int y, int w, int h, int cornerSize, int texSize) {
        //#if MC < 1.21
        MinecraftCompat.getMinecraft().getTextureManager().bindTexture(texture);
        GlStateManagerCompat.enableTexture2D();
        GlStateManagerCompat.enableBlend();
        GlStateManagerCompat.color(1f, 1f, 1f, 1f);

        float c = cornerSize / (float) texSize;
        float m = 1f - c;

        int x2 = x + cornerSize, x3 = x + w - cornerSize;
        int y2 = y + cornerSize, y3 = y + h - cornerSize;

        VertexBuilder vb = TessellatorCompat.beginDraw(TessellatorCompat.QUADS, TessellatorCompat.POSITION_TEX);

        // top-left corner
        drawQuad(vb, x,  y,  x2,  y2,  0, 0, c, c);
        // top-right corner
        drawQuad(vb, x3, y,  x+w, y2,  m, 0, 1, c);
        // bottom-left corner
        drawQuad(vb, x,  y3, x2,  y+h, 0, m, c, 1);
        // bottom-right corner
        drawQuad(vb, x3, y3, x+w, y+h, m, m, 1, 1);
        // top edge
        drawQuad(vb, x2, y,  x3,  y2,  c, 0, m, c);
        // bottom edge
        drawQuad(vb, x2, y3, x3,  y+h, c, m, m, 1);
        // left edge
        drawQuad(vb, x,  y2, x2,  y3,  0, c, c, m);
        // right edge
        drawQuad(vb, x3, y2, x+w, y3,  m, c, 1, m);
        // center
        drawQuad(vb, x2, y2, x3,  y3,  c, c, m, m);

        vb.draw();
        GlStateManagerCompat.disableBlend();
        //#else
        //$$ drawNineRegions(texture, x, y, w, h, cornerSize, texSize);
        //#endif
    }

    private static void drawQuad(VertexBuilder vb,
                                 int x1, int y1, int x2, int y2,
                                 float u1, float v1, float u2, float v2) {
        vb.pos(x1, y2, 0).tex(u1, v2).endVertex();
        vb.pos(x2, y2, 0).tex(u2, v2).endVertex();
        vb.pos(x2, y1, 0).tex(u2, v1).endVertex();
        vb.pos(x1, y1, 0).tex(u1, v1).endVertex();
    }
    private static void drawQuad(VertexBuilder vb,
                                 int x1, int y1, int x2, int y2,
                                 float u1, float v1, float u2, float v2,
                                 float r, float g, float b, float a) {
        vb.pos(x1, y2, 0).tex(u1, v2).color(r, g, b, a).endVertex();
        vb.pos(x2, y2, 0).tex(u2, v2).color(r, g, b, a).endVertex();
        vb.pos(x2, y1, 0).tex(u2, v1).color(r, g, b, a).endVertex();
        vb.pos(x1, y1, 0).tex(u1, v1).color(r, g, b, a).endVertex();
    }

    public static void draw(ResourceLocation texture, int x, int y, int w, int h, int cornerSize, int texSize, boolean hovered) {
        //#if MC < 1.21
        MinecraftCompat.getMinecraft().getTextureManager().bindTexture(texture);
        GlStateManagerCompat.enableTexture2D();
        GlStateManagerCompat.enableBlend();
        GlStateManagerCompat.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);

        // Calculate hover color components
        float r = hovered ? 0.7f : 1.0f;
        float g = hovered ? 0.7f : 1.0f;
        float b = hovered ? 0.7f : 1.0f;
        float a = hovered ? 0.7f : 1.0f;

        GlStateManagerCompat.color(r, g, b, a);

        float c = cornerSize / (float) texSize;
        float m = 1f - c;

        int x2 = x + cornerSize, x3 = x + w - cornerSize;
        int y2 = y + cornerSize, y3 = y + h - cornerSize;

        VertexBuilder vb = TessellatorCompat.beginDraw(TessellatorCompat.QUADS, TessellatorCompat.POSITION_TEX_COLOR);

        // top-left corner
        drawQuad(vb, x,  y,  x2,  y2,  0, 0, c, c, r, g, b, a);
        // top-right corner
        drawQuad(vb, x3, y,  x+w, y2,  m, 0, 1, c, r, g, b, a);
        // bottom-left corner
        drawQuad(vb, x,  y3, x2,  y+h, 0, m, c, 1, r, g, b, a);
        // bottom-right corner
        drawQuad(vb, x3, y3, x+w, y+h, m, m, 1, 1, r, g, b, a);
        // top edge
        drawQuad(vb, x2, y,  x3,  y2,  c, 0, m, c, r, g, b, a);
        // bottom edge
        drawQuad(vb, x2, y3, x3,  y+h, c, m, m, 1, r, g, b, a);
        // left edge
        drawQuad(vb, x,  y2, x2,  y3,  0, c, c, m, r, g, b, a);
        // right edge
        drawQuad(vb, x3, y2, x+w, y3,  m, c, 1, m, r, g, b, a);
        // center
        drawQuad(vb, x2, y2, x3,  y3,  c, c, m, m, r, g, b, a);

        vb.draw();
        GlStateManagerCompat.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManagerCompat.disableBlend();
        //#else
        //$$ float tint = hovered ? 0.7f : 1.0f;
        //$$ GlStateManagerCompat.color(tint, tint, tint, tint);
        //$$ drawNineRegions(texture, x, y, w, h, cornerSize, texSize);
        //$$ GlStateManagerCompat.color(1.0f, 1.0f, 1.0f, 1.0f);
        //#endif
    }

    //#if MC < 1.21
    //#else
    //$$ private static void drawNineRegions(Identifier texture, int x, int y, int w, int h, int cornerSize, int texSize) {
    //$$     int c = cornerSize;
    //$$     int m = texSize - cornerSize;
    //$$     int x2 = x + cornerSize, x3 = x + w - cornerSize;
    //$$     int y2 = y + cornerSize, y3 = y + h - cornerSize;
    //$$     int t = texSize;
    //$$     drawRegion(texture, x, y, x2, y2, 0, 0, c, c, t);
    //$$     drawRegion(texture, x3, y, x + w, y2, m, 0, t, c, t);
    //$$     drawRegion(texture, x, y3, x2, y + h, 0, m, c, t, t);
    //$$     drawRegion(texture, x3, y3, x + w, y + h, m, m, t, t, t);
    //$$     drawRegion(texture, x2, y, x3, y2, c, 0, m, c, t);
    //$$     drawRegion(texture, x2, y3, x3, y + h, c, m, m, t, t);
    //$$     drawRegion(texture, x, y2, x2, y3, 0, c, c, m, t);
    //$$     drawRegion(texture, x3, y2, x + w, y3, m, c, t, m, t);
    //$$     drawRegion(texture, x2, y2, x3, y3, c, c, m, m, t);
    //$$ }
    //$$
    //$$ private static void drawRegion(Identifier texture, int x1, int y1, int x2, int y2, int u1, int v1, int u2, int v2, int texSize) {
    //$$     DrawContextUtils.drawTexturedRegion(texture, x1, y1, x2 - x1, y2 - y1, (float) u1, (float) v1, u2 - u1, v2 - v1, texSize, texSize);
    //$$ }
    //#endif
}