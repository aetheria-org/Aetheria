package io.hamlook.aetheria.features.farming.pests.overlay;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.farming.PestFinderConfig;
import io.hamlook.aetheria.events.ASMTickEvent;
import io.hamlook.aetheria.features.farming.FarmingApi;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.KeybindHelper;
import io.hamlook.aetheria.utils.Position;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.data.SkyblockData;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.overlay.Overlay;
import lombok.Getter;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

@RegisterEvents
public class PestFinderOverlay extends Overlay {

    @Getter
    private static PestFinderOverlay instance;

    public PestFinderOverlay() {
        super(120, 90);
        instance = this;
    }

    private static PestFinderConfig config() {
        if (ATHRConfig.feature == null || ATHRConfig.feature.farming.pests == null) {
            return null;
        }
        return ATHRConfig.feature.farming.pests.pestFinder;
    }

    private static boolean warpHintFailureLogged = false;

    private static Integer targetPlot(boolean preview) {
        try {
            return preview ? 4 : FarmingApi.getTargetInfestedPlot();
        } catch (Throwable t) {
            // The empty-map path is provably null-safe, yet a bare NPE has been
            // observed attributed to this exact line with no deeper frame. Catch
            // at the source and dump full state once (debug-gated) so the next
            // occurrence identifies itself instead of hiding behind the caller.
            logWarpHintFailure(t);
            return null;
        }
    }

    private static void logWarpHintFailure(Throwable t) {
        if (!debugEnabled() || warpHintFailureLogged) return;
        warpHintFailureLogged = true;
        Aetheria.logger.log(Level.WARNING, "[PestFinder] warpHint failed", t);
        StringBuilder frames = new StringBuilder();
        for (StackTraceElement frame : t.getStackTrace()) {
            if (frames.length() > 0) frames.append(" <- ");
            frames.append(frame);
            if (frames.length() > 600) break;
        }
        ChatUtils.sendMessage("§c[ASM] §7Pest Finder warp hint failed: §f" + t
                + " §7at §f" + frames
                + " §7[pests=" + FarmingApi.getActivePests()
                + ", preview=" + "see-log" + "]");
    }

    private static boolean debugEnabled() {
        return ATHRConfig.feature != null && ATHRConfig.feature.debug.enableDebug;
    }

    private static String warpHint(boolean preview) {
        try {
            PestFinderConfig cfg = config();
            if (cfg == null) return null;
            int key = cfg.warpKey;
            if (key == Keyboard.KEY_NONE) return "§7Warp key: §8Not set";
            Integer plot = targetPlot(preview);
            if (plot == null) {
                if (cfg.warpToGardenWhenNoPests) return "§7Press §e" + KeybindHelper.getKeyName(key) + " §7to warp to §bGarden Spawn";
                return null;
            }
            if (!preview && cfg.hideWarpHintInPlot && FarmingApi.isPlayerInPlot(plot)) return null;
            return "§7Press §e" + KeybindHelper.getKeyName(key) + " §7to warp to §bPlot " + plot;
        } catch (Exception e) {
            logWarpHintFailure(e);
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
        PestFinderConfig cfg = config();
        List<Integer> ordinals = cfg != null ? cfg.pestFinderLines : PestFinderConfig.DEFAULT_LINES;
        if (!preview && cfg == null) return new ArrayList<>();
        List<String> lines = new ArrayList<>();
        lines.add("§6§lPests");
        for (int ordinal : ordinals) {
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
        PestFinderConfig cfg = config();
        return cfg != null ? cfg.pestFinderPos : new Position(-368, 52, false, false);
    }

    @Override
    public float getScale() {
        PestFinderConfig cfg = config();
        return cfg != null ? cfg.scale : 1f;
    }

    @Override
    public int getBgColor() {
        PestFinderConfig cfg = config();
        return cfg != null ? cfg.bgColor : 0x80000000;
    }

    @Override
    public int getCornerRadius() {
        PestFinderConfig cfg = config();
        return cfg != null ? cfg.cornerRadius : 4;
    }

    @Override
    protected boolean isEnabled() {
        PestFinderConfig cfg = config();
        if (cfg == null || !cfg.enabled || !SkyblockData.isOnSkyblock() || SkyblockData.getCurrentLocation() != SkyblockData.Location.GARDEN)
            return false;
        if (cfg.showOnlyWhileHoldingVacuum && !FarmingApi.isHoldingVacuum()) return false;
        if (cfg.hideWhileFarming && FarmingApi.isCurrentlyFarming()) return false;
        return !cfg.hideOnFarmingTool || !FarmingApi.isHoldingFarmingTool();
    }

    @Override
    protected boolean hideOnChat() {
        PestFinderConfig cfg = config();
        return cfg == null || cfg.hideOnChat;
    }

    @Override
    protected boolean hideOnTab() {
        PestFinderConfig cfg = config();
        return cfg == null || cfg.hideOnTab;
    }

    @Override
    protected boolean hideOnDebug() {
        PestFinderConfig cfg = config();
        return cfg == null || cfg.hideOnDebug;
    }

    @HandleEvent
    public void onClientTick(ASMTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        PestFinderConfig config = config();
        int warpKey = config != null ? config.warpKey : 0;
        boolean active = config != null && config.enabled && warpKey != Keyboard.KEY_NONE
                && MinecraftCompat.getLocalPlayer() != null && MinecraftCompat.getCurrentScreen() == null
                && SkyblockData.getCurrentLocation() == SkyblockData.Location.GARDEN;
        if (!active || !KeybindHelper.isKeyTapped(warpKey)) {
            KeybindHelper.resetKeyTap(warpKey);
            return;
        }
        if (config.hideWarpHintInPlot) {
            Integer nearest = FarmingApi.getTargetInfestedPlot();
            if (FarmingApi.isPlayerInPlot(nearest)) return;
        }
        FarmingApi.warpToTargetPlot();
    }
}