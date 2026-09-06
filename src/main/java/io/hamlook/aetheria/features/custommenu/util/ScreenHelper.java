package io.hamlook.aetheria.features.custommenu.util;

import io.hamlook.aetheria.utils.compat.GuiScreenUtils;
import lombok.Getter;

public class ScreenHelper {
    private static int screenWidth;
    private static int screenHeight;
    @Getter
    private static float scaleFactor;

    public static void updateScreenDimensions(int width, int height) {
        // GuiScreen resize callbacks can be supplied with display-sized values by
        // compatibility layers. Vanilla GuiScreen layouts use ScaledResolution,
        // so always establish CMM's coordinate space from the same source.
        int scaledWidth = GuiScreenUtils.getScaledWindowWidth();
        int scaledHeight = GuiScreenUtils.getScaledWindowHeight();
        screenWidth = scaledWidth > 0 ? scaledWidth : width;
        screenHeight = scaledHeight > 0 ? scaledHeight : height;
        // Keep this factor neutral. Minecraft's GUI projection already applies
        // the selected GUI scale when these logical coordinates are rendered.
        scaleFactor = 1.0f;
    }

    public static int getWidth() {
        return screenWidth;
    }

    public static int getHeight() {
        return screenHeight;
    }

    public static int getCenterX() {
        return screenWidth / 2;
    }

    public static int getCenterY() {
        return screenHeight / 2;
    }

    public static int getScaledWidth() {
        return screenWidth;
    }

    public static int getScaledHeight() {
        return screenHeight;
    }

    public static int getRelativeX(float percentage) {
        return (int) (screenWidth * percentage);
    }

    public static int getRelativeY(float percentage) {
        return (int) (screenHeight * percentage);
    }

    // Cartesian: origin = anchor point, +X = right, +Y = up
    // Screen coords: (0,0) = top-left, +X = right, +Y = down
    // Convert: screenX = anchorScreenX + offsetX, screenY = anchorScreenY - offsetY
    public static int getAnchorScreenX(Anchor anchor) {
        switch (anchor) {
            case LEFT:
                return 0;
            case CENTER:
                return getCenterX();
            case RIGHT:
                return getWidth();
            default:
                return getCenterX();
        }
    }

    public static int getAnchorScreenY(Anchor anchor) {
        switch (anchor) {
            case TOP:
                return 0;
            case CENTER:
                return getCenterY();
            case BOTTOM:
                return getHeight();
            default:
                return getCenterY();
        }
    }

    public static int getAnchoredX(Anchor anchor, int offset) {
        return getAnchorScreenX(anchor) + offset;
    }

    public static int getAnchoredY(Anchor anchor, int offset) {
        return getAnchorScreenY(anchor) - offset; // minus because screen Y goes down, Cartesian Y goes up
    }

    public enum Anchor {
        LEFT, CENTER, RIGHT, TOP, BOTTOM
    }

    public static int getStaticWidth(int width) {
        return width;
    }
    public static int getStaticHeight(int height) {
        return height;
    }

    public static class Position {
        public final Anchor horizontalAnchor;
        public final Anchor verticalAnchor;
        public final int xOffset;
        public final int yOffset;

        public Position(Anchor horizontalAnchor, Anchor verticalAnchor, int xOffset, int yOffset) {
            this.horizontalAnchor = horizontalAnchor;
            this.verticalAnchor = verticalAnchor;
            this.xOffset = xOffset;
            this.yOffset = yOffset;
        }

        public int getX() {
            return getAnchoredX(horizontalAnchor, xOffset);
        }

        public int getY() {
            return getAnchoredY(verticalAnchor, yOffset);
        }
    }
}
