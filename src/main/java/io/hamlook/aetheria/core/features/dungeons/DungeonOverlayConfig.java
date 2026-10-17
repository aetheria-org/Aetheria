package io.hamlook.aetheria.core.features.dungeons;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.*;
import io.hamlook.aetheria.utils.Position;

public class DungeonOverlayConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Shows run timers overlay and end-of-run stats in chat")
    @ConfigEditorBoolean
    public boolean dungeonStats = true;

    @Expose
    @ConfigOption(name = "Show All Timers", desc = "Show all timers at once. When disabled, only shows the current active timer to reduce clutter")
    @ConfigEditorBoolean
    public boolean dungeonStatsShowAll = true;

    @Expose
    @ConfigOption(name = "Background Color", desc = "Background color of the stats overlay (alpha controls opacity)")
    @ConfigEditorColour
    public String statsBgColor = "0:136:0:0:0";

    @Expose
    @ConfigOption(name = "Corner Radius", desc = "Roundness of overlay corners")
    @ConfigEditorSliderAnnotation(minValue = 0f, maxValue = 12f, minStep = 1f)
    public int statsCornerRadius = 4;

    @Expose
    @ConfigOption(name = "Overlay Scale", desc = "Size of the dungeon stats overlay")
    @ConfigEditorSliderAnnotation(minValue = 0.5f, maxValue = 3f, minStep = 0.1f)
    public float statsScale = 1f;

    @Expose
    @ConfigOption(name = "Edit Position", desc = "Drag the overlay to reposition it")
    @ConfigEditorButton(runnableId = "openStatsEditor", buttonText = "Edit")
    public boolean editStatsPosDummy = false;

    @Expose
    public Position statsPos = new Position(4, 100);
}
