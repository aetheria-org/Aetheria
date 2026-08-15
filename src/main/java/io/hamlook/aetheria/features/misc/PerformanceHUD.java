package io.hamlook.aetheria.features.misc;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import io.hamlook.aetheria.utils.Position;
import io.hamlook.aetheria.events.PacketReceiveStatsEvent;
import io.hamlook.aetheria.events.PacketReceiveTimeUpdateEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.overlay.Overlay;
import io.hamlook.aetheria.utils.overlay.OverlayUtils;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

@RegisterEvents
public class PerformanceHUD extends Overlay {

    public static final int OVERLAY_WIDTH = 100;
    public static final int OVERLAY_HEIGHT = 45;

    private static final String C_LABEL = EnumChatFormatting.AQUA.toString();
    private static final String C_VAL = EnumChatFormatting.WHITE.toString();

    private static final int TPS_SAMPLES = 5;
    private static final long[] tpsTimes = new long[TPS_SAMPLES];
    private static final long PING_TIMEOUT_NS = 5_000_000_000L;
    private static final int PING_INTERVAL_TICKS = 100;
    private static int tpsHead = 0;
    private static int tpsCount = 0;
    private static float currentTps = 20f;
    private static long pingSentAt = -1L;
    private static double pingMs = -1;
    private static int ticksSincePing = 0;

    @Getter
    private static PerformanceHUD instance;

    public PerformanceHUD() {
        super(OVERLAY_WIDTH, OVERLAY_HEIGHT);
        instance = this;
    }

    public static float getCurrentTps() {
        return currentTps;
    }

    public static double getPingMs() {
        return pingMs;
    }

    private static String formatPing() {
        return pingMs < 0 ? "..." : String.format("%.0fms", pingMs);
    }

    private static int estimateWidth(Minecraft mc, List<String> lines, boolean vertical) {
        if (vertical) {
            int max = OVERLAY_WIDTH;
            for (String l : lines) max = Math.max(max, mc.fontRendererObj.getStringWidth(l) + PADDING * 2);
            return max;
        }
        int total = 0;
        for (String l : lines) total += mc.fontRendererObj.getStringWidth(l) + 6;
        return Math.max(total, OVERLAY_WIDTH);
    }

    @Override
    public Position getPosition() {
        return ATHRConfig.feature.misc.performanceHudConfig.hudPos;
    }

    @Override
    public float getScale() {
        return ATHRConfig.feature.misc.performanceHudConfig.hudScale;
    }

    @Override
    public int getBgColor() {
        return ChromaColour.specialToChromaRGB(ATHRConfig.feature.misc.performanceHudConfig.hudBgColor);
    }

    @Override
    public int getCornerRadius() {
        return ATHRConfig.feature.misc.performanceHudConfig.hudCornerRadius;
    }

    @Override
    protected boolean isEnabled() {
        return ATHRConfig.feature.misc.performanceHudConfig.performanceHud;
    }

    @Override
    protected boolean hideOnChat()   { return ATHRConfig.feature.misc.performanceHudConfig.hideOnChat; }
    @Override
    protected boolean hideOnTab()    { return ATHRConfig.feature.misc.performanceHudConfig.hideOnTab; }
    @Override
    protected boolean hideOnDebug()  { return ATHRConfig.feature.misc.performanceHudConfig.hideOnDebug; }

    @SubscribeEvent
    public void onTimeUpdate(PacketReceiveTimeUpdateEvent event) {
        long now = System.currentTimeMillis();
        if (tpsCount > 0) {
            int prev = (tpsHead - 1 + TPS_SAMPLES) % TPS_SAMPLES;
            long delta = now - tpsTimes[prev];
            if (delta > 0) currentTps = Math.max(0f, Math.min(20f, 20_000f / delta));
        }
        tpsTimes[tpsHead] = now;
        tpsHead = (tpsHead + 1) % TPS_SAMPLES;
        if (tpsCount < TPS_SAMPLES) tpsCount++;
    }

    @SubscribeEvent
    public void onStats(PacketReceiveStatsEvent event) {
        if (pingSentAt < 0) return;
        pingMs = Math.abs(System.nanoTime() - pingSentAt) / 1_000_000.0;
        pingSentAt = -1L;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.thePlayer.sendQueue == null) return;

        if (pingSentAt >= 0 && System.nanoTime() - pingSentAt > PING_TIMEOUT_NS) {
            pingSentAt = -1L;
            ticksSincePing = 0;
        }

