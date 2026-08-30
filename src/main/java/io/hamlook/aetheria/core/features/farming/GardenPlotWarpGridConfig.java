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
    @ConfigOption(name = "Edit Position", desc = "Drag to reposition the plot warp grid")
    @ConfigEditorButton(runnableId = "openGardenPlotWarpGridEditor", buttonText = "Edit")
    public boolean editPosDummy = false;

    @Expose
    public Position pos = new Position(-258, 186, false, false);
}
