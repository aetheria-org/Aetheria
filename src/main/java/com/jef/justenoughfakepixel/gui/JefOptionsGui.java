package com.jef.justenoughfakepixel.gui;

import com.jef.justenoughfakepixel.JefMod;
import com.jef.justenoughfakepixel.core.JefConfig;
import com.jef.justenoughfakepixel.features.capes.ui.CapeSelectorGUI;
import com.jef.justenoughfakepixel.repo.JefRepo;
import com.jef.justenoughfakepixel.repo.RepoHandler;
import com.jef.justenoughfakepixel.repo.data.UpdateData;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class JefOptionsGui extends GuiScreen {

    // ── Button IDs ─────────────────────────────────────────────────────────────
    private static final int BTN_CONFIG    = 0;
    private static final int BTN_WAYPOINTS = 1;
    private static final int BTN_CAPES     = 2;
    private static final int BTN_DISCORD   = 3;
    private static final int BTN_GITHUB    = 4;

    // ── Title ──────────────────────────────────────────────────────────────────
    private static final String TITLE    = "JustEnoughFakepixel";
    private float hue       = 0f;
    private float pulseTimer = 0f;
    private static final float HUE_SPEED = 0.006f;
    private static final float HUE_STEP  = 0.055f;

    // ── Particles ──────────────────────────────────────────────────────────────
    private final List<Particle> particles  = new ArrayList<>();
    private static final int     PARTICLE_COUNT = 120;
    private static final Random  RNG            = new Random();

    // ── Update ─────────────────────────────────────────────────────────────────
    private String updateVersion = null;
    private float  splashBounce  = 0f; // drives the Minecraft-style splash bounce

    // ─────────────────────────────────────────────────────────────────────────
    // Particle
    // ─────────────────────────────────────────────────────────────────────────
    private static class Particle {
        float x, y, vx, vy;
        float life, lifeSpeed;
        float hue, size;
        float spin, spinSpeed;
        float twinkle;
        int   shape; // 0=circle 1=star 2=cross 3=diamond 4=ring 5=triangle

        Particle(int w, int h) { respawn(w, h); life = RNG.nextFloat(); }

        void respawn(int w, int h) {
            x         = RNG.nextFloat() * w;
            y         = RNG.nextFloat() * h;
            double ang = RNG.nextDouble() * Math.PI * 2;
            float spd  = 0.1f + RNG.nextFloat() * 0.6f;
            vx        = (float)(Math.cos(ang) * spd);
            vy        = (float)(Math.sin(ang) * spd) - 0.2f;
            life      = 0f;
            lifeSpeed = 0.002f + RNG.nextFloat() * 0.005f;
            hue       = RNG.nextFloat();
            size      = 1.2f + RNG.nextFloat() * 4f;
            spin      = RNG.nextFloat() * (float)(Math.PI * 2);
            spinSpeed = (RNG.nextFloat() - 0.5f) * 0.08f;
            twinkle   = RNG.nextFloat() * (float)(Math.PI * 2);
            shape     = RNG.nextInt(6);
        }

        void tick(int w, int h) {
            x += vx; y += vy;
            life    += lifeSpeed;
            hue      = (hue + 0.004f) % 1f;
            spin     = (spin + spinSpeed) % (float)(Math.PI * 2);
            twinkle  = (twinkle + 0.07f) % (float)(Math.PI * 2);
            if (life >= 1f || x < -20 || x > w + 20 || y < -20 || y > h + 20) respawn(w, h);
        }

        float alpha() {
            if (life < 0.15f) return life / 0.15f;
            if (life > 0.75f) return 1f - (life - 0.75f) / 0.25f;
            return 1f;
        }

        float currentSize() { return size * (0.8f + 0.2f * (float)Math.sin(twinkle)); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // initGui
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public void initGui() {
        super.initGui();
        particles.clear();
        for (int i = 0; i < PARTICLE_COUNT; i++) particles.add(new Particle(width, height));

        UpdateData upd = RepoHandler.get(JefRepo.KEY_UPDATE, UpdateData.class, new UpdateData());
        if (isNewer(JefMod.VERSION, upd.version)) updateVersion = upd.version;

        int cx = width / 2, btnW = 200, btnH = 20, gap = 5;
        int startY = height / 2 - 10;
        buttonList.add(new GuiButton(BTN_CONFIG,    cx - btnW/2, startY,                    btnW, btnH, "Config"));
        buttonList.add(new GuiButton(BTN_WAYPOINTS, cx - btnW/2, startY + (btnH+gap),       btnW, btnH, "Waypoints"));
        buttonList.add(new GuiButton(BTN_CAPES,     cx - btnW/2, startY + (btnH+gap) * 2,   btnW, btnH, "Cape Selector"));
        int sw = 95;
        buttonList.add(new GuiButton(BTN_DISCORD, width - sw*2 - 8, height - btnH - 6, sw, btnH, "Discord"));
        buttonList.add(new GuiButton(BTN_GITHUB,  width - sw   - 4, height - btnH - 6, sw, btnH, "GitHub"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // drawScreen
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        hue          = (hue + HUE_SPEED) % 1f;
        pulseTimer   = (pulseTimer + 0.025f) % (float)(Math.PI * 2);
        splashBounce = (splashBounce + 0.05f) % (float)(Math.PI * 2);

        // ── Particles ─────────────────────────────────────────────────────────
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE); // additive = glow
        GlStateManager.disableDepth();

        for (Particle p : particles) {
            p.tick(width, height);
            Color c = Color.getHSBColor(p.hue, 0.75f, 1f);
            GL11.glColor4f(c.getRed()/255f, c.getGreen()/255f, c.getBlue()/255f, p.alpha() * 0.85f);
            drawParticle(p);
        }

        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableTexture2D();
        GlStateManager.enableDepth();
        GlStateManager.color(1f, 1f, 1f, 1f);

        // ── Rainbow bobbing title ─────────────────────────────────────────────
        float scale   = Math.max(1.8f, Math.min(3.0f, width / 140f));
        float scaledW = fontRendererObj.getStringWidth(TITLE) * scale;
        float titleX  = (width - scaledW) / 2f;
        float titleY  = height * 0.13f;

        GlStateManager.pushMatrix();
        GlStateManager.translate(titleX, titleY, 0);
        GlStateManager.scale(scale, scale, 1f);
        GlStateManager.enableBlend();

        int curX = 0;
        for (int i = 0; i < TITLE.length(); i++) {
            Color  c   = Color.getHSBColor((hue + i * HUE_STEP) % 1f, 0.85f, 1f);
            float  bob = (float)(Math.sin(pulseTimer + i * 0.4f) * 1.5f);
            GlStateManager.pushMatrix();
            GlStateManager.translate(0, bob, 0);
            String ch = String.valueOf(TITLE.charAt(i));
            fontRendererObj.drawStringWithShadow(ch, curX, 0,
                    (0xFF << 24) | (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue());
            GlStateManager.popMatrix();
            curX += fontRendererObj.getStringWidth(ch);
        }
        GlStateManager.popMatrix();

        // ── Version subtitle ──────────────────────────────────────────────────
        String ver = "v" + JefMod.VERSION;
        fontRendererObj.drawStringWithShadow(ver,
                (width - fontRendererObj.getStringWidth(ver)) / 2f,
                titleY + fontRendererObj.FONT_HEIGHT * scale + 3,
                0xAAAAAA);

        // ── Splash "Update!" text — Minecraft style, only when outdated ───────
        if (updateVersion != null) {
            String splash = "Update " + updateVersion + "!";
            float splashScale = scale * 0.6f;
            // Bounce: same as Minecraft's splash — scales between 95% and 100%
            float bounce = 1f - (float)(Math.abs(Math.sin(splashBounce)) * 0.05f);
            // Anchor to top-right end of the title
            float anchorX = titleX + scaledW + 12f;
            float anchorY = titleY + (fontRendererObj.FONT_HEIGHT * scale * 0.25f) + 20f;
            float splashW = fontRendererObj.getStringWidth(splash) * splashScale * bounce;
            float splashH = fontRendererObj.FONT_HEIGHT            * splashScale * bounce;

            GlStateManager.pushMatrix();
            GlStateManager.translate(anchorX, anchorY, 0);
            GlStateManager.rotate(-10f, 0, 0, 1); // fixed -20° tilt like MC
            GlStateManager.scale(splashScale * bounce, splashScale * bounce, 1f);
            GlStateManager.enableBlend();
            fontRendererObj.drawStringWithShadow(splash,
                    -fontRendererObj.getStringWidth(splash) / 2f, -fontRendererObj.FONT_HEIGHT / 2f,
                    0x46923c); // Minecraft splash yellow
            GlStateManager.popMatrix();
        }

        // ── Buttons ───────────────────────────────────────────────────────────
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Particle drawing
    // ─────────────────────────────────────────────────────────────────────────
    private void drawParticle(Particle p) {
        float r = p.currentSize();
        GL11.glPushMatrix();
        GL11.glTranslatef(p.x, p.y, 0);
        GL11.glRotatef((float)Math.toDegrees(p.spin), 0, 0, 1);
        switch (p.shape) {
            case 0: drawCircle(r);   break;
            case 1: drawStar(r);     break;
            case 2: drawCross(r);    break;
            case 3: drawDiamond(r);  break;
            case 4: drawRing(r);     break;
            case 5: drawTriangle(r); break;
        }
        GL11.glPopMatrix();
    }

    private void drawCircle(float r) {
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(0, 0);
        for (int i = 0; i <= 16; i++) {
            double a = i * Math.PI * 2 / 16;
            GL11.glVertex2f((float)(Math.cos(a)*r), (float)(Math.sin(a)*r));
        }
        GL11.glEnd();
    }

    private void drawStar(float r) {
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(0, 0);
        for (int i = 0; i <= 10; i++) {
            double a = i * Math.PI * 2 / 10 - Math.PI / 2;
            float rad = (i % 2 == 0) ? r : r * 0.4f;
            GL11.glVertex2f((float)(Math.cos(a)*rad), (float)(Math.sin(a)*rad));
        }
        GL11.glEnd();
    }

    private void drawCross(float r) {
        float t = r * 0.28f;
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(-r,-t); GL11.glVertex2f(r,-t); GL11.glVertex2f(r,t); GL11.glVertex2f(-r,t);
        GL11.glVertex2f(-t,-r); GL11.glVertex2f(t,-r); GL11.glVertex2f(t,r); GL11.glVertex2f(-t,r);
        GL11.glEnd();
    }

    private void drawDiamond(float r) {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(0,-r); GL11.glVertex2f(r*0.6f,0); GL11.glVertex2f(0,r); GL11.glVertex2f(-r*0.6f,0);
        GL11.glEnd();
    }

    private void drawRing(float r) {
        float inner = r * 0.55f;
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        for (int i = 0; i <= 20; i++) {
            double a = i * Math.PI * 2 / 20;
            float cos = (float)Math.cos(a), sin = (float)Math.sin(a);
            GL11.glVertex2f(cos*inner, sin*inner);
            GL11.glVertex2f(cos*r,     sin*r);
        }
        GL11.glEnd();
    }

    private void drawTriangle(float r) {
        GL11.glBegin(GL11.GL_TRIANGLES);
        for (int i = 0; i < 3; i++) {
            double a = i * Math.PI * 2 / 3 - Math.PI / 2;
            GL11.glVertex2f((float)(Math.cos(a)*r), (float)(Math.sin(a)*r));
        }
        GL11.glEnd();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Button actions
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case BTN_CONFIG:    JefConfig.openGui();                            break;
            case BTN_WAYPOINTS: JefConfig.openWaypointGroupGui();               break;
            case BTN_CAPES:     JefConfig.screenToOpen = new CapeSelectorGUI(); break;
            case BTN_DISCORD:   tryBrowse("https://discord.gg/HHf5yqSy9R");    break;
            case BTN_GITHUB:    tryBrowse("https://github.com/LGatodu47/JustEnoughFakepixel"); break;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────
    private static boolean isNewer(String current, String latest) {
        if (latest == null) return false;
        String[] c = current.replaceAll("[^0-9.]", "").split("\\.");
        String[] l = latest .replaceAll("[^0-9.]", "").split("\\.");
        int len = Math.max(c.length, l.length);
        for (int i = 0; i < len; i++) {
            int cv = i < c.length ? parseSafe(c[i]) : 0;
            int lv = i < l.length ? parseSafe(l[i]) : 0;
            if (lv > cv) return true;
            if (lv < cv) return false;
        }
        return false;
    }

    private static int parseSafe(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }

    private void tryBrowse(String url) {
        try { java.awt.Desktop.getDesktop().browse(new URI(url)); } catch (Exception ignored) {}
    }

    @Override public boolean doesGuiPauseGame() { return false; }
    @Override public void onGuiClosed()          { particles.clear(); }
}