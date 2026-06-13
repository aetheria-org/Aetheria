package io.hamlook.aetheria.features.dungeons.reward;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.Position;
import io.hamlook.aetheria.utils.Utils;
import io.hamlook.aetheria.utils.overlay.Overlay;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RegisterEvents
public class RewardAnalyzerOverlay extends Overlay {

    @Getter
    private static RewardAnalyzerOverlay instance;

    public RewardAnalyzerOverlay() {
        super(25,25);
        instance = this;
    }

    @Override
    public List<String> getLines(boolean preview) {
        if(preview){
            return Collections.singletonList("§aPrice Analyzer");
        }
        if(DungeonRewardProfitEstimator.cache.isEmpty()) return Collections.emptyList();
        List<String> lines = new ArrayList<>();
        lines.add("§aChest Price Analyzer:");
        RewardEstimate highestProfitChest = null;
        for(RewardEstimate estimate : DungeonRewardProfitEstimator.cache.values()){
            String s = DungeonRewardProfitEstimator.getChestHeader(estimate.getChestID());
            double profit = estimate.getProfit();
            if(highestProfitChest == null) highestProfitChest = estimate;
            else if(highestProfitChest.getProfit() < profit)highestProfitChest = estimate;
            lines.add(s + " §7: " + (profit > 0 ? "§a" : "§c") + Utils.shortNumberFormat(profit,0));
        }
        if(highestProfitChest != null) {
            lines.add("§aRecommended Chest: " + DungeonRewardProfitEstimator.getChestHeader(highestProfitChest.getChestID()));
        }
        return lines;
    }

    @Override
    public Position getPosition() {
        return ATHRConfig.feature.dungeons.priceEstimator.analyzerPosition;
    }

    @Override
    public float getScale() {
        return ATHRConfig.feature.dungeons.priceEstimator.overlayScale;
    }

    @Override
    public int getBgColor() {
        return ChromaColour.specialToChromaRGB(ATHRConfig.feature.dungeons.priceEstimator.overlayBgColor);
    }

    @Override
    public int getCornerRadius() {
        return 0;
    }

    @Override
    protected boolean isEnabled() {
        return ATHRConfig.feature.dungeons.priceEstimator.enableAnalyzerOverlay;
    }
}
