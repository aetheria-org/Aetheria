package io.hamlook.aetheria.core.features.dungeons;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.*;
import io.hamlook.aetheria.features.chestanimations.ChestAnimations;

public class DungeonChestAnimationConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Play the selected chest animation when opening dungeon chests")
    @ConfigEditorBoolean
    public boolean caseOpeningAnimation = false;

    @Expose
    @ConfigOption(name = "Wood Chest Animation", desc = "Animation played when opening a Wood chest")
    @ConfigEditorDropdown(values = {ChestAnimations.NONE, ChestAnimations.CASE_OPENING})
    public int woodChestAnimation = 0;

    @Expose
    @ConfigOption(name = "Gold Chest Animation", desc = "Animation played when opening a Gold chest")
    @ConfigEditorDropdown(values = {ChestAnimations.NONE, ChestAnimations.CASE_OPENING})
    public int goldChestAnimation = 0;

    @Expose
    @ConfigOption(name = "Emerald Chest Animation", desc = "Animation played when opening an Emerald chest")
    @ConfigEditorDropdown(values = {ChestAnimations.NONE, ChestAnimations.CASE_OPENING})
    public int emeraldChestAnimation = 0;

    @Expose
    @ConfigOption(name = "Diamond Chest Animation", desc = "Animation played when opening a Diamond chest")
    @ConfigEditorDropdown(values = {ChestAnimations.NONE, ChestAnimations.CASE_OPENING})
    public int diamondChestAnimation = 0;

    @Expose
    @ConfigOption(name = "Obsidian Chest Animation", desc = "Animation played when opening an Obsidian chest")
    @ConfigEditorDropdown(values = {ChestAnimations.NONE, ChestAnimations.CASE_OPENING}, initialIndex = 1)
    public int obsidianChestAnimation = 1;

    @Expose
    @ConfigOption(name = "Bedrock Chest Animation", desc = "Animation played when opening a Bedrock chest")
    @ConfigEditorDropdown(values = {ChestAnimations.NONE, ChestAnimations.CASE_OPENING}, initialIndex = 1)
    public int bedrockChestAnimation = 1;

    @Expose
    @ConfigOption(name = "Show Item Names", desc = "Show item names below each slot in the carousel")
    @ConfigEditorBoolean
    public boolean caseOpeningAllowText = true;

    @Expose
    @ConfigOption(name = "Text Scale", desc = "Scale of the item name text in the carousel")
    @ConfigEditorSliderAnnotation(minValue = 0.3f, maxValue = 2f, minStep = 0.1f)
    public float caseOpeningTextScale = 0.5f;

    @Expose
    @ConfigOption(name = "Slow Time", desc = "Time in seconds to decelerate from the slow point to the reward slot")
    @ConfigEditorSliderAnnotation(minValue = 0.5f, maxValue = 10f, minStep = 0.5f)
    public float caseOpeningSlowTime = 3f;

    @Expose
    @ConfigOption(name = "Slow Distance", desc = "Number of slots before the reward where the carousel starts to slow down")
    @ConfigEditorSliderAnnotation(minValue = 1f, maxValue = 20f, minStep = 1f)
    public int caseOpeningSlowDistance = 8;
}