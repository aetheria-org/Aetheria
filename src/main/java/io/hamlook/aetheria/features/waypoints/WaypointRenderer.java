package io.hamlook.aetheria.features.waypoints;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import io.hamlook.aetheria.events.ASMRenderWorldEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.render.WorldRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.StringUtils;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.List;

@RegisterEvents
public class WaypointRenderer {

    private static final Minecraft mc = MinecraftCompat.getMinecraft();

    private Color argbToColor(int argb) {
        return new Color((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >> 24) & 0xFF);
    }

    private int resolveColor(String chromaField, int fallback) {
        if (ATHRConfig.feature == null) return fallback;
        try { return ChromaColour.specialToChromaRGB(chromaField); }
        catch (Exception e) { return fallback; }
    }

    @HandleEvent
    public void onRenderWorld(ASMRenderWorldEvent event) {
        WaypointState state = WaypointState.getInstance();
        if (!state.enabled || !state.hasGroup()) return;

        if (ATHRConfig.feature != null) {
            state.advanceRange = ATHRConfig.feature.waypoints.autoAdvance.advanceRange;
            state.advanceDelayMs = (long) ATHRConfig.feature.waypoints.autoAdvance.advanceDelayMs;
        }

        tickAdvance(state);

        WaypointPoint target = state.getNext();
        if (target == null) return;

        double vx = mc.getRenderManager().viewerPosX;
        double vy = mc.getRenderManager().viewerPosY;
        double vz = mc.getRenderManager().viewerPosZ;

        // ESP boxes
        WorldRenderUtils.beginWorldRender(2f);
        GL11.glPushMatrix();
        GL11.glTranslated(-vx, -vy, -vz);
        if (state.setupMode) {
            for (WaypointPoint wp : state.loadedGroup.waypoints)
                WorldRenderUtils.drawEspBox(wp.x, wp.y, wp.z, boxColor());
        } else {
            WorldRenderUtils.drawEspBox(target.x, target.y, target.z, boxColor());
        }
        GL11.glPopMatrix();
        WorldRenderUtils.endWorldRender();

        // Tracer
        WorldRenderUtils.drawTracer(new Vec3(target.x + 0.5, target.y + 0.5, target.z + 0.5), event.partialTicks, tracerColor());

        // Labels
        GL11.glPushMatrix();
        GL11.glTranslated(-vx, -vy, -vz);
        if (state.setupMode) drawSetupLabels(state);
        else drawNormalLabels(state);
        GL11.glPopMatrix();
    }

