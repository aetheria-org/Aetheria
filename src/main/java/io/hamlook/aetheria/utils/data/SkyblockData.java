package io.hamlook.aetheria.utils.data;

import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;

import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class SkyblockData {

    private SkyblockData() {
    }

    public static String getServerId() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) return "";
        Scoreboard sb = mc.theWorld.getScoreboard();
        if (sb == null) return "";
        ScoreObjective obj = sb.getObjectiveInDisplaySlot(1);
        if (obj == null) return "";
        return net.minecraft.util.StringUtils.stripControlCodes(obj.getDisplayName());
    }

    public static String getScoreboardTitle() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) return null;
        Scoreboard sb = mc.theWorld.getScoreboard();
        if (sb == null) return null;
        ScoreObjective obj = sb.getObjectiveInDisplaySlot(1);
        if (obj == null) return null;
        return obj.getDisplayName();
    }

    public static Location getCurrentLocation() {
        return TablistParser.getCurrentLocation();
    }

    public static List<String> getScoreboardLines() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) return Collections.emptyList();

        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        if (scoreboard == null) return Collections.emptyList();

        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
        if (objective == null) return Collections.emptyList();

        List<Score> scores;
        try {
            scores = scoreboard.getSortedScores(objective).stream().filter(s -> s != null && s.getPlayerName() != null && !s.getPlayerName().startsWith("#")).collect(Collectors.toList());
        } catch (ConcurrentModificationException e) {
            return Collections.emptyList();
        }

        int size = scores.size();
        return IntStream.range(Math.max(0, size - 15), size).mapToObj(i -> {
            Score score = scores.get(i);
            ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
            return ScorePlayerTeam.formatPlayerName(team, score.getPlayerName());
        }).collect(Collectors.toList());
    }

    public static List<String> getCleanScoreboardLines() {
        return getScoreboardLines().stream().map(s -> net.minecraft.util.StringUtils.stripControlCodes(s).trim()).collect(Collectors.toList());
    }

    private static String currentProfile = "";

    public static String getCurrentProfile() {
        return currentProfile;
    }

    public static void setCurrentProfile(String profile) {
        currentProfile = profile;
    }

    public static boolean isOnSkyblock() {
        return getCurrentLocation() != Location.NONE;
    }

    public static boolean isInDungeon() {
        return getScoreboardLines().stream().anyMatch(l -> l.contains("The Catacombs") || l.contains("Master Mode"));
    }

    public static boolean isInMist() {
        return getCleanScoreboardLines().stream().anyMatch(line -> line.contains("The Mist"));
    }

    public enum Location {
        HUB("skyblock-", "skyblock_sandbox-", "skyblocktest-"), DUNGEON("sbdungeon-", "sbdungeon_sandbox-", "sbdungeon_test-"), DWARVEN("sbm-", "sbm_sandbox-", "sbm_test-"), CRYSTAL_HOLLOWS("sbch-", "sbch_sandbox-", "sbtest_alpha-"), CRIMSON_ISLE("sbcris-", "sbcris_sandbox-", "sbcris_test-"), PRIVATE_ISLAND("sbi-", "sbi_sandbox-", "sbi_test-"), DUNGEON_HUB("sbdh-", "sbdh_sandbox-", "sbdh_test-"), BARN("sbfarms-", "sbfarms_sandbox-", "sbfarms_test-"), PARK("sbpark-", "sbpark_sandbox-", "sbpark_test-"), SPIDERS_DEN("sbspiders-", "sbspiders_sandbox-", "sbspiders_test-"), THE_END("sbend-", "sbend_sandbox-", "sbend_test-"), JERRY("sbj-", "sbj_sandbox-", "sbj_test-"), GOLD_MINE("sbmines-", "sbmines_sandbox-", "sbmines_test-"), NONE("", "", "");

        public final String main, sandbox, alpha;

        Location(String main, String sandbox, String alpha) {
            this.main = main;
            this.sandbox = sandbox;
            this.alpha = alpha;
        }
    }
}