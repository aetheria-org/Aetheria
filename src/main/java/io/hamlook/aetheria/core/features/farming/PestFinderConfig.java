package io.hamlook.aetheria.core.features.farming;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.*;
import io.hamlook.aetheria.utils.Position;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PestFinderConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Show the Garden pest tab data (total pests, plots, spray, repellent, bonus, cooldown) as an overlay")
    @ConfigEditorBoolean
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "Warp Key", desc = "Hold to teleport to the nearest infested plot (/tptoplot). Requires FakePixel or another mod with that command")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    public int warpKey = Keyboard.KEY_NONE;

    @Expose
    @ConfigOption(name = "Hide Warp In Current Plot", desc = "Hide the warp keybind hint and suppress the keybind when you are already in a plot with a pest")
    @ConfigEditorBoolean
    public boolean hideWarpHintInPlot = true;

    @Expose
    @ConfigOption(name = "Show While Holding Vacuum Only", desc = "Only show the overlay while holding a vacuum")
    @ConfigEditorBoolean
    public boolean showOnlyWhileHoldingVacuum = true;

    @Expose
    @ConfigOption(name = "Hide While Farming", desc = "Hide the overlay while actively farming (holding a farming tool and breaking crops)")
    @ConfigEditorBoolean
    public boolean hideWhileFarming = true;

    @Expose
    @ConfigOption(name = "Hide While Holding Farming Tool", desc = "Hide the overlay while holding a farming tool")
    @ConfigEditorBoolean
    public boolean hideOnFarmingTool = true;

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
    @ConfigOption(name = "Display Lines", desc = "Choose which pest finder lines to show and drag to reorder")
    @ConfigEditorDraggableList(exampleText = {
            "§7Total: §e2",
            "§7Plots: §b2, 4",
            "§7Spray: §7None",
            "§7Repellent: §7None",
            "§7Bonus: §c§lINACTIVE",
            "§7Cooldown: §a§lACTIVE",
            "§7Bonus Pest Chance: §245",
            "§7Press §e<Key> §7to warp to §bPlot 4"
    })
    public List<Integer> pestFinderLines = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7));

    @Expose
    @ConfigOption(name = "Edit Position", desc = "Drag to reposition the pest finder overlay")
    @ConfigEditorButton(runnableId = "openPestFinderEditor", buttonText = "Edit")
    public boolean editPestFinderPosDummy = false;

    @Expose
    @ConfigOption(name = "Scale", desc = "Size of the pest finder overlay")
    @ConfigEditorSliderAnnotation(minValue = 0.5f, maxValue = 3f, minStep = 0.1f)
    public float scale = 1f;

    @Expose
    @ConfigOption(name = "Background Color", desc = "Background color of the pest finder overlay")
    @ConfigEditorColour
    public int bgColor = 0x80000000;

    @Expose
    @ConfigOption(name = "Corner Radius", desc = "Roundness of the pest finder overlay corners")
    @ConfigEditorSliderAnnotation(minValue = 0f, maxValue = 12f, minStep = 1f)
    public int cornerRadius = 4;

    @Expose
    public Position pestFinderPos = new Position(-368, 52, false, false);
}