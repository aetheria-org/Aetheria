package io.hamlook.aetheria.core.features.network;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations;
import io.hamlook.aetheria.utils.Position;
import org.lwjgl.input.Keyboard;

public class GlobalChatConfig {

    @Expose
    @ConfigAnnotations.ConfigOption(name = "Open Chat Keybind",desc = "Press to open the Global Chat UI (same as /gchat)")
    @ConfigAnnotations.ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    public int openChatKey = Keyboard.KEY_NONE;

    @Expose
    @ConfigAnnotations.ConfigOption(name = "Reduced Animations",desc = "Only animated gifs/apngs when you hover over them")
    @ConfigAnnotations.ConfigEditorBoolean
    public boolean reducedAnimations = false;

    @Expose
    @ConfigAnnotations.ConfigOption(name = "Max Image/GIF Quality",desc = "Control the max quality a image or gif can have.")
    @ConfigAnnotations.ConfigEditorDropdown(values = {"Lowest(240p)","Low(360p)","Medium(480p)","High(720p)","Higher(1080p)","Ultra(1440p)","Overkill(4k)"},initialIndex = 4)
    public int maxImageGifQuality = 4;

    @Expose
    @ConfigAnnotations.ConfigOption(name = "Notifications",desc = "Show a toast when someone mentions you (@username) or pings @everyone/@here in the Global Chat")
    @ConfigAnnotations.ConfigEditorBoolean
    public boolean notificationsEnabled = true;

    @Expose
    @ConfigAnnotations.ConfigOption(name = "Edit Notifications Position",desc = "Drag the notification toasts to a new spot on the screen")
    @ConfigAnnotations.ConfigEditorButton(runnableId = "openNotificationsOverlayEditor", buttonText = "Edit")
    public boolean notificationsPositionButton = false;

    @Expose
    public Position notificationsPosition = new Position(-6, 6);

    /** Watermark for offline mention catch-up: messages older than this were already seen. */
    @Expose
    public long lastSeenPings = 0L;

}
