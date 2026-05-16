package com.jef.justenoughfakepixel.core.features.overlays;

import com.google.gson.annotations.Expose;
import com.jef.justenoughfakepixel.core.config.gui.config.ConfigAnnotations;

public class Overlays {

    @Expose
    @ConfigAnnotations.Category(name = "Profile Viewer", desc = "Settings for the profile viewer GUI")
    public ProfileViewerConfig profileViewer = new ProfileViewerConfig();

}
