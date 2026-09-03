// SPDX-License-Identifier: LGPL-3.0-only
// Derived from MoulConfig (https://github.com/NotEnoughUpdates/MoulConfig)

package io.hamlook.aetheria.core.moulconfig.editors;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigProcessor;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;

public class GuiOptionEditorVersionDisplay extends GuiOptionEditor {

    private static final int HEIGHT = 40;

    public GuiOptionEditorVersionDisplay(ConfigProcessor.ProcessedOption option) {
        super(option);
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public void render(int x, int y, int width) {
        Minecraft mc = MinecraftCompat.getMinecraft();

        RenderUtils.drawFloatingRectDark(x, y, width, HEIGHT, true);

        String versionText = "v" + Aetheria.VERSION;

        int fontHeight = mc.fontRendererObj.FONT_HEIGHT;
        int scaledHeight = fontHeight * 2;

        // ── CENTERED 2x VERSION TEXT ─────────────────
        GlStateManagerCompat.pushMatrix();

        int centerX = x + width / 2;
        int centerY = y + (HEIGHT - scaledHeight) / 2;

        GlStateManagerCompat.translate(centerX, centerY, 0);
        GlStateManagerCompat.scale(2f, 2f, 1f);

        int textWidth = mc.fontRendererObj.getStringWidth(versionText);

        mc.fontRendererObj.drawString(
                versionText,
                -textWidth / 2,
                0,
                0x55FF55,
                false
        );

        GlStateManagerCompat.popMatrix();
    }

    @Override
    public boolean mouseInput(int x, int y, int width, int mouseX, int mouseY) {
        return false;
    }

    @Override
    public boolean keyboardInput() {
        return false;
    }
}