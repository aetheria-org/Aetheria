package io.hamlook.aetheria.utils.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.Collections;
import java.util.List;

public final class WorldRenderUtils {

    private static final Minecraft mc = Minecraft.getMinecraft();

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
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private static void addBoxLines(WorldRenderer wr, double x0, double y0, double z0, double x1, double y1, double z1, int r, int g, int b, int a) {
        // Bottom
        wr.pos(x0, y0, z0).color(r, g, b, a).endVertex();
        wr.pos(x1, y0, z0).color(r, g, b, a).endVertex();
        wr.pos(x1, y0, z0).color(r, g, b, a).endVertex();
        wr.pos(x1, y0, z1).color(r, g, b, a).endVertex();
        wr.pos(x1, y0, z1).color(r, g, b, a).endVertex();
        wr.pos(x0, y0, z1).color(r, g, b, a).endVertex();
        wr.pos(x0, y0, z1).color(r, g, b, a).endVertex();
        wr.pos(x0, y0, z0).color(r, g, b, a).endVertex();
        // Top
        wr.pos(x0, y1, z0).color(r, g, b, a).endVertex();
        wr.pos(x1, y1, z0).color(r, g, b, a).endVertex();
        wr.pos(x1, y1, z0).color(r, g, b, a).endVertex();
        wr.pos(x1, y1, z1).color(r, g, b, a).endVertex();
        wr.pos(x1, y1, z1).color(r, g, b, a).endVertex();
        wr.pos(x0, y1, z1).color(r, g, b, a).endVertex();
        wr.pos(x0, y1, z1).color(r, g, b, a).endVertex();
        wr.pos(x0, y1, z0).color(r, g, b, a).endVertex();
        // Verticals
        wr.pos(x0, y0, z0).color(r, g, b, a).endVertex();
        wr.pos(x0, y1, z0).color(r, g, b, a).endVertex();
        wr.pos(x1, y0, z0).color(r, g, b, a).endVertex();
        wr.pos(x1, y1, z0).color(r, g, b, a).endVertex();
        wr.pos(x1, y0, z1).color(r, g, b, a).endVertex();
        wr.pos(x1, y1, z1).color(r, g, b, a).endVertex();
        wr.pos(x0, y0, z1).color(r, g, b, a).endVertex();
        wr.pos(x0, y1, z1).color(r, g, b, a).endVertex();
    }

    private static void addBoxQuads(WorldRenderer wr, double x0, double y0, double z0, double x1, double y1, double z1, int r, int g, int b, int a) {
        // Bottom
        wr.pos(x0, y0, z0).color(r, g, b, a).endVertex();
        wr.pos(x1, y0, z0).color(r, g, b, a).endVertex();
        wr.pos(x1, y0, z1).color(r, g, b, a).endVertex();
        wr.pos(x0, y0, z1).color(r, g, b, a).endVertex();
        // Top
        wr.pos(x0, y1, z0).color(r, g, b, a).endVertex();
        wr.pos(x0, y1, z1).color(r, g, b, a).endVertex();
        wr.pos(x1, y1, z1).color(r, g, b, a).endVertex();
        wr.pos(x1, y1, z0).color(r, g, b, a).endVertex();
        // North
        wr.pos(x0, y0, z0).color(r, g, b, a).endVertex();
        wr.pos(x0, y1, z0).color(r, g, b, a).endVertex();
        wr.pos(x1, y1, z0).color(r, g, b, a).endVertex();
        wr.pos(x1, y0, z0).color(r, g, b, a).endVertex();
        // South
        wr.pos(x0, y0, z1).color(r, g, b, a).endVertex();
        wr.pos(x1, y0, z1).color(r, g, b, a).endVertex();
        wr.pos(x1, y1, z1).color(r, g, b, a).endVertex();
        wr.pos(x0, y1, z1).color(r, g, b, a).endVertex();
        // West
        wr.pos(x0, y0, z0).color(r, g, b, a).endVertex();
        wr.pos(x0, y0, z1).color(r, g, b, a).endVertex();
        wr.pos(x0, y1, z1).color(r, g, b, a).endVertex();
        wr.pos(x0, y1, z0).color(r, g, b, a).endVertex();
        // East
        wr.pos(x1, y0, z0).color(r, g, b, a).endVertex();
        wr.pos(x1, y1, z0).color(r, g, b, a).endVertex();
        wr.pos(x1, y1, z1).color(r, g, b, a).endVertex();
        wr.pos(x1, y0, z1).color(r, g, b, a).endVertex();
    }

