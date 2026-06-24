package io.hamlook.aetheria.core.features.farming;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.*;
import org.lwjgl.input.Keyboard;

public class LockMouseConfig {

    @Expose
    @ConfigOption(name = "Lock Mouse", desc = "Locks yaw and pitch so you can't accidentally move your camera")
    @ConfigEditorBoolean
    public boolean lockMouse = false;

    @Expose
    @ConfigOption(name = "Lock Toggle Key", desc = "Keybind to toggle mouse lock on/off")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    public int lockMouseKey = Keyboard.KEY_NONE;

    @Expose
    @ConfigOption(name = "Show Unlock Hint", desc = "Show 'Use /lockyp to unlock mouse' below the lock icon when locked")
    @ConfigEditorBoolean
    public boolean showUnlockHint = true;
}
