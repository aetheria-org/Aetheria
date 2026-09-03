// SPDX-License-Identifier: LGPL-3.0-only
// Derived from MoulConfig (https://github.com/NotEnoughUpdates/MoulConfig)

package io.hamlook.aetheria.core.moulconfig.gui;

import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.utils.LerpUtils;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.MouseCompat;
import io.hamlook.aetheria.utils.render.RenderUtils;
import net.minecraft.util.ResourceLocation;

import java.util.function.Consumer;

public class GuiElementBoolean extends GuiElement {

    public int x, y;
    private boolean value;
    private final int clickRadius;
    private final Consumer<Boolean> toggleCallback;

    private boolean previewValue;
    private int animation = 0;
    private long lastMillis = 0;

    private static final int xSize = 48;
    private static final int ySize = 14;

    public GuiElementBoolean(int x, int y, boolean value, Consumer<Boolean> toggleCallback) {
        this(x, y, value, 0, toggleCallback);
    }

    public GuiElementBoolean(int x, int y, boolean value, int clickRadius, Consumer<Boolean> toggleCallback) {
        this.x = x; this.y = y;
        this.value = value;
        this.previewValue = value;
        this.clickRadius = clickRadius;
        this.toggleCallback = toggleCallback;
        this.lastMillis = System.currentTimeMillis();
        if (value) animation = 36;
    }

    @Override
    public void render() {
        GlStateManagerCompat.color(1, 1, 1, 1);
        MinecraftCompat.getMinecraft().getTextureManager().bindTexture(Resources.BAR);
        RenderUtils.drawTexturedRect(x, y, xSize, ySize);

        ResourceLocation buttonLoc = Resources.ON;
        long currentMillis = System.currentTimeMillis();
        long deltaMillis = currentMillis - lastMillis;
        lastMillis = currentMillis;

        boolean passedLimit = false;
        if (previewValue != value) {
            passedLimit = (previewValue && animation > 12) || (!previewValue && animation < 24);
        }
        if (previewValue != passedLimit) animation += deltaMillis / 10;
        else animation -= deltaMillis / 10;
        lastMillis -= deltaMillis % 10;

        if (previewValue == value) {
            animation = Math.max(0, Math.min(36, animation));
        } else if (!passedLimit) {
            animation = previewValue ? Math.max(0, Math.min(12, animation)) : Math.max(24, Math.min(36, animation));
        } else {
            animation = previewValue ? Math.max(12, animation) : Math.min(24, animation);
        }

        int anim = (int) (LerpUtils.sigmoidZeroOne(this.animation / 36f) * 36);
        if      (anim < 3)  buttonLoc = Resources.OFF;
        else if (anim < 13) buttonLoc = Resources.ONE;
        else if (anim < 23) buttonLoc = Resources.TWO;
        else if (anim < 33) buttonLoc = Resources.THREE;

        MinecraftCompat.getMinecraft().getTextureManager().bindTexture(buttonLoc);
        RenderUtils.drawTexturedRect(x + anim, y, 12, 14);
    }

    @Override
    public boolean mouseInput(int mouseX, int mouseY) {
        if (mouseX > x - clickRadius && mouseX < x + xSize + clickRadius &&
                mouseY > y - clickRadius && mouseY < y + ySize + clickRadius) {
            if (MouseCompat.getEventButton() == 0) {
                if (MouseCompat.getEventButtonState()) {
                    previewValue = !value;
                } else if (previewValue == !value) {
                    value = !value;
                    toggleCallback.accept(value);
                }
            }
        } else {
            previewValue = value;
        }
        return false;
    }

    @Override
    public boolean keyboardInput() { return false; }
}