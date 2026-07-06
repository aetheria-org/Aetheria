package io.hamlook.aetheria.core.features.misc;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations;
import org.lwjgl.input.Keyboard;

public class ItemPriceConfig {

    @Expose
    @ConfigAnnotations.ConfigOption(name = "Enable", desc = "Dynamically Fetch Prices while you play the game without affecting performance.")
    @ConfigAnnotations.ConfigEditorBoolean
    public boolean enabled = true;

    @Expose
    @ConfigAnnotations.ConfigOption(name = "Enable Auction Fetching", desc = "Allow parsing prices while you browse the auction house.")
    @ConfigAnnotations.ConfigEditorBoolean
    public boolean auctionEnabled = true;

    @Expose
    @ConfigAnnotations.ConfigOption(name = "Enable Bazaar Fetching", desc = "Allow parsing prices while you browse the bazaar.")
    @ConfigAnnotations.ConfigEditorBoolean
    public boolean bazaarEnabled = true;

    @Expose
    @ConfigAnnotations.ConfigOption(name = "Send Fetched Data to API", desc = "Allow sending the data to the database, so it can actually be used.")
    @ConfigAnnotations.ConfigEditorBoolean
    public boolean sendToDB = true;;

    @Expose
    @ConfigAnnotations.ConfigOption(name = "Price in Lore", desc = "Allow showing the price in the lore of an item | WARNING: the price may not always be accurate.")
    @ConfigAnnotations.ConfigEditorBoolean
    public boolean showPriceInLore = true;;

    @Expose
    @ConfigAnnotations.ConfigOption(name = "Show Price when Holding a Key", desc = "Make it so that item prices are only visible when a key is held")
    @ConfigAnnotations.ConfigEditorBoolean
    public boolean showPriceWhenShift = true;

    @Expose
    @ConfigAnnotations.ConfigOption(name = "Show Price Key",desc = "The key to hold to see price | Requires Show Price when Holding a Key to be enabled")
    @ConfigAnnotations.ConfigEditorKeybind(defaultKey = Keyboard.KEY_LSHIFT)
    public int showPriceKey = Keyboard.KEY_LSHIFT;
}
