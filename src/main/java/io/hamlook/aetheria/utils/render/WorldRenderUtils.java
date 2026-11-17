package io.hamlook.aetheria.utils.render;

import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.utils.compat.DrawContextUtils;
import io.hamlook.aetheria.utils.compat.EntityCompat;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.WorldCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.TessellatorCompat;
import io.hamlook.aetheria.utils.compat.VertexBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
//#if MC < 1.21
import net.minecraft.client.renderer.GlStateManager;
//#else
//$$ import net.minecraft.client.font.TextRenderer;
//$$ import net.minecraft.client.render.LightmapTextureManager;
//$$ import net.minecraft.client.render.OverlayTexture;
//$$ import net.minecraft.client.render.RenderLayer;
//$$ import net.minecraft.client.render.VertexConsumer;
//$$ import net.minecraft.client.render.VertexConsumerProvider;
//$$ import net.minecraft.client.render.WorldRenderer;
//$$ import net.minecraft.client.util.math.MatrixStack;
//$$ import net.minecraft.util.math.RotationAxis;
//#endif
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.Collections;
import java.util.List;

public final class WorldRenderUtils {

    private WorldRenderUtils() {
    }

    public static double[] viewerPos() {
        //#if MC < 1.21
        Minecraft mc = MinecraftCompat.getMinecraft();
        return new double[]{mc.getRenderManager().viewerPosX, mc.getRenderManager().viewerPosY, mc.getRenderManager().viewerPosZ};
        //#else
        //$$ net.minecraft.util.math.Vec3d pos = MinecraftCompat.getMinecraft().gameRenderer.getCamera().getPos();
        //$$ return new double[]{pos.x, pos.y, pos.z};
        //#endif
    }

    //#if MC < 1.21
    //#else
    //$$ private static MatrixStack worldMatrices;
    //$$ private static VertexConsumerProvider.Immediate worldConsumers;
    //$$
    //$$ /**
    //$$  * Binds the current world-render matrices, mirroring DrawContextUtils
    //$$  * setContext/clearContext. Set by RenderBridge around the world event;
    //$$  * world-space text fails safe (skips) when unbound.
    //$$  */
    //$$ public static void setWorldContext(MatrixStack matrices, VertexConsumerProvider consumers) {
    //$$     worldMatrices = matrices;
    //$$     if (consumers instanceof VertexConsumerProvider.Immediate) {
    //$$         worldConsumers = (VertexConsumerProvider.Immediate) consumers;
    //$$     } else {
    //$$         worldConsumers = null;
    //$$     }
    //$$ }
    //$$
    //$$ public static void clearWorldContext() {
    //$$     worldMatrices = null;
    //$$     worldConsumers = null;
    //$$ }
    //$$
    //$$ private static void drawLayer(RenderLayer layer) {
    //$$     if (worldConsumers != null) worldConsumers.draw(layer);
    //$$ }
    //$$
    //$$ private static void drawXrayLayer(RenderLayer layer) {
    //$$     if (worldConsumers == null) return;
    //$$     GlStateManagerCompat.depthMask(false);
    //$$     GlStateManagerCompat.disableDepth();
    //$$     worldConsumers.draw(layer);
    //$$     GlStateManagerCompat.enableDepth();
    //$$     GlStateManagerCompat.depthMask(true);
    //$$ }
    //$$
    //$$ private static void drawXrayCurrentLayer() {
    //$$     if (worldConsumers == null) return;
    //$$     GlStateManagerCompat.depthMask(false);
    //$$     GlStateManagerCompat.disableDepth();
    //$$     worldConsumers.drawCurrentLayer();
    //$$     GlStateManagerCompat.enableDepth();
    //$$     GlStateManagerCompat.depthMask(true);
    //$$ }
    //#endif

    public static void beginWorldRender(float lineWidth) {
        beginWorldDraw(true, false);
        GL11.glLineWidth(lineWidth);
    }

    public static void endWorldRender() {
        endWorldDraw();
    }

    public static void beginWorldDraw(boolean xray, boolean writeDepth) {
        //#if MC < 1.21
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        //#endif
        GlStateManagerCompat.disableTexture2D();
        GlStateManagerCompat.disableLighting();
        GlStateManagerCompat.enableBlend();
        GlStateManagerCompat.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManagerCompat.disableCull();
        if (xray) {
            GlStateManagerCompat.disableDepth();
            GlStateManagerCompat.depthMask(false);
            return;
        }
        GlStateManagerCompat.enableDepth();
        GlStateManagerCompat.depthMask(writeDepth);
    }

    public static void beginWorldFill(boolean xray) {
        beginWorldDraw(xray, true);
        if (xray) return;
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(-1f, -1f);
    }

