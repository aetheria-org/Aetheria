package io.hamlook.aetheria.features.mining.pristine;

import java.util.HashMap;
import java.util.Map;

public class PristineData {

    public long lastPristineMs = 0L;
    public int totalProcs = 0;
    public long activeTimeMs = 0L;
    public Map<String, Long> gemstones = new HashMap<>();

    public void reset() {
        lastPristineMs = 0L;
        totalProcs = 0;
        activeTimeMs = 0L;
        gemstones.clear();
    }
}
