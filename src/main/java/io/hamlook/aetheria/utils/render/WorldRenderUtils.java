package io.hamlook.aetheria.utils.render;

import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import net.minecraft.client.Minecraft;
import io.hamlook.aetheria.utils.compat.TessellatorCompat;
import io.hamlook.aetheria.utils.compat.VertexBuilder;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.Collections;
import java.util.List;

public final class WorldRenderUtils {

    private static final Minecraft mc = MinecraftCompat.getMinecraft();

    private WorldRenderUtils() {
    }

    private static double[] viewerPos() {
        return new double[]{mc.getRenderManager().viewerPosX, mc.getRenderManager().viewerPosY, mc.getRenderManager().viewerPosZ};
    }

    public static void beginWorldRender(float lineWidth) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glLineWidth(lineWidth);
        GL11.glDisable(GL11.GL_CULL_FACE);
    }

    public static void endWorldRender() {
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glLineWidth(1f);
        GlStateManagerCompat.color(1f, 1f, 1f, 1f);
    }

    private static void addBoxLines(VertexBuilder vb, double x0, double y0, double z0, double x1, double y1, double z1, int r, int g, int b, int a) {
        // Bottom
        vb.pos(x0, y0, z0).color(r, g, b, a).endVertex();
        vb.pos(x1, y0, z0).color(r, g, b, a).endVertex();
        vb.pos(x1, y0, z0).color(r, g, b, a).endVertex();
        vb.pos(x1, y0, z1).color(r, g, b, a).endVertex();
        vb.pos(x1, y0, z1).color(r, g, b, a).endVertex();
        vb.pos(x0, y0, z1).color(r, g, b, a).endVertex();
        vb.pos(x0, y0, z1).color(r, g, b, a).endVertex();
        vb.pos(x0, y0, z0).color(r, g, b, a).endVertex();
        // Top
        vb.pos(x0, y1, z0).color(r, g, b, a).endVertex();
        vb.pos(x1, y1, z0).color(r, g, b, a).endVertex();
        vb.pos(x1, y1, z0).color(r, g, b, a).endVertex();
        vb.pos(x1, y1, z1).color(r, g, b, a).endVertex();
        vb.pos(x1, y1, z1).color(r, g, b, a).endVertex();
        vb.pos(x0, y1, z1).color(r, g, b, a).endVertex();
        vb.pos(x0, y1, z1).color(r, g, b, a).endVertex();
        vb.pos(x0, y1, z0).color(r, g, b, a).endVertex();
        // Verticals
        vb.pos(x0, y0, z0).color(r, g, b, a).endVertex();
        vb.pos(x0, y1, z0).color(r, g, b, a).endVertex();
        vb.pos(x1, y0, z0).color(r, g, b, a).endVertex();
        vb.pos(x1, y1, z0).color(r, g, b, a).endVertex();
        vb.pos(x1, y0, z1).color(r, g, b, a).endVertex();
        vb.pos(x1, y1, z1).color(r, g, b, a).endVertex();
        vb.pos(x0, y0, z1).color(r, g, b, a).endVertex();
        vb.pos(x0, y1, z1).color(r, g, b, a).endVertex();
    }

    private static void addBoxQuads(VertexBuilder vb, double x0, double y0, double z0, double x1, double y1, double z1, int r, int g, int b, int a) {
        // Bottom
        vb.pos(x0, y0, z0).color(r, g, b, a).endVertex();
        vb.pos(x1, y0, z0).color(r, g, b, a).endVertex();
        vb.pos(x1, y0, z1).color(r, g, b, a).endVertex();
        vb.pos(x0, y0, z1).color(r, g, b, a).endVertex();
        // Top
        vb.pos(x0, y1, z0).color(r, g, b, a).endVertex();
        vb.pos(x0, y1, z1).color(r, g, b, a).endVertex();
        vb.pos(x1, y1, z1).color(r, g, b, a).endVertex();
        vb.pos(x1, y1, z0).color(r, g, b, a).endVertex();
        // North
        vb.pos(x0, y0, z0).color(r, g, b, a).endVertex();
        vb.pos(x0, y1, z0).color(r, g, b, a).endVertex();
        vb.pos(x1, y1, z0).color(r, g, b, a).endVertex();
        vb.pos(x1, y0, z0).color(r, g, b, a).endVertex();
        // South
        vb.pos(x0, y0, z1).color(r, g, b, a).endVertex();
        vb.pos(x1, y0, z1).color(r, g, b, a).endVertex();
        vb.pos(x1, y1, z1).color(r, g, b, a).endVertex();
        vb.pos(x0, y1, z1).color(r, g, b, a).endVertex();
        // West
        vb.pos(x0, y0, z0).color(r, g, b, a).endVertex();
        vb.pos(x0, y0, z1).color(r, g, b, a).endVertex();
        vb.pos(x0, y1, z1).color(r, g, b, a).endVertex();
        vb.pos(x0, y1, z0).color(r, g, b, a).endVertex();
        // East
        vb.pos(x1, y0, z0).color(r, g, b, a).endVertex();
        vb.pos(x1, y1, z0).color(r, g, b, a).endVertex();
        vb.pos(x1, y1, z1).color(r, g, b, a).endVertex();
        vb.pos(x1, y0, z1).color(r, g, b, a).endVertex();
    }

    //  Public API

    public static void drawEspBox(double x, double y, double z, Color color) {
        drawEspBox(x, y, z, color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);
    }

    public static void drawEspBox(double x, double y, double z, float r, float g, float b, float a) {
        final double[][] edges = {{0, 0, 0, 1, 0, 0}, {0, 0, 1, 1, 0, 1}, {0, 0, 0, 0, 0, 1}, {1, 0, 0, 1, 0, 1}, {0, 1, 0, 1, 1, 0}, {0, 1, 1, 1, 1, 1}, {0, 1, 0, 0, 1, 1}, {1, 1, 0, 1, 1, 1}, {0, 0, 0, 0, 1, 0}, {1, 0, 0, 1, 1, 0}, {0, 0, 1, 0, 1, 1}, {1, 0, 1, 1, 1, 1}};
        int ri = (int) (r * 255), gi = (int) (g * 255), bi = (int) (b * 255), ai = (int) (a * 255);
        VertexBuilder vb = TessellatorCompat.beginDraw(TessellatorCompat.LINES, TessellatorCompat.POSITION_COLOR);
        for (double[] e : edges) {
            vb.pos(x + e[0], y + e[1], z + e[2]).color(ri, gi, bi, ai).endVertex();
            vb.pos(x + e[3], y + e[4], z + e[5]).color(ri, gi, bi, ai).endVertex();
        }
        vb.draw();
    }

    public static void drawTracer(Vec3 target, float partialTicks, Color color) {
        drawTracer(target, partialTicks, color, 2f);
    }

    public static void drawTracer(Vec3 target, float partialTicks, Color color, float lineWidth) {
        if (MinecraftCompat.getLocalPlayer() == null) return;
        double[] v = viewerPos();
        Vec3 eyes = MinecraftCompat.getLocalPlayer().getPositionEyes(partialTicks);
        int r = color.getRed(), g = color.getGreen(), b = color.getBlue(), a = color.getAlpha();

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        beginWorldRender(lineWidth);
        GL11.glPushMatrix();
        GL11.glTranslated(-v[0], -v[1], -v[2]);

        VertexBuilder vb = TessellatorCompat.beginDraw(TessellatorCompat.LINES, TessellatorCompat.POSITION_COLOR);
        vb.pos(eyes.xCoord, eyes.yCoord, eyes.zCoord).color(r, g, b, a).endVertex();
        vb.pos(target.xCoord, target.yCoord, target.zCoord).color(r, g, b, a).endVertex();
        vb.draw();

        GL11.glPopMatrix();
        endWorldRender();
        GL11.glPopAttrib();
        GlStateManagerCompat.color(1f, 1f, 1f, 1f);
    }

    public static void drawTextInWorld(String text, double x, double y, double z) {
        if (MinecraftCompat.getFontRenderer() == null) return;
        int w = MinecraftCompat.getFontRenderer().getStringWidth(net.minecraft.util.StringUtils.stripControlCodes(text));
        float scale = 0.04f;
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        GL11.glRotatef(-mc.getRenderManager().playerViewY, 0f, 1f, 0f);
        GL11.glRotatef(mc.getRenderManager().playerViewX, 1f, 0f, 0f);
        GL11.glScalef(-scale, -scale, scale);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        MinecraftCompat.getFontRenderer().drawStringWithShadow(text, -w / 2f, 0f, 0xFFFFFF);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glPopMatrix();
    }

    public static void drawSelectionBox(AxisAlignedBB aabb, Color color, float lineWidth) {
        double[] v = viewerPos();
        int r = color.getRed(), g = color.getGreen(), b = color.getBlue(), a = color.getAlpha();

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glDepthMask(false);
        GL11.glLineWidth(lineWidth);
        GL11.glPushMatrix();

        VertexBuilder vb = TessellatorCompat.beginDraw(TessellatorCompat.LINES, TessellatorCompat.POSITION_COLOR);
        addBoxLines(vb, aabb.minX - v[0], aabb.minY - v[1], aabb.minZ - v[2], aabb.maxX - v[0], aabb.maxY - v[1], aabb.maxZ - v[2], r, g, b, a);
        vb.draw();

        GL11.glPopMatrix();
        GL11.glPopAttrib();
        GlStateManagerCompat.color(1f, 1f, 1f, 1f);
    }

    public static void drawFilledBlocks(List<AxisAlignedBB> blocks, Color color) {
        drawFilledBlocks(blocks, color, false);
    }

    public static void drawFilledBlocks(List<AxisAlignedBB> blocks, Color color, boolean solid) {
        if (blocks == null || blocks.isEmpty() || mc.getRenderManager() == null) return;
        double[] v = viewerPos();
        int r = color.getRed(), g = color.getGreen(), b = color.getBlue(), a = solid ? 255 : color.getAlpha();
        double eps = 0.002;

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        if (!solid) {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(-1f, -1f);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glPushMatrix();
        GL11.glTranslated(-v[0], -v[1], -v[2]);

        VertexBuilder vb = TessellatorCompat.beginDraw(TessellatorCompat.QUADS, TessellatorCompat.POSITION_COLOR);
        for (AxisAlignedBB aabb : blocks) {
            addBoxQuads(vb, aabb.minX - eps, aabb.minY - eps, aabb.minZ - eps, aabb.maxX + eps, aabb.maxY + eps, aabb.maxZ + eps, r, g, b, a);
        }
        vb.draw();

        GL11.glPopMatrix();
        GL11.glPopAttrib();
        GlStateManagerCompat.color(1f, 1f, 1f, 1f);
    }

    public static void drawFilledBlock(AxisAlignedBB aabb, Color color) {
        drawFilledBlocks(Collections.singletonList(aabb), color, false);
    }

    public static void drawFilledBlock(AxisAlignedBB aabb, Color color, boolean solid) {
        drawFilledBlocks(Collections.singletonList(aabb), color, solid);
    }

    public static void drawFilledBlock(BlockPos pos, Color color) {
        drawFilledBlocks(Collections.singletonList(new AxisAlignedBB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1)), color);
    }

    /**
     * Draws a vanilla-style beacon beam spanning the block column {@code x..x+1},
     * {@code z..z+1} starting at {@code y}. Depth testing stays enabled, so the
     * beam is occluded by terrain and never renders through blocks.
     */
    public static void drawBeaconBeam(double x, double y, double z, Color color, float partialTicks, double height) {
        if (MinecraftCompat.getLocalWorld() == null || mc.getRenderManager() == null) return;
        double[] v = viewerPos();

        GlStateManagerCompat.pushMatrix();
        GlStateManagerCompat.translate(-v[0], -v[1], -v[2]);

        mc.getTextureManager().bindTexture(io.hamlook.aetheria.Resources.BEACON_BEAM);
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

        GlStateManagerCompat.disableLighting();
        GlStateManagerCompat.disableCull();
        GlStateManagerCompat.enableTexture2D();
        GlStateManagerCompat.enableBlend();
        GlStateManagerCompat.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManagerCompat.enableDepth();
        GlStateManagerCompat.depthFunc(GL11.GL_LEQUAL);
        GlStateManagerCompat.depthMask(false);

        double time = MinecraftCompat.getLocalWorld().getTotalWorldTime() + partialTicks;
        double texShift = -time * 0.2 - Math.floor(-time * 0.1);
        texShift -= Math.floor(texShift);

        float r = color.getRed() / 255f, g = color.getGreen() / 255f, b = color.getBlue() / 255f;

        VertexBuilder vb;

        // Inner rotating core
        double angle = time * 0.025 * -1.5;
        double d4 = 0.5 + Math.cos(angle + Math.PI * 3 / 4) * 0.2;
        double d5 = 0.5 + Math.sin(angle + Math.PI * 3 / 4) * 0.2;
        double d6 = 0.5 + Math.cos(angle + Math.PI / 4) * 0.2;
        double d7 = 0.5 + Math.sin(angle + Math.PI / 4) * 0.2;
        double d8 = 0.5 + Math.cos(angle + Math.PI * 5 / 4) * 0.2;
        double d9 = 0.5 + Math.sin(angle + Math.PI * 5 / 4) * 0.2;
        double d10 = 0.5 + Math.cos(angle + Math.PI * 7 / 4) * 0.2;
        double d11 = 0.5 + Math.sin(angle + Math.PI * 7 / 4) * 0.2;
        double v0 = -1.0 + texShift;
        double v1 = height * 2.5 + v0;

        vb = TessellatorCompat.beginDraw(TessellatorCompat.QUADS, TessellatorCompat.POSITION_TEX_COLOR);
        vb.pos(x + d4, y + height, z + d5).tex(1.0, v1).color(r, g, b, 1f).endVertex();
        vb.pos(x + d4, y, z + d5).tex(1.0, v0).color(r, g, b, 1f).endVertex();
        vb.pos(x + d6, y, z + d7).tex(0.0, v0).color(r, g, b, 1f).endVertex();
        vb.pos(x + d6, y + height, z + d7).tex(0.0, v1).color(r, g, b, 1f).endVertex();
        vb.pos(x + d10, y + height, z + d11).tex(1.0, v1).color(r, g, b, 1f).endVertex();
        vb.pos(x + d10, y, z + d11).tex(1.0, v0).color(r, g, b, 1f).endVertex();
        vb.pos(x + d8, y, z + d9).tex(0.0, v0).color(r, g, b, 1f).endVertex();
        vb.pos(x + d8, y + height, z + d9).tex(0.0, v1).color(r, g, b, 1f).endVertex();
        vb.pos(x + d6, y + height, z + d7).tex(1.0, v1).color(r, g, b, 1f).endVertex();
        vb.pos(x + d6, y, z + d7).tex(1.0, v0).color(r, g, b, 1f).endVertex();
        vb.pos(x + d10, y, z + d11).tex(0.0, v0).color(r, g, b, 1f).endVertex();
        vb.pos(x + d10, y + height, z + d11).tex(0.0, v1).color(r, g, b, 1f).endVertex();
        vb.pos(x + d8, y + height, z + d9).tex(1.0, v1).color(r, g, b, 1f).endVertex();
        vb.pos(x + d8, y, z + d9).tex(1.0, v0).color(r, g, b, 1f).endVertex();
        vb.pos(x + d4, y, z + d5).tex(0.0, v0).color(r, g, b, 1f).endVertex();
        vb.pos(x + d4, y + height, z + d5).tex(0.0, v1).color(r, g, b, 1f).endVertex();
        vb.draw();

        // Outer translucent shell
        double s0 = -1.0 + texShift;
        double s1 = height + s0;
        float sa = 0.25f;

        vb = TessellatorCompat.beginDraw(TessellatorCompat.QUADS, TessellatorCompat.POSITION_TEX_COLOR);
        vb.pos(x + 0.2, y + height, z + 0.2).tex(1.0, s1).color(r, g, b, sa).endVertex();
        vb.pos(x + 0.2, y, z + 0.2).tex(1.0, s0).color(r, g, b, sa).endVertex();
        vb.pos(x + 0.8, y, z + 0.2).tex(0.0, s0).color(r, g, b, sa).endVertex();
        vb.pos(x + 0.8, y + height, z + 0.2).tex(0.0, s1).color(r, g, b, sa).endVertex();
        vb.pos(x + 0.8, y + height, z + 0.8).tex(1.0, s1).color(r, g, b, sa).endVertex();
        vb.pos(x + 0.8, y, z + 0.8).tex(1.0, s0).color(r, g, b, sa).endVertex();
        vb.pos(x + 0.2, y, z + 0.8).tex(0.0, s0).color(r, g, b, sa).endVertex();
        vb.pos(x + 0.2, y + height, z + 0.8).tex(0.0, s1).color(r, g, b, sa).endVertex();
        vb.pos(x + 0.8, y + height, z + 0.2).tex(1.0, s1).color(r, g, b, sa).endVertex();
        vb.pos(x + 0.8, y, z + 0.2).tex(1.0, s0).color(r, g, b, sa).endVertex();
        vb.pos(x + 0.8, y, z + 0.8).tex(0.0, s0).color(r, g, b, sa).endVertex();
        vb.pos(x + 0.8, y + height, z + 0.8).tex(0.0, s1).color(r, g, b, sa).endVertex();
        vb.pos(x + 0.2, y + height, z + 0.8).tex(1.0, s1).color(r, g, b, sa).endVertex();
        vb.pos(x + 0.2, y, z + 0.8).tex(1.0, s0).color(r, g, b, sa).endVertex();
        vb.pos(x + 0.2, y, z + 0.2).tex(0.0, s0).color(r, g, b, sa).endVertex();
        vb.pos(x + 0.2, y + height, z + 0.2).tex(0.0, s1).color(r, g, b, sa).endVertex();
        vb.draw();

        GlStateManagerCompat.depthMask(true);
        GlStateManagerCompat.disableBlend();
        GlStateManagerCompat.enableCull();
        GlStateManagerCompat.enableLighting();
        GlStateManagerCompat.popMatrix();
        GlStateManagerCompat.color(1f, 1f, 1f, 1f);
    }
}