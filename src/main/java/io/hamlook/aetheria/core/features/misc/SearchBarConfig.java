package io.hamlook.aetheria.core.features.misc;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.*;
import io.hamlook.aetheria.utils.Position;
import org.lwjgl.input.Keyboard;

public class SearchBarConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Shows a search bar in supported GUIs")
    @ConfigEditorBoolean
    public boolean searchBar = true;

    @Expose
    @ConfigOption(name = "Highlight Color", desc = "Color used to highlight matching items in search results")
    @ConfigEditorColour
    public String searchBarHighlightColor = "0:102:255:0:0";

    @Expose
    @ConfigOption(name = "Edit Search Bar Position", desc = "Drag to reposition the search bar")
    @ConfigEditorButton(runnableId = "openSearchBarEditor", buttonText = "Edit")
    public boolean editSearchBarPosDummy = false;

    @Expose
    public Position searchBarPos = new Position(0, -55, true, false);

    @Expose
    @ConfigOption(name = "Persist Search", desc = "Keep main searchbar text between GUI opens")
    @ConfigEditorBoolean
    public boolean persistSearchText = true;

    @Expose
    @ConfigOption(name = "Persist Item List Search", desc = "Keep Item List local search text between GUI opens (only applies when not using global search)")
    @ConfigEditorBoolean
    public boolean persistItemListSearch = false;

    @Expose
    @ConfigOption(name = "Persist Storage Search", desc = "Keep Storage Overlay search text between GUI opens")
    @ConfigEditorBoolean
    public boolean persistStorageSearch = false;

    @Expose
    @ConfigOption(name = "Enter Clears Expression", desc = "In calculator mode, pressing the search submit key will remove the expression and leave only the result in the search bar")
    @ConfigEditorBoolean
    public boolean calcEnterClearText = true;

    @Expose
    @ConfigOption(name = "Result on Enter", desc = "In calculator mode, pressing the search submit key will copy the result to your clipboard")
    @ConfigEditorBoolean
    public boolean calcEnterCopyResult = true;

    @Expose
    @ConfigOption(name = "Hover Item Copy Key", desc = "While hovering an item's tooltip, press this key to paste its name into the search bar. Unbound by default; set a key to enable this.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    public int hoverPasteKey = Keyboard.KEY_NONE;

    @Expose
    @ConfigOption(name = "Search Submit Key", desc = "Key that applies a calculator result into the search bar, sends a typed command, and records the current search/result into the recent list. Defaults to Enter.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_RETURN)
    public int submitKey = Keyboard.KEY_RETURN;

    @Expose
    @ConfigOption(name = "Recent Searches", desc = "Shows a scrollable dropdown of recently searched/calculated entries below the search bar while it's focused. Press the search submit key to add the current search to this list; it's session-only and clears on restart.")
    @ConfigEditorBoolean
    public boolean recentSearchesEnabled = true;
}
