package io.hamlook.aetheria.core.features.farming;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.*;
import io.hamlook.aetheria.utils.Position;

public class VisitorsConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Visitor shopping list: track what each Garden visitor wants and gives")
    @ConfigEditorBoolean
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "Show Have Counts", desc = "Show how many of each required item you already have in your inventory")
    @ConfigEditorBoolean
    public boolean showHaveCounts = true;

    @Expose
    @ConfigOption(name = "Show Time To Farm Estimate", desc = "Show an ESTIMATED farming time for missing items, based on your Farming Tracker rate")
    @ConfigEditorBoolean
    public boolean showTimeToFarm = true;

    @Expose
    @ConfigOption(name = "Copper Price", desc = "Show coins-per-copper on the Copper reward line in visitor menus")
    @ConfigEditorBoolean
    public boolean copperPriceDisplay = true;

    @Expose
    @ConfigOption(name = "Copper Threshold", desc = "Coins per copper above this counts as an expensive deal (colored red)")
    @ConfigEditorSliderAnnotation(minValue = 0f, maxValue = 100_000f, minStep = 500f)
    public int copperThreshold = 20_000;

    @Expose
    @ConfigOption(name = "Confirm Good Copper Refusals", desc = "Require a second Refuse click before refusing visitors paying below your copper limit")
    @ConfigEditorBoolean
    public boolean confirmGoodCopperRefuse = true;

    @Expose
    @ConfigOption(name = "Confirm Expensive Copper Accepts", desc = "Require a second Accept click before accepting visitors paying above your copper limit")
    @ConfigEditorBoolean
    public boolean confirmExpensiveCopperAccept = false;

    /** Set via /visitortip hide; hides the one-time shopping list tip. Not shown in the GUI. */
    @Expose
    public boolean shoppingListTipHidden = false;

    @Expose
    @ConfigOption(name = "Sign Fill Mode", desc = "Autofill writes the required amount into the Bazaar amount sign automatically. Click to Fill fills it when you click a shopping list row while the sign is open")
    @ConfigEditorDropdown(values = {"Autofill", "Click to Fill", "None"}, initialIndex = 0)
    public int signFillMode = 0;

    @Expose
    @ConfigOption(name = "Reset List", desc = "Clear all learned visitor items and rewards")
    @ConfigEditorButton(runnableId = "resetVisitorList", buttonText = "Reset")
    public boolean resetVisitorListDummy = false;

    @Expose
    @Category(name = "Panel", desc = "Clickable shopping list shown next to containers")
    public PanelConfig panel = new PanelConfig();

    @Expose
    @Category(name = "Overlay", desc = "Read-only shopping list overlay for the Garden")
    public OverlayConfig overlay = new OverlayConfig();

    public static class PanelConfig {

        @Expose
        @ConfigOption(name = "Enable", desc = "Show the shopping list panel next to the visitor menu, Bazaar and your inventory")
        @ConfigEditorBoolean
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "Edit Position", desc = "Drag to reposition the shopping list panel")
        @ConfigEditorButton(runnableId = "openVisitorPanelEditor", buttonText = "Edit")
        public boolean editPosDummy = false;

        @Expose
        public Position panelPos = new Position(-22, 127, false, false);

        @Expose
        @ConfigOption(name = "Scale", desc = "Size of the panel")
        @ConfigEditorSliderAnnotation(minValue = 0.5f, maxValue = 3f, minStep = 0.1f)
        public float scale = 0.7f;

        @Expose
        @ConfigOption(name = "Visible", desc = "Where the panel is shown")
        @ConfigEditorDropdown(values = {"Only on Garden", "Farming Islands", "Anywhere"}, initialIndex = 0)
        public int visible = 0;

        @Expose
        @ConfigOption(name = "Show Price Estimates", desc = "Show an estimated coin price next to each item and the total cost in the title")
        @ConfigEditorBoolean
        public boolean showPrices = true;

        @Expose
        @ConfigOption(name = "Show Profit Estimate", desc = "Show total reward value and profit estimate (rewards minus items you still need to buy)")
        @ConfigEditorBoolean
        public boolean showProfit = true;
    }

    public static class OverlayConfig {

        @Expose
        @ConfigOption(name = "Enable", desc = "Show the shopping list overlay on the Garden")
        @ConfigEditorBoolean
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "Visible", desc = "Where the overlay is shown")
        @ConfigEditorDropdown(values = {"Only on Garden", "Farming Islands", "Anywhere"}, initialIndex = 0)
        public int visible = 0;

        @Expose
        @ConfigOption(name = "Show Price Estimates", desc = "Show an estimated coin price next to each item and the total cost in the title")
        @ConfigEditorBoolean
        public boolean showPrices = true;

        @Expose
        @ConfigOption(name = "Show Profit Estimate", desc = "Show total reward value and profit estimate (rewards minus items you still need to buy)")
        @ConfigEditorBoolean
        public boolean showProfit = true;

        @Expose
        @ConfigOption(name = "Hide While Farming", desc = "Hide the overlay while actively farming (holding a farming tool and breaking crops)")
        @ConfigEditorBoolean
        public boolean hideWhileFarming = true;

        @Expose
        @ConfigOption(name = "Edit Position", desc = "Drag to reposition the visitor shopping list overlay")
        @ConfigEditorButton(runnableId = "openVisitorOverlayEditor", buttonText = "Edit")
        public boolean editPosDummy = false;

        @Expose
        @ConfigOption(name = "Scale", desc = "Size of the overlay")
        @ConfigEditorSliderAnnotation(minValue = 0.5f, maxValue = 3f, minStep = 0.1f)
        public float scale = 1f;

        @Expose
        @ConfigOption(name = "Background Color", desc = "Background color of the overlay")
        @ConfigEditorColour
        public int bgColor = 0x80000000;

        @Expose
        @ConfigOption(name = "Corner Radius", desc = "Roundness of the overlay corners")
        @ConfigEditorSliderAnnotation(minValue = 0f, maxValue = 12f, minStep = 1f)
        public int cornerRadius = 4;

        @Expose
        public Position overlayPos = new Position(-373, 192, false, false);
    }
}
