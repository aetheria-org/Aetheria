package com.jef.justenoughfakepixel.core.features.mining;

import com.google.gson.annotations.Expose;
import com.jef.justenoughfakepixel.core.config.gui.config.ConfigAnnotations.*;
import com.jef.justenoughfakepixel.core.config.utils.Position;
import com.jef.justenoughfakepixel.core.features.mining.*;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Mining {


    @Expose
    @ConfigOption(name = "Commission Highlight", desc = "Highlights completed commissions in green inside the Commissions menu")
    @ConfigEditorBoolean
    public boolean commissionHighlight = true;

    @Expose
    @Category(name = "Fetchur Overlay", desc = "Settings for the Fetchur item overlay")
    public FetchurConfig fetchur = new FetchurConfig();

    @Expose
    @Category(name = "Powder Tracker", desc = "Tracks gemstone powder and chest drops in Crystal Hollows")
    public PowderTrackerConfig powderTrackerConfig = new PowderTrackerConfig();

    @Expose
    @Category(name = "/hotm Powder Display", desc = "Powder cost info on HOTM perk tooltips")
    public HotmPowderConfig hotmPowder = new HotmPowderConfig();
}