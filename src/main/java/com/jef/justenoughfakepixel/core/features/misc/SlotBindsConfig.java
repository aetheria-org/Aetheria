package com.jef.justenoughfakepixel.core.features.misc;

import com.google.gson.annotations.Expose;
import com.jef.justenoughfakepixel.core.config.gui.config.ConfigAnnotations.*;
import org.lwjgl.input.Keyboard;

public class SlotBindsConfig {

    @Expose
    @ConfigOption(name = "Enable Slot Binds", desc = "Bind inventory slots together; shift-click a bound slot to swap it with its paired hotbar slot")
    @ConfigEditorBoolean
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Bind Set Key", desc = "Hover a slot and press this key to start/finish setting a bind (press on a bound slot to remove it)")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    public int bindKey = Keyboard.KEY_NONE;

    @Expose
    @ConfigOption(name = "Line Color", desc = "Color of the line drawn between a bound slot pair (visible when hovering + holding Shift)")
    @ConfigEditorColour
    public String lineColor = "0:255:255:170:0"; // opaque gold: chromaSpeed=0, a=255, r=255, g=170, b=0
}
