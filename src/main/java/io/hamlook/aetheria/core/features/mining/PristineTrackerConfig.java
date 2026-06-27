package io.hamlook.aetheria.core.features.mining;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.*;
import io.hamlook.aetheria.utils.Position;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PristineTrackerConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Show the Pristine Tracker overlay while in Crystal Hollows")
    @ConfigEditorBoolean
    public boolean pristineTracker = true;

    @Expose
    @ConfigOption(name = "Show Compacted", desc = "Display gemstones in compacted format (Flawless-Fine-Flawed)")
    @ConfigEditorBoolean
    public boolean showCompacted = false;

    @Expose
    @ConfigOption(name = "Pause on Chat", desc = "Pause tracking while chat GUI is open; resumes on next tracked action")
    @ConfigEditorBoolean
    public boolean pauseOnChat = false;

    @Expose
    @ConfigOption(name = "Toggle Key", desc = "Keybind to pause/resume the pristine tracker")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    public int pristineToggleKey = Keyboard.KEY_NONE;

    @Expose
    @ConfigOption(name = "Background Color", desc = "Background color of the pristine tracker overlay")
    @ConfigEditorColour
    public String pristineBgColor = "0:136:0:0:0";

    @Expose
    @ConfigOption(name = "Corner Radius", desc = "Roundness of the pristine tracker overlay corners")
    @ConfigEditorSliderAnnotation(minValue = 0f, maxValue = 12f, minStep = 1f)
    public int pristineCornerRadius = 4;

    @Expose
    @ConfigOption(name = "Scale", desc = "Size of the pristine tracker overlay")
    @ConfigEditorSliderAnnotation(minValue = 0.5f, maxValue = 3f, minStep = 0.1f)
    public float pristineOverlayScale = 1f;

    @Expose
    @ConfigOption(name = "Edit Position", desc = "Drag to reposition the pristine tracker overlay")
    @ConfigEditorButton(runnableId = "openPristineEditor", buttonText = "Edit")
    public boolean editPristinePosDummy = false;

    @Expose
    @ConfigOption(name = "Reset Tracker", desc = "Wipe all tracked pristine gemstone data")
    @ConfigEditorButton(runnableId = "resetPristineTracker", buttonText = "Reset")
    public boolean resetPristineDummy = false;

    @Expose
    @ConfigOption(name = "Display Lines", desc = "Choose which lines to show and drag to reorder")
    @ConfigEditorDraggableList(exampleText = {
            "§d§lPristine Tracker",
            "§7Total Gems: §a1,500 §7(150/h)",
            "§7Procs: §d42 §7(5/h)",
            "§1Playtime: §f2h 30m  §1Session: §f45m",
            "§a200 §cRuby Gemstone",
            "§a200 §bSapphire Gemstone",
            "§a200 §6Amber Gemstone",
            "§a200 §5Amethyst Gemstone",
            "§a200 §aJade Gemstone",
            "§a200 §eTopaz Gemstone",
            "§a200 §cJasper Gemstone",
            "§a200 §fOpal Gemstone",
            "§a200 §6Citrine Gemstone",
            "§a200 §3Aquamarine Gemstone",
            "§a200 §aPeridot Gemstone",
            "§a200 §8Onyx Gemstone"
    })
    public List<Integer> pristineDisplayLines = new ArrayList<>(Arrays.asList(0, 15, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14));

    @Expose
    public Position pristineOverlayPos = new Position(4, -5);
}
