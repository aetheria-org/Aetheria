package io.hamlook.aetheria.utils.data;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.events.ASMTickEvent;
import io.hamlook.aetheria.events.ASMWorldLoadEvent;
import io.hamlook.aetheria.events.ASMWorldUnloadEvent;
import io.hamlook.aetheria.features.farming.FarmingApi;
import io.hamlook.aetheria.features.scoreboard.BankParser;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.ElectionUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.TextCompat;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RegisterEvents
public class TablistParser {

    private static final int TICK_INTERVAL = 20;
    private static final int FAST_PARSE_INTERVAL = 5;
    private static final long FAST_PARSE_WINDOW_MS = 3000;
    private static final Ordering<NetworkPlayerInfo> PLAYER_ORDERING = Ordering.from(new PlayerComparator());
    private static final Pattern SB_LEVEL = Pattern.compile("SB Level: \\[(\\d+)\\] (\\d+)/(\\d+) XP");
    private static final Pattern PEST_PLOT_ENTRY = Pattern.compile("^(\\d+)(?:\\s*[xX]\\s*(\\d+))?$");
    @Getter
    private static SkyblockData.Location currentLocation = SkyblockData.Location.NONE;
    @Getter
    private static String activeEvent = null;
    @Getter
    private static String activeEventTimeLeft = null;
    @Getter
    private static long gemstonePowder = 0;
    @Getter
    private static long mithrilPowder = 0;
    @Getter
    private static long glacitePowder = 0;
    @Getter
    private static int sbLevel = 0;
    @Getter
    private static int sbCurrentXp = 0;
    @Getter
    private static int sbMaxXp = 0;
    @Getter
    private static String miningSpeed = "";
    @Getter
    private static String miningFortune = "";
    @Getter
    private static String miningSpread = "";
    @Getter
    private static String gemstoneFortune = "";
    @Getter
    private static String pristine = "";
    @Getter
    private static String serverPrefix = "";
    @Getter
    private static SkyblockData.Environment scoreboardEnvironment = SkyblockData.Environment.UNKNOWN;
    @Setter
    private static java.util.function.BiConsumer<Long, Long> gemstonePowderChangeListener = null;
    private int tickCounter = 0;
    private long worldJoinTime = -1;

    public static boolean isEventActive(String eventName) {
        return activeEvent != null && activeEvent.contains(eventName);
    }

    /**
     * Tab entries in the exact order parseTablist iterates them (team-then-name
     * alphabetical). Debug dumps must use this, not raw getPlayerInfoMap() order.
     */
    public static List<NetworkPlayerInfo> getParserOrderedInfos(Minecraft mc) {
        return PLAYER_ORDERING.sortedCopy(mc.thePlayer.sendQueue.getPlayerInfoMap());
    }

    private static net.minecraft.util.IChatComponent getTabFooter() {
        try {
            Minecraft mc = MinecraftCompat.getMinecraft();
            if (mc.thePlayer == null) return null;
            java.lang.reflect.Field f = mc.ingameGUI.getTabList().getClass().getDeclaredField("field_175255_h");
            f.setAccessible(true);
            return (net.minecraft.util.IChatComponent) f.get(mc.ingameGUI.getTabList());
        } catch (Exception e) {
            return null;
        }
    }

    public static String readGems() {
        Minecraft mc = MinecraftCompat.getMinecraft();
        if (mc.thePlayer == null) return null;
        GuiPlayerTabOverlay tab = mc.ingameGUI.getTabList();
        List<NetworkPlayerInfo> infos = PLAYER_ORDERING.sortedCopy(mc.thePlayer.sendQueue.getPlayerInfoMap());
        boolean inServer = false;
        for (NetworkPlayerInfo info : infos) {
            String raw = tab.getPlayerName(info);
            if (raw == null || raw.isEmpty()) continue;
            String line = net.minecraft.util.StringUtils.stripControlCodes(raw).trim();
            if (line.isEmpty()) continue;
            if (line.equals("Server Info") || raw.contains("Server Info")) {
                inServer = true;
                continue;
            }
            if (inServer && (line.equals("Account Info") || line.equals("Player Stats"))) {
                inServer = false;
                continue;
            }
            if (inServer && line.startsWith("Gems: ")) return line.substring("Gems: ".length()).trim();
        }
        return null;
    }

