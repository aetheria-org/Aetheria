package io.hamlook.aetheria.core.features.mining;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.config.gui.config.ConfigAnnotations.*;

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
    @Category(name = "Pristine Tracker", desc = "Tracks pristine gemstone drops in Crystal Hollows")
    public PristineTrackerConfig pristineTrackerConfig = new PristineTrackerConfig();

    @Expose
    @Category(name = "/hotm Powder Display", desc = "Powder cost info on HOTM perk tooltips")
    public HotmPowderConfig hotmPowder = new HotmPowderConfig();
    
    @Expose
    @ConfigOption(name = "Pickobulus Preview", desc = "Shows a wireframe cube and block count preview for the Pickobulus ability blast radius")
    @ConfigEditorBoolean
    public boolean pickobulusPreview = false;
}