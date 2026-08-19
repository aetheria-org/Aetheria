package io.hamlook.aetheria.features.farming;

import io.hamlook.aetheria.events.BlockBreakEvent;
import io.hamlook.aetheria.features.farming.sensitivityreducer.FarmingToolIds;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.data.SkyblockData;
import io.hamlook.aetheria.utils.item.ItemUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RegisterEvents
public final class FarmingApi {

    private static final long FARMING_WINDOW_MS = 5_000L;
    private static volatile long lastFarmingBreakMs = 0L;

    private static final long VACUUM_WINDOW_MS = 10_000L;
    private static volatile long lastVacuumHeldMs = 0L;
    private static volatile String cachedHeldItemId = "";

    // Garden plots 1-24
    private static final AxisAlignedBB[] GARDEN_PLOTS = {
            new AxisAlignedBB(192, 67, 96, 288, 100, 192),
            new AxisAlignedBB(96, 67, 192, 192, 100, 288),
            new AxisAlignedBB(192, 67, 288, 288, 100, 384),
            new AxisAlignedBB(288, 67, 192, 384, 100, 288),
            new AxisAlignedBB(96, 67, 96, 192, 100, 192),
            new AxisAlignedBB(288, 67, 96, 384, 100, 192),
            new AxisAlignedBB(96, 67, 288, 192, 100, 384),
            new AxisAlignedBB(288, 67, 288, 384, 100, 384),
            new AxisAlignedBB(192, 67, 0, 288, 100, 96),
            new AxisAlignedBB(0, 67, 192, 96, 100, 288),
            new AxisAlignedBB(384, 67, 192, 480, 100, 288),
            new AxisAlignedBB(192, 67, 384, 288, 100, 480),
            new AxisAlignedBB(96, 67, 0, 192, 100, 96),
            new AxisAlignedBB(288, 67, 0, 384, 100, 96),
            new AxisAlignedBB(0, 67, 96, 96, 100, 192),
            new AxisAlignedBB(384, 67, 96, 480, 100, 192),
            new AxisAlignedBB(0, 67, 288, 96, 100, 384),
            new AxisAlignedBB(384, 67, 288, 480, 100, 384),
            new AxisAlignedBB(96, 67, 384, 192, 100, 480),
            new AxisAlignedBB(288, 67, 384, 384, 100, 480),
            new AxisAlignedBB(0, 67, 0, 96, 100, 96),
            new AxisAlignedBB(384, 67, 0, 480, 100, 96),
            new AxisAlignedBB(0, 67, 384, 96, 100, 480),
            new AxisAlignedBB(384, 67, 384, 480, 100, 480)
    };

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
    private static final Map<Integer, Integer> ACTIVE_PESTS = new HashMap<>();

    private FarmingApi() {
    }

    @SubscribeEvent
    public void onBlockBreak(BlockBreakEvent event) {
        if (isHoldingFarmingTool()) lastFarmingBreakMs = System.currentTimeMillis();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;
        cachedHeldItemId = ItemUtils.getInternalName(mc.thePlayer.getHeldItem());
        if (cachedHeldItemId.contains("VACUUM")) {
            lastVacuumHeldMs = System.currentTimeMillis();
        }
    }

    public static boolean isCurrentlyFarming() {
        return isHoldingFarmingTool() && System.currentTimeMillis() - lastFarmingBreakMs < FARMING_WINDOW_MS;
    }

    public static boolean isHoldingFarmingTool() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc.thePlayer != null && FarmingToolIds.isFarmingTool(cachedHeldItemId);
    }

    public static boolean isInFarmingLocation() {
        SkyblockData.Location location = SkyblockData.getCurrentLocation();
        return location == SkyblockData.Location.BARN
                || location == SkyblockData.Location.PRIVATE_ISLAND
                || location == SkyblockData.Location.GARDEN;
    }

    public static boolean isHoldingVacuum() {
        Minecraft mc = Minecraft.getMinecraft();
        boolean holdingNow = mc.thePlayer != null && cachedHeldItemId.contains("VACUUM");
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
    }

    public static Integer getNearestInfestedPlot() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || ACTIVE_PESTS.isEmpty()) return null;
        Integer best = null;
        double bestDist = Double.MAX_VALUE;
        for (Integer id : ACTIVE_PESTS.keySet()) {
            if (id == null || id < 1 || id > GARDEN_PLOTS.length) continue;
            AxisAlignedBB box = GARDEN_PLOTS[id - 1];
            double dx = mc.thePlayer.posX - (box.minX + box.maxX) / 2.0;
            double dz = mc.thePlayer.posZ - (box.minZ + box.maxZ) / 2.0;
            double dist = dx * dx + dz * dz;
            if (dist < bestDist) {
                bestDist = dist;
                best = id;
            }
        }
        return best;
    }

    public static void warpToNearestInfestedPlot() {
        Integer plot = getNearestInfestedPlot();
        if (plot == null) return;
        Minecraft.getMinecraft().thePlayer.sendChatMessage("/tptoplot " + plot);
    }

    public static java.util.List<Integer> getSortedInfestedPlotIds() {
        java.util.List<Integer> ids = new java.util.ArrayList<>(ACTIVE_PESTS.keySet());
        Collections.sort(ids);
        return ids;
    }
}