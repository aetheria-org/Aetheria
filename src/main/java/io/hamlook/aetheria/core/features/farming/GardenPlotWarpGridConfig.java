package io.hamlook.aetheria.core.features.farming;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.*;
import io.hamlook.aetheria.utils.Position;

public class GardenPlotWarpGridConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Shows a clickable grid of Garden plots over your inventory for quick /tptoplot warps")
    @ConfigEditorBoolean
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "Show Locked Plots", desc = "When off, plots you haven't unlocked yet render dimmed and can't be clicked. Requires opening Configure Plots at least once to know which plots are unlocked")
    @ConfigEditorBoolean
    public boolean showLockedPlots = false;

    @Expose
    @ConfigOption(name = "Edit Position", desc = "Drag to reposition the plot warp grid")
    @ConfigEditorButton(runnableId = "openGardenPlotWarpGridEditor", buttonText = "Edit")
    public boolean editPosDummy = false;

    @Expose
    public Position pos = new Position(-258, 186, false, false);
}
