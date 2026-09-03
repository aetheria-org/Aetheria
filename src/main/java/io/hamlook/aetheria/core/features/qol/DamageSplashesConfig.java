package io.hamlook.aetheria.core.features.qol;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorBoolean;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigOption;

public class DamageSplashesConfig {

    @Expose
    @ConfigOption(name = "Hide Crit Splashes", desc = "Hides crit damage nametags (✧ stars)")
    @ConfigEditorBoolean
    public boolean hideCritSplashes = false;

    @Expose
    @ConfigOption(name = "Hide Non-Crit Splashes", desc = "Hides gray and fire-aspect damage numbers")
    @ConfigEditorBoolean
    public boolean hideNonCritSplashes = false;

    @Expose
    @ConfigOption(name = "Format Damage", desc = "Shortens large damage numbers (1234567 → 1.2M)")
    @ConfigEditorBoolean
    public boolean formatDamage = false;
}
