package io.hamlook.aetheria.core.features.qol;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.Category;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorBoolean;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorKeybind;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorSliderAnnotation;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigOption;
import org.lwjgl.input.Keyboard;

public class TooltipsConfig {

    @Expose
    @Category(name = "Scale", desc = "Tooltip scaling and temporary zoom")
    public Scale scale = new Scale();

    @Expose
    @Category(name = "Scroll", desc = "Tooltip scrolling")
    public Scroll scroll = new Scroll();

    public static class Scale {

        @Expose
        @ConfigOption(name = "Enable", desc = "Scale item tooltips by a custom factor")
        @ConfigEditorBoolean
        public boolean tooltipScaling = true;

        @Expose
        @ConfigOption(name = "Tooltip Scale", desc = "Scale factor applied to item tooltips")
        @ConfigEditorSliderAnnotation(minValue = 0.25f, maxValue = 2.0f, minStep = 0.05f)
        public float tooltipScale = 1.0f;

        @Expose
        @ConfigOption(name = "Scale Key", desc = "Hold this key and use the mouse wheel to temporarily scale a tooltip")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_LCONTROL)
        public int tooltipScaleKey = Keyboard.KEY_LCONTROL;

        @Expose
        @ConfigOption(name = "Zoom Step", desc = "Multiplier applied per mouse wheel notch while the scale key is held")
        @ConfigEditorSliderAnnotation(minValue = 1.05f, maxValue = 1.5f, minStep = 0.05f)
        public float zoomStep = 1.15f;

        @Expose
        @ConfigOption(name = "Zoom Limit", desc = "Maximum zoom while the scale key is held")
        @ConfigEditorSliderAnnotation(minValue = 1.5f, maxValue = 5.0f, minStep = 0.25f)
        public float zoomLimit = 3.0f;

        @Expose
        @ConfigOption(name = "Smooth Zoom", desc = "Animate zoom changes, including the reset when the scale key is released")
        @ConfigEditorBoolean
        public boolean zoomSmooth = true;

        @Expose
        @ConfigOption(name = "Zoom Duration", desc = "Milliseconds for zoom changes to animate")
        @ConfigEditorSliderAnnotation(minValue = 50f, maxValue = 1000f, minStep = 10f)
        public int zoomDurationMs = 200;
    }

    public static class Scroll {

        @Expose
        @ConfigOption(name = "Scrollable Tooltips", desc = "Scroll a tooltip with the mouse wheel when it is taller than the screen")
        @ConfigEditorBoolean
        //#if MC < 1.21
        public boolean tooltipScrolling = false;
        //#else
        //$$ public boolean tooltipScrolling = true;
        //#endif

        @Expose
        @ConfigOption(name = "Scroll Speed", desc = "Pixels scrolled per mouse wheel notch")
        @ConfigEditorSliderAnnotation(minValue = 1f, maxValue = 30f, minStep = 1f)
        public int tooltipScrollSpeed = 10;

        @Expose
        @ConfigOption(name = "Scroll Duration", desc = "Milliseconds for scroll changes to animate; 0 = instant")
        @ConfigEditorSliderAnnotation(minValue = 0f, maxValue = 1000f, minStep = 10f)
        public int scrollDurationMs = 50;
    }
}
