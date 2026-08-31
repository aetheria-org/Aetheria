package io.hamlook.aetheria.features.farming;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.events.BlockBreakEvent;
import io.hamlook.aetheria.features.farming.farmingtracker.FarmingTrackerData;
import io.hamlook.aetheria.features.farming.gardenplots.GardenPlotData;
import io.hamlook.aetheria.features.farming.sensitivityreducer.FarmingToolIds;
import io.hamlook.aetheria.features.farming.visitors.VisitorBonus;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.data.SkyblockData;
import io.hamlook.aetheria.utils.item.ItemUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RegisterEvents
public final class FarmingApi {

    /** How long after switching away from a farming tool its drops are still accepted. */
    public static final long TOOL_HOLD_WINDOW_MS = 3_000L;
    /** Warp target modes: 0 = closest plot, 1 = most pests (ties broken by distance) */
    public static final int WARP_CLOSEST = 0;
    public static final int WARP_MOST_PESTS = 1;
    private static final long FARMING_WINDOW_MS = 5_000L;
    private static final long VACUUM_WINDOW_MS = 10_000L;
    // Garden plots 1-24
    private static final AxisAlignedBB[] GARDEN_PLOTS = {new AxisAlignedBB(192, 67, 96, 288, 100, 192), new AxisAlignedBB(96, 67, 192, 192, 100, 288), new AxisAlignedBB(192, 67, 288, 288, 100, 384), new AxisAlignedBB(288, 67, 192, 384, 100, 288), new AxisAlignedBB(96, 67, 96, 192, 100, 192), new AxisAlignedBB(288, 67, 96, 384, 100, 192), new AxisAlignedBB(96, 67, 288, 192, 100, 384), new AxisAlignedBB(288, 67, 288, 384, 100, 384), new AxisAlignedBB(192, 67, 0, 288, 100, 96), new AxisAlignedBB(0, 67, 192, 96, 100, 288), new AxisAlignedBB(384, 67, 192, 480, 100, 288), new AxisAlignedBB(192, 67, 384, 288, 100, 480), new AxisAlignedBB(96, 67, 0, 192, 100, 96), new AxisAlignedBB(288, 67, 0, 384, 100, 96), new AxisAlignedBB(0, 67, 96, 96, 100, 192), new AxisAlignedBB(384, 67, 96, 480, 100, 192), new AxisAlignedBB(0, 67, 288, 96, 100, 384), new AxisAlignedBB(384, 67, 288, 480, 100, 384), new AxisAlignedBB(96, 67, 384, 192, 100, 480), new AxisAlignedBB(288, 67, 384, 384, 100, 480), new AxisAlignedBB(0, 67, 0, 96, 100, 96), new AxisAlignedBB(384, 67, 0, 480, 100, 96), new AxisAlignedBB(0, 67, 384, 96, 100, 480), new AxisAlignedBB(384, 67, 384, 480, 100, 480)};
    private static final Map<Integer, Integer> ACTIVE_PESTS = new ConcurrentHashMap<>();
    private static final List<String> ACTIVE_VISITORS = new CopyOnWriteArrayList<>();
    private static final List<String> LAST_GARDEN_VISITORS = new CopyOnWriteArrayList<>();
    private static final Map<String, LinkedHashMap<String, Integer>> VISITOR_NEEDS = new ConcurrentHashMap<>();
    private static final Map<String, LinkedHashMap<String, Integer>> VISITOR_REWARDS = new ConcurrentHashMap<>();
    private static final Map<String, List<VisitorBonus>> VISITOR_BONUSES = new ConcurrentHashMap<>();
    private static final long COMPLETION_TTL_MS = 10_000L;
    private static final Map<String, Long> COMPLETED_VISITORS = new ConcurrentHashMap<>();
    private static volatile long lastFarmingBreakMs = 0L;
    private static volatile long lastFarmingToolHoldMs = 0L;
    private static volatile long lastVacuumHeldMs = 0L;
    private static volatile String cachedHeldItemId = "";
    @Getter
    private static int currentPlotId = -1;
    @Setter
    @Getter
    private static String gardenAlive = "";
    @Setter
    @Getter
    private static String gardenSpray = "";
    @Setter
    @Getter
    private static String gardenRepellent = "";
    @Setter
    @Getter
    private static String gardenBonus = "";
    @Setter
    @Getter
    private static String gardenCooldown = "";
    @Setter
    @Getter
    private static String gardenBonusPestChance = "";
    private static volatile boolean visitorsSectionSeen = false;
    @Getter
    private static volatile String searchedItemId = "";
    @Getter
    private static volatile String searchedItemName = "";
    private static volatile long searchedAtMs = 0L;
    @Getter
    private static volatile String lastChestSignature = "";
    private static volatile int pendingSignAmount = 0;
    private FarmingApi() {
    }

