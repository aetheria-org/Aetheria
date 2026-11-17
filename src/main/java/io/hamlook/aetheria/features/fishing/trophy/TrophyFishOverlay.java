package io.hamlook.aetheria.features.fishing.trophy;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.Position;
import io.hamlook.aetheria.utils.data.SkyblockData;
import io.hamlook.aetheria.utils.overlay.Overlay;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;


@RegisterEvents
public class TrophyFishOverlay extends Overlay {

    @Getter
    private static TrophyFishOverlay instance;

    public TrophyFishOverlay() {
        super(180, 20);
        instance = this;
    }

    @Override
    public Position getPosition() {
        return ATHRConfig.feature.fishing.trophyFish.trophyFishPos;
    }

    @Override
    public float getScale() {
        return ATHRConfig.feature.fishing.trophyFish.trophyFishScale;
    }

    @Override
    public int getBgColor() {
        return ChromaColour.specialToChromaRGB(ATHRConfig.feature.fishing.trophyFish.trophyFishBgColor);
    }

    @Override
    public int getCornerRadius() {
        return ATHRConfig.feature.fishing.trophyFish.trophyFishCornerRadius;
    }

    @Override
    protected boolean isEnabled() {
        return ATHRConfig.feature != null && ATHRConfig.feature.fishing.trophyFish.trophyOverlay;
    }

    @Override
    protected boolean hideOnChat()   { return ATHRConfig.feature.fishing.trophyFish.hideOnChat; }
    @Override
    protected boolean hideOnTab()    { return ATHRConfig.feature.fishing.trophyFish.hideOnTab; }
    @Override
    protected boolean hideOnDebug()  { return ATHRConfig.feature.fishing.trophyFish.hideOnDebug; }

    @Override
    protected boolean extraGuard() {
        if (!ATHRConfig.feature.fishing.trophyFish.trophyOnlyCrimson) return true;
        return SkyblockData.getCurrentLocation() == SkyblockData.Location.CRIMSON_ISLE;
    }

    @Override
    public List<String> getLines(boolean preview) {
        List<String> out = new ArrayList<>();
        out.add("§6§lTrophy Fish");

        if (preview) {
            out.add("§9Lavahorse     §88  §75  §62  §b1");
            out.add("§5Soul Fish     §840 §720 §61  §b0");
            out.add("§6Golden Fish   §8100 §750 §625 §b5");
            return out;
        }

        TrophyFishStorage storage = TrophyFishStorage.getInstance();
        Map<String, Map<String, Integer>> fish = storage.getFish();

        if (fish.isEmpty()) {
            out.add("§cNo data — open Trophy Fishing at Odger");
            return out;
        }

        fish.entrySet().stream().filter(e -> storage.getTotal(e.getKey()) > 0).sorted(Comparator.comparingInt((Map.Entry<String, Map<String, Integer>> e) -> storage.getTotal(e.getKey())).reversed()).forEach(entry -> {
            String name = entry.getKey();
            int b = storage.getCount(name, TrophyRarity.BRONZE);
            int s = storage.getCount(name, TrophyRarity.SILVER);
            int g = storage.getCount(name, TrophyRarity.GOLD);
            int d = storage.getCount(name, TrophyRarity.DIAMOND);
            String coloredName = storage.getBestRarity(name).formatCode + name;
            out.add(coloredName + "  §8" + b + " §7" + s + " §6" + g + " §b" + d);
        });

        return out;
    }
}