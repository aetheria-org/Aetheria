package io.hamlook.aetheria.core.features.network;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations;

public class GlobalChatConfig {

    @Expose
    @ConfigAnnotations.ConfigOption(name = "Reduced Animations",desc = "Only animated gifs/apngs when you hover over them")
    @ConfigAnnotations.ConfigEditorBoolean
    public boolean reducedAnimations = false;

    @Expose
    @ConfigAnnotations.ConfigOption(name = "Max Image/GIF Quality",desc = "Control the max quality a image or gif can have.")
    @ConfigAnnotations.ConfigEditorDropdown(values = {"Lowest(240p)","Low(360p)","Medium(480p)","High(720p)","Higher(1080p)","Ultra(1440p)","Overkill(4k)"},initialIndex = 4)
    public int maxImageGifQuality = 4;

}
