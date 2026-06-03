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
import java.util.List;

public final class WorldRenderUtils {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private WorldRenderUtils() {
    }

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

        double vx = mc.getRenderManager().viewerPosX;
        double vy = mc.getRenderManager().viewerPosY;
        double vz = mc.getRenderManager().viewerPosZ;

        Vec3 eyes = mc.thePlayer.getPositionEyes(partialTicks);

        int ri = color.getRed(), gi = color.getGreen(), bi = color.getBlue(), ai = color.getAlpha();

        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableDepth();
        GL11.glDepthMask(false);
        GL11.glLineWidth(lineWidth);

        GL11.glPushMatrix();
        GL11.glTranslated(-vx, -vy, -vz);

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(eyes.xCoord, eyes.yCoord, eyes.zCoord).color(ri, gi, bi, ai).endVertex();
        wr.pos(target.xCoord, target.yCoord, target.zCoord).color(ri, gi, bi, ai).endVertex();
        tess.draw();

        GL11.glPopMatrix();

        GL11.glDepthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GL11.glLineWidth(1f);
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
        double vx = mc.getRenderManager().viewerPosX;
        double vy = mc.getRenderManager().viewerPosY;
        double vz = mc.getRenderManager().viewerPosZ;

        double x0 = aabb.minX - vx, y0 = aabb.minY - vy, z0 = aabb.minZ - vz;
        double x1 = aabb.maxX - vx, y1 = aabb.maxY - vy, z1 = aabb.maxZ - vz;

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
        // Bottom face
        wr.pos(x0, y0, z0).color(r, g, b, a).endVertex();
        wr.pos(x1, y0, z0).color(r, g, b, a).endVertex();
        wr.pos(x1, y0, z0).color(r, g, b, a).endVertex();
        wr.pos(x1, y0, z1).color(r, g, b, a).endVertex();
        wr.pos(x1, y0, z1).color(r, g, b, a).endVertex();
        wr.pos(x0, y0, z1).color(r, g, b, a).endVertex();
        wr.pos(x0, y0, z1).color(r, g, b, a).endVertex();
        wr.pos(x0, y0, z0).color(r, g, b, a).endVertex();
        // Top face
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
        tess.draw();

        GL11.glPopMatrix();
        GL11.glPopAttrib();
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }

    public static void drawFilledBlocks(List<AxisAlignedBB> blocks, Color color) {
        drawFilledBlocks(blocks, color, false);
    }

    public static void drawFilledBlocks(List<AxisAlignedBB> blocks, Color color, boolean solid) {
        if (blocks == null || blocks.isEmpty() || mc.getRenderManager() == null) return;

        double vx = mc.getRenderManager().viewerPosX;
        double vy = mc.getRenderManager().viewerPosY;
        double vz = mc.getRenderManager().viewerPosZ;

        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        int a = solid ? 255 : color.getAlpha();

        // small expansion to avoid z-fighting
        double eps = 0.002;

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        if (!solid) {
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }

        GlStateManager.enableDepth();
        GL11.glDepthMask(true);

        // fix z-fighting properly
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(-1.0f, -1.0f);

        GlStateManager.disableCull();

        GL11.glPushMatrix();
        GL11.glTranslated(-vx, -vy, -vz);

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        for (AxisAlignedBB aabb : blocks) {

            double x0 = aabb.minX - eps;
            double y0 = aabb.minY - eps;
            double z0 = aabb.minZ - eps;

            double x1 = aabb.maxX + eps;
            double y1 = aabb.maxY + eps;
            double z1 = aabb.maxZ + eps;

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

        tess.draw();

        GL11.glPopMatrix();

        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);

        GlStateManager.enableCull();
        if (!solid) {
            GlStateManager.disableBlend();
        }
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();

        GL11.glPopAttrib();
    }

    public static void drawFilledBlock(AxisAlignedBB aabb, Color color) {
        drawFilledBlocks(java.util.Collections.singletonList(aabb), color, false);
    }

    public static void drawFilledBlock(AxisAlignedBB aabb, Color color, boolean solid) {
        drawFilledBlocks(java.util.Collections.singletonList(aabb), color, solid);
    }

    public static void drawFilledBlock(BlockPos pos, Color color) {
        AxisAlignedBB aabb = new AxisAlignedBB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
        drawFilledBlocks(java.util.Collections.singletonList(aabb), color);
    }

    public static void beginWorldRender(float lineWidth) {
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableDepth();
        GL11.glDepthMask(false);
        GL11.glLineWidth(lineWidth);
        GlStateManager.disableCull();
    }

    public static void endWorldRender() {
        GL11.glDepthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.enableCull();
        GL11.glLineWidth(1f);
        GlStateManager.color(1f, 1f, 1f, 1f);
    }
}