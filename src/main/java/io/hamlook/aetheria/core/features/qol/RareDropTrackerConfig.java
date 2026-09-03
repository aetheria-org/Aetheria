package io.hamlook.aetheria.core.features.qol;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.*;
import io.hamlook.aetheria.utils.Position;
import org.lwjgl.input.Keyboard;

import java.util.LinkedHashMap;

public class RareDropTrackerConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Alert you when you pick up an item you're tracking. Manage your tracked items with the button below or /rdt")
    @ConfigEditorBoolean
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "Manage Tracked Items", desc = "Search the item database and add/remove tracked items from a GUI")
    @ConfigEditorButton(runnableId = "openRareDropTrackerGui", buttonText = "Open")
    public boolean manageTrackedItemsDummy = false;

    @Expose
    @ConfigOption(name = "Tracker Key", desc = "Keybind to open the Rare Drop Tracker GUI")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    public int trackerGuiKey = Keyboard.KEY_NONE;

    @Expose
    @ConfigOption(name = "Alert Mode", desc = "Always: alert every time the item drops. First Time Only: only alert the first time you pick it up per session.")
    @ConfigEditorDropdown(values = {"Always", "First Time Only"})
    public int alertMode = 0;

    @Expose
    @ConfigOption(name = "Chat Alert", desc = "Send a chat message when a tracked item drops")
    @ConfigEditorBoolean
    public boolean chatAlert = true;

    @Expose
    @ConfigOption(name = "Title Alert", desc = "Flash a big title on screen when a tracked item drops")
    @ConfigEditorBoolean
    public boolean titleAlert = true;

    @Expose
    @ConfigOption(name = "Play Sound", desc = "Play a sound when a tracked item drops")
    @ConfigEditorBoolean
    public boolean playSound = true;

    @Expose
    @ConfigOption(name = "Command Alert", desc = "If a tracked item has a command set (from the tracker GUI), prompt you to click anywhere within 5 seconds of picking it up to run that command. Example uses: /echest 5 to jump to enderchest slot 5, /storage 3 to open backpack 3.")
    @ConfigEditorBoolean
    public boolean commandAlert = true;

    // ── overlay ──────────────────────────────────────────────────────────────
    @Expose
    @ConfigOption(name = "Show Overlay", desc = "Show a HUD overlay with the drop counts/goals of your tracked items")
    @ConfigEditorBoolean
    public boolean showOverlay = true;

    @Expose
    @ConfigOption(name = "Edit Overlay Position", desc = "Drag the overlay around the screen")
    @ConfigEditorButton(runnableId = "openRareDropTrackerOverlayEditor", buttonText = "Edit")
    public boolean editOverlayPosDummy = false;

    @Expose
    @ConfigOption(name = "Only Show Goals", desc = "Only show tracked items that have a goal amount set (hides items with no goal)")
    @ConfigEditorBoolean
    public boolean overlayOnlyShowGoals = false;

    @Expose
    @ConfigOption(name = "Hide Completed Goals", desc = "Hide items from the overlay once their goal amount has been reached")
    @ConfigEditorBoolean
    public boolean overlayHideCompleted = false;

    @Expose
    @ConfigOption(name = "Hide in Chat", desc = "Hide the overlay when the chat GUI is open")
    @ConfigEditorBoolean
    public boolean hideOnChat = true;

    @Expose
    @ConfigOption(name = "Hide on Tab", desc = "Hide the overlay when the tab list is shown")
    @ConfigEditorBoolean
    public boolean hideOnTab = true;

    @Expose
    @ConfigOption(name = "Hide on F3 Debug", desc = "Hide the overlay when the F3 debug screen is open")
    @ConfigEditorBoolean
    public boolean hideOnDebug = true;

    @Expose
    @ConfigOption(name = "Background Color", desc = "Background color of the overlay (alpha controls opacity)")
    @ConfigEditorColour
    public String overlayBgColor = "0:136:0:0:0";

    @Expose
    @ConfigOption(name = "Corner Radius", desc = "Roundness of the overlay corners")
    @ConfigEditorSliderAnnotation(minValue = 0f, maxValue = 12f, minStep = 1f)
    public int overlayCornerRadius = 4;

    @Expose
    @ConfigOption(name = "Scale", desc = "Size of the overlay")
    @ConfigEditorSliderAnnotation(minValue = 0.5f, maxValue = 3f, minStep = 0.1f)
    public float overlayScale = 1f;

    @Expose
    public Position overlayPos = new Position(4, 150);

    // skyblockID (lowercase) -> tracked item data (display name, goal, running count)
    @Expose
    public LinkedHashMap<String, TrackedItem> trackedItems = new LinkedHashMap<>();

    public static class TrackedItem {
        @Expose
        public String displayName;

        // 0 = no goal set, just count drops forever
        @Expose
        public int goal = 0;

        @Expose
        public int count = 0;

        // Command to run when this item drops, e.g. "/echest 5" or "/storage 3".
        // Set from the tracker GUI. Null/empty = no command bound.
        @Expose
        public String command = null;

        public TrackedItem() {
        }

        public TrackedItem(String displayName) {
            this.displayName = displayName;
        }
    }
}
