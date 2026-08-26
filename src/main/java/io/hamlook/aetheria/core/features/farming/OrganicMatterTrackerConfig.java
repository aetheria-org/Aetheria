package io.hamlook.aetheria.core.features.farming;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.*;
import io.hamlook.aetheria.utils.Position;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OrganicMatterTrackerConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Track Organic Matter and show the organic matter overlay")
    @ConfigEditorBoolean
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "Require Farming Location", desc = "Only track Organic Matter while in farming locations (Barn, Private Island, Garden)")
    @ConfigEditorBoolean
    public boolean requireFarmingIsland = true;

    @Expose
    @ConfigOption(name = "Keep Tracker Across Sessions", desc = "Persist item counts and Organic Matter across world unloads / game restarts instead of resetting")
    @ConfigEditorBoolean
    public boolean persistAcrossSessions = true;

    @Expose
    @ConfigOption(name = "Pause on Chat", desc = "Pause the playtime timer while the chat GUI is open")
    @ConfigEditorBoolean
    public boolean pauseOnChat = true;

    @Expose
    @ConfigOption(name = "Hide in Chat", desc = "Hide the overlay when the chat GUI is open")
    @ConfigEditorBoolean
    public boolean hideOnChat = true;

    @Expose
    @ConfigOption(name = "Hide on Tab", desc = "Hide the overlay when the tab list is shown")
    @ConfigEditorBoolean
    public boolean hideOnTab = true;

    @Expose
    @ConfigOption(name = "Hide on F3 Debug", desc = "Hide the overlay when the F3 debug screen is open")
    @ConfigEditorBoolean
    public boolean hideOnDebug = true;

    @Expose
    @ConfigOption(name = "Hide When Paused", desc = "Hide the overlay while the playtime timer is paused")
    @ConfigEditorBoolean
    public boolean hideWhenPaused = true;

    @Expose
    @ConfigOption(name = "Show Only While Farming", desc = "Only show the overlay while actively farming (holding a farming tool and breaking crops)")
    @ConfigEditorBoolean
    public boolean showOnlyWhileFarming = true;

    @Expose
    @ConfigOption(name = "Show Only While Holding Farming Tool", desc = "Only show the overlay while holding a farming tool")
    @ConfigEditorBoolean
    public boolean showOnlyWhileHoldingFarmingTool = true;

    @Expose
    @ConfigOption(name = "Tracked Crops", desc = "Choose which crops (including Seeds) count toward Organic Matter")
    @ConfigEditorDraggableList(exampleText = {
            "Wheat", "Carrot", "Potato", "Pumpkin", "Melon", "Sugar Cane", "Cocoa Beans",
            "Cactus", "Red Mushroom", "Brown Mushroom", "Nether Wart", "Seeds",
            "Squash", "Cropie", "Fermento"
    })
    public List<Integer> trackedCrops = new ArrayList<>(Arrays.asList(11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0, 12, 13, 14));

    @Expose
    @ConfigOption(name = "Display Lines", desc = "Choose which lines to show and drag to reorder")
    @ConfigEditorDraggableList(exampleText = {
            "§a§lOrganic Matter Tracker",
            "§aTotal Organic Matter: §f4,830,000",
            "§a402,500/h organic matter",
            "§1Playtime: §f2h 30m  §1Session: §f45m",
            "§bTotal: §f108,240 items"
    })
    public List<Integer> organicMatterDisplayLines = new ArrayList<>(Arrays.asList(0, 1, 2, 4, 3));

    @Expose
    @ConfigOption(name = "Edit Position", desc = "Drag to reposition the organic matter tracker overlay")
    @ConfigEditorButton(runnableId = "openOrganicMatterTrackerEditor", buttonText = "Edit")
    public boolean editOrganicMatterTrackerPosDummy = false;

    @Expose
    @ConfigOption(name = "Reset Tracker", desc = "Wipe the current session's tracked Organic Matter")
    @ConfigEditorButton(runnableId = "resetOrganicMatterTracker", buttonText = "Reset")
    public boolean resetOrganicMatterTrackerDummy = false;

    @Expose
    @ConfigOption(name = "Scale", desc = "Size of the organic matter tracker overlay")
    @ConfigEditorSliderAnnotation(minValue = 0.5f, maxValue = 3f, minStep = 0.1f)
    public float organicMatterTrackerScale = 1f;

    @Expose
    @ConfigOption(name = "Background Color", desc = "Background color of the organic matter tracker overlay")
    @ConfigEditorColour
    public int organicMatterTrackerBgColor = 0x80000000;

    @Expose
    @ConfigOption(name = "Corner Radius", desc = "Roundness of the organic matter tracker overlay corners")
    @ConfigEditorSliderAnnotation(minValue = 0f, maxValue = 12f, minStep = 1f)
    public int organicMatterTrackerCornerRadius = 4;

    @Expose
    public Position organicMatterTrackerPosition = new Position(-242, -80, false, false);
}

