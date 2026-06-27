package io.hamlook.aetheria.core.features.mining;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.*;
import io.hamlook.aetheria.utils.Position;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PowderTrackerConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Show the Powder Tracker overlay while in Crystal Hollows")
    @ConfigEditorBoolean
    public boolean powderTracker = true;

    @Expose
    @ConfigOption(name = "Pause on Chat", desc = "Pause tracking while chat GUI is open; resumes on next tracked action")
    @ConfigEditorBoolean
    public boolean pauseOnChat = false;

    @Expose
    @ConfigOption(name = "Toggle Key", desc = "Keybind to pause/resume the powder tracker")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    public int powderToggleKey = Keyboard.KEY_NONE;

    @Expose
    @ConfigOption(name = "Background Color", desc = "Background color of the powder tracker overlay")
    @ConfigEditorColour
    public String powderBgColor = "0:136:0:0:0";

    @Expose
    @ConfigOption(name = "Corner Radius", desc = "Roundness of the powder tracker overlay corners")
    @ConfigEditorSliderAnnotation(minValue = 0f, maxValue = 12f, minStep = 1f)
    public int powderCornerRadius = 4;

    @Expose
    @ConfigOption(name = "Scale", desc = "Size of the powder tracker overlay")
    @ConfigEditorSliderAnnotation(minValue = 0.5f, maxValue = 3f, minStep = 0.1f)
    public float powderOverlayScale = 1f;

    @Expose
    @ConfigOption(name = "Edit Position", desc = "Drag to reposition the powder tracker overlay")
    @ConfigEditorButton(runnableId = "openPowderEditor", buttonText = "Edit")
    public boolean editPowderPosDummy = false;

    @Expose
    @ConfigOption(name = "Reset Tracker", desc = "Wipe all tracked powder and drop data")
    @ConfigEditorButton(runnableId = "resetPowderTracker", buttonText = "Reset")
    public boolean resetPowderDummy = false;

    @Expose
    @ConfigOption(name = "Display Lines", desc = "Choose which lines to show and drag to reorder")
    @ConfigEditorDraggableList(exampleText = {"§b§lPowder Tracker",
            "§7420 Chests §7(120/h)",
            "§b2x Powder: §aActive!",
            "§d1,337 Gemstone Powder §7(2.5K/h)",
            "§1Playtime: §f2h 30m  §1Session: §f45m",
            "§b12 Diamond Essence",
            "§66 Gold Essence",
            "§88 Oil Barrels",
            "§53 Ascension Ropes",
            "§92 Wishing Compasses",
            "§61 Jungle Hearts",
            "§a512 Enchanted Hard Stone §8(5 compact) §7(1.5K/h)",
            "§51-§93-§a4-§f0 §cRuby",
            "§51-§93-§a4-§f0 §bSapphire",
            "§51-§93-§a4-§f0 §6Amber",
            "§51-§93-§a4-§f0 §5Amethyst",
            "§51-§93-§a4-§f0 §aJade",
            "§51-§93-§a4-§f0 §eTopaz",
            "§51-§93-§a4-§f0 §cJasper",
            "§51-§93-§a4-§f0 §fOpal",
            "§51-§93-§a4-§f0 §6Citrine",
            "§51-§93-§a4-§f0 §3Aquamarine",
            "§51-§93-§a4-§f0 §aPeridot",
            "§51-§93-§a4-§f0 §8Onyx",
            "§33-§c2-§e1-§a1-§91 §fGoblin Eggs"
    })
    public List<Integer> powderDisplayLines = new ArrayList<>(Arrays.asList(0,24,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23));

    @Expose
    public Position powderOverlayPos = new Position(4, 60);
}
