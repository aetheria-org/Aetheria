package io.hamlook.aetheria.features.custommenu.editor;

/** Geometry contract for the full-screen editor. Keeps toolbar/canvas math out of the screen class. */
public final class EditorLayout {
    public static final int TOOLBAR_HEIGHT = 34;
    public static final int GRID_SIZE = 8;
    public static final int SIDE_PADDING = 8;
    private EditorLayout() { }

    public static int canvasTop() { return TOOLBAR_HEIGHT; }
    public static int canvasWidth(int screenWidth) { return screenWidth; }
    public static int canvasHeight(int screenHeight) { return Math.max(1, screenHeight - TOOLBAR_HEIGHT); }
    public static int historyX() { return SIDE_PADDING; }
    public static int historyY(int screenHeight, int height) { return Math.max(TOOLBAR_HEIGHT + 4, screenHeight / 2 - height / 2); }
}
