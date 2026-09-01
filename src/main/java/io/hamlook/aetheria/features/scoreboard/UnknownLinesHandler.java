package io.hamlook.aetheria.features.scoreboard;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.data.SkyblockData;
import net.minecraft.util.EnumChatFormatting;
import io.hamlook.aetheria.api.event.HandleEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;
import io.hamlook.aetheria.events.ASMWorldUnloadEvent;

@RegisterEvents
public class UnknownLinesHandler {

    private static final long WARN_AFTER_MS    = 10_000L;      // warn if same line seen for 10s
    private static final long WARN_COOLDOWN_MS = 30 * 60_000L; // at most once per 30min per line
    private static final long BURST_WINDOW_MS  = 6_000L;
    private static final int  BURST_THRESHOLD  = 5;
    private static final long BURST_COOLDOWN_MS = 30 * 60_000L;

    private static final List<Entry> allUnknown = new ArrayList<>();
    private static long lastBurstWarnTime = 0L;

    @HandleEvent
    public void onWorldUnload(ASMWorldUnloadEvent event) {
        allUnknown.clear();
        lastBurstWarnTime = 0L;
    }

    /** Called from CustomScoreboard for every unrecognised line each frame. */
    public static void handle(String line) {
        if (line == null || line.trim().isEmpty()) return;
        if (!SkyblockData.isOnSkyblock()) return;

        long now = System.currentTimeMillis();
        Entry entry = findEntry(line);

        if (entry == null) {
            // First time seeing this line — add it, no warning yet
            allUnknown.add(new Entry(line, now));
            System.out.println("[ATHR SB UNKNOWN] " + line);
            checkBurstWarning(now);
        } else {
            // Update last seen
            entry.lastFound = now;

            // Only warn if it's been visible for 10s and we haven't warned recently
            long knownFor  = now - entry.firstFound;
            long sinceWarn = now - entry.lastWarned;
            if (knownFor > WARN_AFTER_MS && sinceWarn > WARN_COOLDOWN_MS) {
                entry.lastWarned = now;
                warn(line);
            }
        }

        // Prune lines not seen in the last 15 seconds (they flickered out)
        allUnknown.removeIf(e -> (now - e.lastFound) > 15_000L);
    }

    public static Set<String> getSeen() {
        Set<String> out = new LinkedHashSet<>();
        for (Entry e : allUnknown) out.add(e.line);
        return out;
    }

    private static Entry findEntry(String line) {
        for (Entry e : allUnknown)
            if (e.line.equals(line)) return e;
        return null;
    }

    private static void checkBurstWarning(long now) {
        if ((now - lastBurstWarnTime) < BURST_COOLDOWN_MS) return;
        int recent = 0;
        for (Entry e : allUnknown)
            if ((now - e.firstFound) < BURST_WINDOW_MS) recent++;
        if (recent >= BURST_THRESHOLD) {
            lastBurstWarnTime = now;
            warn(allUnknown.get(allUnknown.size() - 1).line);
        }
    }

    private static void warn(String line) {
        if (ATHRConfig.feature == null) return;
        if (!ATHRConfig.feature.scoreboard.unknownLinesWarning) return;
        if (SkyblockData.isInDungeon()) return;
        ChatUtils.sendMessage(
                EnumChatFormatting.RED + "[ATHR]: CustomScoreboard detected an unknown line: '" + line + "'"
        );
    }

    static class Entry {
        final String line;
        final long firstFound;
        long lastFound;
        long lastWarned = 0L;

        Entry(String line, long now) {
            this.line = line;
            this.firstFound = now;
            this.lastFound = now;
        }
    }
}