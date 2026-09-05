package io.hamlook.aetheria.features.custommenu.selector;

import io.hamlook.aetheria.features.custommenu.CustomMMConfig;
import io.hamlook.aetheria.features.custommenu.Position;
import io.hamlook.aetheria.features.custommenu.util.ScreenHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Shared, deterministic selector filtering and card layout. */
public final class CMMSelectorLayout {
    private CMMSelectorLayout() { }

    public static Map<CustomMMConfig, Position> layout(Map<String, CustomMMConfig> presets, String query,
                                                        int screenWidth, int cardWidth, int cardHeight, int padding) {
        String filter = query == null ? "" : query.trim().toLowerCase();
        List<CustomMMConfig> configs = new ArrayList<>();
        for (CustomMMConfig config : presets.values()) {
            if (config == null || config.configName == null) continue;
            if (filter.isEmpty() || config.configName.toLowerCase().contains(filter)) configs.add(config);
        }
        configs.sort(Comparator.comparing(c -> c.configName, String.CASE_INSENSITIVE_ORDER));
        Map<CustomMMConfig, Position> result = new LinkedHashMap<>();
        int x = 0;
        int y = 0;
        for (CustomMMConfig config : configs) {
            result.put(config, new Position("LEFT", "TOP", ScreenHelper.getStaticWidth(10) + x,
                    -(ScreenHelper.getStaticHeight(80) + y)));
            x += cardWidth + padding;
            if (x + cardWidth > screenWidth - ScreenHelper.getStaticWidth(10)) {
                y += cardHeight + padding;
                x = 0;
            }
        }
        return result;
    }
}
