package io.hamlook.aetheria.features.custommenu.ui;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.features.custommenu.Position;
import io.hamlook.aetheria.features.custommenu.animation.CMMAnimation;
import io.hamlook.aetheria.features.custommenu.animation.AnimationController;

public class CMMElement {

    public Position position;
    public int width, height;
    public int xPos, yPos;
    public boolean locked = false;
    public boolean visible = true;
    public float opacity = 1.0f;
    public float rotation = 0.0f;
    public float scaleX = 1.0f;
    public float scaleY = 1.0f;
    public int zIndex = 0;
    public String elementId = "";
    public String displayName = "";
    public CMMAnimation animation = new CMMAnimation();
    public CMMAnimation openAnimation = new CMMAnimation();
    public CMMAnimation hoverAnimation = new CMMAnimation();
    public CMMAnimation clickAnimation = new CMMAnimation();
    public CMMAnimation closeAnimation = new CMMAnimation();
    public transient AnimationController animationController = new AnimationController();
    public transient long lastAnimationTrigger;
    public transient boolean wasHovered;

    public void triggerAnimation(CMMAnimation value) { if (animationController == null) animationController = new AnimationController(); animationController.start(value); lastAnimationTrigger = System.currentTimeMillis(); }
    public float animationFactor() { return animationController == null ? 1f : animationController.value(); }

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
    }

    public int[] getCorners(boolean preview){
        int[] corners = new int[4];
        corners[0] = this.xPos;
        corners[1] = this.yPos;
        corners[2] = this.xPos + this.width;
        corners[3] = this.yPos + this.height;
        return corners;
    }

    public int[] getEditorBounds() { return getCorners(false); }
    public void draw(int mouseX, int mouseY, float partialTicks) {}
}
