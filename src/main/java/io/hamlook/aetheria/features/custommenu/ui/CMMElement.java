package io.hamlook.aetheria.features.custommenu.ui;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.features.custommenu.Position;

public class CMMElement {

    public Position position;
    public int width, height;
    public int xPos, yPos;
    public boolean locked = false;

    public CMMElement() {}

    public CMMElement(Position position, int width, int height) {
        this.position = position;
        this.width = width;
        this.height = height;
        if (position != null && position.useRelativePositioning) {
            updatePosition();
        }
    }

    public CMMElement(Position position, int width, int height, int xPos, int yPos) {
        this.position = position;
        this.width = width;
        this.height = height;
        this.xPos = xPos;
        this.yPos = yPos;
    }

    public void updatePosition() {
        if (position != null && position.useRelativePositioning) {
            this.xPos = position.getX();
            this.yPos = position.getY();
        }
        Aetheria.logger.info("[CMM] Element Position: " + this.xPos + " | " + this.yPos);
    }

    public int[] getCorners(){
        int[] corners = new int[4];
        corners[0] = this.xPos;
        corners[1] = this.yPos;
        corners[2] = this.xPos + this.width;
        corners[3] = this.yPos + this.height;
        return corners;
    }
    public void draw(int mouseX, int mouseY, float partialTicks) {}
}
