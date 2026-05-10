package com.jef.justenoughfakepixel.core.features.misc;

import com.google.gson.annotations.Expose;
import com.jef.justenoughfakepixel.core.config.gui.config.ConfigAnnotations.*;

public class Misc {

    @Expose
    @Category(name = "Performance HUD", desc = "Settings for the performance HUD")
    public PerformanceHudConfig performanceHudConfig = new PerformanceHudConfig();

    @Expose
    @Category(name = "Search Bar", desc = "Search bar settings")
    public SearchBarConfig searchBarConfig = new SearchBarConfig();

    @Expose
    @Category(name = "Current Pet", desc = "Shows your active pet as a HUD overlay")
    public CurrentPetConfig currentPet = new CurrentPetConfig();

    @Expose
    @Category(name = "Item Pickup Log", desc = "Settings for the item pickup log")
    public ItemPickupLogConfig itemPickupLogConfig = new ItemPickupLogConfig();

    @Expose
    @Category(name = "Inventory Buttons", desc = "Clickable shortcut buttons on inventories")
    public InvButtonsConfig invButtons = new InvButtonsConfig();

    @Expose
    @Category(name = "JEFProtect", desc = "Prevent dropping protected items; use /jefprotect to toggle protection on the held item")
    public ProtectItemConfig protectItem = new ProtectItemConfig();

    // ── standalone options ───────────────────────────────────────────────────
    @Expose
    @ConfigOption(name = "Item Stack Tips", desc = "Shows enchant levels on books and floor numbers on Catacombs passes")
    @ConfigEditorBoolean
    public boolean itemStackTips = true;

    @Expose
    @ConfigOption(name = "Party Finder Floor Tip", desc = "Shows floor label (F1-F7, M1-M7) on listings in the Party Finder")
    @ConfigEditorBoolean
    public boolean partyFinderFloorTip = true;

    @Expose
    @ConfigOption(name = "Skill XP Display", desc = "Hold SHIFT on a skill item to see XP remaining to max level")
    @ConfigEditorBoolean
    public boolean skillXpDisplay = true;

    @Expose
    @ConfigOption(name = "No Swap Animation", desc = "Removes the item lowering animation when switching hotbar slots")
    @ConfigEditorBoolean
    public boolean noItemSwitchAnimation = true;

    @Expose
    @ConfigOption(name = "Show Own Nametag", desc = "Shows your own nametag in third person")
    @ConfigEditorBoolean
    public boolean showOwnNametag = true;

    @Expose
    @ConfigOption(name = "Disable Entity Fire", desc = "Hides the fire overlay rendered on burning entities")
    @ConfigEditorBoolean
    public boolean disableEntityFire = true;

    @Expose
    @ConfigOption(name = "SkyBlock XP in Chat", desc = "Sends SkyBlock XP gains from the action bar into chat")
    @ConfigEditorBoolean
    public boolean skyblockXpInChat = false;

    @Expose
    @ConfigOption(name = "Sign Calculator", desc = "Auto-calculate expressions on signs when line 2 is ^^^^^^ (e.g., 3m x 3 → 9000000)")

    @ConfigEditorBoolean
    public boolean signCalculator = true;

    @Expose
    @ConfigOption(name = "Hoppity Highlight", desc = "Highlights new rabbit items in Hoppity's egg menu")
    @ConfigEditorBoolean
    public boolean hoppityHighlight = true;

    @Expose
    @ConfigOption(name = "DVD", desc = "Description(yes)")
    @ConfigEditorBoolean
    public boolean dvdScreensaver = false;

    @Expose
    @ConfigOption(name = "DVD Size", desc = "Size of the DVD logo (width in pixels)")
    @ConfigEditorSliderAnnotation(minValue = 50, maxValue = 300, minStep = 10)
    public int dvdSize = 80;
}