    public static void endWorldDraw() {
        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        GlStateManagerCompat.depthFunc(GL11.GL_LESS);
        GlStateManagerCompat.depthMask(true);
        GlStateManagerCompat.enableDepth();
        GlStateManagerCompat.disableBlend();
        GlStateManagerCompat.enableTexture2D();
        GlStateManagerCompat.enableLighting();
        GlStateManagerCompat.enableCull();
        GL11.glLineWidth(1f);
        GlStateManagerCompat.color(1f, 1f, 1f, 1f);
        //#if MC < 1.21
        GL11.glPopAttrib();
        //#endif
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
        drawEspBox(x, y, z, color, false, 2f);
    }

    public static void drawEspBox(double x, double y, double z, Color color, boolean xray) {
        drawEspBox(x, y, z, color, xray, 2f);
    }

    public static void drawEspBox(double x, double y, double z, Color color, boolean xray, float lineWidth) {
        drawEspBoxes(Collections.singletonList(new AxisAlignedBB(x, y, z, x + 1, y + 1, z + 1)), color, xray, lineWidth);
    }

    public static void drawTracer(Vec3 target, float partialTicks, Color color) {
        drawTracer(target, partialTicks, color, 2f);
    }

    public static void drawTracer(Vec3 target, float partialTicks, Color color, float lineWidth) {
        if (MinecraftCompat.getLocalPlayer() == null) return;
        //#if MC < 1.21
        //#else
        //$$ if (drawTracerModern(target, partialTicks, color)) return;
        //#endif
        double[] v = viewerPos();
        Vec3 eyes = EntityCompat.getEyePosition(MinecraftCompat.getLocalPlayer(), partialTicks);
        int r = color.getRed(), g = color.getGreen(), b = color.getBlue(), a = color.getAlpha();

        beginWorldRender(lineWidth);
        try {
            VertexBuilder vb = TessellatorCompat.beginDraw(TessellatorCompat.LINES, TessellatorCompat.POSITION_COLOR);
            vb.pos(eyes.xCoord - v[0], eyes.yCoord - v[1], eyes.zCoord - v[2]).color(r, g, b, a).endVertex();
            vb.pos(target.xCoord - v[0], target.yCoord - v[1], target.zCoord - v[2]).color(r, g, b, a).endVertex();
            vb.draw();
        } finally {
            endWorldRender();
        }
    }

    //#if MC >= 1.21
    // Tracer from the crosshair to the target. The start sits on the look
    // vector, never on the eyes-target ray (a camera-ray segment collapses).
    //$$ private static boolean drawTracerModern(Vec3d target, float partialTicks, Color color) {
    //$$     if (worldMatrices == null || worldConsumers == null) return false;
    //$$     double[] v = viewerPos();
    //$$     double yaw = Math.toRadians(MinecraftCompat.getViewerYaw());
    //$$     double pitch = Math.toRadians(MinecraftCompat.getViewerPitch());
    //$$     double lx = -Math.sin(yaw) * Math.cos(pitch);
    //$$     double ly = -Math.sin(pitch);
    //$$     double lz = Math.cos(yaw) * Math.cos(pitch);
    //$$
    //$$     double near = 0.5;
    //$$     double tx = target.x - v[0], ty = target.y - v[1], tz = target.z - v[2];
    //$$     double f = tx * lx + ty * ly + tz * lz;
    //$$     if (f < 1.0) {
    //$$         double ax = tx - lx * f, ay = ty - ly * f, az = tz - lz * f;
    //$$         double al = Math.sqrt(ax * ax + ay * ay + az * az);
    //$$         if (al < 1e-4) return true;
    //$$         double k = 100.0 / al;
    //$$         tx = lx + ax * k;
    //$$         ty = ly + ay * k;
    //$$         tz = lz + az * k;
    //$$     }
    //$$
    //$$     double sx = lx * near, sy = ly * near, sz = lz * near;
    //$$     double dx = tx - sx, dy = ty - sy, dz = tz - sz;
    //$$     double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
    //$$     if (len < 1e-6) return true;
    //$$     float nx = (float) (dx / len), ny = (float) (dy / len), nz = (float) (dz / len);
    //$$
    //$$     float r = color.getRed() / 255f, g = color.getGreen() / 255f;
    //$$     float b = color.getBlue() / 255f, a = color.getAlpha() / 255f;
    //$$     VertexConsumer vc = worldConsumers.getBuffer(AsmRenderLayers.ESP_LINES);
    //$$     MatrixStack.Entry entry = worldMatrices.peek();
    //$$     vc.vertex(entry, (float) sx, (float) sy, (float) sz).color(r, g, b, a).normal(entry, nx, ny, nz);
    //$$     vc.vertex(entry, (float) tx, (float) ty, (float) tz).color(r, g, b, a).normal(entry, nx, ny, nz);
    //$$     drawXrayLayer(AsmRenderLayers.ESP_LINES);
    //$$     return true;
    //$$ }
    //#endif

