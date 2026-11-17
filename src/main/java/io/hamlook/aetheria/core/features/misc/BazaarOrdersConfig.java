package io.hamlook.aetheria.core.features.misc;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorBoolean;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorColour;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigOption;

public class BazaarOrdersConfig {

    @Expose
    @ConfigOption(name = "Highlight Filled Sell Orders", desc = "Highlights slots with a claimable sell order in the Bazaar Orders menu")
    @ConfigEditorBoolean
    public boolean highlightSellOrders = true;

    @Expose
    @ConfigOption(name = "Sell Order Color", desc = "Highlight color for filled sell orders")
    @ConfigEditorColour
    public String sellOrderColor = "0:128:255:170:0";

    @Expose
    @ConfigOption(name = "Highlight Filled Buy Orders", desc = "Highlights slots with a claimable buy order in the Bazaar Orders menu")
    @ConfigEditorBoolean
    public boolean highlightBuyOrders = true;

    @Expose
    @ConfigOption(name = "Buy Order Color", desc = "Highlight color for filled buy orders")
    @ConfigEditorColour
    public String buyOrderColor = "0:128:0:200:50";
}