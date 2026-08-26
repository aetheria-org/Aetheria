package io.hamlook.aetheria.core.features.scoreboard;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.*;
import io.hamlook.aetheria.utils.Position;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Scoreboard {

    @Expose
    @ConfigOption(name = "Enable", desc = "Replace the vanilla sidebar with a custom scoreboard")
    @ConfigEditorBoolean
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "Background Color", desc = "Background color of the scoreboard")
    @ConfigEditorColour
    public String scoreboardBg = "0:136:0:0:0";

    @Expose
    @ConfigOption(name = "Corner Radius", desc = "Roundness of the scoreboard corners")
    @ConfigEditorSliderAnnotation(minValue = 0f, maxValue = 20f, minStep = 1f)
    public float cornerRadius = 8f;

    @Expose
    @ConfigOption(name = "Scale", desc = "Size of the scoreboard")
    @ConfigEditorSliderAnnotation(minValue = 0.5f, maxValue = 2.5f, minStep = 0.1f)
    public float scale = 1.0f;

    @Expose
    @ConfigOption(name = "Minimum Width", desc = "Minimum width of the scoreboard in pixels (prevents it shrinking too small)")
    @ConfigEditorSliderAnnotation(minValue = 60f, maxValue = 300f, minStep = 5f)
    public int minWidth = 100;

    @Expose
    @ConfigOption(name = "Line Alignment", desc = "Alignment for all lines except the title (which is always centered)")
    @ConfigEditorDropdown(values = {"Left", "Center", "Right"})
    public int lineAlignment = 0;

    @Expose
    @ConfigOption(name = "Hide when Tab held", desc = "Hide the scoreboard when the tab key is held")
    @ConfigEditorBoolean
    public boolean hideOnTab = true;

    @Expose
    @ConfigOption(name = "Hide on F3 debug", desc = "Hide the scoreboard when the F3 debug screen is open")
    @ConfigEditorBoolean
    public boolean hideOnDebug = true;

    @Expose
    @ConfigOption(name = "Edit Position", desc = "Drag to reposition the scoreboard")
    @ConfigEditorButton(runnableId = "openScoreboardEditor", buttonText = "Edit")
    public boolean editPosDummy = false;

    @Expose
    @ConfigOption(name = "Scoreboard Lines", desc = "Choose which lines to show and drag to reorder. Unrecognised lines are grouped under 'Extra Lines' — drag it to the bin to hide them all.")
    @ConfigEditorDraggableList(exampleText = {
            "§e03/15/26 §8hub-67",
            "§f10:40pm",
            "§7♲ Ironman (Profile Type)",
            "§fLate Summer §b11th",
            "§7㋖ §bSkyblock Hub",                                                                                           // 4  ISLAND
            "§7⏣ §bVillage",                                                                                                 // 5  LOCATION
            "§8─────────────────",                                                                                           // 6  EMPTY
            "§fPurse: §6952,763,737",                                                                                        // 7  PURSE
            "§fBank: §6969M",                                                                                                // 8  BANK
            "§9§lPowder\n §7- §fMithril: §21.2M\n §7- §fGemstone: §d800K\n §7- §fGlacite: §b250K",                         // 10 POWDER
            "§cHeat: §c♨ 14",
            "§fBits: §b59,364,034",
            "§fGems: §a67,676,767",
            "§dNorth Stars: §d756",
            "§6Fishing/Mining Fiesta(variety of Events) §f12m 30s",
            "§fPower: §dBizzare",
            "§dCookie Buff: §f67d 21h",
            "§8─────────────────",
            "§fFetchur: §eMilk",
            "§fSlayer Quest\n§4Voidgloom Seraph IV\n§7(17/6,767) Combat XP",
            "§8──────────────────(emptyline)",
            "§8──────────────────(emptyline)",
            "§8──────────────────(emptyline)",
            "§8──────────────────(emptyline)",
            "§8[?] §7Lines that the mod doesnt detect",
            "§8─────────────────",
            "§fCopper: §c359,485,574",
            "§aPlot §7- §b20",
            "§fCleanup: §a75%"

    })
    public List<Integer> scoreboardLines = new ArrayList<>(Arrays.asList(
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 24, 25, 26, 27, 28
    ));

    @Expose
    @ConfigOption(name = "Unknown Lines Warning", desc = "Show a chat warning when the scoreboard contains a line the mod doesn't recognise.\n§eReporting these in the Discord helps get them added!")
    @ConfigEditorBoolean
    public boolean unknownLinesWarning = false;

    @Expose
    public Position position = new Position(-2, 140);
}