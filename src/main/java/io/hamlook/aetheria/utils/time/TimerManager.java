package io.hamlook.aetheria.utils.time;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TimerManager {

    private volatile Map<String, Long> durations;
    private final Map<String, Long> endTimes;

    public TimerManager(Map<String, Long> durations) {
        this.durations = durations;
        this.endTimes = new HashMap<>();
    }

    public void updateDurations(Map<String, Long> durations) {
        this.durations = durations;
    }

    public void markActive(String itemId) {
        Long dur = durations.get(itemId);
        if (dur == null) return;
        endTimes.put(itemId, System.currentTimeMillis() + dur);
    }

    public void markActive(String itemId, long durationMs) {
        endTimes.put(itemId, System.currentTimeMillis() + durationMs);
    }

    public long getRemainingMs(String itemId) {
        Long end = endTimes.get(itemId);
        if (end == null) return 0;
        return Math.max(0, end - System.currentTimeMillis());
    }

    public boolean isActive(String itemId) {
        return getRemainingMs(itemId) > 0;
    }

    public List<String> getActiveTimers() {
        long now = System.currentTimeMillis();
        List<String> active = new ArrayList<>();
        for (Map.Entry<String, Long> e : endTimes.entrySet()) {
            if (e.getValue() > now) active.add(e.getKey());
        }
        active.sort((a, b) -> Long.compare(endTimes.get(b), endTimes.get(a)));
        return active;
    }

    public Long getDuration(String itemId) {
        return durations.get(itemId);
    }
}