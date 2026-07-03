package io.hamlook.aetheria.features.misc.ghosttracker;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.events.ScavengerGainEvent;
import io.hamlook.aetheria.utils.data.SkyblockData;
import net.minecraftforge.common.MinecraftForge;

public class PurseTracker {
    private static final long KILL_WINDOW_MS = 5000;
    private static final String PURSE_START = "(+";
    private static final String PURSE_END = ")";

    private static long lastKillTime = 0;
    private static int lastRecordedGain = 0;

    public static void tick() {
        if (SkyblockData.getScoreboardLines().isEmpty() || !SkyblockData.isInMist()) return;

        String purseLine = getPurseLine();
        if (purseLine == null) return;

        int scavengerGain = parseScavengerGain(purseLine);
        if (scavengerGain == 0) return;

        if (scavengerGain != lastRecordedGain && isValidGain(scavengerGain)) {
            Aetheria.logger.info("[GhostTracker/PurseTracker] Valid scavenger gain event: " + scavengerGain);
            lastRecordedGain = scavengerGain;
            MinecraftForge.EVENT_BUS.post(new ScavengerGainEvent(scavengerGain));
        } else if (scavengerGain != lastRecordedGain) {
            Aetheria.logger.info("[GhostTracker/PurseTracker] Invalid scavenger gain rejected: " + scavengerGain);
        }
    }

    public static void recordKill() {
        lastKillTime = System.currentTimeMillis();
        lastRecordedGain = 0;
    }

    private static String getPurseLine() {
        return SkyblockData.getCleanScoreboardLines().stream().filter(l -> l.contains("Purse") || l.contains("Piggy")).findFirst().orElse(null);
    }

    private static boolean isValidGain(int scavengerGain) {
        long now = System.currentTimeMillis();
        long timeSinceKill = now - lastKillTime;
        boolean inWindow = timeSinceKill <= KILL_WINDOW_MS;
        boolean inRange = scavengerGain >= GhostTrackerConstants.MIN_SCAVENGER_GAIN && scavengerGain <= GhostTrackerConstants.MAX_SCAVENGER_GAIN;

        if (!inRange) {
            Aetheria.logger.info("[GhostTracker/PurseTracker] Scavenger gain out of range: " + scavengerGain + " (min=" + GhostTrackerConstants.MIN_SCAVENGER_GAIN + " max=" + GhostTrackerConstants.MAX_SCAVENGER_GAIN + ")");
        }
        if (!inWindow) {
            Aetheria.logger.info("[GhostTracker/PurseTracker] Scavenger gain outside kill window: timeSinceKill=" + timeSinceKill + "ms (max=" + KILL_WINDOW_MS + "ms)");
        }

        return inRange && inWindow;
    }

    private static int parseScavengerGain(String purseLine) {
        try {
            int startIdx = purseLine.indexOf(PURSE_START);
            if (startIdx == -1) return 0;

            int endIdx = purseLine.indexOf(PURSE_END, startIdx);
            if (endIdx == -1) return 0;

            String gainStr = purseLine.substring(startIdx + PURSE_START.length(), endIdx);
            return Integer.parseInt(gainStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