    private static int computePlayerPlotId() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return -1;
        double px = mc.thePlayer.posX;
        double pz = mc.thePlayer.posZ;
        for (int i = 0; i < GARDEN_PLOTS.length; i++) {
            AxisAlignedBB box = GARDEN_PLOTS[i];
            if (px >= box.minX && px <= box.maxX && pz >= box.minZ && pz <= box.maxZ) {
                return i + 1;
            }
        }
        return -1;
    }

    public static boolean isCurrentlyFarming() {
        return isHoldingFarmingTool() && System.currentTimeMillis() - lastFarmingBreakMs < FARMING_WINDOW_MS;
    }

    public static boolean isHoldingFarmingTool() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc != null && mc.thePlayer != null && FarmingToolIds.isFarmingTool(cachedHeldItemId);
    }

    /** True when a farming tool was held within the last {@link #TOOL_HOLD_WINDOW_MS} */
    public static boolean heldFarmingToolRecently() {
        return System.currentTimeMillis() - lastFarmingToolHoldMs < TOOL_HOLD_WINDOW_MS;
    }

    public static boolean isInFarmingLocation() {
        SkyblockData.Location location = SkyblockData.getCurrentLocation();
        return location == SkyblockData.Location.BARN || location == SkyblockData.Location.PRIVATE_ISLAND || location == SkyblockData.Location.GARDEN;
    }

    public static boolean isHoldingVacuum() {
        Minecraft mc = Minecraft.getMinecraft();
        boolean holdingNow = mc != null && mc.thePlayer != null && cachedHeldItemId.contains("VACUUM");
        if (holdingNow) lastVacuumHeldMs = System.currentTimeMillis();
        return holdingNow || System.currentTimeMillis() - lastVacuumHeldMs < VACUUM_WINDOW_MS;
    }

    public static Map<Integer, Integer> getActivePests() {
        return ACTIVE_PESTS;
    }

    public static void setActivePests(Map<Integer, Integer> plots) {
        ACTIVE_PESTS.clear();
        ACTIVE_PESTS.putAll(plots);
    }

    public static void clearGardenPestData() {
        gardenAlive = "";
        gardenSpray = "";
        gardenRepellent = "";
        gardenBonus = "";
        gardenCooldown = "";
        gardenBonusPestChance = "";
        ACTIVE_PESTS.clear();
        currentPlotId = -1;
    }

    public static boolean isPlayerInPlot(Integer plotId) {
        return plotId != null && plotId == currentPlotId;
    }

    private static final Pattern COOLDOWN_TIME = Pattern.compile("(?:(\\d+)\\s*h)?\\s*(?:(\\d+)\\s*m)?\\s*(?:(\\d+)\\s*s)");

    public static long getGardenCooldownMs() {
        String s = ColorUtils.stripColor(gardenCooldown).trim();
        if (s.isEmpty()) return -1L;
        if (s.matches("\\d+:\\d{2}")) {
            String[] parts = s.split(":");
            return (Long.parseLong(parts[0]) * 60L + Long.parseLong(parts[1])) * 1000L;
        }
        Matcher m = COOLDOWN_TIME.matcher(s);
        if (!m.find()) return -1L;
        long ms = 0L;
        boolean any = false;
        if (m.group(1) != null) { ms += Long.parseLong(m.group(1)) * 3_600_000L; any = true; }
        if (m.group(2) != null) { ms += Long.parseLong(m.group(2)) * 60_000L; any = true; }
        if (m.group(3) != null) { ms += Long.parseLong(m.group(3)) * 1000L; any = true; }
        return any ? ms : -1L;
    }

    /** The plot the warp key targets, selected by the configured {@code PestFinderConfig.warpTarget} strategy */
    public static Integer getTargetInfestedPlot() {
        return getTargetInfestedPlot(configuredWarpMode());
    }

    public static Integer getTargetInfestedPlot(int mode) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null || ACTIVE_PESTS.isEmpty()) return null;
        Integer best = null;
        for (Integer id : ACTIVE_PESTS.keySet()) {
            if (!isValidPlot(id)) continue;
            if (best == null || beats(mode, mc, id, best)) best = id;
        }
        return best;
    }

    private static int configuredWarpMode() {
        if (ATHRConfig.feature == null || ATHRConfig.feature.farming.pests == null || ATHRConfig.feature.farming.pests.pestFinder == null) {
            return WARP_CLOSEST;
        }
        return ATHRConfig.feature.farming.pests.pestFinder.warpTarget;
    }

    private static boolean isValidPlot(Integer id) {
        return id != null && id >= 1 && id <= GARDEN_PLOTS.length;
    }

    /** True when plot {@code candidate} outranks the current {@code best} under the given warp mode */
    private static boolean beats(int mode, Minecraft mc, Integer candidate, Integer best) {
        if (mode == WARP_MOST_PESTS) {
            int candidateCount = pestCountAt(candidate);
            int bestCount = pestCountAt(best);
            if (candidateCount != bestCount) return candidateCount > bestCount;
        }
        return plotDistanceSq(mc, candidate) < plotDistanceSq(mc, best);
    }

    private static int pestCountAt(Integer plotId) {
        Integer count = ACTIVE_PESTS.get(plotId);
        return count == null ? 0 : count;
    }

    private static double plotDistanceSq(Minecraft mc, Integer plotId) {
        if (mc.thePlayer == null) return Double.MAX_VALUE;
        AxisAlignedBB box = GARDEN_PLOTS[plotId - 1];
        double dx = mc.thePlayer.posX - (box.minX + box.maxX) / 2.0;
        double dz = mc.thePlayer.posZ - (box.minZ + box.maxZ) / 2.0;
        return dx * dx + dz * dz;
    }

    public static void warpToTargetPlot() {
        Integer plot = getTargetInfestedPlot();
        if (plot != null) {
            warpToPlot(plot);
            return;
        }
        if (ATHRConfig.feature != null && ATHRConfig.feature.farming.pests != null
                && ATHRConfig.feature.farming.pests.pestFinder != null
                && ATHRConfig.feature.farming.pests.pestFinder.warpToGardenWhenNoPests) {
            warpToPlot(0);
        }
    }

    /** /tptoplot 19 and /tptoplot 20 are swapped in-game, so correct for it here before sending. */
    private static int correctPlotWarpSwap(int plot) {
        if (plot == 19) return 20;
        if (plot == 20) return 19;
        return plot;
    }

    public static void warpToPlot(Integer plot) {
        if (plot == null) return;
        ChatUtils.sendChatCommand("/tptoplot " + correctPlotWarpSwap(plot));
    }

    public static boolean isPlotUnlocked(int plotId) {
        return GardenPlotData.getInstance().isUnlocked(plotId);
    }

    public static java.util.List<Integer> getSortedInfestedPlotIds() {
        java.util.List<Integer> ids = new java.util.ArrayList<>(ACTIVE_PESTS.keySet());
        Collections.sort(ids);
        return ids;
    }

    public static List<String> getActiveVisitors() {
        return ACTIVE_VISITORS;
    }

    public static List<String> getLastGardenVisitorsSnapshot() {
        return new ArrayList<>(LAST_GARDEN_VISITORS);
    }
    public static void setActiveVisitors(List<String> visitors, boolean sectionSeen) {
        visitorsSectionSeen = sectionSeen;
        ACTIVE_VISITORS.clear();
        ACTIVE_VISITORS.addAll(visitors);
        if (sectionSeen) {
            LAST_GARDEN_VISITORS.clear();
            LAST_GARDEN_VISITORS.addAll(visitors);
            pruneVisitors();
        }
    }

    public static void clearActiveVisitors() {
        ACTIVE_VISITORS.clear();
        visitorsSectionSeen = false;
    }

    public static Map<String, LinkedHashMap<String, Integer>> getVisitorNeeds() {
        return VISITOR_NEEDS;
    }

    public static Map<String, LinkedHashMap<String, Integer>> getVisitorRewards() {
        return VISITOR_REWARDS;
    }

    public static void recordVisitorNeeds(String visitor, Map<String, Integer> needs) {
        if (visitor == null || visitor.isEmpty() || needs.isEmpty()) return;
        VISITOR_NEEDS.put(visitor, new LinkedHashMap<>(needs));
    }

    public static void recordVisitorRewards(String visitor, Map<String, Integer> rewards) {
        if (visitor == null || visitor.isEmpty()) return;
        VISITOR_REWARDS.put(visitor, new LinkedHashMap<>(rewards));
    }

    public static void recordVisitorBonuses(String visitor, List<VisitorBonus> bonuses) {
        if (visitor == null || visitor.isEmpty()) return;
        VISITOR_BONUSES.put(visitor, new ArrayList<>(bonuses));
    }

    public static double bonusTotal(VisitorBonus.Type type) {
        double total = 0;
        for (Map.Entry<String, List<VisitorBonus>> entry : VISITOR_BONUSES.entrySet()) {
            int multiplier = effectiveVisitorCount(entry.getKey());
            if (multiplier <= 0) continue;
            for (VisitorBonus bonus : entry.getValue()) {
                if (bonus.type == type) total += bonus.amount * multiplier;
            }
        }
        return total;
    }

    public static List<VisitorBonus> getVisitorBonuses(String visitor) {
        List<VisitorBonus> bonuses = VISITOR_BONUSES.get(visitor);
        return bonuses == null ? Collections.emptyList() : bonuses;
    }

    /**
     * Personal overall farming rate (crops broken per ms of active farming time),
     * derived from FarmingTracker's persisted session data. 0 when there is no
     * meaningful data yet (less than 30s of tracked active farming)
     */
    public static double getCropsPerMs() {
        Map<String, Long> counts = FarmingTrackerData.getInstance().getCounts();
        long activeTimeMs = FarmingTrackerData.getInstance().getActiveTimeMs();
        if (activeTimeMs < 30_000L || counts.isEmpty()) return 0;
        long total = 0;
        for (long count : counts.values()) total += count;
        if (total <= 0) return 0;
        return total / (double) activeTimeMs;
    }

    /**
     * Estimated farming time for a raw-crop amount at the player's Farming Tracker rate
     */
    public static long getTimeToFarmMs(long rawEquivalent) {
        if (rawEquivalent <= 0) return 0;
        double cropsPerMs = getCropsPerMs();
        if (cropsPerMs <= 0) return -1;
        return (long) (rawEquivalent / cropsPerMs);
    }

    public static void pruneVisitors() {
        if (!visitorsSectionSeen) return;
        if (SkyblockData.getCurrentLocation() != SkyblockData.Location.GARDEN) return;
        VISITOR_NEEDS.keySet().retainAll(ACTIVE_VISITORS);
        VISITOR_REWARDS.keySet().retainAll(ACTIVE_VISITORS);
        VISITOR_BONUSES.keySet().retainAll(ACTIVE_VISITORS);
    }

    private static List<String> visitorsForCounts() {
        return ACTIVE_VISITORS.isEmpty() ? LAST_GARDEN_VISITORS : ACTIVE_VISITORS;
    }

    public static int effectiveVisitorCount(String visitor) {
        if (visitor == null || visitor.isEmpty()) return 0;
        int frequency = Collections.frequency(visitorsForCounts(), visitor);
        if (frequency == 0) return 0;
        Long completedAt = COMPLETED_VISITORS.get(visitor);
        if (completedAt != null && System.currentTimeMillis() - completedAt < COMPLETION_TTL_MS) {
            frequency--;
        }
        return Math.max(0, frequency);
    }

    public static boolean hasVisitorData() {
        for (String visitor : VISITOR_NEEDS.keySet()) {
            if (effectiveVisitorCount(visitor) > 0) return true;
        }
        return false;
    }

    public static void markVisitorCompleted(String visitor) {
        if (visitor == null || visitor.isEmpty()) return;
        COMPLETED_VISITORS.put(visitor, System.currentTimeMillis());
    }

    public static void clearVisitorData() {
        VISITOR_NEEDS.clear();
        VISITOR_REWARDS.clear();
        VISITOR_BONUSES.clear();
        COMPLETED_VISITORS.clear();
        LAST_GARDEN_VISITORS.clear();
        searchedItemId = "";
        searchedItemName = "";
        lastChestSignature = "";
        pendingSignAmount = 0;
    }

    public static void setSearchedItem(String id, String name) {
        searchedItemId = id == null ? "" : id;
        searchedItemName = name == null ? "" : name;
        searchedAtMs = System.currentTimeMillis();
    }

    public static boolean isSearchFresh(long maxAgeMs) {
        return !searchedItemName.isEmpty() && System.currentTimeMillis() - searchedAtMs < maxAgeMs;
    }

    public static void setLastChestSignature(String signature) {
        lastChestSignature = signature == null ? "" : signature;
    }

    public static void setPendingSign(int amount) {
        pendingSignAmount = amount;
    }

    public static int consumePendingSign() {
        int amount = pendingSignAmount;
        pendingSignAmount = 0;
        return amount;
    }

    @SubscribeEvent
    public void onBlockBreak(BlockBreakEvent event) {
        if (isHoldingFarmingTool()) lastFarmingBreakMs = System.currentTimeMillis();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;
        cachedHeldItemId = ItemUtils.getInternalName(mc.thePlayer.getHeldItem());
        if (cachedHeldItemId.contains("VACUUM")) {
            lastVacuumHeldMs = System.currentTimeMillis();
        }
        if (FarmingToolIds.isFarmingTool(cachedHeldItemId)) {
            lastFarmingToolHoldMs = System.currentTimeMillis();
        }
        if (mc.thePlayer.ticksExisted % 5 == 0) {
            currentPlotId = SkyblockData.getCurrentLocation() == SkyblockData.Location.GARDEN ? computePlayerPlotId() : -1;
        }
    }
}
