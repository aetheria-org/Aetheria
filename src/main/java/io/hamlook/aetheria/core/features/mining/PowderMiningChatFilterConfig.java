package io.hamlook.aetheria.core.features.mining;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.*;

public class PowderMiningChatFilterConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Hide powder mining reward popup and chat messages")
    @ConfigEditorBoolean
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Chest Uncovered", desc = "\"You uncovered a treasure chest!\"")
    @ConfigEditorBoolean
    public boolean hideChestOpen = true;

    @Expose
    @ConfigOption(name = "Chest Already Looted", desc = "\"This chest has already been looted.\"")
    @ConfigEditorBoolean
    public boolean hideChestAlreadyLooted = true;

    @Expose
    @ConfigOption(name = "Breaking Power Warning", desc = "Hide the \"You need a tool with a Breaking Power of …\" warning")
    @ConfigEditorBoolean
    public boolean hideBreakingPower = true;

    @Expose
    @ConfigOption(name = "Compact Messages", desc = "\"COMPACT! You found an Enchanted Hard Stone!\"")
    @ConfigEditorBoolean
    public boolean hideCompact = false;

    @Expose
    @ConfigOption(name = "Reward Wrappers", desc = "Hide ▬▬▬ separator lines, CHEST LOCKPICKED, LOOT CHEST COLLECTED and REWARDS headers")
    @ConfigEditorBoolean
    public boolean hideRewardWrappers = true;

    @Expose
    @ConfigOption(name = "Hide Powder", desc = "Hide Gemstone Powder and Mithril Powder reward lines")
    @ConfigEditorBoolean
    public boolean hidePowder = true;

    @Expose
    @ConfigOption(name = "Powder Threshold", desc = "Only hide powder lines below this amount (0 = hide all, 60000 = hide none)")
    @ConfigEditorSliderAnnotation(minValue = 0f, maxValue = 60000f, minStep = 100f)
    public int powderThreshold = 0;

    @Expose
    @ConfigOption(name = "Hide Essence", desc = "Hide Diamond Essence and Gold Essence reward lines")
    @ConfigEditorBoolean
    public boolean hideEssence = true;

    @Expose
    @ConfigOption(name = "Essence Threshold", desc = "Only hide essence lines below this amount (0 = hide all, 20 = hide none)")
    @ConfigEditorSliderAnnotation(minValue = 0f, maxValue = 20f, minStep = 1f)
    public int essenceThreshold = 0;

    @Expose
    @ConfigOption(name = "Hide Gemstones", desc = "Hide gemstone drop lines (tier filter below)")
    @ConfigEditorBoolean
    public boolean hideGemstones = true;

    @Expose
    @ConfigOption(name = "Gemstone Tier Filter", desc = "Which gemstone tiers to hide: Show All, Hide Rough, Hide Rough & Flawed, or Hide All")
    @ConfigEditorDropdown(values = {"Show All", "Hide Rough", "Hide Rough & Flawed", "Hide All"})
    public String gemstoneTierFilter = "Hide Rough";

    @Expose
    @ConfigOption(name = "Hide Goblin Eggs", desc = "Hide all Goblin Egg reward lines")
    @ConfigEditorBoolean
    public boolean hideGoblinEggs = false;

    @Expose
    @ConfigOption(name = "Oil Barrel", desc = "Hide Oil Barrel rewards")
    @ConfigEditorBoolean
    public boolean hideOilBarrel = true;

    @Expose
    @ConfigOption(name = "Ascension Rope", desc = "Hide Ascension Rope rewards")
    @ConfigEditorBoolean
    public boolean hideAscensionRope = true;

    @Expose
    @ConfigOption(name = "Wishing Compass", desc = "Hide Wishing Compass rewards")
    @ConfigEditorBoolean
    public boolean hideWishingCompass = true;

    @Expose
    @ConfigOption(name = "Jungle Heart", desc = "Hide Jungle Heart rewards")
    @ConfigEditorBoolean
    public boolean hideJungleHeart = true;

    @Expose
    @ConfigOption(name = "Prehistoric Egg", desc = "Hide Prehistoric Egg rewards")
    @ConfigEditorBoolean
    public boolean hidePrehistoricEgg = true;

    @Expose
    @ConfigOption(name = "Pickonimbus 2000", desc = "Hide Pickonimbus 2000 rewards")
    @ConfigEditorBoolean
    public boolean hidePickonimbus = true;

    @Expose
    @ConfigOption(name = "Sludge Juice", desc = "Hide Sludge Juice rewards")
    @ConfigEditorBoolean
    public boolean hideSludgeJuice = true;

    @Expose
    @ConfigOption(name = "Yoggie", desc = "Hide Yoggie rewards")
    @ConfigEditorBoolean
    public boolean hideYoggie = true;

    @Expose
    @ConfigOption(name = "Robot Parts", desc = "Hide Robot Parts (FTX 3070, Synthetic Heart, etc.) rewards")
    @ConfigEditorBoolean
    public boolean hideRobotParts = true;

    @Expose
    @ConfigOption(name = "Treasurite", desc = "Hide Treasurite rewards")
    @ConfigEditorBoolean
    public boolean hideTreasurite = true;
}
