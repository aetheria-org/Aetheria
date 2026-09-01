// SPDX-License-Identifier: LGPL-3.0-only
// Derived from MoulConfig (https://github.com/NotEnoughUpdates/MoulConfig)

package io.hamlook.aetheria.core.moulconfig.editors;

import io.hamlook.aetheria.utils.KeybindHelper;
import io.hamlook.aetheria.utils.Position;
import io.hamlook.aetheria.utils.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.function.IntSupplier;

public class GuiPositionEditor extends GuiScreen {

    private final Position position;
    private final Position originalPosition;
    private final IntSupplier elementWidth;
    private final IntSupplier elementHeight;
    private final Runnable renderCallback;
    private final Runnable positionChangedCallback;
    private final Runnable closedCallback;
    private boolean clicked = false;
    private int grabbedX = 0;
    private int grabbedY = 0;

    private int guiScaleOverride = -1;
    private float overlayScale = 1f;
    private GuiScreen parentScreen = null;

    public GuiPositionEditor(Position position, IntSupplier elementWidth, IntSupplier elementHeight, Runnable renderCallback, Runnable positionChangedCallback, Runnable closedCallback) {
        this.position = position;
        this.originalPosition = position.clone();
        this.elementWidth = elementWidth;
        this.elementHeight = elementHeight;
        this.renderCallback = renderCallback;
        this.positionChangedCallback = positionChangedCallback;
        this.closedCallback = closedCallback;
    }

    public GuiPositionEditor(Position position, int elementWidth, int elementHeight, Runnable renderCallback, Runnable positionChangedCallback, Runnable closedCallback) {
        this(position, () -> elementWidth, () -> elementHeight, renderCallback, positionChangedCallback, closedCallback);
    }

    public GuiPositionEditor withScale(int scale) {
        this.guiScaleOverride = scale;
        return this;
    }

    public GuiPositionEditor withOverlayScale(float scale) {
        this.overlayScale = scale;
        return this;
    }

    public GuiPositionEditor withParent(GuiScreen parent) {
        this.parentScreen = parent;
        return this;
    }

    private int scaledW() { return (int)(elementWidth.getAsInt()  * overlayScale); }
    private int scaledH() { return (int)(elementHeight.getAsInt() * overlayScale); }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        ScaledResolution scaledResolution;
        if (guiScaleOverride >= 0) {
            scaledResolution = Utils.pushGuiScale(guiScaleOverride);
        } else {
            scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        }

        this.width = scaledResolution.getScaledWidth();
        this.height = scaledResolution.getScaledHeight();
        int[] coords = KeybindHelper.getMouseCoords(this.width, this.height);
        mouseX = coords[0];
        mouseY = coords[1];

        drawDefaultBackground();

        if (clicked) {
            grabbedX += position.moveX(mouseX - grabbedX, scaledW(), scaledResolution);
            grabbedY += position.moveY(mouseY - grabbedY, scaledH(), scaledResolution);
        }

        renderCallback.run();

        int x = position.getAbsX(scaledResolution, scaledW());
        int y = position.getAbsY(scaledResolution, scaledH());

        if (position.isCenterX()) x -= scaledW() / 2;
        if (position.isCenterY()) y -= scaledH() / 2;
        Gui.drawRect(x, y, x + scaledW(), y + scaledH(), 0x80404040);

        if (guiScaleOverride >= 0) {
            Utils.pushGuiScale(-1);
        }

        scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        Utils.drawStringCentered("Position Editor", Minecraft.getMinecraft().fontRendererObj, scaledResolution.getScaledWidth() / 2, 8, true, 0xffffff);
        Utils.drawStringCentered("R to Reset - Arrow keys/mouse to move", Minecraft.getMinecraft().fontRendererObj, (float) scaledResolution.getScaledWidth() / 2, 18, true, 0xffffff);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseButton == 0) {
            ScaledResolution scaledResolution;
            if (guiScaleOverride >= 0) {
                scaledResolution = Utils.pushGuiScale(guiScaleOverride);
            } else {
                scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
            }
            int[] coords = KeybindHelper.getMouseCoords(width, height);
            mouseX = coords[0];
            mouseY = coords[1];

            int x = position.getAbsX(scaledResolution, scaledW());
            int y = position.getAbsY(scaledResolution, scaledH());
            if (position.isCenterX()) x -= scaledW() / 2;
            if (position.isCenterY()) y -= scaledH() / 2;

            if (mouseX >= x && mouseY >= y && mouseX <= x + scaledW() && mouseY <= y + scaledH()) {
                clicked = true;
                grabbedX = mouseX;
                grabbedY = mouseY;
            }

            if (guiScaleOverride >= 0) {
                Utils.pushGuiScale(-1);
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        Keyboard.enableRepeatEvents(true);

        if (keyCode == Keyboard.KEY_ESCAPE && parentScreen != null) {
            closedCallback.run();
            mc.displayGuiScreen(parentScreen);
            return;
        }

        if (keyCode == Keyboard.KEY_R) {
            position.set(originalPosition);
        } else if (!clicked) {
            boolean shiftHeld = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
            int dist = shiftHeld ? 10 : 1;
            if (keyCode == Keyboard.KEY_DOWN) {
                position.moveY(dist, scaledH(), new ScaledResolution(Minecraft.getMinecraft()));
            } else if (keyCode == Keyboard.KEY_UP) {
                position.moveY(-dist, scaledH(), new ScaledResolution(Minecraft.getMinecraft()));
            } else if (keyCode == Keyboard.KEY_LEFT) {
                position.moveX(-dist, scaledW(), new ScaledResolution(Minecraft.getMinecraft()));
            } else if (keyCode == Keyboard.KEY_RIGHT) {
                position.moveX(dist, scaledW(), new ScaledResolution(Minecraft.getMinecraft()));
            }
        }

        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        clicked = false;
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);

        if (clicked) {
            ScaledResolution scaledResolution;
            if (guiScaleOverride >= 0) {
                scaledResolution = Utils.pushGuiScale(guiScaleOverride);
            } else {
                scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
            }
            int[] coords = KeybindHelper.getMouseCoords(width, height);
            mouseX = coords[0];
            mouseY = coords[1];

            grabbedX += position.moveX(mouseX - grabbedX, scaledW(), scaledResolution);
            grabbedY += position.moveY(mouseY - grabbedY, scaledH(), scaledResolution);
            positionChangedCallback.run();

            if (guiScaleOverride >= 0) {
                Utils.pushGuiScale(-1);
            }
        }
    }
}