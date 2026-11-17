package io.hamlook.aetheria.features.farming.sensitivityreducer;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.farming.SensitivityReducerConfig;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.data.SkyblockData;
import io.hamlook.aetheria.utils.item.ItemUtils;
import net.minecraft.client.Minecraft;

public final class SensitivityReducer {

    private SensitivityReducer() {
    }

    private static SensitivityReducerConfig config() {
        return ATHRConfig.feature.farming.sensitivityReducer;
    }

    private static boolean isInFarmingLocation() {
        SkyblockData.Location location = SkyblockData.getCurrentLocation();
        return location == SkyblockData.Location.BARN
                || location == SkyblockData.Location.PRIVATE_ISLAND
                || location == SkyblockData.Location.GARDEN;
    }

    public static boolean isHoldingFarmingTool() {
        Minecraft mc = MinecraftCompat.getMinecraft();
        if (MinecraftCompat.getLocalPlayer() == null) return false;
        return FarmingToolIds.isFarmingTool(ItemUtils.getInternalName(MinecraftCompat.getLocalPlayer().getHeldItem()));
    }

    public static boolean isActive() {
        if (ATHRConfig.feature == null || !config().enabled) return false;
        if (!SkyblockData.isOnSkyblock()) return false;
        boolean locationOk = !config().requireFarmingIsland || isInFarmingLocation();
        if (!locationOk) return false;
        return isHoldingFarmingTool();
    }

    public static float getSensitivityScale() {
        return Math.max(0.05f, Math.min(1.0f, config().sensitivityPercent / 100f));
    }
}
