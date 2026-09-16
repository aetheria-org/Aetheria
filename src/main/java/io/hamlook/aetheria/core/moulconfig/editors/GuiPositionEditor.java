// SPDX-License-Identifier: LGPL-3.0-only
// Derived from MoulConfig (https://github.com/NotEnoughUpdates/MoulConfig)

package io.hamlook.aetheria.core.moulconfig.editors;

import io.hamlook.aetheria.utils.KeybindHelper;
import io.hamlook.aetheria.utils.Position;
import io.hamlook.aetheria.utils.Utils;
import io.hamlook.aetheria.utils.compat.AetheriaBaseScreen;
import io.hamlook.aetheria.utils.compat.DrawContextUtils;
import io.hamlook.aetheria.utils.compat.GuiScreenUtils;
import io.hamlook.aetheria.utils.compat.KeyboardCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import java.util.function.IntSupplier;

public class GuiPositionEditor extends AetheriaBaseScreen {

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
    public void guiClosed() {
    }

    @Override
    public void onDrawScreen(int mouseX, int mouseY, float partialTicks) {
        int scaledW, scaledH;
        if (guiScaleOverride >= 0) {
            int[] pushDims = Utils.pushGuiScale(guiScaleOverride);
            scaledW = pushDims[0];
            scaledH = pushDims[1];
        } else {
            scaledW = GuiScreenUtils.getScaledWindowWidth();
            scaledH = GuiScreenUtils.getScaledWindowHeight();
        }

        this.width = scaledW;
        this.height = scaledH;
        int[] coords = KeybindHelper.getMouseCoords(this.width, this.height);
        mouseX = coords[0];
        mouseY = coords[1];

        drawDefaultBackground();

        if (clicked) {
            grabbedX += position.moveX(mouseX - grabbedX, scaledW(), scaledW);
            grabbedY += position.moveY(mouseY - grabbedY, scaledH(), scaledH);
        }

        renderCallback.run();

        int x = position.getAbsX(scaledW, scaledW());
        int y = position.getAbsY(scaledH, scaledH());

        if (position.isCenterX()) x -= scaledW() / 2;
        if (position.isCenterY()) y -= scaledH() / 2;
        DrawContextUtils.fill(x, y, x + scaledW(), y + scaledH(), 0x80404040);

        if (guiScaleOverride >= 0) {
            Utils.pushGuiScale(-1);
        }

        int textW = GuiScreenUtils.getScaledWindowWidth();
        Utils.drawStringCentered("Position Editor", MinecraftCompat.getFontRenderer(), textW / 2, 8, true, 0xffffff);
        Utils.drawStringCentered("R to Reset - Arrow keys/mouse to move", MinecraftCompat.getFontRenderer(), (float) textW / 2, 18, true, 0xffffff);
    }

    @Override
    protected void onMouseClicked(int mouseX, int mouseY, int mouseButton) {

        if (mouseButton == 0) {
            int screenW = GuiScreenUtils.getScaledWindowWidth();
            int screenH = GuiScreenUtils.getScaledWindowHeight();
            int[] coords = KeybindHelper.getMouseCoords(width, height);
            mouseX = coords[0];
            mouseY = coords[1];

            int x = position.getAbsX(screenW, scaledW());
            int y = position.getAbsY(screenH, scaledH());
            if (position.isCenterX()) x -= scaledW() / 2;
            if (position.isCenterY()) y -= scaledH() / 2;

            if (mouseX >= x && mouseY >= y && mouseX <= x + scaledW() && mouseY <= y + scaledH()) {
                clicked = true;
                grabbedX = mouseX;
                grabbedY = mouseY;
            }
        }
    }

    @Override
    protected void onKeyTyped(char typedChar, int keyCode) {
        KeyboardCompat.enableRepeatEvents(true);

        if (keyCode == Keyboard.KEY_ESCAPE && parentScreen != null) {
            closedCallback.run();
            mc.displayGuiScreen(parentScreen);
            return;
        }

        if (keyCode == Keyboard.KEY_R) {
            position.set(originalPosition);
        } else if (!clicked) {
            boolean shiftHeld = KeyboardCompat.isKeyDown(Keyboard.KEY_LSHIFT) || KeyboardCompat.isKeyDown(Keyboard.KEY_RSHIFT);
            int dist = shiftHeld ? 10 : 1;
            int sw = GuiScreenUtils.getScaledWindowWidth();
            int sh = GuiScreenUtils.getScaledWindowHeight();
            if (keyCode == Keyboard.KEY_DOWN) {
                position.moveY(dist, scaledH(), sh);
            } else if (keyCode == Keyboard.KEY_UP) {
                position.moveY(-dist, scaledH(), sh);
            } else if (keyCode == Keyboard.KEY_LEFT) {
                position.moveX(-dist, scaledW(), sw);
            } else if (keyCode == Keyboard.KEY_RIGHT) {
                position.moveX(dist, scaledW(), sw);
            }
        }
    }

    @Override
    protected void onMouseReleased(int mouseX, int mouseY, int state) {
        clicked = false;
    }

    @Override
    protected void onMouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {

        if (clicked) {
            int screenW, screenH;
            if (guiScaleOverride >= 0) {
                int[] pushDims = Utils.pushGuiScale(guiScaleOverride);
                screenW = pushDims[0];
                screenH = pushDims[1];
            } else {
                screenW = GuiScreenUtils.getScaledWindowWidth();
                screenH = GuiScreenUtils.getScaledWindowHeight();
            }
            int[] coords = KeybindHelper.getMouseCoords(width, height);
            mouseX = coords[0];
            mouseY = coords[1];

            grabbedX += position.moveX(mouseX - grabbedX, scaledW(), screenW);
            grabbedY += position.moveY(mouseY - grabbedY, scaledH(), screenH);
            positionChangedCallback.run();

            if (guiScaleOverride >= 0) {
                Utils.pushGuiScale(-1);
            }
        }
    }
}