package io.hamlook.aetheria.core.features.farming;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.*;

public class GardenPlotsConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Show plot numbers on plot slots in the Configure Plots menu")
    @ConfigEditorBoolean
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "Chroma Mode", desc = "All Same: one color. Fade: color shifts across the plot grid.")
    @ConfigEditorDropdown(values = {"All Same", "Fade"})
    public int chromaMode = 1;

    @Expose
    @ConfigOption(name = "Chroma Size", desc = "Width of the Fade gradient in pixels.")
    @ConfigEditorSliderAnnotation(minValue = 20f, maxValue = 400f, minStep = 5f)
    public float chromaSize = 110f;

    @Expose
    @ConfigOption(name = "Unlocked Color", desc = "Plot number color for unlocked plots (can modify/teleport)")
    @ConfigEditorColour
    public String tipColorUnlocked = "0:255:57:255:87";

    @Expose
    @ConfigOption(name = "Buyable Color", desc = "Plot number color for locked plots you can buy")
    @ConfigEditorColour
    public String tipColorBuyable = "0:255:255:241:3";

    @Expose
    @ConfigOption(name = "Missing Materials Color", desc = "Plot number color for buyable plots you lack materials for")
    @ConfigEditorColour
    public String tipColorNoMaterials = "0:255:255:6:10";

    @Expose
    @ConfigOption(name = "Locked Color", desc = "Plot number color for plots locked by garden level or adjacent plot")
    @ConfigEditorColour
    public String tipColorLocked = "0:255:170:170:170";

    @Expose
    @ConfigOption(name = "Highlight Unlocked", desc = "Highlight unlocked plots with a colored slot background")
    @ConfigEditorBoolean
    public boolean highlightUnlocked = false;

    @Expose
    @ConfigOption(name = "Unlocked Highlight Color", desc = "Color of the unlocked plot slot highlight")
    @ConfigEditorColour
    public String unlockedHighlightColor = "0:80:85:255:85";

    @Expose
    @ConfigOption(name = "Highlight Unlockable", desc = "Highlight buyable plots with a colored slot background")
    @ConfigEditorBoolean
    public boolean highlightUnlockable = false;

    @Expose
    @ConfigOption(name = "Unlockable Highlight Color", desc = "Color of the buyable plot slot highlight")
    @ConfigEditorColour
    public String unlockableHighlightColor = "0:80:255:255:85";
}