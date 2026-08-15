package io.hamlook.aetheria.core.moulconfig.editors;

public class ChromaStyle {

    private final int speed;
    private final int alpha;
    private final int mode;
    private final float size;

    private ChromaStyle(int speed, int alpha, int mode, float size) {
        this.speed = speed;
        this.alpha = alpha;
        this.mode = mode;
        this.size = size;
    }

    public static ChromaStyle of(String colorString, int mode, float size) {
        return new ChromaStyle(
                ChromaColour.getSpeed(colorString),
                (ChromaColour.specialToSimpleRGB(colorString) >>> 24) & 255,
                mode,
                size
        );
    }

    public static ChromaStyle of(String colorString) {
        return of(colorString, 0, 120F);
    }

    public int getMode() {
        return mode;
    }

    public float getSize() {
        return size;
    }

    public int toArgb() {
        return ChromaColour.animatedRainbow(speed, alpha);
    }

    public int toArgb(float x, float y) {
        return ChromaColour.applyChromaShift(toArgb(), x, y, mode, size);
    }
}
