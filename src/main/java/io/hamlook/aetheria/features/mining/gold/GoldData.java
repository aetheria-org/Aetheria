package io.hamlook.aetheria.features.mining.gold;

public class GoldData {

    public long ingotCount = 0;
    public long enchantedCount = 0;
    public long compactCount = 0;
    public long activeTimeMs = 0;

    public void reset() {
        ingotCount = 0;
        enchantedCount = 0;
        compactCount = 0;
        activeTimeMs = 0;
    }
}