    /**
     * Opens a camera-facing billboard transform at world (x, y, z), uniformly
     * scaled. Translates viewer-relative on both versions, so callers must
     * NOT pre-translate by the viewer position. Pair every call with
     * {@link #endBillboard()}; both sides no-op when the world context is
     * unbound, so the pairing stays balanced. Depth handling stays with the
     * caller (see SEE_THROUGH below).
     */
    public static void beginBillboard(double x, double y, double z, float scale) {
        //#if MC < 1.21
        double[] v = viewerPos();
        GlStateManager.pushMatrix();
        GL11.glTranslated(x - v[0], y - v[1], z - v[2]);
        GL11.glRotatef(-MinecraftCompat.getViewerYaw(), 0f, 1f, 0f);
        GL11.glRotatef(MinecraftCompat.getViewerPitch(), 1f, 0f, 0f);
        GL11.glScalef(-scale, -scale, scale);
        //#else
        //$$ if (worldMatrices == null) return;
        //$$ double[] v = viewerPos();
        //$$ worldMatrices.push();
        //$$ worldMatrices.translate((float) (x - v[0]), (float) (y - v[1]), (float) (z - v[2]));
        //$$ worldMatrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-MinecraftCompat.getViewerYaw()));
        //$$ worldMatrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(MinecraftCompat.getViewerPitch()));
        //$$ worldMatrices.scale(-scale, -scale, scale);
        //#endif
    }

    public static void endBillboard() {
        //#if MC < 1.21
        GlStateManager.popMatrix();
        //#else
        //$$ if (worldMatrices != null) worldMatrices.pop();
        //#endif
    }

    public static void pushMatrix() {
        //#if MC < 1.21
        GL11.glPushMatrix();
        //#else
        //$$ if (worldMatrices != null) worldMatrices.push();
        //#endif
    }

    public static void popMatrix() {
        //#if MC < 1.21
        GL11.glPopMatrix();
        //#else
        //$$ if (worldMatrices != null) worldMatrices.pop();
        //#endif
    }

    public static void translate(double x, double y, double z) {
        //#if MC < 1.21
        GL11.glTranslated(x, y, z);
        //#else
        //$$ if (worldMatrices != null) worldMatrices.translate((float) x, (float) y, (float) z);
        //#endif
    }

    public static void scale(float x, float y, float z) {
        //#if MC < 1.21
        GL11.glScalef(x, y, z);
        //#else
        //$$ if (worldMatrices != null) worldMatrices.scale(x, y, z);
        //#endif
    }

/**
 * Draws text at the current billboard position. Legacy draws through GL;
 * modern draws through the bound world matrices and flushes the text layer
 * itself with depth disabled, because the see-through text layer only stops
 * depth writes and never disables the depth test.
 */
public static void drawBillboardText(String text, float x, float y, int color, boolean shadow) {
    //#if MC < 1.21
    MinecraftCompat.getFontRenderer().drawString(text, x, y, color, shadow);
    //#else
    //$$ if (worldMatrices == null || worldConsumers == null) return;
    //$$ TextRenderer fr = MinecraftCompat.getFontRenderer();
    //$$ fr.draw(text, x, y, color, shadow, worldMatrices.peek().getPositionMatrix(), worldConsumers, TextRenderer.TextLayerType.SEE_THROUGH, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV);
    //$$ drawXrayCurrentLayer();
    //#endif
}

    public static void drawTextInWorld(String text, double x, double y, double z) {
        if (MinecraftCompat.getFontRenderer() == null) return;
        int w = DrawContextUtils.getStringWidth(MinecraftCompat.getFontRenderer(), net.minecraft.util.StringUtils.stripControlCodes(text));
        float scale = 0.04f;
        //#if MC < 1.21
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        beginBillboard(x, y, z, scale);
        MinecraftCompat.getFontRenderer().drawStringWithShadow(text, -w / 2f, 0f, 0xFFFFFF);
        endBillboard();
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        //#else
        //$$ beginBillboard(x, y, z, scale);
        //$$ drawBillboardText(text, -w / 2f, 0f, 0xFFFFFF, true);
        //$$ endBillboard();
        //#endif
    }

    public static void drawSelectionBox(AxisAlignedBB aabb, Color color, float lineWidth) {
        //#if MC < 1.21
        //#else
        //$$ if (drawSelectionBoxModern(aabb, color)) return;
        //#endif
        double[] v = viewerPos();
        int r = color.getRed(), g = color.getGreen(), b = color.getBlue(), a = color.getAlpha();

        beginWorldDraw(false, false);
        GlStateManagerCompat.depthFunc(GL11.GL_LEQUAL);
        GL11.glLineWidth(lineWidth);
        //#if MC < 1.21
        GL11.glPushMatrix();
        //#endif

        try {
            VertexBuilder vb = TessellatorCompat.beginDraw(TessellatorCompat.LINES, TessellatorCompat.POSITION_COLOR);
            addBoxLines(vb, aabb.minX - v[0], aabb.minY - v[1], aabb.minZ - v[2], aabb.maxX - v[0], aabb.maxY - v[1], aabb.maxZ - v[2], r, g, b, a);
            vb.draw();
        } finally {
            //#if MC < 1.21
            GL11.glPopMatrix();
            //#endif
            endWorldDraw();
        }
    }

