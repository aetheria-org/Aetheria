package io.hamlook.aetheria.core.features.mining;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.*;
import io.hamlook.aetheria.utils.Position;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GoldTrackerConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Show the Gold Tracker overlay while mining gold")
    @ConfigEditorBoolean
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "Track Mode", desc = "Which gold item to track")
    @ConfigEditorDropdown(values = {"Track Ingot", "Track Enchanted"})
    public String trackMode = "Track Ingot";

    @Expose
    @ConfigOption(name = "Display Unit", desc = "How totals and rates are displayed")
    @ConfigEditorDropdown(values = {"Show as Ingots", "Show as Enchanted"})
    public String displayUnit = "Show as Ingots";

    @Expose
    @ConfigOption(name = "Profit Unit", desc = "Which unit to calculate profit with")
    @ConfigEditorDropdown(values = {"As Ingots", "As Enchanted", "As Blocks"})
    public String profitUnit = "As Blocks";

    @Expose
    @ConfigOption(name = "Only Track While Mining", desc = "Only count pickups when a gold ore/block was mined in the last 20 seconds")
    @ConfigEditorBoolean
    public boolean onlyTrackWhileMining = true;

    @Expose
    @ConfigOption(name = "Show Icons", desc = "Show item icons next to total and rate lines")
    @ConfigEditorBoolean
    public boolean showIcons = true;

    @Expose
    @ConfigOption(name = "Show Compacts", desc = "Show compact drop count and rate")
    @ConfigEditorBoolean
    public boolean showCompacts = true;

    @Expose
    @ConfigOption(name = "Show Mining Stats", desc = "Show mining speed, fortune, spread, and pristine from the tablist")
    @ConfigEditorBoolean
    public boolean showStats = false;

    @Expose
    @ConfigOption(name = "Pause on Chat", desc = "Pause tracking while chat GUI is open")
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
    @ConfigOption(name = "Hide When Paused", desc = "Hide the overlay when the tracker is paused")
    @ConfigEditorBoolean
    public boolean hideWhenPaused = true;

    @Expose
    @ConfigOption(name = "Background Color", desc = "Background color of the gold tracker overlay")
    @ConfigEditorColour
    public String bgColor = "0:136:0:0:0";

    @Expose
    @ConfigOption(name = "Corner Radius", desc = "Roundness of the overlay corners")
    @ConfigEditorSliderAnnotation(minValue = 0f, maxValue = 12f, minStep = 1f)
    public int cornerRadius = 4;

    @Expose
    @ConfigOption(name = "Scale", desc = "Size of the overlay")
    @ConfigEditorSliderAnnotation(minValue = 0.5f, maxValue = 3f, minStep = 0.1f)
    public float scale = 1f;

    @Expose
    @ConfigOption(name = "Edit Position", desc = "Drag to reposition the overlay")
    @ConfigEditorButton(runnableId = "openGoldEditor", buttonText = "Edit")
    public boolean editPosDummy = false;

    @Expose
    @ConfigOption(name = "Reset Tracker", desc = "Wipe all tracked gold data")
    @ConfigEditorButton(runnableId = "resetGoldTracker", buttonText = "Reset")
    public boolean resetDummy = false;

    @Expose
    @ConfigOption(name = "Display Lines", desc = "Choose which lines to show and drag to reorder")
    @ConfigEditorDraggableList(exampleText = {
        "§f1,200 gold ingots/h",
        "§f42.5K gold ingots",
        "§6Profit: §a12.3M coins §7(340K/h)",
        "§7Playtime: §e2h 30m  §7Session: §f45m",
        "§a42 compact §7(10/h)",
        "§fMining Speed: §6⸕2,940",
        "§fMining Fortune: §6☘1,134",
        "§fMining Spread: §e▚135"
    })
    public List<Integer> displayLines = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5));

    @Expose
    public Position goldOverlayPos = new Position(4, 220);
}
