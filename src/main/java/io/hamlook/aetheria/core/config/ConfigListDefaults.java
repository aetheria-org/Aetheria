package io.hamlook.aetheria.core.config;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.core.Config;
import io.hamlook.aetheria.core.features.scoreboard.Scoreboard;

import java.util.List;

public final class ConfigListDefaults {

    private ConfigListDefaults() {}

    public static boolean apply(Config feature) {
        if (feature == null) return false;
        Scoreboard scoreboard = feature.scoreboard;
        if (scoreboard.scoreboardLines == null) {
            Aetheria.logger.warning("[ATHR] scoreboardLines is null; leaving it untouched");
            return false;
        }
        int maxId = Scoreboard.maxLineId();
        int seen = scoreboard.scoreboardLinesSeen;
        if (seen >= maxId) return false;

        List<Integer> rebuilt = ConfigListUpdator.reconcile(
                scoreboard.scoreboardLines, seen, maxId,
                Scoreboard::isLineDefaultOn, Scoreboard.LINE_ANCHORS);
        if (rebuilt != scoreboard.scoreboardLines) {
            scoreboard.scoreboardLines = rebuilt;
        }
        scoreboard.scoreboardLinesSeen = maxId;
        return true;
    }
}
