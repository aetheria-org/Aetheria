package io.hamlook.aetheria.core.features.farming;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.*;
import io.hamlook.aetheria.utils.Position;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FarmingTrackerConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Track crop value and show the farming profit overlay")
    @ConfigEditorBoolean
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Require Farming Location", desc = "Only track crops while in farming locations (Barn, Private Island)")
    @ConfigEditorBoolean
    public boolean requireFarmingIsland = true;

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
    @ConfigOption(name = "Display Lines", desc = "Choose which crop lines to show and drag to reorder")
    @ConfigEditorDraggableList(exampleText = {
            "§a§lFarming Tracker",
            "§76,144,000 coins §7(3.2M/h)",
            "§aWheat: §f2,304 §7E.Wheat: §f12 §7E.Hay Bale: §f1",
            "§aCarrot: §f1,120 §7E.Carrot: §f6",
            "§aPotato: §f980 §7E.Potato: §f4",
            "§aPumpkin: §f560 §7E.Pumpkin: §f3 §7Polished: §f1",
            "§aMelon: §f8 §7E.Melon: §f92 §7E.Melon Block: §f2",
            "§aSugar Cane: §f640 §7E.Sugar: §f5",
            "§aCocoa Beans: §f300 §7E.Cocoa Beans: §f2",
            "§aCactus: §f150 §7E.Cactus Green: §f3",
            "§aRed Mushroom: §f80 §7E.Red Mushroom: §f1",
            "§aBrown Mushroom: §f75 §7E.Brown Mushroom: §f1",
            "§aNether Wart: §f420 §7E.Nether Wart: §f9",
            "§aWild Rose: §f40 §7E.Wild Rose: §f2",
            "§aMoonflower: §f30 §7E.Moonflower: §f1",
            "§aSunflower: §f35 §7E.Sunflower: §f1"
    })
    public List<Integer> farmingDisplayLines = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15));

    @Expose
    @ConfigOption(name = "Edit Position", desc = "Drag to reposition the farming tracker overlay")
    @ConfigEditorButton(runnableId = "openFarmingTrackerEditor", buttonText = "Edit")
    public boolean editFarmingTrackerPosDummy = false;

    @Expose
    @ConfigOption(name = "Reset Tracker", desc = "Wipe the current session's tracked crop counts")
    @ConfigEditorButton(runnableId = "resetFarmingTracker", buttonText = "Reset")
    public boolean resetFarmingTrackerDummy = false;

    @Expose
    @ConfigOption(name = "Scale", desc = "Size of the farming tracker overlay")
    @ConfigEditorSliderAnnotation(minValue = 0.5f, maxValue = 3f, minStep = 0.1f)
    public float farmingTrackerScale = 1f;

    @Expose
    @ConfigOption(name = "Background Color", desc = "Background color of the farming tracker overlay")
    @ConfigEditorColour
    public int farmingTrackerBgColor = 0x80000000;

    @Expose
    @ConfigOption(name = "Corner Radius", desc = "Roundness of the farming tracker overlay corners")
    @ConfigEditorSliderAnnotation(minValue = 0f, maxValue = 12f, minStep = 1f)
    public int farmingTrackerCornerRadius = 4;

    @Expose
    public Position farmingTrackerPosition = new Position(10, 120, false, false);
}
