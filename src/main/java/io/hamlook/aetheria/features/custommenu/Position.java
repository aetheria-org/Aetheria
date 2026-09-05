package io.hamlook.aetheria.features.custommenu;

import io.hamlook.aetheria.features.custommenu.util.ScreenHelper;

public class Position {
    public String horizontalAnchor = "CENTER";
    public String verticalAnchor = "CENTER";
    public int xOffset = 0;
    public int yOffset = 0;
    public float relativeX = -1;
    public float relativeY = -1;
    public boolean useRelativePositioning = false;

    public Position() {}

    public Position(String horizontalAnchor, String verticalAnchor, int xOffset, int yOffset) {
        this.horizontalAnchor = horizontalAnchor;
        this.verticalAnchor = verticalAnchor;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.useRelativePositioning = true;
    }

    public Position(float relativeX, float relativeY) {
        this.relativeX = relativeX;
        this.relativeY = relativeY;
        this.useRelativePositioning = true;
    }

    public static Position absolute(int x, int y) {
        Position position = new Position();
        position.xOffset = x;
        position.yOffset = y;
        position.useRelativePositioning = false;
        return position;
    }

    public int getX() {
        if (!useRelativePositioning) return xOffset;
        if (relativeX >= 0 && relativeY >= 0) {
            return ScreenHelper.getRelativeX(relativeX);
        }
        ScreenHelper.Anchor hAnchor = ScreenHelper.Anchor.valueOf(horizontalAnchor);
        ScreenHelper.Anchor vAnchor = ScreenHelper.Anchor.valueOf(verticalAnchor);
        return ScreenHelper.getAnchoredX(hAnchor, xOffset);
    }

    public int getY() {
        if (!useRelativePositioning) return yOffset;
        if (relativeX >= 0 && relativeY >= 0) {
            return ScreenHelper.getRelativeY(relativeY);
        }
        ScreenHelper.Anchor hAnchor = ScreenHelper.Anchor.valueOf(horizontalAnchor);
        ScreenHelper.Anchor vAnchor = ScreenHelper.Anchor.valueOf(verticalAnchor);
        return ScreenHelper.getAnchoredY(vAnchor, yOffset);
    }

    public static String[] getAnchors() {
        return new String[]{"LEFT", "CENTER", "RIGHT", "TOP", "BOTTOM"};
    }
}
