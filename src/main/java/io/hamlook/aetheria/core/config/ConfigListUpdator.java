package io.hamlook.aetheria.core.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntPredicate;

public final class ConfigListUpdator {

    private ConfigListUpdator() {}

    public static List<Integer> reconcile(List<Integer> list, int seen, int maxId,
                                          IntPredicate defaultOn, Map<Integer, Integer> anchors) {
        if (seen >= maxId) return list;
        if (anchors == null) anchors = Collections.emptyMap();

        Set<Integer> present = new HashSet<>(list);
        List<Integer> rebuilt = new ArrayList<>(list);
        Map<Integer, Integer> insertedAfter = new HashMap<>();
        boolean added = false;

        int start = Math.max(0, seen + 1);
        for (int id = start; id <= maxId; id++) {
            if (!defaultOn.test(id) || present.contains(id)) continue;
            Integer anchor = anchors.get(id);
            int index = anchor == null ? -1 : rebuilt.indexOf(anchor);
            if (index < 0) {
                rebuilt.add(id);
            } else {
                int offset = insertedAfter.merge(anchor, 1, Integer::sum);
                rebuilt.add(index + offset, id);
            }
            present.add(id);
            added = true;
        }
        return added ? rebuilt : list;
    }
}
