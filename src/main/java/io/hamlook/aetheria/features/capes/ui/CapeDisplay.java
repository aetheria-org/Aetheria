package io.hamlook.aetheria.features.capes.ui;

import io.hamlook.aetheria.ThemeContext;
import io.hamlook.aetheria.features.capes.Cape;
import io.hamlook.aetheria.features.capes.CapeManager;
import io.hamlook.aetheria.utils.LerpUtils;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.render.PlayerRenderer;
import net.minecraft.client.gui.FontRenderer;

/** A cape entry and its independent player-preview rotation state. */
public class CapeDisplay {

    private final LerpUtils.LerpingFloat hover = new LerpUtils.LerpingFloat(0f, 140);
    private float yaw = 180f;
    private long lastRotationTime = System.currentTimeMillis();
    private int dragStartX;
    private float dragStartYaw;
    private int modelX, modelY, modelW, modelH;
    private boolean dragging;
    private boolean manualRotation;
    public final String capeID;
    public int xPos = -1, yPos = -1, width, height;

    public CapeDisplay(Cape cape) {
        this.capeID = cape.id;
    }

    public String name() {
        Cape cape = CapeManager.getCape(capeID);
        return cape == null || cape.name == null || cape.name.trim().isEmpty() ? capeID : cape.name;
    }

    public float rotation(long now, boolean allowManualRotation) {
        long elapsed = Math.max(0L, Math.min(100L, now - lastRotationTime));
        lastRotationTime = now;
        if ((!allowManualRotation || !manualRotation) && !dragging) yaw = (yaw + elapsed * 0.01f) % 360f;
        return yaw;
    }

    public void beginRotationDrag(int mouseX) {
        dragging = true;
        dragStartX = mouseX;
        dragStartYaw = yaw;
        lastRotationTime = System.currentTimeMillis();
    }

    public void dragRotation(int mouseX) {
        if (!dragging) return;
        yaw = (dragStartYaw - (mouseX - dragStartX) * 0.45f) % 360f;
        if (yaw < 0f) yaw += 360f;
        manualRotation = true;
    }

    public void endRotationDrag() {
        dragging = false;
        lastRotationTime = System.currentTimeMillis();
    }

    public void resumeAutoRotation() {
        manualRotation = false;
        lastRotationTime = System.currentTimeMillis();
    }

    public boolean isManuallyRotated() {
        return manualRotation;
    }

    public void draw(int x, int y, int w, int h, boolean hovering, boolean equipped,
                     String playerName, float partialTicks, FontRenderer font,
                     ThemeContext.HitList hits, Runnable equipAction) {
        xPos = x;
        yPos = y;
        width = w;
        height = h;

        float target = hovering ? 1f : 0f;
        if (hover.getTarget() != target) {
            hover.resetTimer();
            hover.setTarget(target);
        }
        hover.tick();

        float hoverAmount = LerpUtils.sigmoidZeroOne(hover.getValue());
        ThemeContext.rect(x, y, w, h, ThemeContext.mixArgb(ThemeContext.PANEL, 0xFF303030,
                0.10f + hoverAmount * 0.34f));
        ThemeContext.border(x, y, w, h, equipped ? ThemeContext.ACCENT : ThemeContext.LINE);
        if (equipped || hovering) {
            ThemeContext.rect(x, y, w, 2,
                    ThemeContext.withAlpha(ThemeContext.ACCENT, equipped ? 1f : hoverAmount * 0.7f));
        }

        int modelBaseY = y + h - 54;
        int modelScale = Math.max(28, Math.min(52, (h - 56) / 2));
        ThemeContext.rect(x + 5, y + 5, w - 10, h - 54,
                ThemeContext.mixArgb(ThemeContext.PANEL, ThemeContext.BTN_HOVER, 0.25f));
        ThemeContext.border(x + 5, y + 5, w - 10, h - 54, ThemeContext.LINE);
        if (playerName != null && !playerName.isEmpty()) {
            Cape cape = CapeManager.getCape(capeID);
            if (cape != null && cape.isLoaded()) {
                drawModel(playerName, cape, x, y, w, h - 34, modelBaseY, modelScale, partialTicks, false);
            } else {
                ThemeContext.textCenter(font, "Loading cape...", x + w / 2, y + h / 2, ThemeContext.MUTED);
            }
        } else {
            ThemeContext.textCenter(font, "Join a world to preview", x + w / 2, y + h / 2, ThemeContext.MUTED);
        }

        String label = hovering ? "Click to View More" : name();
        label = ThemeContext.fit(font, label, w - 20);
        ThemeContext.textCenter(font, label, x + w / 2, y + h - 46, ThemeContext.TEXT);
        // Text rendering can restore depth testing after the player model was drawn.
        GlStateManagerCompat.disableDepth();
        if (equipAction != null) {
            if (equipped) {
                ThemeContext.button(hits, font, "Unequip", x + 8, y + h - 29, w - 16,
                        ThemeContext.FIELD_H, false, true, equipAction, null);
            } else {
                drawEquipButton(font, hits, x + 8, y + h - 29, w - 16, equipAction);
            }
        } else {
            String disabledLabel = playerName == null || playerName.isEmpty()
                    ? "Join a world to equip" : "Loading cape...";
            ThemeContext.buttonDisabled(font, disabledLabel, x + 8, y + h - 29,
                    w - 16, ThemeContext.FIELD_H);
        }
        if (equipped) {
            ThemeContext.text(font, "Worn", x + 9, y + 8, ThemeContext.ACCENT);
        }
    }

    private void drawEquipButton(FontRenderer font, ThemeContext.HitList hits,
                                 int x, int y, int w, Runnable action) {
        boolean hovered = hits.hover(x, y, w, ThemeContext.FIELD_H);
        GlStateManagerCompat.resetColor();
        GlStateManagerCompat.disableLighting();
        GlStateManagerCompat.disableDepth();
        GlStateManagerCompat.depthMask(false);
        ThemeContext.rect(x, y, w, ThemeContext.FIELD_H,
                hovered ? ThemeContext.ACCENT_HOVER : ThemeContext.ACCENT);
        ThemeContext.border(x, y, w, ThemeContext.FIELD_H, ThemeContext.ACCENT_HOVER);
        ThemeContext.textCenter(font, "Equip", x + w / 2,
                y + (ThemeContext.FIELD_H - 8) / 2, ThemeContext.ACCENT_ON);
        GlStateManagerCompat.depthMask(true);
        GlStateManagerCompat.enableDepth();
        hits.add(x, y, w, ThemeContext.FIELD_H, null, action);
    }

    public void drawModel(String playerName, Cape cape, int x, int y, int w, int h,
                          int baseY, int scale, float partialTicks, boolean allowManualRotation) {
        modelX = x + 4;
        modelY = y + 4;
        modelW = Math.max(0, w - 8);
        modelH = Math.max(0, h - 4);
        GlStateManagerCompat.enableDepth();
        try {
            PlayerRenderer.renderCapePreview(playerName, cape, x + w / 2, baseY,
                    scale, rotation(System.currentTimeMillis(), allowManualRotation), partialTicks);
        } finally {
            GlStateManagerCompat.disableDepth();
        }
    }

    public boolean contains(int mouseX, int mouseY) {
        return xPos >= 0 && mouseX >= xPos && mouseX < xPos + width
                && mouseY >= yPos && mouseY < yPos + height;
    }

    public boolean containsModel(int mouseX, int mouseY) {
        return mouseX >= modelX && mouseX < modelX + modelW
                && mouseY >= modelY && mouseY < modelY + modelH;
    }
}
