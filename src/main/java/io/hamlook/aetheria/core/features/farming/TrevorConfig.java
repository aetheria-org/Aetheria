package io.hamlook.aetheria.core.features.farming;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.*;
import io.hamlook.aetheria.utils.Position;

public class TrevorConfig {

    @Expose
    @ConfigOption(name = "Trevor Solver", desc = "Highlight possible spawn spots when Trevor gives you a hunt and mark the animal once it spawns")
    @ConfigEditorBoolean
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "Spot Color", desc = "Color of the highlighted spawn spots (alpha controls opacity)")
    @ConfigEditorColour
    public String spotColor = "0:120:0:255:255";

    @Expose
    @ConfigOption(name = "Spot Labels", desc = "Show a floating label with distance above each possible spawn spot")
    @ConfigEditorBoolean
    public boolean spotLabels = true;

    @Expose
    @ConfigOption(name = "Animal Beacon", desc = "Draw a beacon beam at the detected animal (occluded by blocks, no x-ray)")
    @ConfigEditorBoolean
    public boolean animalBeacon = true;

    @Expose
    @ConfigOption(name = "Beacon Color", desc = "Color of the animal beacon beam")
    @ConfigEditorColour
    public String beaconColor = "0:255:255:170:0";

    @Expose
    @ConfigOption(name = "Animal Detected Alert", desc = "Show a title and chat message the first time an animal is detected")
    @ConfigEditorBoolean
    public boolean detectAlert = true;

    @Expose
    @ConfigOption(name = "Pelt Tracker", desc = "Overlay tracking pelts earned this session and your pelts/hour rate")
    @ConfigEditorBoolean
    public boolean peltTracker = true;

    @Expose
    @ConfigOption(name = "Edit Pelt Overlay Position", desc = "Drag the pelt tracker overlay to reposition it")
    @ConfigEditorButton(runnableId = "openPeltTrackerEditor", buttonText = "Edit")
    public boolean peltEditPosDummy = false;

    @Expose
    @ConfigOption(name = "Reset Pelt Tracker", desc = "Reset the session pelt count and pelts/hour rate")
    @ConfigEditorButton(runnableId = "resetPeltTracker", buttonText = "Reset")
    public boolean peltResetDummy = false;

    @Expose
    public Position peltTrackerPos = new Position(2, 100, false, false);

    @Expose
    @Category(name = "Hotkeys", desc = "Hotkeys to warp to or call Trevor after a hunt, and to warp closer to a Desert Settlement/Oasis hunt")
    public TrevorHotkeysConfig hotkeys = new TrevorHotkeysConfig();
}