    public static void drawEspBoxes(List<AxisAlignedBB> boxes, Color color) {
        drawEspBoxes(boxes, color, true, 2.5f);
    }

    public static void drawEspBoxes(List<AxisAlignedBB> boxes, Color color, boolean xray, float lineWidth) {
        if (boxes.isEmpty()) return;
        //#if MC >= 1.21
        //$$ if (drawEspBoxesModern(boxes, color, xray)) return;
        //#endif
        double[] v = viewerPos();
        int r = color.getRed(), g = color.getGreen(), b = color.getBlue(), a = color.getAlpha();

        beginWorldDraw(xray, false);
        GL11.glLineWidth(lineWidth);
        //#if MC < 1.21
        GL11.glPushMatrix();
        GL11.glTranslated(-v[0], -v[1], -v[2]);
        //#endif

        try {
            VertexBuilder vb = TessellatorCompat.beginDraw(TessellatorCompat.LINES, TessellatorCompat.POSITION_COLOR);
            for (AxisAlignedBB bb : boxes) {
                addBoxLines(vb, bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ, r, g, b, a);
            }
            vb.draw();
        } finally {
            //#if MC < 1.21
            GL11.glPopMatrix();
            //#endif
            endWorldDraw();
        }
    }

    public static void drawFilledBlocks(List<AxisAlignedBB> blocks, Color color) {
        drawFilledBlocks(blocks, color, false, false);
    }

    public static void drawFilledBlocks(List<AxisAlignedBB> blocks, Color color, boolean solid) {
        drawFilledBlocks(blocks, color, solid, false);
    }

    public static void drawFilledBlocks(List<AxisAlignedBB> blocks, Color color, boolean solid, boolean xray) {
        //#if MC < 1.21
        //#else
        //$$ if (drawFilledBlocksModern(blocks, color, solid, xray)) return;
        //#endif
        if (blocks == null || blocks.isEmpty() || MinecraftCompat.getMinecraft().getRenderManager() == null) return;
        double[] v = viewerPos();
        int r = color.getRed(), g = color.getGreen(), b = color.getBlue(), a = solid ? 255 : color.getAlpha();
        double eps = 0.002;

        beginWorldFill(xray);
        //#if MC < 1.21
        GL11.glPushMatrix();
        GL11.glTranslated(-v[0], -v[1], -v[2]);
        //#endif

        try {
            VertexBuilder vb = TessellatorCompat.beginDraw(TessellatorCompat.QUADS, TessellatorCompat.POSITION_COLOR);
            //#if MC < 1.21
            double ox = 0, oy = 0, oz = 0;
            //#else
            //$$ double ox = -v[0], oy = -v[1], oz = -v[2];
            //#endif
            for (AxisAlignedBB aabb : blocks) {
                addBoxQuads(vb, aabb.minX - eps + ox, aabb.minY - eps + oy, aabb.minZ - eps + oz, aabb.maxX + eps + ox, aabb.maxY + eps + oy, aabb.maxZ + eps + oz, r, g, b, a);
            }
            vb.draw();
        } finally {
            //#if MC < 1.21
            GL11.glPopMatrix();
            //#endif
            endWorldDraw();
        }
    }

