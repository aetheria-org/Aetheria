package io.hamlook.aetheria.features.farming.pests;

import java.util.LinkedHashMap;
import java.util.Map;

public class PestData {

    public Map<String, Long> kills = new LinkedHashMap<>();
    public Map<String, Long> drops = new LinkedHashMap<>();
    public long activeTimeMs = 0L;
}