package io.hamlook.aetheria.core.features.misc;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.*;
import io.hamlook.aetheria.utils.Position;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GhostTrackerConfig {

    @Expose
    @ConfigOption(name = "Enable Ghost Tracker", desc = "Show Ghost Tracker overlay while in Dwarven Mines / The Mist")
    @ConfigEditorBoolean
    public boolean ghostTrackerEnabled = true;

    @Expose
    @ConfigOption(name = "Pause on Chat", desc = "Pause tracking while chat GUI is open; resumes on next tracked action")
    @ConfigEditorBoolean
    public boolean pauseOnChat = false;

    @Expose
    @ConfigOption(name = "Ghost Tracker Lines", desc = "Choose which lines to show and drag to reorder. Drag lines to the trash to hide them.")
    @ConfigEditorDraggableList(exampleText = {
            "Kills: 1,234",
            "Kills/h: 567",
            "XP: 123.4M",
            "XP/h: 45.6M",
            "Sorrows: 12 (345)",
            "Voltas: 8 (678)",
            "Plasmas: 5 (901)",
            "Boots: 3 (234)",
            "Bag of Cash: 6 (567)",
            "Scavenger: 45,678",
            "Avg MF: 234",
            "Best MF: 567",
            "Time: 1h 23m",
            "Estimated Profit: 1.2M"
    })
    public List<Integer> ghostTrackerLines = new ArrayList<>(Arrays.asList(
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13
    ));

    @Expose
    @ConfigOption(name = "Drop Color", desc = "Colour of drop counters")
    @ConfigEditorColour
    public String dropColor = "0:255:255:85:85";

    @Expose
    @ConfigOption(name = "Kill Color", desc = "Colour of the kill counter")
    @ConfigEditorColour
    public String killColor = "0:255:85:255:255";

    @Expose
    @ConfigOption(name = "Magic Find Color", desc = "Colour of magic find values")
    @ConfigEditorColour
    public String mfColor = "0:255:255:170:0";

    @Expose
    @ConfigOption(name = "Scavenger Color", desc = "Colour of scavenger coin counter")
    @ConfigEditorColour
    public String scavengerColor = "0:255:255:170:0";

    @Expose
    @ConfigOption(name = "Coin Color", desc = "Colour of coin counters")
    @ConfigEditorColour
    public String coinColor = "0:255:255:255:0";

    @Expose
    @ConfigOption(name = "Background Color", desc = "Background colour of the overlay")
    @ConfigEditorColour
    public String ghostBgColor = "0:0:0:0:0";

    @Expose
    @ConfigOption(name = "Corner Radius", desc = "Roundness of the overlay corners")
    @ConfigEditorSliderAnnotation(minValue = 0f, maxValue = 12f, minStep = 1f)
    public int ghostCornerRadius = 4;

    @Expose
    @ConfigOption(name = "Scale", desc = "Size of the overlay")
    @ConfigEditorSliderAnnotation(minValue = 0.5f, maxValue = 3f, minStep = 0.1f)
    public float ghostScale = 1f;

    @Expose
    @ConfigOption(name = "Toggle Key", desc = "Keybind to pause/resume the ghost session tracker")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    public int ghostToggleKey = Keyboard.KEY_NONE;

    @Expose
    @ConfigOption(name = "Reset Key", desc = "Keybind to reset the ghost session tracker")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    public int ghostResetKey = Keyboard.KEY_NONE;

    @Expose
    @ConfigOption(name = "Edit Overlay Position", desc = "Drag to reposition the ghost tracker overlay")
    @ConfigEditorButton(runnableId = "openGhostEditor", buttonText = "Edit")
    public boolean ghostEditPosDummy = false;

    @Expose
    @ConfigOption(name = "Reset Tracker", desc = "Wipe all tracked ghost data")
    @ConfigEditorButton(runnableId = "resetGhostTracker", buttonText = "Reset")
    public boolean ghostResetDummy = false;

    @Expose
    public Position ghostOverlayPos = new Position(8, 29, false, false);
}
