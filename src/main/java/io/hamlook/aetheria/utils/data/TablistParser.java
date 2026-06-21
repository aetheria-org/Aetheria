package io.hamlook.aetheria.utils.data;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import io.hamlook.aetheria.features.scoreboard.BankParser;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.ColorUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

@RegisterEvents
public class TablistParser {

    private static final int TICK_INTERVAL = 20;
    private static final Ordering<NetworkPlayerInfo> PLAYER_ORDERING = Ordering.from(new PlayerComparator());
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
    private static final Pattern SB_LEVEL = Pattern.compile("SB Level: \\[(\\d+)\\] (\\d+)/(\\d+) XP");
    private static String currentMayor = "";
    @Setter
    private static java.util.function.BiConsumer<Long, Long> gemstonePowderChangeListener = null;

    public static boolean isDianaMayor() {
        return "Diana".equals(currentMayor);
    }
    private int tickCounter = 0;

    public static boolean isEventActive(String eventName) {
        return activeEvent != null && activeEvent.contains(eventName);
    }

    private static net.minecraft.util.IChatComponent getTabFooter() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer == null) return null;
            java.lang.reflect.Field f = mc.ingameGUI.getTabList().getClass().getDeclaredField("field_175255_h");
            f.setAccessible(true);
            return (net.minecraft.util.IChatComponent) f.get(mc.ingameGUI.getTabList());
        } catch (Exception e) {
            return null;
        }
    }

    public static String readGems() {
        Minecraft mc = Minecraft.getMinecraft();
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
        String[] lines = net.minecraft.util.StringUtils.stripControlCodes(footer.getFormattedText()).split("\n");
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
        GuiPlayerTabOverlay tab = mc.ingameGUI.getTabList();
        List<NetworkPlayerInfo> infos = PLAYER_ORDERING.sortedCopy(mc.thePlayer.sendQueue.getPlayerInfoMap());

        boolean inServerSection = false;
        boolean inAccountSection = false;
        boolean expectEventTime = false;

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

            if (inServerSection) {
                if (line.startsWith("Dungeon: ")) {
                    currentLocation = SkyblockData.Location.DUNGEON;
                    continue;
                }
                if (line.startsWith("Server: ")) {
                    String s = line.substring("Server: ".length()).trim();
                    int dash = indexOfDashDigits(s);
                    if (dash >= 0) s = s.substring(0, dash + 1);
                    currentLocation = matchLocation(s);
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
                if (line.startsWith("Current Mayor: ")) {
                    currentMayor = line.substring("Current Mayor: ".length()).trim();
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
    }

    private static String parseAmount(String raw, String fallback) {
        String afterColon = raw.substring(raw.indexOf(": ") + 2);
        String clean = ColorUtils.stripColor(afterColon).trim();
        if (clean.contains(" / ")) {
            String[] parts = clean.split(" / ", 2);
            return parts[0].trim() + " §7/ §6" + parts[1].trim();
        }
        return clean.isEmpty() ? fallback : clean;
    }

    private static SkyblockData.Location matchLocation(String s) {
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

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if ((tickCounter = (tickCounter + 1) % TICK_INTERVAL) != 0) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        parseTablist(mc);
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        currentLocation = SkyblockData.Location.NONE;
        activeEvent = null;
        activeEventTimeLeft = null;
        gemstonePowder = 0;
        mithrilPowder = 0;
        glacitePowder = 0;
        sbLevel = 0;
        sbCurrentXp = 0;
        sbMaxXp = 0;
        currentMayor = "";
        BankParser.clear();
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