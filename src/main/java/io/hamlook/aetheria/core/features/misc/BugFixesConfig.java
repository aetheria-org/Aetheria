package io.hamlook.aetheria.core.features.misc;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorBoolean;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigOption;

public class BugFixesConfig {

    //#if MC < 1.21
    @Expose
    @ConfigOption(name = "Bow Draw Fix", desc = "Keeps the bow draw going when the inventory updates (1.8.9 only)")
    @ConfigEditorBoolean
    public boolean bowDrawFix = true;
    //#endif

    @Expose
    @ConfigOption(name = "Swap Animation Fix", desc = "Stops the swap animation from replaying when the held item's data changes (mending, fuel)")
    @ConfigEditorBoolean
    public boolean swapAnimationFix = true;
}
