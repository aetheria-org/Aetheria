package io.hamlook.aetheria.features.dungeons.overlays;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.dungeons.utils.DungeonFloor;
import io.hamlook.aetheria.features.dungeons.utils.DungeonTimers;
import io.hamlook.aetheria.utils.time.TimeFormatter;
import net.minecraft.util.EnumChatFormatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PhaseOverlay {

    private static final String C_FLOOR_NM = EnumChatFormatting.GREEN.toString();
    private static final String C_FLOOR_MM = EnumChatFormatting.RED.toString();
    private static final String C_DUNGEON = EnumChatFormatting.YELLOW.toString();
    private static final String C_CLEAR = EnumChatFormatting.GREEN.toString();
    private static final String C_BLOOD = EnumChatFormatting.RED.toString();
    private static final String C_ENTRY = EnumChatFormatting.GOLD.toString();
    private static final String C_BOSS = EnumChatFormatting.DARK_RED.toString();
    private static final String C_MAXOR = EnumChatFormatting.YELLOW.toString();
    private static final String C_STORM = EnumChatFormatting.AQUA.toString();
    private static final String C_GOLDOR = EnumChatFormatting.GOLD.toString();
    private static final String C_NECRON = EnumChatFormatting.DARK_PURPLE.toString();
    private static final String C_WITHER = EnumChatFormatting.DARK_GRAY.toString();
    private static final String C_SCARF = EnumChatFormatting.DARK_PURPLE.toString();
    private static final String C_PROFESSOR = EnumChatFormatting.BLUE.toString();
    private static final String C_TERRA = EnumChatFormatting.GOLD.toString();
    private static final String C_GIANTS = EnumChatFormatting.RED.toString();
    private static final String C_SADAN = EnumChatFormatting.DARK_RED.toString();
    private static final String C_VAL = EnumChatFormatting.WHITE.toString();

    private final DungeonTimers timers;

    public PhaseOverlay(DungeonTimers timers) {
        this.timers = timers;
    }

    public List<String> getLines(boolean preview) {
        boolean ended = timers.isRunEnded();
        long now = ended ? (timers.getBossDeadTime() > 0 ? timers.getBossDeadTime() : timers.elapsed()) : timers.elapsed();
        DungeonFloor f = preview ? DungeonFloor.F7 : timers.getCurrentFloor();
        boolean mm = f.isMasterMode();
        String floorCol = mm ? C_FLOOR_MM : C_FLOOR_NM;
        String floorName = f == DungeonFloor.NONE ? "?" : f.name();
        boolean showAll = (ATHRConfig.feature != null && ATHRConfig.feature.dungeons.dungeonOverlay.dungeonStatsShowAll) || ended || timers.isRunFailed();

        List<String> out = new ArrayList<>();

        long cleared = preview ? 0 : timers.getClearedTime();
        long blood = preview ? 0 : timers.getBloodTime();
        long boss = preview ? 0 : timers.getBossTime();
        long dead = preview ? 0 : timers.getBossDeadTime();

        if (preview || cleared == 0 || cleared > 0) {
            out.add(floorCol + "Floor: " + C_VAL + (preview ? "F7" : floorName));
        }

        out.add(C_DUNGEON + "Dungeon: " + C_VAL + (preview ? "4:32.100" : TimeFormatter.formatDungeonTime(now)));

        if (preview) {
            out.add(line(C_CLEAR, "Clear", cleared, now, true, "3:10.500"));
            out.add(line(C_BLOOD, "Blood Rush", blood, now, true, "3:45.200"));
            out.add(line(C_ENTRY, "Boss Entry", boss, now, true, "3:55.100"));
        } else {
            if (showAll) {
                if (cleared > 0 && cleared != now) out.add(line(C_CLEAR, "Clear", cleared, now, false, null));
                if (blood > 0) out.add(line(C_BLOOD, "Blood Rush", blood, now, false, null));
                if (boss > 0) out.add(line(C_ENTRY, "Boss Entry", boss, now, false, null));
            } else {
                if (boss > 0) out.add(line(C_ENTRY, "Boss Entry", boss, now, false, null));
                else if (blood > 0) out.add(line(C_BLOOD, "Blood Rush", blood, now, false, null));
                else if (cleared > 0 && cleared != now) out.add(line(C_CLEAR, "Clear", cleared, now, false, null));
            }
        }

        if (preview || boss > 0) {
            long bossDur = dead > 0 ? dead - boss : (boss > 0 ? now - boss : 0);
            if (dead > 0) out.add(C_BOSS + "Boss took: " + C_VAL + (preview ? "0:47.300" : TimeFormatter.formatDungeonTime(dead - boss)));
            else if (boss > 0) out.add(C_BOSS + "Boss: " + C_VAL + TimeFormatter.formatDungeonTime(bossDur));
        }

        addFloorPhases(out, f, now, preview);

        out.removeIf(Objects::isNull);
        return out;
    }

    private void addFloorPhases(List<String> out, DungeonFloor f, long now, boolean preview) {
        if (preview || f.isF7orM7()) {
            addPhase(out, C_MAXOR, "Maxor", timers.getMaxorStart(), timers.getMaxorEnd(), now, preview, "0:18.000");
            addPhase(out, C_STORM, "Storm", timers.getStormStart(), timers.getStormEnd(), now, preview, "0:12.000");
            addPhase(out, C_GOLDOR, "Terminals", timers.getTerminalStart(), timers.getGoldorFight(), now, preview, "0:20.000");
            addPhase(out, C_GOLDOR, "Goldor", timers.getGoldorFight(), timers.getGoldorEnd(), now, preview, "0:08.000");
            addPhase(out, C_NECRON, "Necron", timers.getNecronStart(), timers.getNecronEnd(), now, preview, "0:05.000");
            if (preview || f.isMasterMode()) {
                addPhase(out, C_WITHER, "Wither King", timers.getWitherStart(), timers.getWitherEnd(), now, preview, "0:04.000");
            }
        }

        if (preview || f.isF2orM2()) {
            addPhase(out, C_SCARF, "Scarf P1", timers.getScarfP1Start(), timers.getScarfP1End(), now, preview, "0:15.000");
            addPhase(out, C_SCARF, "Scarf P2", timers.getScarfP2Start(), timers.getScarfP2End(), now, preview, "0:20.000");
        }

        if (preview || f.isF3orM3()) {
            addPhase(out, C_PROFESSOR, "Professor P1", timers.getProfessorP1Start(), timers.getProfessorP1End(), now, preview, "0:25.000");
            addPhase(out, C_PROFESSOR, "Professor P2", timers.getProfessorP2Start(), timers.getProfessorP2End(), now, preview, "0:20.000");
            addPhase(out, C_PROFESSOR, "Professor P3", timers.getProfessorP3Start(), timers.getProfessorP3End(), now, preview, "0:15.000");
        }

        if (preview || f.isF6orM6()) {
            addPhase(out, C_TERRA, "Terracotta", timers.getTerraStart(), timers.getTerraEnd(), now, preview, "0:45.000");
            addPhase(out, C_GIANTS, "Giants", timers.getGiantsStart(), timers.getGiantsEnd(), now, preview, "0:30.000");
            addPhase(out, C_SADAN, "Sadan", timers.getSadanStart(), timers.getSadanEnd(), now, preview, "0:20.000");
        }
    }

    private static String line(String color, String label, long locked, long now, boolean preview, String previewVal) {
        if (preview) return color + label + " took: " + C_VAL + previewVal;
        if (locked > 0) return color + label + " took: " + C_VAL + TimeFormatter.formatDungeonTime(locked);
        return color + label + ": " + C_VAL + TimeFormatter.formatDungeonTime(now);
    }

    private static void addPhase(List<String> out, String color, String name, long start, long end, long now, boolean preview, String previewVal) {
        if (preview) {
            out.add(color + name + " took: " + C_VAL + previewVal);
            return;
        }
        if (start == 0) return;
        if (end > 0) out.add(color + name + " took: " + C_VAL + TimeFormatter.formatDungeonTime(end - start));
        else out.add(color + name + ": " + C_VAL + TimeFormatter.formatDungeonTime(now - start));
    }
}