    //  Public API

    public static void drawEspBox(double x, double y, double z, Color color) {
        drawEspBox(x, y, z, color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);
    }

    public static void drawEspBox(double x, double y, double z, float r, float g, float b, float a) {
        final double[][] edges = {{0, 0, 0, 1, 0, 0}, {0, 0, 1, 1, 0, 1}, {0, 0, 0, 0, 0, 1}, {1, 0, 0, 1, 0, 1}, {0, 1, 0, 1, 1, 0}, {0, 1, 1, 1, 1, 1}, {0, 1, 0, 0, 1, 1}, {1, 1, 0, 1, 1, 1}, {0, 0, 0, 0, 1, 0}, {1, 0, 0, 1, 1, 0}, {0, 0, 1, 0, 1, 1}, {1, 0, 1, 1, 1, 1}};
        int ri = (int) (r * 255), gi = (int) (g * 255), bi = (int) (b * 255), ai = (int) (a * 255);
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        for (double[] e : edges) {
            wr.pos(x + e[0], y + e[1], z + e[2]).color(ri, gi, bi, ai).endVertex();
            wr.pos(x + e[3], y + e[4], z + e[5]).color(ri, gi, bi, ai).endVertex();
        }
        tess.draw();
    }

    public static void drawTracer(Vec3 target, float partialTicks, Color color) {
        drawTracer(target, partialTicks, color, 2f);
    }

    public static void drawTracer(Vec3 target, float partialTicks, Color color, float lineWidth) {
        if (mc.thePlayer == null) return;
        double[] v = viewerPos();
        Vec3 eyes = mc.thePlayer.getPositionEyes(partialTicks);
        int r = color.getRed(), g = color.getGreen(), b = color.getBlue(), a = color.getAlpha();

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        beginWorldRender(lineWidth);
        GL11.glPushMatrix();
        GL11.glTranslated(-v[0], -v[1], -v[2]);

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(eyes.xCoord, eyes.yCoord, eyes.zCoord).color(r, g, b, a).endVertex();
        wr.pos(target.xCoord, target.yCoord, target.zCoord).color(r, g, b, a).endVertex();
        tess.draw();

        GL11.glPopMatrix();
        endWorldRender();
        GL11.glPopAttrib();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    public static void drawTextInWorld(String text, double x, double y, double z) {
        if (mc.fontRendererObj == null) return;
        int w = mc.fontRendererObj.getStringWidth(net.minecraft.util.StringUtils.stripControlCodes(text));
        float scale = 0.04f;
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        GL11.glRotatef(-mc.getRenderManager().playerViewY, 0f, 1f, 0f);
        GL11.glRotatef(mc.getRenderManager().playerViewX, 1f, 0f, 0f);
        GL11.glScalef(-scale, -scale, scale);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        mc.fontRendererObj.drawStringWithShadow(text, -w / 2f, 0f, 0xFFFFFF);
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

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        addBoxLines(wr, aabb.minX - v[0], aabb.minY - v[1], aabb.minZ - v[2], aabb.maxX - v[0], aabb.maxY - v[1], aabb.maxZ - v[2], r, g, b, a);
        tess.draw();

        GL11.glPopMatrix();
        GL11.glPopAttrib();
        GlStateManager.color(1f, 1f, 1f, 1f);
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

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (AxisAlignedBB aabb : blocks) {
            addBoxQuads(wr, aabb.minX - eps, aabb.minY - eps, aabb.minZ - eps, aabb.maxX + eps, aabb.maxY + eps, aabb.maxZ + eps, r, g, b, a);
        }
        tess.draw();

        GL11.glPopMatrix();
        GL11.glPopAttrib();
        GlStateManager.color(1f, 1f, 1f, 1f);
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
}