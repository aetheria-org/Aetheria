package com.jef.justenoughfakepixel.features.misc.timer;

import com.jef.justenoughfakepixel.core.JefConfig;
import com.jef.justenoughfakepixel.core.config.editors.ChromaColour;
import com.jef.justenoughfakepixel.core.config.utils.Position;
import com.jef.justenoughfakepixel.init.RegisterEvents;
import com.jef.justenoughfakepixel.utils.overlay.Overlay;
import com.jef.justenoughfakepixel.utils.time.TimeFormatter;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

@RegisterEvents
public class UptimeOverlay extends Overlay {

    private static final int EXPIRY_SHOW_TICKS = 200; // 10 s overlay linger
    private static final int EXPIRY_TITLE_TICKS = 60; // 3 s on-screen title fade
    @Getter
    private static UptimeOverlay instance;
    private boolean showingExpiry = false;
    private int expiryTicksLeft = 0;

    public UptimeOverlay() {
        super(160, 40);
        instance = this;
    }

    @Override
    public Position getPosition() {
        return JefConfig.feature.misc.uptimeConfig.uptimePos;
    }

    @Override
    public float getScale() {
        return JefConfig.feature.misc.uptimeConfig.uptimeScale;
    }

    @Override
    public int getBgColor() {
        return ChromaColour.specialToChromaRGB(JefConfig.feature.misc.uptimeConfig.uptimeBgColor);
    }

    @Override
    public int getCornerRadius() {
        return JefConfig.feature.misc.uptimeConfig.uptimeCornerRadius;
    }

    @Override
    protected boolean isEnabled() {
        return JefConfig.feature != null && JefConfig.feature.misc.uptimeConfig.uptimeEnabled;
    }

    @Override
    public List<String> getLines(boolean preview) {
        List<String> lines = new ArrayList<>();

        if (preview) {
            lines.add("§b§lTimer");
            lines.add("§a" + TimeFormatter.formatCountdown(3661000L)); // 1h 1m preview
            return lines;
        }

        UptimeManager mgr = UptimeManager.getInstance();

        if (mgr.isActive() && mgr.getRemainingMs() > 0) {
            String header = mgr.isPaused() ? "§b§lTimer §7(paused)" : "§b§lTimer";
            lines.add(header);

            long rem = mgr.getRemainingMs();
            lines.add(TimeFormatter.getCountdownColor(rem, mgr.getTotalDurationMs())
                    + TimeFormatter.formatCountdown(rem));

            return lines;
        }

        // Timer just expired and "show when expired" is on, keep overlay visible briefly
        if (showingExpiry && JefConfig.feature.misc.uptimeConfig.uptimeShowWhenExpired) {
            lines.add("§b§lTimer");
            lines.add("§7Done!");
        }

        return lines;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !isEnabled()) return;

        if (UptimeManager.getInstance().pollExpired()) {
            showingExpiry = true;
            expiryTicksLeft = EXPIRY_SHOW_TICKS;
        }

        if (showingExpiry && --expiryTicksLeft <= 0) {
            showingExpiry = false;
        }
    }

    @Override
    public void render(boolean preview) {
        super.render(preview);
        if (!preview && showingExpiry && isEnabled()) renderExpiryMessage();
    }

    private void renderExpiryMessage() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        // Only show the big on-screen pop for the first EXPIRY_TITLE_TICKS ticks
        int titleTicksLeft = expiryTicksLeft - (EXPIRY_SHOW_TICKS - EXPIRY_TITLE_TICKS);
        if (titleTicksLeft <= 0) return;

        ScaledResolution sr = new ScaledResolution(mc);
        float alpha = Math.min(1f, titleTicksLeft / 20f); // fade in for 1s, fade out last 20t
        if (expiryTicksLeft < 20) alpha = expiryTicksLeft / 20f;
        int a = (int) (alpha * 255) & 0xFF;

        String line1 = "§c§l⏰ Timer Done!";
        String line2 = "§eCountdown has ended.";

        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glTranslatef(sr.getScaledWidth() / 2f, sr.getScaledHeight() / 3f, 0f);
        GL11.glScalef(2f, 2f, 1f);

        int white = (a << 24) | 0x00FFFFFF;
        mc.fontRendererObj.drawStringWithShadow(line1, -mc.fontRendererObj.getStringWidth(line1) / 2f, -12f, white);
        mc.fontRendererObj.drawStringWithShadow(line2, -mc.fontRendererObj.getStringWidth(line2) / 2f, 2f, white);

        GL11.glPopMatrix();
    }
}