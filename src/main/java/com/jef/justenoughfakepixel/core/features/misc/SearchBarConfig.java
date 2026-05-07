package com.jef.justenoughfakepixel.core.features.misc;

import com.google.gson.annotations.Expose;
import com.jef.justenoughfakepixel.core.config.gui.config.ConfigAnnotations.*;
import com.jef.justenoughfakepixel.core.config.utils.Position;

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
    public Position searchBarPos = new Position(0, -30, true, false);
}
