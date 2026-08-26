package io.hamlook.aetheria.core.features.dungeons;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.*;

public class CaseOpeningConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Play the case opening animation when opening Obsidian or Bedrock chests")
    @ConfigEditorBoolean
    public boolean caseOpeningAnimation = false;

    @Expose
    @ConfigOption(name = "Show Item Names", desc = "Show item names below each slot in the carousel")
    @ConfigEditorBoolean
    public boolean caseOpeningAllowText = true;

    @Expose
    @ConfigOption(name = "Text Scale", desc = "Scale of the item name text in the carousel")
    @ConfigEditorSliderAnnotation(minValue = 0.3f, maxValue = 2f, minStep = 0.1f)
    public float caseOpeningTextScale = 0.5f;

    @Expose
    @ConfigOption(name = "Slow Time", desc = "Time in seconds to decelerate from the slow point to the reward slot")
    @ConfigEditorSliderAnnotation(minValue = 0.5f, maxValue = 10f, minStep = 0.5f)
    public float caseOpeningSlowTime = 3f;

    @Expose
    @ConfigOption(name = "Slow Distance", desc = "Number of slots before the reward where the carousel starts to slow down")
    @ConfigEditorSliderAnnotation(minValue = 1f, maxValue = 20f, minStep = 1f)
    public int caseOpeningSlowDistance = 8;
}