        if (pingSentAt >= 0) return;
        if (++ticksSincePing < PING_INTERVAL_TICKS) return;
        ticksSincePing = 0;
        pingSentAt = System.nanoTime();
        mc.thePlayer.sendQueue.getNetworkManager().sendPacket(new C16PacketClientStatus(C16PacketClientStatus.EnumState.REQUEST_STATS));
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        tpsCount = 0;
        tpsHead = 0;
        currentTps = 20f;
        pingSentAt = -1L;
        pingMs = -1;
        ticksSincePing = 0;
    }

    @Override
    public List<String> getLines(boolean preview) {
        List<String> out = new ArrayList<>();
        if (ATHRConfig.feature.misc.performanceHudConfig.hudShowFps)
            out.add(C_LABEL + "FPS: " + C_VAL + (preview ? 60 : Minecraft.getDebugFPS()));
        if (ATHRConfig.feature.misc.performanceHudConfig.hudShowTps)
            out.add(C_LABEL + "TPS: " + C_VAL + (preview ? "20.0" : String.format("%.1f", currentTps)));
        if (ATHRConfig.feature.misc.performanceHudConfig.hudShowPing) out.add(C_LABEL + "Ping: " + C_VAL + (preview ? "42ms" : formatPing()));
        if (ATHRConfig.feature.misc.performanceHudConfig.hudShowCoords) {
            if (preview) {
                out.add(C_LABEL + "XYZ: " + C_VAL + "0 / 64 / 0");
            } else {
                net.minecraft.entity.player.EntityPlayer p = Minecraft.getMinecraft().thePlayer;
                if (p != null)
                    out.add(C_LABEL + "XYZ: " + C_VAL + (int) Math.floor(p.posX) + " / " + (int) Math.floor(p.posY) + " / " + (int) Math.floor(p.posZ));
            }
        }
        if (ATHRConfig.feature.misc.performanceHudConfig.hudShowRotation) {
            if (preview) {
                out.add(C_LABEL + "Yaw: " + C_VAL + "180.0  " + C_LABEL + "Pitch: " + C_VAL + "0.0");
            } else {
                net.minecraft.entity.player.EntityPlayer p = Minecraft.getMinecraft().thePlayer;
                if (p != null) {
                    float yaw = p.rotationYaw % 360.0f;
                    if (yaw >= 180.0f) yaw -= 360.0f;
                    if (yaw < -180.0f) yaw += 360.0f;
                    out.add(C_LABEL + "Yaw: " + C_VAL + String.format("%.1f", yaw) + "  " + C_LABEL + "Pitch: " + C_VAL + String.format("%.1f", p.rotationPitch));
                }
            }
        }
        return out;
    }

    @Override
    public void render(boolean preview) {
        if (!preview && OverlayUtils.shouldHide()) return;

        List<String> lines = getLines(preview);
        if (lines.isEmpty()) return;

        Minecraft mc = Minecraft.getMinecraft();
        float scale = getScale();
        boolean vert = ATHRConfig.feature.misc.performanceHudConfig.hudVertical;

        int w = estimateWidth(mc, lines, vert);
        int h = vert ? lines.size() * LINE_HEIGHT + PADDING * 2 : LINE_HEIGHT + PADDING * 2;
        lastW = w;
        lastH = h;

        Position pos = getPosition();
        int x = pos.getAbsX(sr, (int) (w * scale));
        int y = pos.getAbsY(sr, (int) (h * scale));
        if (pos.isCenterX()) x -= (int) (w * scale / 2);
        if (pos.isCenterY()) y -= (int) (h * scale / 2);

        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0);
        GL11.glScalef(scale, scale, 1f);

        int bgColor = getBgColor();
        if ((bgColor >>> 24) != 0) drawRoundedRect(-PADDING, -PADDING, w, h - PADDING, getCornerRadius(), bgColor);

        if (vert) {
            int dy = 0;
            for (String line : lines) {
                mc.fontRendererObj.drawStringWithShadow(line, 0, dy, 0xFFFFFF);
                dy += LINE_HEIGHT;
            }
        } else {
            int cx = 0;
            for (String line : lines) {
                mc.fontRendererObj.drawStringWithShadow(line, cx, 0, 0xFFFFFF);
                cx += mc.fontRendererObj.getStringWidth(line) + 6;
            }
        }

        GL11.glPopMatrix();
    }
}