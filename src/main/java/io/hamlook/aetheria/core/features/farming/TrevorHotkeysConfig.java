package io.hamlook.aetheria.core.features.farming;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.*;
import org.lwjgl.input.Keyboard;

public class TrevorHotkeysConfig {

    @Expose
    @ConfigOption(name = "Trevor Quest Hotkey", desc = "Press the hotkey to either warp to or call Trevor, and to confirm a hunt when Trevor asks (the [YES]/[NO] prompt); the key does whichever applies at the moment. §cIf using Warp Trapper, you must have unlocked the Trapper's Den warp. Call Trevor needs no unlock.")
    @ConfigEditorBoolean
    public boolean warpHelper = false;

    @Expose
    @ConfigOption(name = "Trevor Quest Action", desc = "What the hotkey does outside of a confirm prompt: Warp Trapper runs /warp trapper, Call Trevor runs /call trevor")
    @ConfigEditorDropdown(values = {"Warp Trapper", "Call Trevor"}, initialIndex = 0)
    public int warpHelperAction = 0;

    @Expose
    @ConfigOption(name = "Warp Key", desc = "Key that runs the selected Trevor Quest Action, or confirms a hunt while Trevor is asking")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_F)
    public int warpKey = Keyboard.KEY_F;

    @Expose
    @ConfigOption(name = "Confirm Lock Timeout", desc = "The [YES]/[NO] prompt doesn't expire on its own, so the hotkey normally waits for it indefinitely. Enable this to give up on the prompt after 10s and let the key go back to its usual warp/call action.")
    @ConfigEditorBoolean
    public boolean confirmLockTimeout = false;

    @Expose
    @ConfigOption(name = "Desert Warp Helper", desc = "If the hunt spawns in Desert Settlement or Oasis, press the desert warp key to run /warp desert. §cOnly enable this if you have unlocked the Desert warp!")
    @ConfigEditorBoolean
    public boolean desertWarpHelper = false;

    @Expose
    @ConfigOption(name = "Desert Warp Key", desc = "Key that runs /warp desert while the hunt is in Desert Settlement or Oasis")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_G)
    public int desertWarpKey = Keyboard.KEY_G;
}
