package io.hamlook.aetheria;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.chat.globalchat.ui.ChatUI;
import io.hamlook.aetheria.utils.render.RenderUtils;
import io.hamlook.aetheria.features.capes.ui.CapeSelectorGUI;
import io.hamlook.aetheria.repo.ATHRRepo;
import io.hamlook.aetheria.repo.RepoHandler;
import io.hamlook.aetheria.repo.data.UpdateData;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class OptionsMenu extends GuiScreen {

    private static final String TITLE = "Aetheria's Skyblock Mod";
    private static final Random RNG = new Random();
    private static final int PARTICLE_COUNT = 55;

    // Main action buttons
    private static final int BTN_W = 180;
    private static final int BTN_H = 22;
    private static final int BTN_GAP = 6;
    private static final String[] BTN_LABELS = {"⚙ Config", "Waypoints", "Cape Selector", "Chat Filters", "Global Chat"};

    // Social icon strip
    private static final int ICON_SIZE = 24;
    private static final int ICON_GAP = 10;
    private static final ResourceLocation[] SOCIAL_ICONS = {Resources.DISCORD, Resources.GITHUB, Resources.MODRINTH, Resources.SKYATLAS};
    private static final String[] SOCIAL_URLS = {"https://discord.gg/HHf5yqSy9R", "https://github.com/aetheria-org/Aetheria", "https://modrinth.com/mod/aetheriamod","https://skyatlas.lol"};

    private final List<Particle> particles = new ArrayList<>();
    private float globalTime = 0f;
    private float openProgress = 0f;
    private float splashBounce = 0f;
    private String updateVersion = null;
    private float updateButtonX = -1, updateButtonY = -1, updateButtonW = -1, updateButtonH = -1;

    private static boolean isNewer(String latest) {
        if (latest == null) return false;
        String[] c = Aetheria.VERSION.replaceAll("[^0-9.]", "").split("\\.");
        String[] l = latest.replaceAll("[^0-9.]", "").split("\\.");
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
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        openProgress = 0f;
        particles.clear();
        for (int i = 0; i < PARTICLE_COUNT; i++) particles.add(new Particle(width, height));

        UpdateData upd = RepoHandler.get(ATHRRepo.KEY_UPDATE, UpdateData.class, new UpdateData());
        if (isNewer(upd.version)) updateVersion = upd.version;

    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        globalTime += 0.018f;
        splashBounce = (splashBounce + 0.02f) % (float) (Math.PI * 2);
        openProgress = Math.min(1f, openProgress + 0.045f);

        drawDefaultBackground();
        drawRect(0, 0, width, height, 0x90000000);

        // Particles (additive blend)
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableDepth();
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ONE);
        drawSoftBloom(width / 2f, height / 2f, Math.min(width, height) * 0.55f);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        for (Particle p : particles) {
            p.tick(width, height);
            Color col = Color.getHSBColor(p.hue, 0.70f, 1f);
            float alpha = p.alpha() * openProgress * 0.65f;
            GL11.glColor4f(col.getRed() / 255f, col.getGreen() / 255f, col.getBlue() / 255f, alpha);
            drawDot(p.x, p.y, p.currentSize());
        }
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.enableDepth();
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.color(1f, 1f, 1f, 1f);

        // Animated title
        float scale = Math.max(1.8f, Math.min(2.8f, width / 155f));
        float scaledW = fontRendererObj.getStringWidth(TITLE) * scale;
        float titleX = (width - scaledW) / 2f;
        float titleY = height * 0.22f;

        GlStateManager.pushMatrix();
        GlStateManager.translate(titleX, titleY, 0f);
        GlStateManager.scale(scale, scale, 1f);
        int curX = 0;
        for (int i = 0; i < TITLE.length(); i++) {
            float t = (float) i / Math.max(1, TITLE.length() - 1);
            float hue = 0.53f + t * 0.22f;
            float wave = globalTime * 1.4f - t * 3.5f;
            float shimmer = (float) (Math.sin(wave) * 0.5f + 0.5f);
            Color col = Color.getHSBColor(hue, 0.75f - shimmer * 0.20f, 0.85f + shimmer * 0.15f);
            int argb = (0xFF << 24) | (col.getRed() << 16) | (col.getGreen() << 8) | col.getBlue();
            float bob = (float) (Math.sin(globalTime * 0.9f + i * 0.35f) * 0.6f);
            GlStateManager.pushMatrix();
            GlStateManager.translate(0f, bob, 0f);
            String ch = String.valueOf(TITLE.charAt(i));
            fontRendererObj.drawStringWithShadow(ch, curX, 0, argb);
            GlStateManager.popMatrix();
            curX += fontRendererObj.getStringWidth(ch);
        }
        GlStateManager.popMatrix();

        // Version string
        String ver = "v" + Aetheria.VERSION;
        float verX = (width - fontRendererObj.getStringWidth(ver)) / 2f;
        float verY = titleY + fontRendererObj.FONT_HEIGHT * scale + 5f;
        fontRendererObj.drawStringWithShadow(ver, verX, verY, blendColor(openProgress));

        // Update badge
        if (updateVersion != null) drawUpdateBadge();

        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.color(1f, 1f, 1f, 1f);

        int btnX = width / 2 - BTN_W / 2;
        int btnBaseY = height / 2 + 10;
        for (int i = 0; i < BTN_LABELS.length; i++) {
            int by = btnBaseY + i * (BTN_H + BTN_GAP);
            boolean hov = mouseX >= btnX && mouseX <= btnX + BTN_W && mouseY >= by && mouseY <= by + BTN_H;

            RenderUtils.drawFloatingRectDark(btnX, by, BTN_W, BTN_H, false);
            if (hov) drawRect(btnX, by, btnX + BTN_W, by + BTN_H, 0x18FFFFFF);

            int labelColor = hov ? 0x00C8C8 : 0xA0A0A0;
            String lbl = BTN_LABELS[i];
            fontRendererObj.drawStringWithShadow(lbl, btnX + (BTN_W - fontRendererObj.getStringWidth(lbl)) / 2f, by + (BTN_H - fontRendererObj.FONT_HEIGHT) / 2f, labelColor);
        }

        int iconY = height - ICON_SIZE - 8;

        for (int i = 0; i < SOCIAL_ICONS.length; i++) {
            int ix = iconStripX() + i * (ICON_SIZE + ICON_GAP);
            boolean hov = mouseX >= ix && mouseX <= ix + ICON_SIZE && mouseY >= iconY && mouseY <= iconY + ICON_SIZE;

            GlStateManager.enableBlend();
            GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
            GlStateManager.color(1f, 1f, 1f, hov ? 1f : 0.65f);
            mc.getTextureManager().bindTexture(SOCIAL_ICONS[i]);
            RenderUtils.drawTexturedRect(ix, iconY, ICON_SIZE, ICON_SIZE, GL11.GL_LINEAR);
        }

        GlStateManager.color(1f, 1f, 1f, 1f);
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        // Update badge
        if (updateVersion != null && mouseX >= updateButtonX && mouseX <= updateButtonX + updateButtonW && mouseY >= updateButtonY && mouseY <= updateButtonY + updateButtonH) {
            tryBrowse("https://modrinth.com/mod/aetheriamod");
            return;
        }

        // Main buttons
        int btnX = width / 2 - BTN_W / 2;
        int btnBaseY = height / 2 + 10;
        for (int i = 0; i < BTN_LABELS.length; i++) {
            int by = btnBaseY + i * (BTN_H + BTN_GAP);
            if (mouseX >= btnX && mouseX <= btnX + BTN_W && mouseY >= by && mouseY <= by + BTN_H) {
                switch (i) {
                    case 0:
                        ATHRConfig.openGui();
                        break;
                    case 1:
                        ATHRConfig.openWaypointGroupGui();
                        break;
                    case 2:
                        ATHRConfig.screenToOpen = new CapeSelectorGUI();
                        break;
                    case 3:
                        ATHRConfig.openChatFilterUI();
                        break;
                    case 4:
                        ATHRConfig.screenToOpen = new ChatUI();
                        break;
                }
                return;
            }
        }

        // Social icons
        int iconBaseX = iconStripX();
        int iconY = height - ICON_SIZE - 8;
        for (int i = 0; i < SOCIAL_URLS.length; i++) {
            int ix = iconBaseX + i * (ICON_SIZE + ICON_GAP);
            if (mouseX >= ix && mouseX <= ix + ICON_SIZE && mouseY >= iconY && mouseY <= iconY + ICON_SIZE) {
                tryBrowse(SOCIAL_URLS[i]);
                return;
            }
        }
    }

    private int iconStripX() {
        return (width - (ICON_SIZE * SOCIAL_ICONS.length + ICON_GAP * (SOCIAL_ICONS.length - 1))) / 2;
    }

    private void drawUpdateBadge() {
        String updateText = "✦" + updateVersion + " available";
        float titleScale = Math.max(1.8f, Math.min(2.8f, width / 155f));
        float splashScale = titleScale * 0.52f;
        float bounce = 1f - (float) (Math.abs(Math.sin(splashBounce)) * 0.06f);
        float scaledW = fontRendererObj.getStringWidth(TITLE) * titleScale;
        float anchorX = (width - scaledW) / 2f + scaledW + 10f;
        float anchorY = height * 0.22f + fontRendererObj.FONT_HEIGHT * titleScale * 0.3f + 15f;

        float renderedW = fontRendererObj.getStringWidth(updateText) * splashScale * bounce;
        float renderedH = fontRendererObj.FONT_HEIGHT * splashScale * bounce;
        updateButtonX = anchorX - renderedW / 2f;
        updateButtonY = anchorY - renderedH / 2f;
        updateButtonW = renderedW;
        updateButtonH = renderedH;

        GlStateManager.pushMatrix();
        GlStateManager.translate(anchorX, anchorY, 0f);
        GlStateManager.rotate(-12f, 0f, 0f, 1f);
        GlStateManager.scale(splashScale * bounce, splashScale * bounce, 1f);
        fontRendererObj.drawStringWithShadow(updateText, -fontRendererObj.getStringWidth(updateText) / 2f, -fontRendererObj.FONT_HEIGHT / 2f, 0xFF00FFB3);
        GlStateManager.popMatrix();
    }

    private void drawDot(float cx, float cy, float r) {
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(cx, cy);
        int segs = Math.max(8, (int) (r * 4));
        for (int i = 0; i <= segs; i++) {
            double a = i * Math.PI * 2 / segs;
            GL11.glVertex2f(cx + (float) (Math.cos(a) * r), cy + (float) (Math.sin(a) * r));
        }
        GL11.glEnd();
    }

    private void drawSoftBloom(float cx, float cy, float r) {
        Color inner = Color.getHSBColor((float) 0.56, 0.40f, 0.20f);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glColor4f(inner.getRed() / 255f, inner.getGreen() / 255f, inner.getBlue() / 255f, (float) 0.04);
        GL11.glVertex2f(cx, cy);
        GL11.glColor4f(0f, 0f, 0f, 0f);
        for (int i = 0; i <= 48; i++) {
            double a = i * Math.PI * 2 / 48;
            GL11.glVertex2f(cx + (float) (Math.cos(a) * r), cy + (float) (Math.sin(a) * r));
        }
        GL11.glEnd();
    }

    private int blendColor(float alphaMul) {
        int a = (int) (((-11886936 >> 24) & 0xFF) * alphaMul);
        return (-11886936 & 0x00FFFFFF) | (a << 24);
    }

    private void tryBrowse(String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception ignored) {
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void onGuiClosed() {
        particles.clear();
    }

    private static class Particle {
        float x, y, vx, vy, life, lifeSpeed, hue, size, twinkle, twinkleSpeed;

        Particle(int w, int h) {
            scatter(w, h);
            life = RNG.nextFloat();
        }

        void scatter(int w, int h) {
            x = RNG.nextFloat() * w;
            y = RNG.nextFloat() * h;
            double angle = RNG.nextDouble() * Math.PI * 2;
            float speed = 0.05f + RNG.nextFloat() * 0.25f;
            vx = (float) (Math.cos(angle) * speed);
            vy = (float) (Math.sin(angle) * speed) - 0.08f;
            life = 0f;
            lifeSpeed = 0.0015f + RNG.nextFloat() * 0.003f;
            hue = 0.48f + RNG.nextFloat() * 0.22f;
            size = 0.8f + RNG.nextFloat() * 2.8f;
            twinkle = RNG.nextFloat() * (float) (Math.PI * 2);
            twinkleSpeed = 0.03f + RNG.nextFloat() * 0.05f;
        }

        void tick(int w, int h) {
            x += vx;
            y += vy;
            life += lifeSpeed;
            twinkle = (twinkle + twinkleSpeed) % (float) (Math.PI * 2);
            if (life >= 1f || x < -30 || x > w + 30 || y < -30 || y > h + 30) scatter(w, h);
        }

        float alpha() {
            if (life < 0.12f) return life / 0.12f;
            if (life > 0.80f) return 1f - (life - 0.80f) / 0.20f;
            return 1f;
        }

        float currentSize() {
            return size * (0.88f + 0.12f * (float) Math.sin(twinkle));
        }
    }
}