package com.jef.justenoughfakepixel.core.features.qol;

import com.google.gson.annotations.Expose;
import com.jef.justenoughfakepixel.core.config.gui.config.ConfigAnnotations.Category;
import com.jef.justenoughfakepixel.core.config.gui.config.ConfigAnnotations.ConfigEditorBoolean;
import com.jef.justenoughfakepixel.core.config.gui.config.ConfigAnnotations.ConfigOption;

public class Qol {

    @Expose
    @Category(name = "Damage Splashes", desc = "Settings for damage number nametags")
    public DamageSplashesConfig damageSplashes = new DamageSplashesConfig();

    @Expose
    @Category(name = "Enchant Parser", desc = "Settings for enchants and layout")
    public EnchantParserConfig enchantParser = new EnchantParserConfig();

    @Expose
    @Category(name = "Gyro Wand Ring", desc = "Settings for the Gyrokinetic Wand AoE ring")
    public GyroWandConfig gyroWandConfig = new GyroWandConfig();

    @Expose
    @Category(name = "Item Cooldown Overlay", desc = "Settings for the item ability cooldown overlay")
    public ItemCooldownConfig itemCooldown = new ItemCooldownConfig();

    @Expose
    @Category(name = "Ability Timer Overlay", desc = "Settings for the item ability active-duration overlay")
    public AbilityTimerConfig abilityTimer = new AbilityTimerConfig();

    @Expose
    @Category(name = "Invincibility Overlay", desc = "Settings for the post-save invincibility timer overlay")
    public InvincibilityConfig invincibility = new InvincibilityConfig();

    @Expose
    @Category(name = "Block Selection Overlay", desc = "Customize the block selection highlight")
    public BlockSelectionConfig blockSelection = new BlockSelectionConfig();

    // ── standalone options (no accordion) ───────────────────────────────────
    @Expose
    @ConfigOption(name = "Roman Numerals", desc = "Converts Roman numerals to integers in tooltips and tab list")
    @ConfigEditorBoolean
    public boolean romanNumerals = true;

    @Expose
    @ConfigOption(name = "Prevent Cursor Reset", desc = "Prevents the mouse cursor from resetting when opening GUIs")
    @ConfigEditorBoolean
    public boolean preventCursorReset = true;

    @Expose
    @ConfigOption(name = "Skyblock ID", desc = "Shows the skyblock item ID at the bottom of item tooltips")
    @ConfigEditorBoolean
    public boolean showSkyblockId = true;

    @Expose
    @ConfigOption(name = "Disable Enchant Glint", desc = "Removes the enchantment glint effect")
    @ConfigEditorBoolean
    public boolean disableEnchantGlint = false;

    @Expose
    @ConfigOption(name = "Brewing helper", desc = "Highlights brewing stands when done brewing")
    @ConfigEditorBoolean
    public boolean colorBrewingStands = true;

    @Expose
    @ConfigOption(name = "Missing Enchants", desc = "Hold SHIFT on an enchanted item to see missing enchants")
    @ConfigEditorBoolean
    public boolean missingEnchants = true;

    @Expose
    @ConfigOption(name = "Confirm Disconnect", desc = "Makes you click twice to disconnect")
    @ConfigEditorBoolean
    public boolean confirmDisconnect = true;

    @Expose
    @ConfigOption(name = "Chat State Restore(Let me speak)", desc = "Restores your chat text when server closes chat")
    @ConfigEditorBoolean
    public boolean chatStateRestore = true;
}
