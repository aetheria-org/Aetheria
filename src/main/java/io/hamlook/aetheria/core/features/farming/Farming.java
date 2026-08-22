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

    @Expose
    @Category(name = "Farming Tracker", desc = "Track crop value and coins/hour")
    public FarmingTrackerConfig farmingTracker = new FarmingTrackerConfig();

    @Expose
    @Category(name = "Organic Matter Tracker", desc = "Track Organic Matter and organic matter/hour")
    public OrganicMatterTrackerConfig organicMatterTracker = new OrganicMatterTrackerConfig();

    @Expose
    @Category(name = "Pests", desc = "Garden pest tracker, finder and helpers")
    public PestsConfig pests = new PestsConfig();

    @Expose
    @Category(name = "Visitors", desc = "Visitor shopping list, rewards tracking and Bazaar helpers")
    public VisitorsConfig visitors = new VisitorsConfig();

    @Expose
    @Category(name = "Garden Plots", desc = "Plot numbers and highlights in the Configure Plots menu")
    public GardenPlotsConfig gardenPlots = new GardenPlotsConfig();

    @Expose
    @Category(name = "Sensitivity Reducer", desc = "Reduce mouse sensitivity while holding a crop farming tool")
    public SensitivityReducerConfig sensitivityReducer = new SensitivityReducerConfig();

    @Expose
    @Category(name = "Trevor Solver", desc = "Trapper quest helper – spawn spot highlights & animal beacon")
    public TrevorConfig trevor = new TrevorConfig();
}