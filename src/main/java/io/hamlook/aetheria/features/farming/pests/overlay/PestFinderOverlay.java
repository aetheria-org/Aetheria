package io.hamlook.aetheria.features.farming.pests.overlay;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.farming.PestFinderConfig;
import io.hamlook.aetheria.features.farming.FarmingApi;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.KeybindHelper;
import io.hamlook.aetheria.utils.Position;
import io.hamlook.aetheria.utils.data.SkyblockData;
import io.hamlook.aetheria.utils.overlay.Overlay;
import lombok.Getter;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;

@RegisterEvents
public class PestFinderOverlay extends Overlay {

    @Getter
    private static PestFinderOverlay instance;

    public PestFinderOverlay() {
        super(120, 90);
        instance = this;
    }

    private static PestFinderConfig config() {
        return ATHRConfig.feature.farming.pests.pestFinder;
    }

    private static String warpHint(boolean preview) {
        int key = config().warpKey;
        if (key == Keyboard.KEY_NONE) return "§7Warp key: §8Not set";
        Integer plot = preview ? 4 : FarmingApi.getNearestInfestedPlot();
        if (plot == null) return null;
        return "§7Press §e" + KeybindHelper.getKeyName(key) + " §7to warp to §bPlot " + plot;
    }

    private static String value(String stored) {
        return stored == null || stored.isEmpty() ? "§8-" : stored;
    }

    private static String orZero(String stored) {
        return stored == null || stored.isEmpty() ? "0" : stored;
    }

    private static String plotsText() {
        List<Integer> ids = FarmingApi.getSortedInfestedPlotIds();
        if (ids.isEmpty()) return "§8-";
        StringBuilder sb = new StringBuilder();
        for (int id : ids) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(id);
        }
        return sb.toString();
    }

    @Override
    public List<String> getLines(boolean preview) {
        List<String> lines = new ArrayList<>();
        lines.add("§6§lPests");
        for (int ordinal : config().pestFinderLines) {
            String line = entryLine(ordinal, preview);
            if (line != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    private static String entryLine(int ordinal, boolean preview) {
        switch (ordinal) {
            case 0:
                return preview ? "§7Total: §e2" : "§7Total: §e" + orZero(FarmingApi.getGardenAlive());
            case 1:
                if (preview) return "§7Plots: §b2, 4";
                return FarmingApi.getActivePests().isEmpty() ? null : "§7Plots: §b" + plotsText();
            case 2:
                return "§7Spray: " + (preview ? "§7None" : value(FarmingApi.getGardenSpray()));
            case 3:
                return "§7Repellent: " + (preview ? "§7None" : value(FarmingApi.getGardenRepellent()));
            case 4:
                return "§7Bonus: " + (preview ? "§6INACTIVE" : value(FarmingApi.getGardenBonus()));
            case 5:
                return "§7Cooldown: " + (preview ? "§6ACTIVE" : value(FarmingApi.getGardenCooldown()));
            case 6:
                return "§7Bonus Pest Chance: " + (preview ? "§65" : value(FarmingApi.getGardenBonusPestChance()));
            case 7:
                return warpHint(preview);
            default:
                return null;
        }
    }

    @Override
    public Position getPosition() {
        return config().pestFinderPos;
    }

    @Override
    public float getScale() {
        return config().scale;
    }

    @Override
    public int getBgColor() {
        return config().bgColor;
    }

    @Override
    public int getCornerRadius() {
        return config().cornerRadius;
    }

    @Override
    protected boolean isEnabled() {
        if (!config().enabled || !SkyblockData.isOnSkyblock() || SkyblockData.getCurrentLocation() != SkyblockData.Location.GARDEN)
            return false;
        if (config().showOnlyWhileHoldingVacuum && !FarmingApi.isHoldingVacuum()) return false;
        if (config().hideWhileFarming && FarmingApi.isCurrentlyFarming()) return false;
        return !config().hideOnFarmingTool || !FarmingApi.isHoldingFarmingTool();
    }

    @Override
    protected boolean hideOnChat() {
        return config().hideOnChat;
    }

    @Override
    protected boolean hideOnTab() {
        return config().hideOnTab;
    }

    @Override
    protected boolean hideOnDebug() {
        return config().hideOnDebug;
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        PestFinderConfig config = config();
        if (config == null || !config.enabled || config.warpKey == Keyboard.KEY_NONE) return;
        if (mc.thePlayer == null || mc.currentScreen != null) return;
        if (SkyblockData.getCurrentLocation() != SkyblockData.Location.GARDEN) return;
        if (!KeybindHelper.isKeyPressed(config.warpKey)) return;
        FarmingApi.warpToNearestInfestedPlot();
    }
}