package io.hamlook.aetheria.core.features.misc;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.config.gui.config.ConfigAnnotations;

public class ItemListConfig {

    @Expose
    @ConfigAnnotations.ConfigOption(name = "Use Global SearchBar", desc = "If enabled, the item list search bar is removed, and the global search bar is used")
    @ConfigAnnotations.ConfigEditorBoolean
    public boolean searchItemList = true;

    @Expose
    @ConfigAnnotations.ConfigOption(name = "Item List GUI Scale", desc = "Configure the grid scale of the item list")
    @ConfigAnnotations.ConfigEditorSliderAnnotation(minValue = 0.5f,maxValue = 2f,minStep = 0.1f)
    public float itemListScale = 1f;
}