    private void tickAdvance(WaypointState state) {
        if (mc.thePlayer == null) return;
        WaypointPoint next = state.getNext();
        if (next == null) return;
        double dist = next.distanceTo(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
        if (dist <= state.advanceRange) {
            if (state.advanceTimerStart < 0) state.advanceTimerStart = System.currentTimeMillis();
            else if (System.currentTimeMillis() - state.advanceTimerStart >= state.advanceDelayMs) state.advance();
        } else {
            state.advanceTimerStart = -1L;
        }
    }

    private void drawSetupLabels(WaypointState state) {
        List<WaypointPoint> wps = state.loadedGroup.waypoints;
        int cur = state.currentIndex;
        int nxt = state.getNextIndex();
        for (int i = 0; i < wps.size(); i++) {
            WaypointPoint wp = wps.get(i);
            String col = (i == cur) ? "§a" : (i == nxt) ? "§e" : "§7";
            String label = (wp.name != null && !wp.name.isEmpty()) ? wp.name : String.valueOf(i + 1);
            drawLabel(wp.x + 0.5, wp.y + 2.2, wp.z + 0.5, col + label,labelColor(),distanceLabelColor());
        }
    }

    private void drawNormalLabels(WaypointState state) {
        WaypointPoint prev = state.getPrev();
        WaypointPoint cur = state.getCurrent();
        WaypointPoint nxt = state.getNext();
        if (prev != null && prev != cur)
            drawLabel(prev.x + 0.5, prev.y + 2.2, prev.z + 0.5, "§7" + safeName(prev, "Prev"),labelColor(),distanceLabelColor());
        if (cur != null) drawLabel(cur.x + 0.5, cur.y + 2.2, cur.z + 0.5, "§a" + safeName(cur, "Current"),labelColor(),distanceLabelColor());
        if (nxt != null && nxt != cur) drawLabel(nxt.x + 0.5, nxt.y + 2.2, nxt.z + 0.5, "§e" + safeName(nxt, "Next"),labelColor(),distanceLabelColor());
    }

    public static void drawLabel(double wx, double wy, double wz, String text, int nameColor, int distColor) {
        drawLabel(wx, wy, wz, text, nameColor, distColor, 1.0);
    }

    public static void drawLabel(double wx, double wy, double wz, String text, int nameColor, int distColor, double scaleMultiplier) {
        if (mc.thePlayer == null || mc.fontRendererObj == null) return;

        double dx = wx - mc.thePlayer.posX;
        double dy = wy - mc.thePlayer.posY;
        double dz = wz - mc.thePlayer.posZ;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        float scale = Math.max(0.025f, (float) (Math.min(dist, 50.0) / 300.0));
        scale *= (float) scaleMultiplier;

        String nameStr = StringUtils.stripControlCodes(text);
        String distNum = String.valueOf((int) Math.round(dist));

        int nameW = mc.fontRendererObj.getStringWidth(nameStr);
        String bracketOpen = "[";
        String bracketClose = "]";
        int distNumW = mc.fontRendererObj.getStringWidth(distNum);
        int bracketW = mc.fontRendererObj.getStringWidth(bracketOpen) + distNumW + mc.fontRendererObj.getStringWidth(bracketClose);

        GL11.glPushMatrix();
        GL11.glTranslated(wx, wy, wz);
        GL11.glRotatef(-mc.getRenderManager().playerViewY, 0f, 1f, 0f);
        GL11.glRotatef(mc.getRenderManager().playerViewX, 1f, 0f, 0f);
        GL11.glScalef(-scale, -scale, scale);

        GlStateManagerCompat.disableLighting();
        GlStateManagerCompat.disableDepth();

        float sx = -nameW / 2f;
        mc.fontRendererObj.drawString(nameStr, sx, 0f, nameColor, false);

        float bx = -bracketW / 2f;
        float bw1 = mc.fontRendererObj.getStringWidth(bracketOpen);
        mc.fontRendererObj.drawString(bracketOpen, bx, 10f, 0xAAAAAA, false);
        mc.fontRendererObj.drawString(distNum, bx + bw1, 10f, nameColor, false);
        mc.fontRendererObj.drawString(bracketClose, bx + bw1 + mc.fontRendererObj.getStringWidth(distNum), 10f, 0xAAAAAA, false);

        GlStateManagerCompat.enableDepth();
        GL11.glPopMatrix();
    }

    private String safeName(WaypointPoint wp, String fallback) {
        return (wp.name != null && !wp.name.isEmpty()) ? wp.name : fallback;
    }

    private Color boxColor() {
        return argbToColor(resolveColor(ATHRConfig.feature != null ? ATHRConfig.feature.waypoints.colors.boxColour : null, 0xFFFFFF00));
    }

    private Color tracerColor() {
        return argbToColor(resolveColor(ATHRConfig.feature != null ? ATHRConfig.feature.waypoints.colors.tracerColour : null, 0xFFFFFF00));
    }

    private int labelColor() {
        return resolveColor(ATHRConfig.feature != null ? ATHRConfig.feature.waypoints.colors.labelColour : null, 0xFFFFFFFF);
    }

    private int distanceLabelColor() {
        return resolveColor(ATHRConfig.feature != null ? ATHRConfig.feature.waypoints.colors.distanceLabelColour : null, 0xFF55FFFF);
    }
}