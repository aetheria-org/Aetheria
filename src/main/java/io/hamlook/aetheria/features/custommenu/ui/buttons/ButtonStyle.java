package io.hamlook.aetheria.features.custommenu.ui.buttons;

/** Nine-slice button skins exposed by the Custom Main Menu editor. */
public enum ButtonStyle {
    DEFAULT(0, "Default", false, 0xff263b4d, 0xff71d7ff),
    DARK(1, "Dark", false, 0xff20242b, 0xffaaaaaa),
    BLUE(2, "Blue", false, 0xff1d4160, 0xff8fd7ff),
    GREEN(3, "Green", false, 0xff234d3c, 0xff8fffc0),
    PURPLE(4, "Purple", false, 0xff44305e, 0xffd0a6ff),
    RED(5, "Red", false, 0xff5e3036, 0xffff9c9c),
    GOLD(6, "Gold", false, 0xff604b24, 0xffffd27d);

    public final int index;
    public final String label;
    public final boolean rounded;
    public final int fillColor;
    public final int accentColor;

    ButtonStyle(int index, String label, boolean rounded, int fillColor, int accentColor) {
        this.index = index; this.label = label; this.rounded = rounded; this.fillColor = fillColor; this.accentColor = accentColor;
    }

    public static ButtonStyle fromIndex(int index) {
        for (ButtonStyle style : values()) if (style.index == index) return style;
        return DEFAULT;
    }

    public static String[] getLabels() {
        String[] labels = new String[values().length];
        for (int i = 0; i < values().length; i++) {
            labels[i] = values()[i].label + " (" + values()[i].index + ")";
        }
        return labels;
    }
}
