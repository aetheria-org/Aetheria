package io.hamlook.aetheria.core.features.farming;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.*;
import io.hamlook.aetheria.utils.Position;

public class SensitivityReducerConfig {

    @Expose
    @ConfigOption(name = "Enable Sensitivity Reducer", desc = "Reduces your mouse sensitivity while holding a crop farming tool (Melon Dicer, Pumpkin Dicer, etc.)")
    @ConfigEditorBoolean
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Sensitivity", desc = "Percentage of your normal sensitivity to use while holding a farming tool")
    @ConfigEditorSliderAnnotation(minValue = 5, maxValue = 100, minStep = 5)
    public float sensitivityPercent = 5f;

    @Expose
    @ConfigOption(name = "Require Farming Island", desc = "Only reduce sensitivity while on a farming location (Barn, Private Island, Garden)")
    @ConfigEditorBoolean
    public boolean requireFarmingIsland = true;

    @Expose
    @ConfigOption(name = "Show Pitch/Yaw Overlay", desc = "Shows a small overlay with your current pitch and yaw")
    @ConfigEditorBoolean
    public boolean showPitchYawOverlay = true;

    @Expose
    @ConfigOption(name = "Show Only While Farming", desc = "Only show the overlay while actively farming (holding a farming tool and breaking crops)")
    @ConfigEditorBoolean
    public boolean showOnlyWhileFarming = true;

    @Expose
    @ConfigOption(name = "Show Only While Holding Farming Tool", desc = "Only show the overlay while holding a farming tool")
    @ConfigEditorBoolean
    public boolean showOnlyWhileHoldingFarmingTool = true;

    @Expose
    @ConfigOption(name = "Pitch/Yaw Label Color", desc = "Color of the 'Pitch' and 'Yaw' labels in the overlay (not the numbers)")
    @ConfigEditorColour
    public String pitchYawLabelColor = "0:255:255:255:255";

    @Expose
    @ConfigOption(name = "Edit Overlay Position", desc = "Drag the pitch/yaw overlay to reposition it")
    @ConfigEditorButton(runnableId = "openPitchYawEditor", buttonText = "Edit")
    public boolean pitchYawEditPosDummy = false;

    @Expose
    public Position pitchYawOverlayPos = new Position(276, -176, false, false);

    @Expose
    @ConfigOption(name = "Overlay Scale", desc = "Size of the pitch/yaw overlay")
    @ConfigEditorSliderAnnotation(minValue = 0.5f, maxValue = 3f, minStep = 0.1f)
    public float pitchYawOverlayScale = 1f;
}