    public static String readCookieBuff() {
        net.minecraft.util.IChatComponent footer = getTabFooter();
        if (footer == null) return null;
        String[] lines = net.minecraft.util.StringUtils.stripControlCodes(TextCompat.getFormattedText(footer)).split("\n");
        boolean sawCookie = false;
        for (String line : lines) {
            String l = line.trim();
            if (l.isEmpty()) continue;
            if (!sawCookie && l.contains("Cookie Buff")) {
                sawCookie = true;
                continue;
            }
            if (sawCookie && l.contains("Active")) continue;
            if (sawCookie) return l;
        }
        return null;
    }

    private static void parseTablist(Minecraft mc) {
        scoreboardEnvironment = SkyblockData.detectEnvironmentFromScoreboard();
        GuiPlayerTabOverlay tab = mc.ingameGUI.getTabList();
        List<NetworkPlayerInfo> infos = getParserOrderedInfos(mc);

        boolean inServerSection = false;
        boolean inAccountSection = false;
        boolean expectEventTime = false;
        boolean readingVisitors = false;
        boolean visitorsSectionSeen = false;
        boolean inStatsSection = false;
        List<String> parsedVisitors = new ArrayList<>();

        String pendingEvent = null;

        for (NetworkPlayerInfo info : infos) {
            String raw = tab.getPlayerName(info);
            if (raw == null || raw.isEmpty()) continue;

            String line = net.minecraft.util.StringUtils.stripControlCodes(raw).trim();

            if (raw.contains("§3§l Server Info§r")) {
                inServerSection = true;
                inAccountSection = false;
                expectEventTime = false;
                continue;
            }
            if (raw.contains("§6§lAccount Info") || line.equals("Account Info")) {
                inAccountSection = true;
                inServerSection = false;
                expectEventTime = false;
                continue;
            }
            if (raw.contains("§2§lPlayer Stats§r") || line.equals("Player Stats") || line.equals("Quests") || line.equals("Party") || line.equals("Dungeon")) {
                inServerSection = false;
                inAccountSection = false;
                expectEventTime = false;
                continue;
            }

            if (line.isEmpty()) {
                if (expectEventTime) {
                    activeEvent = pendingEvent;
                    activeEventTimeLeft = null;
                    expectEventTime = false;
                    pendingEvent = null;
                }
                continue;
            }

            if (line.startsWith("Alive: ")) {
                String alive = line.substring("Alive: ".length()).trim();
                FarmingApi.setGardenAlive(alive);
                if ("0".equals(alive)) {
                    FarmingApi.setActivePests(Collections.emptyMap());
                }
                continue;
            }
            if (line.startsWith("Plots: ")) {
                FarmingApi.setActivePests(parsePestPlots(line.substring("Plots: ".length())));
                continue;
            }
            if (line.startsWith("Spray: ")) {
                FarmingApi.setGardenSpray(valueAfter(raw));
                continue;
            }
            if (line.startsWith("Repellent: ")) {
                FarmingApi.setGardenRepellent(valueAfter(raw));
                continue;
            }
            if (line.startsWith("Bonus: ")) {
                FarmingApi.setGardenBonus(valueAfter(raw));
                continue;
            }
            if (line.startsWith("Cooldown: ")) {
                FarmingApi.setGardenCooldown(valueAfter(raw));
                continue;
            }
            if (line.startsWith("Bonus Pest Chance: ")) {
                FarmingApi.setGardenBonusPestChance(valueAfter(raw));
                continue;
            }

            if (line.startsWith("Visitors")) {
                readingVisitors = true;
                visitorsSectionSeen = true;
                continue;
            }
            if (readingVisitors) {
                if (line.startsWith("Next Visitor")) {
                    readingVisitors = false;
                } else {
                    String name = ColorUtils.stripColor(raw).trim();
                    if (!name.isEmpty()) parsedVisitors.add(name);
                }
                continue;
            }

            if (inServerSection) {
                if (line.startsWith("Dungeon: ")) {
                    currentLocation = SkyblockData.Location.DUNGEON;
                    continue;
                }
                if (line.startsWith("Server: ")) {
                    String s = line.substring("Server: ".length()).trim();
                    int dash = indexOfDashDigits(s);
                    if (dash >= 0) s = s.substring(0, dash + 1);
                    serverPrefix = s;
                    currentLocation = matchLocation(s);
                    if (currentLocation != SkyblockData.Location.GARDEN) {
                        FarmingApi.clearGardenPestData();
                    }
                    SkyblockData.Environment env = SkyblockData.detectEnvironment(s);
                    if (env != SkyblockData.getEnvironment()) {
                        ProfileDetector.onEnvironmentChanged(SkyblockData.getEnvironment(), env);
                    }
                    continue;
                }
                if (line.startsWith("Mithril Powder: ")) {
                    String num = line.substring("Mithril Powder: ".length()).replaceAll(",", "");
                    try {
                        mithrilPowder = Long.parseLong(num);
                    } catch (NumberFormatException ignored) {
                    }
                    continue;
                }
                if (line.startsWith("Gemstone Powder: ")) {
                    String num = line.substring("Gemstone Powder: ".length()).replaceAll(",", "");
                    try {
                        long newValue = Long.parseLong(num);
                        long oldValue = gemstonePowder;
                        gemstonePowder = newValue;
                        if (gemstonePowderChangeListener != null && newValue != oldValue) {
                            gemstonePowderChangeListener.accept(oldValue, newValue);
                        }
                    } catch (NumberFormatException ignored) {
                    }
                    continue;
                }
                if (line.startsWith("Glacite Powder: ")) {
                    String num = line.substring("Glacite Powder: ".length()).replaceAll(",", "");
                    try {
                        glacitePowder = Long.parseLong(num);
                    } catch (NumberFormatException ignored) {
                    }
                    continue;
                }
            }

            if (inAccountSection) {
                if (raw.contains("§e§lStats:")) {
                    inStatsSection = true;
                    continue;
                }
                if (raw.contains("§l")) {
                    inStatsSection = false;
                }

                if (inStatsSection) {
                    if (line.startsWith("Mining Speed: ")) {
                        miningSpeed = valueAfter(raw);
                        continue;
                    }
                    if (line.startsWith("Mining Fortune: ")) {
                        miningFortune = valueAfter(raw);
                        continue;
                    }
                    if (line.startsWith("Mining Spread: ")) {
                        miningSpread = valueAfter(raw);
                        continue;
                    }
                    if (line.startsWith("Gemstone Fortune: ")) {
                        gemstoneFortune = valueAfter(raw);
                        continue;
                    }
                    if (line.startsWith("Pristine: ")) {
                        pristine = valueAfter(raw);
                        continue;
                    }
                }

                if (expectEventTime) {
                    if (line.startsWith("Ends in: ")) {
                        activeEventTimeLeft = line.substring("Ends in: ".length()).trim();
                    } else if (line.equals("No active event")) {
                        activeEvent = null;
                        activeEventTimeLeft = null;
                    } else {
                        activeEventTimeLeft = null;
                    }
                    expectEventTime = false;
                    pendingEvent = null;
                    continue;
                }

                if (line.startsWith("Event: ")) {
                    activeEvent = line.substring("Event: ".length()).trim();
                    activeEventTimeLeft = null;
                    expectEventTime = true;
                    pendingEvent = activeEvent;
                    continue;
                }

                if (line.equals("Mining Event:") || line.startsWith("Mining Event: ")) {
                    activeEvent = null;
                    activeEventTimeLeft = null;
                    expectEventTime = true;
                    pendingEvent = null;
                    continue;
                }

                if (line.startsWith("Bank: ")) {
                    BankParser.setBank(parseAmount(raw, line.substring("Bank: ".length())));
                    continue;
                }
                if (line.startsWith("Purse: ") || line.startsWith("Piggy: ")) {
                    int colon = line.indexOf(": ");
                    String amt = ColorUtils.stripColor(raw.substring(raw.indexOf(": ") + 2)).trim();
                    BankParser.setPurse(amt.isEmpty() ? line.substring(colon + 2) : amt);
                    continue;
                }
                if (line.startsWith("Current Mayor: ") || line.startsWith("Winner: ")) {
                    ElectionUtils.updateMayorFromTablist(line.substring(line.indexOf(": ") + 2).trim());
                    continue;
                }
                if (line.startsWith("SB Level:")) {
                    java.util.regex.Matcher m = SB_LEVEL.matcher(line);
                    if (m.find()) {
                        sbLevel = Integer.parseInt(m.group(1));
                        sbCurrentXp = Integer.parseInt(m.group(2));
                        sbMaxXp = Integer.parseInt(m.group(3));
                    }
                }
            }
        }

        if (expectEventTime && pendingEvent == null) {
            activeEvent = null;
            activeEventTimeLeft = null;
        }

        FarmingApi.setActiveVisitors(parsedVisitors, visitorsSectionSeen);

        if (serverPrefix.isEmpty()) {
            SkyblockData.Environment env = scoreboardEnvironment;
            if (env != SkyblockData.Environment.UNKNOWN && env != SkyblockData.getEnvironment()) {
                ProfileDetector.onEnvironmentChanged(SkyblockData.getEnvironment(), env);
            }
        }
    }

