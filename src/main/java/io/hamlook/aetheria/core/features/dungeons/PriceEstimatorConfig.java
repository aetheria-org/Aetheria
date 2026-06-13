package io.hamlook.aetheria.core.features.dungeons;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations;
import io.hamlook.aetheria.utils.Position;

public class PriceEstimatorConfig {

    @Expose
    public Position analyzerPosition = new Position(15,15);

    @Expose
    @ConfigAnnotations.ConfigOption(name = "Estimate Meaning", desc = "Meaning of Estimate")
    @ConfigAnnotations.ConfigEditorTextDisplay(text = "§7An §6estimate §7is an educated guess, approximate calculation, or judgment regarding the size, value, cost, or extent of something. It is used when an §6exact measurement §7is §cnot §7possible or practical")
    public String estimateMeaning = "";

    @Expose
    @ConfigAnnotations.ConfigOption(name = "Dungeon Reward §6Estimator§r", desc = "§6Estimates§7 whether you will get profit by taking a reward chest or not.")
    @ConfigAnnotations.ConfigEditorBoolean
    public boolean rewardProfitEstimator = false;

    @Expose
    @ConfigAnnotations.ConfigOption(name = "Enable Analyzer Overlay",desc = "Enable overlay which shows you which chest is §6estimated§r to be the best to §rchoose.")
    @ConfigAnnotations.ConfigEditorBoolean
    public boolean enableAnalyzerOverlay = false;

    @Expose
    @ConfigAnnotations.ConfigOption(name = "Overlay Position",desc = "Configure where the analyzer overlay appears")
    @ConfigAnnotations.ConfigEditorButton(runnableId = "editAnalyzerOverlay",buttonText = "Edit")
    public boolean analyzerOverlayPosEdit = false;

    @Expose
    @ConfigAnnotations.ConfigOption(name = "Overlay Scale",desc = "Configure how big the analyzer overlay is")
    @ConfigAnnotations.ConfigEditorSliderAnnotation(minValue = 0.5f,maxValue = 2.5f,minStep = 0.1f)
    public float overlayScale = 1f;

    @Expose
    @ConfigAnnotations.ConfigOption(name = "Overlay Background",desc = "Configure how the bg the analyzer overlay is")
    @ConfigAnnotations.ConfigEditorColour
    public String overlayBgColor = "0:136:0:0:0";

}
