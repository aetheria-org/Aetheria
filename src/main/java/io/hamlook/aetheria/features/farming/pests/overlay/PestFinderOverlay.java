package io.hamlook.aetheria.features.farming.pests.overlay;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.core.features.farming.PestFinderConfig;
import io.hamlook.aetheria.features.farming.FarmingApi;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.KeybindHelper;
import io.hamlook.aetheria.utils.Position;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.data.SkyblockData;
import io.hamlook.aetheria.utils.overlay.Overlay;
import lombok.Getter;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    private static boolean warpHintFailureLogged = false;

    private static Integer targetPlot(boolean preview) {
        return preview ? 4 : FarmingApi.getTargetInfestedPlot();
    }

    private static String warpHint(boolean preview) {
        try {
            PestFinderConfig cfg = config();
            if (cfg == null) return null;
            int key = cfg.warpKey;
            if (key == Keyboard.KEY_NONE) return "§7Warp key: §8Not set";
            Integer plot = targetPlot(preview);
            if (plot == null) return null;
            if (!preview && cfg.hideWarpHintInPlot && FarmingApi.isPlayerInPlot(plot)) return null;
            return "§7Press §e" + KeybindHelper.getKeyName(key) + " §7to warp to §bPlot " + plot;
        } catch (Exception e) {
            if (!warpHintFailureLogged) {
                warpHintFailureLogged = true;
                Aetheria.logger.warning("[PestFinder] warpHint failed: " + e);
                ChatUtils.sendMessage("§c[Aetheria] §7Pest Finder warp hint failed: §f" + e);
            }
            return null;
        }
    }

    private static String value(String stored) {
        return stored == null || stored.isEmpty() ? "§8-" : stored;
    }

    private static String orZero(String stored) {
        return stored == null || stored.isEmpty() ? "0" : stored;
    }

    private static String plotsText() {
        Map<Integer, Integer> pests = FarmingApi.getActivePests();
        if (pests.isEmpty()) return "§8-";
        StringBuilder sb = new StringBuilder();
        for (Integer id : FarmingApi.getSortedInfestedPlotIds()) {
            if (sb.length() > 0) sb.append("§7, ");
            sb.append("§b").append(id);
            Integer count = pests.get(id);
            if (count != null && count > 1) sb.append(" §8x§c").append(count);
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
                if (preview) return "§7Plots: §b1, §b5 §8x§c3";
                return FarmingApi.getActivePests().isEmpty() ? null : "§7Plots: " + plotsText();
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
        if (config.hideWarpHintInPlot) {
            Integer nearest = FarmingApi.getTargetInfestedPlot();
            if (FarmingApi.isPlayerInPlot(nearest)) return;
        }
        FarmingApi.warpToTargetPlot();
    }
}