    private static String parseAmount(String raw, String fallback) {        String afterColon = raw.substring(raw.indexOf(": ") + 2);
        String clean = ColorUtils.stripColor(afterColon).trim();
        if (clean.contains(" / ")) {
            String[] parts = clean.split(" / ", 2);
            return parts[0].trim() + " §7/ §6" + parts[1].trim();
        }
        return clean.isEmpty() ? fallback : clean;
    }

    private static String valueAfter(String raw) {
        int idx = raw.indexOf(": ");
        return idx < 0 ? "" : raw.substring(idx + 2).trim();
    }

    private static Map<Integer, Integer> parsePestPlots(String plotsText) {
        Map<Integer, Integer> out = new HashMap<>();
        for (String entry : plotsText.split(",")) {
            Matcher m = PEST_PLOT_ENTRY.matcher(entry.trim());
            if (!m.matches()) continue;
            int plot;
            int count;
            try {
                plot = Integer.parseInt(m.group(1));
                count = m.group(2) == null ? 1 : Integer.parseInt(m.group(2));
            } catch (NumberFormatException ignored) {
                continue;
            }
            if (plot >= 1 && plot <= 24 && count > 0) out.put(plot, count);
        }
        return out;
    }

    private static SkyblockData.Location matchLocation(String s) {
        if (s.startsWith("sbg")) return SkyblockData.Location.GARDEN;
        for (SkyblockData.Location loc : SkyblockData.Location.values()) {
            if (loc.main.isEmpty()) continue;
            if (loc.main.equals(s) || loc.sandbox.equals(s) || loc.alpha.equals(s)) return loc;
        }
        return SkyblockData.Location.NONE;
    }

