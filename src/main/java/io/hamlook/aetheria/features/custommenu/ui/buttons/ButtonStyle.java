package io.hamlook.aetheria.features.custommenu.ui.buttons;

/** Nine-slice button skins exposed by the Custom Main Menu editor. */
public enum ButtonStyle {
    DEFAULT(0, "Dark"),
    DARK(1, "Dark Filled" ),
    TRANSPARENT(2,"Transparent"),
    LIGHT(3,"Light"),
    LIGHT_FILL(4,"Light Filled"),
    LIGHT_FILL_SMOOTH(5,"Light Filled Smooth");


    public final int index;
    public final String label;

    ButtonStyle(int index, String label) {
        this.index = index; this.label = label;
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
