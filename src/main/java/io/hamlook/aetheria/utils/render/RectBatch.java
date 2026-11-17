package io.hamlook.aetheria.utils.render;

import io.hamlook.aetheria.utils.compat.DrawContextUtils;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.TessellatorCompat;
import io.hamlook.aetheria.utils.compat.VertexBuilder;
import org.lwjgl.opengl.GL11;

// one draw for many rects, emit order keeps blending identical
public final class RectBatch {

    private static VertexBuilder builder;
    private static boolean blendWasEnabled;
    private static int quads;

    private RectBatch() {
    }

    public static void begin() {
        //#if MC < 1.16
        GlStateManagerCompat.disableTexture2D();
        //#else
        //$$ DrawContextUtils.flushIfPresent();
        //#endif
        builder = TessellatorCompat.beginDraw(TessellatorCompat.QUADS, TessellatorCompat.POSITION_COLOR);
        quads = 0;
        blendWasEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        GlStateManagerCompat.enableBlend();
        GlStateManagerCompat.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
    }

    public static void rect(int x1, int y1, int x2, int y2, int argb) {
        if (x2 <= x1 || y2 <= y1) return;

        int alpha = (argb >>> 24) & 0xFF;
        int red = (argb >> 16) & 0xFF;
        int green = (argb >> 8) & 0xFF;
        int blue = argb & 0xFF;

        builder.pos(x1, y1, 0).color(red, green, blue, alpha).endVertex();
        builder.pos(x1, y2, 0).color(red, green, blue, alpha).endVertex();
        builder.pos(x2, y2, 0).color(red, green, blue, alpha).endVertex();
        builder.pos(x2, y1, 0).color(red, green, blue, alpha).endVertex();
        quads++;
    }

    public static void end() {
        if (quads > 0) builder.draw();
        builder = null;
        //#if MC < 1.16
        GlStateManagerCompat.enableTexture2D();
        //#endif
        if (!blendWasEnabled) GlStateManagerCompat.disableBlend();
        GlStateManagerCompat.color(1f, 1f, 1f, 1f);
    }
}
