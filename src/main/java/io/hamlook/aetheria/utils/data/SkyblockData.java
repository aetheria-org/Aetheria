package io.hamlook.aetheria.utils.data;

import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.ScoreboardCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.BlockPos;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class SkyblockData {

    private static String currentProfile = "";
    private static Environment currentEnvironment = Environment.UNKNOWN;

    private SkyblockData() {
    }

    public static String getServerId() {
        Minecraft mc = MinecraftCompat.getMinecraft();
        if (MinecraftCompat.getLocalWorld() == null) return "";
        Scoreboard sb = MinecraftCompat.getLocalWorld().getScoreboard();
        if (sb == null) return "";
        ScoreObjective obj = ScoreboardCompat.getSidebarObjective(sb);
        if (obj == null) return "";
        return net.minecraft.util.StringUtils.stripControlCodes(obj.getDisplayName());
    }

    public static String getScoreboardTitle() {
        Minecraft mc = MinecraftCompat.getMinecraft();
        if (MinecraftCompat.getLocalWorld() == null) return null;
        Scoreboard sb = MinecraftCompat.getLocalWorld().getScoreboard();
        if (sb == null) return null;
        ScoreObjective obj = ScoreboardCompat.getSidebarObjective(sb);
        if (obj == null) return null;
        return obj.getDisplayName();
    }

    public static Location getCurrentLocation() {
        return TablistParser.getCurrentLocation();
    }

    public static List<String> getScoreboardLines() {
        Minecraft mc = MinecraftCompat.getMinecraft();
        if (MinecraftCompat.getLocalWorld() == null) return Collections.emptyList();

        Scoreboard scoreboard = MinecraftCompat.getLocalWorld().getScoreboard();
        if (scoreboard == null) return Collections.emptyList();

        ScoreObjective objective = ScoreboardCompat.getSidebarObjective(scoreboard);
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

    private static String purseLineCache;
    private static int purseLineCacheTick = -1;

    /**
     * Stripped sidebar line containing "Purse"/"Piggy", or null. Memoized per
     * game tick so multiple consumers share one scoreboard read per tick.
     */
    public static String getPurseLine() {
        Minecraft mc = MinecraftCompat.getMinecraft();
        if (MinecraftCompat.getLocalWorld() == null || MinecraftCompat.getLocalPlayer() == null) {
            purseLineCacheTick = -1;
            purseLineCache = null;
            return null;
        }
        if (purseLineCacheTick != MinecraftCompat.getLocalPlayer().ticksExisted) {
            purseLineCacheTick = MinecraftCompat.getLocalPlayer().ticksExisted;
            purseLineCache = getCleanScoreboardLines().stream()
                    .filter(l -> l.contains("Purse") || l.contains("Piggy"))
                    .findFirst().orElse(null);
        }
        return purseLineCache;
    }

    public static String getCurrentProfile() {
        return currentProfile;
    }

    public static void setCurrentProfile(String profile) {
        currentProfile = profile;
    }

    public static Environment getEnvironment() {
        return currentEnvironment;
    }

    public static void setEnvironment(Environment environment) {
        currentEnvironment = environment;
    }

    public static String getEnvironmentKey() {
        switch (currentEnvironment) {
            case SANDBOX:
                return "sandbox";
            case ALPHA:
                return "alpha";
            default:
                return "normal";
        }
    }

    public static Environment detectEnvironment(String serverPrefix) {
        if (serverPrefix == null) return Environment.NORMAL;
        if (serverPrefix.contains("sandbox")) return Environment.SANDBOX;
        if (serverPrefix.contains("test") || serverPrefix.contains("alpha")) return Environment.ALPHA;
        return Environment.NORMAL;
    }

    public static Environment detectEnvironmentFromScoreboard() {
        String title = getScoreboardTitle();
        if (title == null) return Environment.UNKNOWN;
        String clean = net.minecraft.util.StringUtils.stripControlCodes(title).trim().toUpperCase(Locale.ROOT);
        if (clean.contains("SANDBOX")) return Environment.SANDBOX;
        if (clean.startsWith("SKYBLOCK")) return Environment.NORMAL;
        return Environment.UNKNOWN;
    }

    public static String getIgn() {
        String name = MinecraftCompat.getMinecraft().getSession().getUsername();
        return name == null ? "" : name;
    }

    public static boolean isOnSkyblock() {
        if (getCurrentLocation() != Location.NONE) return true;
        String prefix = TablistParser.getServerPrefix();
        if (prefix != null && !prefix.isEmpty()
                && (prefix.startsWith("skyblock") || prefix.startsWith("sb"))) return true;
        return TablistParser.getScoreboardEnvironment() != Environment.UNKNOWN;
    }

    public static boolean isInDungeon() {
        return getScoreboardLines().stream().anyMatch(l -> l.contains("The Catacombs") || l.contains("Master Mode"));
    }

    public static boolean isInMist() {
        return getCleanScoreboardLines().stream().anyMatch(line -> line.contains("The Mist"));
    }

    /**
     * Trevor (Trapper) animal spawn spots on the Mushroom Desert, grouped by
     * the area name Trevor announces in his quest start message. Keys are
     * lower-cased area names; lists are immutable.
     */
    private static final Map<String, List<BlockPos>> TREVOR_SPOTS;

    static {
        Map<String, List<BlockPos>> spots = new HashMap<>();
        spots.put("desert settlement", Collections.unmodifiableList(Arrays.asList(
                new BlockPos(184, 77, -352),
                new BlockPos(139, 77, -375))));
        spots.put("oasis", Collections.unmodifiableList(Arrays.asList(
                new BlockPos(104, 65, -473),
                new BlockPos(116, 65, -416),
                new BlockPos(165, 77, -464))));
        spots.put("mushroom gorge", Collections.unmodifiableList(Arrays.asList(
                new BlockPos(220, 41, -578),
                new BlockPos(234, 54, -500),
                new BlockPos(187, 42, -520),
                new BlockPos(303, 51, -409),
                new BlockPos(189, 43, -443))));
        spots.put("overgrown mushroom cave", Collections.unmodifiableList(Arrays.asList(
                new BlockPos(247, 57, -421),
                new BlockPos(248, 58, -369))));
        TREVOR_SPOTS = Collections.unmodifiableMap(spots);
    }

    public static List<BlockPos> getTrevorSpotsForArea(String area) {
        if (area == null) return Collections.emptyList();
        List<BlockPos> spots = TREVOR_SPOTS.get(area.toLowerCase(Locale.ROOT).trim());
        return spots == null ? Collections.emptyList() : spots;
    }

    /**
     * Returns the Trevor spawn area the player is currently in, or null.
     * Scoreboard area lines carry a "⏣ " prefix, so this matches by contains
     * rather than key equality.
     */
    public static String getCurrentTrevorAreaFromScoreboard() {
        for (String line : getCleanScoreboardLines()) {
            String clean = line.toLowerCase(Locale.ROOT).trim();
            for (String key : TREVOR_SPOTS.keySet()) {
                if (clean.contains(key)) return key;
            }
        }
        return null;
    }

    public enum Environment {
        NORMAL, SANDBOX, ALPHA, UNKNOWN;

        public boolean isTest() {
            return this == SANDBOX || this == ALPHA;
        }
    }

    public enum Location {
        HUB("skyblock-", "skyblock_sandbox-", "skyblocktest-"),
        DUNGEON("sbdungeon-", "sbdungeon_sandbox-", "sbdungeon_test-"),
        DWARVEN("sbm-", "sbm_sandbox-", "sbm_test-"),
        CRYSTAL_HOLLOWS("sbch-", "sbch_sandbox-", "sbtest_alpha-"),
        CRIMSON_ISLE("sbcris-", "sbcris_sandbox-", "sbcris_test-"),
        PRIVATE_ISLAND("sbi-", "sbi_sandbox-", "sbi_test-"),
        DUNGEON_HUB("sbdh-", "sbdh_sandbox-", "sbdh_test-"),
        BARN("sbfarms-", "sbfarms_sandbox-", "sbfarms_test-"),
        PARK("sbpark-", "sbpark_sandbox-", "sbpark_test-"),
        SPIDERS_DEN("sbspiders-", "sbspiders_sandbox-", "sbspiders_test-"),
        THE_END("sbend-", "sbend_sandbox-", "sbend_test-"),
        JERRY("sbj-", "sbj_sandbox-", "sbj_test-"),
        GOLD_MINE("sbmines-", "sbmines_sandbox-", "sbmines_test-"),
        GARDEN("sbg-", "sbg_sandbox-", "sbg_test-"),
        NONE("", "", "");

        public final String main, sandbox, alpha;

        Location(String main, String sandbox, String alpha) {
            this.main = main;
            this.sandbox = sandbox;
            this.alpha = alpha;
        }
    }
}