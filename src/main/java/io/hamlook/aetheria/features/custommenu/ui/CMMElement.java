package io.hamlook.aetheria.features.custommenu.ui;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.features.custommenu.Position;
import io.hamlook.aetheria.features.custommenu.animation.CMMAnimation;
import io.hamlook.aetheria.features.custommenu.animation.AnimationController;
import io.hamlook.aetheria.features.custommenu.util.ScreenHelper;
import io.hamlook.aetheria.features.custommenu.util.PercentageUtils;

public class CMMElement {

    public Position position;
    public int width, height;
    public int xPos, yPos;
    /** Normalized geometry. 0,0 is top-left and 100,100 is bottom-right. */
    public float xPercent, yPercent, widthPercent, heightPercent;
    /** False for legacy presets; the first layout pass converts their pixel/anchor geometry. */
    public boolean percentageGeometry;
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
        // Relative/legacy geometry is resolved lazily by the first screen layout pass,
        // after ScreenHelper knows the active Minecraft GUI dimensions.
    }

    public CMMElement(Position position, int width, int height, int xPos, int yPos) {
        this.position = position;
        this.width = width;
        this.height = height;
        this.xPos = xPos;
        this.yPos = yPos;
    }

    public void updatePosition() {
        if (!percentageGeometry) {
            if (position != null && position.useRelativePositioning) {
                this.xPos = position.getX();
                this.yPos = position.getY();
            }
            syncPercentFromPixels();
            percentageGeometry = true;
        }
        this.xPos = PercentageUtils.x(xPercent);
        this.yPos = PercentageUtils.y(yPercent);
        if (width >= 0) this.width = PercentageUtils.width(widthPercent);
        if (height >= 0) this.height = PercentageUtils.height(heightPercent);
    }

    private int logicalWidth() { int value = ScreenHelper.getScaledWidth(); return value > 0 ? value : 854; }
    private int logicalHeight() { int value = ScreenHelper.getScaledHeight(); return value > 0 ? value : 480; }

    public void syncPercentFromPixels() {
        xPercent = clampPercent(PercentageUtils.xPercent(xPos));
        yPercent = clampPercent(PercentageUtils.yPercent(yPos));
        if (width >= 0) widthPercent = clampPercent(PercentageUtils.widthPercent(width));
        if (height >= 0) heightPercent = clampPercent(PercentageUtils.heightPercent(height));
    }

    public void setPixelGeometry(int x, int y, int elementWidth, int elementHeight) {
        xPos = x; yPos = y;
        if (elementWidth >= 0) width = elementWidth;
        if (elementHeight >= 0) height = elementHeight;
        percentageGeometry = true;
        syncPercentFromPixels();
    }

    private float clampPercent(float value) { return Math.max(0f, Math.min(100f, value)); }

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