    private static int indexOfDashDigits(String s) {
        for (int i = 0; i < s.length() - 1; i++) {
            if (s.charAt(i) == '-' && Character.isDigit(s.charAt(i + 1))) return i;
        }
        return -1;
    }

    @HandleEvent
    public void onTick(ASMTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        int interval = worldJoinTime > 0 && System.currentTimeMillis() - worldJoinTime < FAST_PARSE_WINDOW_MS
                ? FAST_PARSE_INTERVAL
                : TICK_INTERVAL;
        if ((tickCounter = (tickCounter + 1) % interval) != 0) return;

        Minecraft mc = MinecraftCompat.getMinecraft();
        if (mc.thePlayer == null) return;

        parseTablist(mc);
    }

    @HandleEvent
    public void onWorldLoad(ASMWorldLoadEvent event) {
        worldJoinTime = System.currentTimeMillis();
    }

    @HandleEvent
    public void onWorldUnload(ASMWorldUnloadEvent event) {
        currentLocation = SkyblockData.Location.NONE;
        activeEvent = null;
        activeEventTimeLeft = null;
        gemstonePowder = 0;
        mithrilPowder = 0;
        glacitePowder = 0;
        sbLevel = 0;
        sbCurrentXp = 0;
        sbMaxXp = 0;
        miningSpeed = "";
        miningFortune = "";
        miningSpread = "";
        gemstoneFortune = "";
        pristine = "";
        FarmingApi.clearActiveVisitors();
        ElectionUtils.clearTablistMayor();
        serverPrefix = "";
        scoreboardEnvironment = SkyblockData.Environment.UNKNOWN;
        BankParser.clear();
        FarmingApi.clearGardenPestData();
    }

    private static class PlayerComparator implements Comparator<NetworkPlayerInfo> {
        @Override
        public int compare(NetworkPlayerInfo o1, NetworkPlayerInfo o2) {
            ScorePlayerTeam t1 = o1.getPlayerTeam();
            ScorePlayerTeam t2 = o2.getPlayerTeam();
            return ComparisonChain.start().compareTrueFirst(o1.getGameType() != WorldSettings.GameType.SPECTATOR, o2.getGameType() != WorldSettings.GameType.SPECTATOR).compare(t1 != null ? t1.getRegisteredName() : "", t2 != null ? t2.getRegisteredName() : "").compare(o1.getGameProfile().getName(), o2.getGameProfile().getName()).result();
        }
    }
}