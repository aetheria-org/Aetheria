package io.hamlook.aetheria.core.features.fishing;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.Category;

public class Fishing {

    @Expose
    @Category(name = "Trophy Fish", desc = "Trophy fish tracking and display")
    public TrophyFishConfig trophyFish = new TrophyFishConfig();

    @Expose
    @Category(name = "Fishing Timer", desc = "Fishing timer overlay settings")
    public FishingTimerConfig fishingTimerConfig = new FishingTimerConfig();
}
