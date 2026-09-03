package io.hamlook.aetheria.core.features.qol;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorBoolean;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorColour;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorKeybind;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigOption;
import org.lwjgl.input.Keyboard;

import java.util.HashMap;
import java.util.Map;

public class SlotBindsConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Bind inventory slots to hotbar slots for quick swapping (Shift+click to trigger)")
    @ConfigEditorBoolean
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Bind Key", desc = "Hold this key over a slot to start/finish setting a bind, or remove an existing one")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    public int bindKey = Keyboard.KEY_NONE;

    @Expose
    @ConfigOption(name = "Line Color", desc = "Color of the line drawn between bound slots")
    @ConfigEditorColour
    public String lineColor = "200:255:170:0:255";

    @Expose
    @ConfigOption(name = "SkyBlock Only", desc = "Only works while on SkyBlock")
    @ConfigEditorBoolean
    public boolean skyblockOnly = true;

    @Expose
    @ConfigOption(name = "Always Show Lines", desc = "Draw lines between all bound slots at all times")
    @ConfigEditorBoolean
    public boolean alwaysShowLines = false;

    @Expose
    public Map<Integer, Integer> binds = new HashMap<>();
}