package io.hamlook.aetheria.core.features.cosmetics;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorBoolean;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorButton;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorSliderAnnotation;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigOption;

public class ItemAnimationsConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Customise the held item in first person")
    @ConfigEditorBoolean
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Offset X", desc = "Moves the held item sideways")
    @ConfigEditorSliderAnnotation(minValue = -1.5f, maxValue = 1.5f, minStep = 0.05f)
    public float offsetX = 0f;

    @Expose
    @ConfigOption(name = "Offset Y", desc = "Moves the held item up (positive) or down (negative)")
    @ConfigEditorSliderAnnotation(minValue = -1.5f, maxValue = 1.5f, minStep = 0.05f)
    public float offsetY = 0f;

    @Expose
    @ConfigOption(name = "Offset Z", desc = "Moves the held item closer (negative) or further away (positive)")
    @ConfigEditorSliderAnnotation(minValue = -1.5f, maxValue = 1.5f, minStep = 0.05f)
    public float offsetZ = 0f;

    @Expose
    @ConfigOption(name = "Pitch", desc = "Tilts the held item")
    @ConfigEditorSliderAnnotation(minValue = -180f, maxValue = 180f, minStep = 1f)
    public float pitch = 0f;

    @Expose
    @ConfigOption(name = "Yaw", desc = "Turns the held item left or right")
    @ConfigEditorSliderAnnotation(minValue = -180f, maxValue = 180f, minStep = 1f)
    public float yaw = 0f;

    @Expose
    @ConfigOption(name = "Roll", desc = "Twists the held item")
    @ConfigEditorSliderAnnotation(minValue = -180f, maxValue = 180f, minStep = 1f)
    public float roll = 0f;

    @Expose
    @ConfigOption(name = "Scale", desc = "Scales the held item; 1 is the vanilla size")
    @ConfigEditorSliderAnnotation(minValue = 0.25f, maxValue = 3f, minStep = 0.05f)
    public float scale = 1f;

    @Expose
    @ConfigOption(name = "Swing Range", desc = "How far the held item swings; 1 is the vanilla range")
    @ConfigEditorSliderAnnotation(minValue = 0.25f, maxValue = 2f, minStep = 0.05f)
    public float swingRange = 1f;

    @Expose
    @ConfigOption(name = "Swing Speed", desc = "Speed of the swing animation, also in third person; 1 is the vanilla speed, 0 removes the swing")
    @ConfigEditorSliderAnnotation(minValue = 0f, maxValue = 3f, minStep = 0.05f)
    public float swingSpeed = 1f;

    @Expose
    @ConfigOption(name = "Ignore Haste", desc = "Swing speed ignores the Haste and Mining Fatigue effects")
    @ConfigEditorBoolean
    public boolean ignoreHaste = false;

    @Expose
    @ConfigOption(name = "No Swap Animation", desc = "Removes the item lowering animation when switching hotbar slots")
    @ConfigEditorBoolean
    public boolean noItemSwitchAnimation = true;

    @Expose
    @ConfigOption(name = "Reset", desc = "Restores the default item transform values")
    @ConfigEditorButton(runnableId = "resetItemAnimations", buttonText = "Reset")
    public boolean resetDummy = false;

    public void reset() {
        offsetX = 0f;
        offsetY = 0f;
        offsetZ = 0f;
        pitch = 0f;
        yaw = 0f;
        roll = 0f;
        scale = 1f;
        swingRange = 1f;
        swingSpeed = 1f;
        ignoreHaste = false;
    }
}
