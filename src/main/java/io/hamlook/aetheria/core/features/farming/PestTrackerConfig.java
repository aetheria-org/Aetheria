package io.hamlook.aetheria.core.features.farming;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.*;
import io.hamlook.aetheria.utils.Position;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PestTrackerConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Track pest kills and crop drops in the Garden and show the pest tracker overlay")
    @ConfigEditorBoolean
    public boolean enabled = true;

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
    @ConfigOption(name = "Hide While Farming", desc = "Hide the overlay while actively farming (holding a farming tool and breaking crops)")
    @ConfigEditorBoolean
    public boolean hideWhileFarming = true;

    @Expose
    @ConfigOption(name = "Hide While Holding Farming Tool", desc = "Hide the overlay while holding a farming tool")
    @ConfigEditorBoolean
    public boolean hideOnFarmingTool = true;

    @Expose
    @ConfigOption(name = "Show Icons", desc = "Show item icons next to crop drop lines")
    @ConfigEditorBoolean
    public boolean showIcons = true;

    @Expose
    @ConfigOption(name = "Rate Basis", desc = "Basis for per-hour rates: all-time playtime or this session's playtime")
    @ConfigEditorDropdown(values = {"All Time", "Session"})
    public int rateBasis = 0;

    @Expose
    @ConfigOption(name = "Display Lines", desc = "Choose which pest tracker lines to show and drag to reorder")
    @ConfigEditorDraggableList(exampleText = {
            "§7Pests: §e125 §7(§b50/h§7)",
            "§7Drops: §a1,523 §7(§b600/h§7)",
            "§7Session: §f42:17",
            "§7Total: §e42:17",
            "§6Slug §7- §a125 §7(§b1k/h§7)",
            "§7- Enchanted Red Mushroom §a1523 §7(§b1k/h§7)",
            "§71,234,567 coins §7(2.1M/h)"
    })
    public List<Integer> pestTrackerLines = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6));

    @Expose
    @ConfigOption(name = "Edit Position", desc = "Drag to reposition the pest tracker overlay")
    @ConfigEditorButton(runnableId = "openPestEditor", buttonText = "Edit")
    public boolean editPestPosDummy = false;

    @Expose
    @ConfigOption(name = "Reset Tracker", desc = "Wipe all tracked pest kills and drops")
    @ConfigEditorButton(runnableId = "resetPestTracker", buttonText = "Reset")
    public boolean resetPestTrackerDummy = false;

    @Expose
    @ConfigOption(name = "Scale", desc = "Size of the pest tracker overlay")
    @ConfigEditorSliderAnnotation(minValue = 0.5f, maxValue = 3f, minStep = 0.1f)
    public float scale = 1f;

    @Expose
    @ConfigOption(name = "Background Color", desc = "Background color of the pest tracker overlay")
    @ConfigEditorColour
    public int bgColor = 0x80000000;

    @Expose
    @ConfigOption(name = "Corner Radius", desc = "Roundness of the pest tracker overlay corners")
    @ConfigEditorSliderAnnotation(minValue = 0f, maxValue = 12f, minStep = 1f)
    public int cornerRadius = 4;

    @Expose
    public Position pestOverlayPos = new Position(0, 230, false, false);
}