    public static void drawBlockQuads(BlockPos pos, float[] vertices, Color color, boolean throughWalls) {
        if (vertices.length == 0) return;

        //#if MC < 1.21
        double[] v = viewerPos();
        int r = color.getRed(), g = color.getGreen(), b = color.getBlue(), a = color.getAlpha();

        beginWorldFill(throughWalls);
        pushMatrix();
        translate(pos.getX() - v[0], pos.getY() - v[1], pos.getZ() - v[2]);
        try {
            VertexBuilder vb = TessellatorCompat.beginDraw(TessellatorCompat.QUADS, TessellatorCompat.POSITION_COLOR);
            for (int i = 0; i + 2 < vertices.length; i += 3) {
                vb.pos(vertices[i], vertices[i + 1], vertices[i + 2]).color(r, g, b, a).endVertex();
            }
            vb.draw();
        } finally {
            popMatrix();
            endWorldDraw();
        }
        //#else
        //$$ if (worldMatrices == null || worldConsumers == null) return;
        //$$ float r = color.getRed() / 255f, g = color.getGreen() / 255f, b = color.getBlue() / 255f;
        //$$ float a = color.getAlpha() / 255f;
        //$$ VertexConsumer vc = worldConsumers.getBuffer(throughWalls
        //$$         ? AsmRenderLayers.ESP_QUADS : RenderLayer.getDebugFilledBox());
        //$$ double[] v = viewerPos();
        //$$ worldMatrices.push();
        //$$ try {
        //$$     worldMatrices.translate((float) (pos.getX() - v[0]), (float) (pos.getY() - v[1]), (float) (pos.getZ() - v[2]));
        //$$     MatrixStack.Entry entry = worldMatrices.peek();
        //$$     for (int i = 0; i + 2 < vertices.length; i += 3) {
        //$$         vc.vertex(entry, vertices[i], vertices[i + 1], vertices[i + 2]).color(r, g, b, a);
        //$$     }
        //$$ } finally {
        //$$     worldMatrices.pop();
        //$$ }
        //$$ if (throughWalls) drawXrayLayer(AsmRenderLayers.ESP_QUADS);
        //#endif
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

    //#if MC < 1.21
    //#else
    // Horizontal ring as flat translucent quads through the event's consumer
    // pipeline. Line width is not expressible on the core profile, so pixel
    // thickness converts to world units at the target's distance.
    //$$ public static boolean drawWorldCircleModern(double cx, double cy, double cz, double radius,
    //$$                                             int steps, float thickness, float r, float g, float b, float a) {
    //$$     if (worldMatrices == null || worldConsumers == null) return false;
    //$$     double[] v = viewerPos();
    //$$     double dx = cx - v[0], dy = cy - v[1], dz = cz - v[2];
    //$$     double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
    //$$     if (dist < 1e-3) return false;
    //$$     // Ring sits coplanar with the block top; lift past depth precision.
    //$$     dy += Math.max(0.02, dist * 0.001);
    //$$
    //$$     int fov = MinecraftCompat.getMinecraft().options.getFov().getValue();
    //$$     int fbH = MinecraftCompat.getMinecraft().getWindow().getFramebufferHeight();
    //$$     if (fbH <= 0) return false;
    //$$     double halfWidth = thickness * (2.0 * dist * Math.tan(Math.toRadians(fov / 2.0)) / fbH) / 2.0;
    //$$
    //$$     worldMatrices.push();
    //$$     try {
    //$$         worldMatrices.translate((float) dx, (float) dy, (float) dz);
    //$$         VertexConsumer vc = worldConsumers.getBuffer(AsmRenderLayers.ESP_QUADS);
    //$$         MatrixStack.Entry entry = worldMatrices.peek();
    //$$         for (int i = 0; i < steps; i++) {
    //$$             double a0 = (Math.PI * 2) * i / steps;
    //$$             double a1 = (Math.PI * 2) * (i + 1) / steps;
    //$$             float inR = (float) (radius - halfWidth), outR = (float) (radius + halfWidth);
    //$$             vc.vertex(entry, (float) (Math.cos(a0) * outR), 0f, (float) (Math.sin(a0) * outR)).color(r, g, b, a);
    //$$             vc.vertex(entry, (float) (Math.cos(a1) * outR), 0f, (float) (Math.sin(a1) * outR)).color(r, g, b, a);
    //$$             vc.vertex(entry, (float) (Math.cos(a1) * inR), 0f, (float) (Math.sin(a1) * inR)).color(r, g, b, a);
    //$$             vc.vertex(entry, (float) (Math.cos(a0) * inR), 0f, (float) (Math.sin(a0) * inR)).color(r, g, b, a);
    //$$         }
    //$$     } finally {
    //$$         worldMatrices.pop();
    //$$     }
    //$$     GlStateManagerCompat.color(1f, 1f, 1f, 1f);
    //$$     drawXrayLayer(AsmRenderLayers.ESP_QUADS);
    //$$     return true;
    //$$ }
    //#endif

    //#if MC >= 1.21
    // Solid cylinder wall through the event's consumer pipeline. POSITION_COLOR
    // needs no normals; culling is disabled on this layer so winding is free.
    //$$ public static boolean drawCylinderModern(double cx, double cy, double cz, double radius, double height,
    //$$                                          int steps, float r, float g, float b, float a) {
    //$$     if (worldMatrices == null || worldConsumers == null) return false;
    //$$     double[] v = viewerPos();
    //$$     worldMatrices.push();
    //$$     try {
    //$$         worldMatrices.translate((float) (cx - v[0]), (float) (cy - v[1]), (float) (cz - v[2]));
    //$$         VertexConsumer vc = worldConsumers.getBuffer(RenderLayer.getDebugQuads());
    //$$         MatrixStack.Entry entry = worldMatrices.peek();
    //$$         for (int i = 0; i < steps; i++) {
    //$$             double a0 = (Math.PI * 2) * i / steps;
    //$$             double a1 = (Math.PI * 2) * (i + 1) / steps;
    //$$             float x0 = (float) (Math.cos(a0) * radius), z0 = (float) (Math.sin(a0) * radius);
    //$$             float x1 = (float) (Math.cos(a1) * radius), z1 = (float) (Math.sin(a1) * radius);
    //$$             float h = (float) height;
    //$$             vc.vertex(entry, x0, 0f, z0).color(r, g, b, a);
    //$$             vc.vertex(entry, x1, 0f, z1).color(r, g, b, a);
    //$$             vc.vertex(entry, x1, h, z1).color(r, g, b, a);
    //$$             vc.vertex(entry, x0, h, z0).color(r, g, b, a);
    //$$         }
    //$$     } finally {
    //$$         worldMatrices.pop();
    //$$     }
    //$$     GlStateManagerCompat.color(1f, 1f, 1f, 1f);
    //$$     return true;
    //$$ }
    //#endif

    //#if MC >= 1.21
    // Raw GL draws are invisible on modern; use the event's consumer pipeline.
    //$$ private static boolean drawSelectionBoxModern(Box box, Color color) {
    //$$     return drawSelectionBoxModern(box, color, false);
    //$$ }
    //$$
    //$$ private static boolean drawSelectionBoxModern(Box box, Color color, boolean xray) {
    //$$     if (worldMatrices == null || worldConsumers == null) return false;
    //$$     double[] v = viewerPos();
    //$$     worldMatrices.push();
    //$$     try {
    //$$         worldMatrices.translate((float) -v[0], (float) -v[1], (float) -v[2]);
    //$$         VertexConsumer vc = worldConsumers.getBuffer(xray ? AsmRenderLayers.ESP_LINES : RenderLayer.getLines());
    //$$         WorldRenderer.drawBox(worldMatrices, vc, box,
    //$$                 color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);
    //$$     } finally {
    //$$         worldMatrices.pop();
    //$$     }
    //$$     if (xray) drawXrayLayer(AsmRenderLayers.ESP_LINES);
    //$$     return true;
    //$$ }
    //$$
    //$$ private static boolean drawEspBoxesModern(List<Box> boxes, Color color, boolean xray) {
    //$$     if (worldMatrices == null || worldConsumers == null) return false;
    //$$     double[] v = viewerPos();
    //$$     float r = color.getRed() / 255f, g = color.getGreen() / 255f, b = color.getBlue() / 255f, a = color.getAlpha() / 255f;
    //$$     worldMatrices.push();
    //$$     try {
    //$$         worldMatrices.translate((float) -v[0], (float) -v[1], (float) -v[2]);
    //$$         VertexConsumer vc = worldConsumers.getBuffer(xray ? AsmRenderLayers.ESP_LINES : RenderLayer.getLines());
    //$$         for (Box box : boxes) {
    //$$             WorldRenderer.drawBox(worldMatrices, vc, box, r, g, b, a);
    //$$         }
    //$$     } finally {
    //$$         worldMatrices.pop();
    //$$     }
    //$$     if (xray) drawXrayLayer(AsmRenderLayers.ESP_LINES);
    //$$     return true;
    //$$ }
    //$$
    //$$ private static boolean drawFilledBlocksModern(List<Box> blocks, Color color, boolean solid, boolean xray) {
    //$$     if (worldMatrices == null || worldConsumers == null) return false;
    //$$     if (blocks == null || blocks.isEmpty()) return false;
    //$$     float r = color.getRed() / 255f, g = color.getGreen() / 255f;
    //$$     float b = color.getBlue() / 255f, a = solid ? 1f : color.getAlpha() / 255f;
    //$$     double eps = 0.002;
    //$$     double[] v = viewerPos();
    //$$     worldMatrices.push();
    //$$     try {
    //$$         worldMatrices.translate((float) -v[0], (float) -v[1], (float) -v[2]);
    //$$         VertexConsumer vc = worldConsumers.getBuffer(xray ? AsmRenderLayers.ESP_QUADS : RenderLayer.getDebugFilledBox());
    //$$         for (Box box : blocks) {
    //$$             WorldRenderer.renderFilledBox(worldMatrices, vc,
    //$$                     box.minX - eps, box.minY - eps, box.minZ - eps,
    //$$                     box.maxX + eps, box.maxY + eps, box.maxZ + eps, r, g, b, a);
    //$$         }
    //$$     } finally {
    //$$         worldMatrices.pop();
    //$$     }
    //$$     if (xray) drawXrayLayer(AsmRenderLayers.ESP_QUADS);
    //$$     return true;
    //$$ }
    //#endif

    public static final class Beam {
        private final double x;
        private final double y;
        private final double z;
        private final double height;

        public Beam(double x, double y, double z, double height) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.height = height;
        }
    }

