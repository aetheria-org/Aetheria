package io.hamlook.aetheria.features.dungeons;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import io.hamlook.aetheria.utils.Position;
import io.hamlook.aetheria.features.dungeons.overlays.PhaseOverlay;
import io.hamlook.aetheria.features.dungeons.utils.*;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.data.DungeonUtils;
import io.hamlook.aetheria.utils.data.SkyblockData;
import io.hamlook.aetheria.utils.overlay.Overlay;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;
import java.util.regex.Pattern;


@RegisterEvents
public class DungeonStats extends Overlay {

    public static final int OVERLAY_WIDTH = 160;
    public static final int OVERLAY_HEIGHT = 160;

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Pattern TIME_ELAPSED = Pattern.compile("Time Elapsed: (\\d+)");
    private static final int[][] BOSS_COORDS = {{29, 71, 80},  // F1/M1
            {32, 69, 11},  // F2/M2
            {41, 69, 57},  // F3/M3
            {45, 69, 20},  // F4/M4
            {45, 69, 9},   // F5/M5
            {32, 69, 7},   // F6/M6
            {85, 221, 21}  // F7/M7
    };

    private static DungeonStats instance;

    // Manual getter for Kotlin interop
    public static DungeonStats getInstance() {
        return instance;
    }

    private final DungeonTimers timers;
    private final DungeonEndStats endStats;
    private final PBTracker pbTracker;
    private final PhaseDetector phaseDetector;
    private final PhaseOverlay statsOverlay;
    private final StatsPrinter statsPrinter;

    private int tickCounter = 0;

    public DungeonStats() {
        super(OVERLAY_WIDTH, OVERLAY_HEIGHT);
        instance = this;

        this.timers = new DungeonTimers();
        this.endStats = new DungeonEndStats();
        this.pbTracker = new PBTracker();
        this.phaseDetector = new PhaseDetector(timers, endStats, pbTracker);
        this.statsOverlay = new PhaseOverlay(timers);
        this.statsPrinter = new StatsPrinter(timers, endStats, pbTracker);
    }


    public static boolean isInBossFight() {
        DungeonStats s = getInstance();
        return s != null && s.timers.getBossTime() > 0 && s.timers.getBossDeadTime() == 0;
    }

    public boolean isInStormPhase() {
        return timers.getStormStart() > 0 && timers.getStormEnd() == 0;
    }

    public static String getFormattedPb(String arg1, String arg2) {
        return PBTracker.getFormattedPb(arg1, arg2);
    }

    private static int[] getBossCoords(DungeonFloor floor) {
        int idx;
        switch (floor) {
            case F1:
            case M1:
                idx = 0;
                break;
            case F2:
            case M2:
                idx = 1;
                break;
            case F3:
            case M3:
                idx = 2;
                break;
            case F4:
            case M4:
                idx = 3;
                break;
            case F5:
            case M5:
                idx = 4;
                break;
            case F6:
            case M6:
                idx = 5;
                break;
            case F7:
            case M7:
                idx = 6;
                break;
            default:
                idx = -1;
                break;
        }
        return idx >= 0 ? BOSS_COORDS[idx] : null;
    }

    public DungeonFloor getCurrentFloor() {
        return timers.getCurrentFloor();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ++tickCounter % 10 != 0) return;
        if (ATHRConfig.feature == null) return;
        if (mc.thePlayer == null) return;

        if (!timers.isInDungeon()) {
            detectDungeonStart();
            return;
        }

        if (timers.isRunEnded()) return;

        updateFloorFromScoreboard();
        updateClearProgress();
        updateBossEntry();
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (ATHRConfig.feature == null) return;
        if (!timers.isInDungeon()) return;

        String clean = ChatUtils.clean(event);

        if (!timers.isRunEnded()) {
            boolean handled = phaseDetector.handleGeneralEvents(clean);
            if (handled && timers.isRunEnded()) {
                if (ATHRConfig.feature.dungeons.dungeonOverlay.dungeonStats) statsPrinter.printEndStats();
                return;
            }
            phaseDetector.handleFloorPhases(clean);
        } else {
            phaseDetector.handleGeneralEvents(clean);
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        timers.reset();
        endStats.reset();
    }

    private void detectDungeonStart() {
        for (String line : SkyblockData.getCleanScoreboardLines()) {
            if (TIME_ELAPSED.matcher(line).find()) {
                timers.setInDungeon(true);
                timers.setRunStart(System.currentTimeMillis());
                break;
            }
        }
    }

    private void updateFloorFromScoreboard() {
        DungeonFloor floor = DungeonUtils.getFloorFromScoreboard();
        if (floor != DungeonFloor.NONE) timers.setCurrentFloor(floor);
    }

    private void updateClearProgress() {
        if (timers.getClearedTime() > 0 || timers.getBossTime() > 0) return;

        for (String line : SkyblockData.getCleanScoreboardLines()) {
            if (!line.startsWith("Dungeon Cleared: ")) continue;
            try {
                int pct = Integer.parseInt(line.replace("Dungeon Cleared: ", "").replace("%", "").trim());
                if (pct == 100 && timers.getLastClearedPct() < 100) {
                    timers.setClearedTime(timers.elapsed());
                }
                timers.setLastClearedPct(pct);
            } catch (NumberFormatException ignored) {
            }
            break;
        }
    }

    private void updateBossEntry() {
        if (timers.getBossTime() > 0 || timers.getCurrentFloor() == DungeonFloor.NONE) return;

        int[] c = getBossCoords(timers.getCurrentFloor());
        if (c != null) {
            double dx = mc.thePlayer.posX - c[0];
            double dy = mc.thePlayer.posY - c[1];
            double dz = mc.thePlayer.posZ - c[2];
            if (dx * dx + dy * dy + dz * dz <= 30 * 30) {
                timers.setBossTime(timers.elapsed());
                if (timers.getClearedTime() == 0) {
                    timers.setClearedTime(timers.getBossTime());
                }
            }
        }
    }

    @Override
    public Position getPosition() {
        return ATHRConfig.feature.dungeons.dungeonOverlay.statsPos;
    }

    @Override
    public float getScale() {
        return ATHRConfig.feature.dungeons.dungeonOverlay.statsScale;
    }

    @Override
    public int getBgColor() {
        return ChromaColour.specialToChromaRGB(ATHRConfig.feature.dungeons.dungeonOverlay.statsBgColor);
    }

    @Override
    public int getCornerRadius() {
        return ATHRConfig.feature.dungeons.dungeonOverlay.statsCornerRadius;
    }

    @Override
    protected int getBaseWidth() {
        return 60;
    }

    @Override
    protected boolean extraGuard() {
        return timers.isInDungeon() && timers.getRunStart() != 0;
    }

    @Override
    protected boolean isEnabled() {
        return ATHRConfig.feature.dungeons.dungeonOverlay.dungeonStats;
    }

    @Override
    public List<String> getLines(boolean preview) {
        return statsOverlay.getLines(preview);
    }
}