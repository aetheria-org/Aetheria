package io.hamlook.aetheria.core.features.farming;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.*;
import org.lwjgl.input.Keyboard;

public class Farming {

    @Expose
    @Category(name = "Mouse Lock", desc = "Lock camera movement and visual indicators")
    public LockMouseConfig lockMouseConfig = new LockMouseConfig();

    @Expose
    @Category(name = "BPS Calculator", desc = "Blocks per second calculator for farming")
    public BPSConfig bps = new BPSConfig();
}