    /**
     * Draws a vanilla-style beacon beam spanning the block column {@code x..x+1},
     * {@code z..z+1} starting at {@code y}. Depth testing stays enabled, so the
     * beam is occluded by terrain and never renders through blocks.
     */
    public static void drawBeaconBeam(double x, double y, double z, Color color, float partialTicks, double height) {
        drawBeaconBeams(Collections.singletonList(new Beam(x, y, z, height)), color, partialTicks);
    }

    public static void drawBeaconBeams(List<Beam> beams, Color color, float partialTicks) {
        if (beams.isEmpty()) return;
        if (MinecraftCompat.getLocalWorld() == null || MinecraftCompat.getMinecraft().getRenderManager() == null) return;
        //#if MC < 1.21
        //#else
        //$$ if (worldMatrices == null || worldConsumers == null) return;
        //#endif
        double[] v = viewerPos();

        pushMatrix();
        translate(-v[0], -v[1], -v[2]);

        //#if MC < 1.21
        boolean lightingWasOn = GL11.glIsEnabled(GL11.GL_LIGHTING);
        MinecraftCompat.getMinecraft().getTextureManager().bindTexture(Resources.BEACON_BEAM);
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        //#endif

        GlStateManagerCompat.disableLighting();
        GlStateManagerCompat.disableCull();
        GlStateManagerCompat.enableTexture2D();
        GlStateManagerCompat.enableBlend();
        GlStateManagerCompat.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManagerCompat.enableDepth();
        GlStateManagerCompat.depthFunc(GL11.GL_LEQUAL);
        GlStateManagerCompat.depthMask(false);

        double time = WorldCompat.getTotalWorldTime(MinecraftCompat.getLocalWorld()) + partialTicks;
        double texShift = -time * 0.2 - Math.floor(-time * 0.1);
        texShift -= Math.floor(texShift);

        float r = color.getRed() / 255f, g = color.getGreen() / 255f, b = color.getBlue() / 255f;

        //#if MC < 1.21
        VertexBuilder vb = TessellatorCompat.beginDraw(TessellatorCompat.QUADS, TessellatorCompat.POSITION_TEX_COLOR);
        //#else
        //$$ RenderLayer beamLayer = RenderLayer.getBeaconBeam(Resources.BEACON_BEAM, false);
        //$$ VertexConsumer beamVc = worldConsumers.getBuffer(beamLayer);
        //$$ MatrixStack.Entry beamEntry = worldMatrices.peek();
        //$$ int beamLight = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        //#endif

        for (Beam beam : beams) {
        double x = beam.x, y = beam.y, z = beam.z, height = beam.height;

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

        //#if MC < 1.21
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
        //#else
        //$$ beamVertex(beamVc, beamEntry, x + d4, y + height, z + d5, 1.0, v1, r, g, b, 1f, 0f, 0f, 1f, beamLight);
        //$$ beamVertex(beamVc, beamEntry, x + d4, y, z + d5, 1.0, v0, r, g, b, 1f, 0f, 0f, 1f, beamLight);
        //$$ beamVertex(beamVc, beamEntry, x + d6, y, z + d7, 0.0, v0, r, g, b, 1f, 0f, 0f, 1f, beamLight);
        //$$ beamVertex(beamVc, beamEntry, x + d6, y + height, z + d7, 0.0, v1, r, g, b, 1f, 0f, 0f, 1f, beamLight);
        //$$ beamVertex(beamVc, beamEntry, x + d10, y + height, z + d11, 1.0, v1, r, g, b, 1f, 0f, 0f, -1f, beamLight);
        //$$ beamVertex(beamVc, beamEntry, x + d10, y, z + d11, 1.0, v0, r, g, b, 1f, 0f, 0f, -1f, beamLight);
        //$$ beamVertex(beamVc, beamEntry, x + d8, y, z + d9, 0.0, v0, r, g, b, 1f, 0f, 0f, -1f, beamLight);
        //$$ beamVertex(beamVc, beamEntry, x + d8, y + height, z + d9, 0.0, v1, r, g, b, 1f, 0f, 0f, -1f, beamLight);
        //$$ beamVertex(beamVc, beamEntry, x + d6, y + height, z + d7, 1.0, v1, r, g, b, 1f, 0.70711f, 0f, -0.70711f, beamLight);
        //$$ beamVertex(beamVc, beamEntry, x + d6, y, z + d7, 1.0, v0, r, g, b, 1f, 0.70711f, 0f, -0.70711f, beamLight);
        //$$ beamVertex(beamVc, beamEntry, x + d10, y, z + d9, 0.0, v0, r, g, b, 1f, 0.70711f, 0f, -0.70711f, beamLight);
        //$$ beamVertex(beamVc, beamEntry, x + d10, y + height, z + d11, 0.0, v1, r, g, b, 1f, 0.70711f, 0f, -0.70711f, beamLight);
        //$$ beamVertex(beamVc, beamEntry, x + d8, y + height, z + d9, 1.0, v1, r, g, b, 1f, -0.70711f, 0f, -0.70711f, beamLight);
        //$$ beamVertex(beamVc, beamEntry, x + d8, y, z + d9, 1.0, v0, r, g, b, 1f, -0.70711f, 0f, -0.70711f, beamLight);
        //$$ beamVertex(beamVc, beamEntry, x + d4, y, z + d5, 0.0, v0, r, g, b, 1f, -0.70711f, 0f, -0.70711f, beamLight);
        //$$ beamVertex(beamVc, beamEntry, x + d4, y + height, z + d5, 0.0, v1, r, g, b, 1f, -0.70711f, 0f, -0.70711f, beamLight);
        //#endif

        // Outer translucent shell
        double s0 = -1.0 + texShift;
        double s1 = height + s0;
        float sa = 0.25f;

        //#if MC < 1.21
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
        //#else
        //$$ beamVertex(beamVc, beamEntry, x + 0.2, y + height, z + 0.2, 1.0, s1, r, g, b, sa, 0f, 0f, -1f, beamLight);
        //$$ beamVertex(beamVc, beamEntry, x + 0.2, y, z + 0.2, 1.0, s0, r, g, b, sa, 0f, 0f, -1f, beamLight);
        //$$ beamVertex(beamVc, beamEntry, x + 0.8, y, z + 0.2, 0.0, s0, r, g, b, sa, 0f, 0f, -1f, beamLight);
        //$$ beamVertex(beamVc, beamEntry, x + 0.8, y + height, z + 0.2, 0.0, s1, r, g, b, sa, 0f, 0f, -1f, beamLight);
        //$$ beamVertex(beamVc, beamEntry, x + 0.8, y + height, z + 0.8, 1.0, s1, r, g, b, sa, 1f, 0f, 0f, beamLight);
        //$$ beamVertex(beamVc, beamEntry, x + 0.8, y, z + 0.8, 1.0, s0, r, g, b, sa, 1f, 0f, 0f, beamLight);
        //$$ beamVertex(beamVc, beamEntry, x + 0.2, y, z + 0.8, 0.0, s0, r, g, b, sa, 1f, 0f, 0f, beamLight);
        //$$ beamVertex(beamVc, beamEntry, x + 0.2, y + height, z + 0.8, 0.0, s1, r, g, b, sa, 1f, 0f, 0f, beamLight);
        //$$ beamVertex(beamVc, beamEntry, x + 0.8, y + height, z + 0.2, 1.0, s1, r, g, b, sa, -1f, 0f, 0f, beamLight);
        //$$ beamVertex(beamVc, beamEntry, x + 0.8, y, z + 0.2, 1.0, s0, r, g, b, sa, -1f, 0f, 0f, beamLight);
        //$$ beamVertex(beamVc, beamEntry, x + 0.8, y, z + 0.8, 0.0, s0, r, g, b, sa, -1f, 0f, 0f, beamLight);
        //$$ beamVertex(beamVc, beamEntry, x + 0.8, y + height, z + 0.8, 0.0, s1, r, g, b, sa, -1f, 0f, 0f, beamLight);
        //$$ beamVertex(beamVc, beamEntry, x + 0.2, y + height, z + 0.8, 1.0, s1, r, g, b, sa, -1f, 0f, 0f, beamLight);
        //$$ beamVertex(beamVc, beamEntry, x + 0.2, y, z + 0.8, 1.0, s0, r, g, b, sa, -1f, 0f, 0f, beamLight);
        //$$ beamVertex(beamVc, beamEntry, x + 0.2, y, z + 0.2, 0.0, s0, r, g, b, sa, -1f, 0f, 0f, beamLight);
        //$$ beamVertex(beamVc, beamEntry, x + 0.2, y + height, z + 0.2, 0.0, s1, r, g, b, sa, -1f, 0f, 0f, beamLight);
        //#endif
        }

        //#if MC < 1.21
        vb.draw();
        //#else
        //$$ drawLayer(beamLayer);
        //#endif

        GlStateManagerCompat.depthMask(true);
        GlStateManagerCompat.disableBlend();
        GlStateManagerCompat.enableCull();
        GlStateManagerCompat.depthFunc(GL11.GL_LESS);
        //#if MC < 1.21
        if (lightingWasOn) GlStateManagerCompat.enableLighting();
        //#endif
        popMatrix();
        GlStateManagerCompat.color(1f, 1f, 1f, 1f);
    }

    //#if MC >= 1.21
    //$$ private static void beamVertex(VertexConsumer vc, MatrixStack.Entry entry,
    //$$         double x, double y, double z, double u, double v,
    //$$         float r, float g, float b, float a, float nx, float ny, float nz, int light) {
    //$$     vc.vertex(entry, (float) x, (float) y, (float) z)
    //$$             .color(r, g, b, a).texture((float) u, (float) v)
    //$$             .overlay(OverlayTexture.DEFAULT_UV).light(light)
    //$$             .normal(entry, nx, ny, nz);
    //$$ }
    //#endif
}