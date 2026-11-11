package io.hamlook.aetheria.core.features.mining;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.Category;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorBoolean;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigOption;

public class Mining {


    @Expose
    @ConfigOption(name = "Commission Highlight", desc = "Highlights completed commissions in green inside the Commissions menu")
    @ConfigEditorBoolean
    public boolean commissionHighlight = true;

    @Expose
    @ConfigOption(name = "Mining Progress Fix", desc = "Keeps block-breaking progress when the held drill's data updates (fuel, durability)")
    @ConfigEditorBoolean
    public boolean miningProgressFix = true;

    @Expose
    @Category(name = "Gold Tracker", desc = "Tracks gold ingot and enchanted gold drops")
    public GoldTrackerConfig goldTracker = new GoldTrackerConfig();

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

    @Expose
    @Category(name = "Powder Mining Chat", desc = "Hide powder mining chest reward messages from chat")
    public PowderMiningChatFilterConfig powderMiningChat = new PowderMiningChatFilterConfig();

    @Expose
    @Category(name = "Bal Alert", desc = "Alerts when Bal is defeated and tracks its respawn timer in the Crystal Hollows")
    public BalConfig bal = new BalConfig();
}