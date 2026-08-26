package io.hamlook.aetheria.core.features.farming;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.*;

public class PestsConfig {

    @Expose
    @Category(name = "Pest Tracker", desc = "Track pest kills and crop drops in the Garden")
    public PestTrackerConfig pestTracker = new PestTrackerConfig();

    @Expose
    @Category(name = "Pest Finder", desc = "Garden pest tab data overlay and warp keybind")
    public PestFinderConfig pestFinder = new PestFinderConfig();

    @Expose
    @Category(name = "Pest Alert", desc = "Warn you when the pest cooldown drops below a set time")
    public PestAlertConfig pestAlert = new PestAlertConfig();
}