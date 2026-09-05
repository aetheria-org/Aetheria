package io.hamlook.aetheria.features.farming.gardenplots;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.farming.GardenPlotsConfig;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaStyle;
import io.hamlook.aetheria.events.GuiContainerRenderBeforeTooltipEvent;
import io.hamlook.aetheria.events.RenderItemOverlayEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.ContainerUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.data.SkyblockData;
import io.hamlook.aetheria.utils.item.ItemStackUtils;
import io.hamlook.aetheria.utils.item.ItemUtils;
import io.hamlook.aetheria.utils.render.HighlightUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RegisterEvents
public class GardenPlotNumber {

    private static final Pattern PLOT_PATTERN = Pattern.compile("Plot - (\\d+)");
    private static final String TITLE = "Configure Plots";
    private static final int CACHE_CAP = 256;

    private static final int UNLOCKED = 0;
    private static final int BUYABLE = 1;
    private static final int NO_MATERIALS = 2;
    private static final int LOCKED = 3;

    private static final PlotInfo NON_PLOT = new PlotInfo(-1, -1);

    private static final Map<ItemStack, PlotInfo> PLOT_CACHE = new IdentityHashMap<>();
    private static GuiScreen cacheScreen;

    private static final Set<Integer> frameUnlockedPlots = new HashSet<>();

    static {
        HighlightUtils.registerHighlighter((gui, slot) -> {
            if (notInGardenConfigurePlots()) return null;
            GardenPlotsConfig config = config();
            if (config == null || !config.enabled) return null;
            if (!config.highlightUnlocked && !config.highlightUnlockable) return null;

            ItemStack stack = slot.getStack();
            if (stack == null || stack.getItem() == null) return null;
            PlotInfo info = info(stack);
            if (info.type == UNLOCKED && config.highlightUnlocked) {
                return color(config.unlockedHighlightColor, slot.xDisplayPosition, slot.yDisplayPosition);
            }
            if ((info.type == BUYABLE || info.type == NO_MATERIALS) && config.highlightUnlockable) {
                return color(config.unlockableHighlightColor, slot.xDisplayPosition, slot.yDisplayPosition);
            }
            return null;
        });
    }

    @HandleEvent
    public void onItemOverlay(RenderItemOverlayEvent event) {
        if (notInGardenConfigurePlots()) return;
        GardenPlotsConfig config = config();
        if (config == null || !config.enabled) return;

        ItemStack stack = event.stack;
        if (stack == null || stack.getItem() == null) return;
        PlotInfo info = info(stack);
        if (info.number < 0) return;
        if (info.type == UNLOCKED) frameUnlockedPlots.add(info.number);
        String color = tipColor(config, info.type);
        if (color == null) return;
        ItemStackUtils.drawTip(String.valueOf(info.number), event.x, event.y, color(color, event.x, event.y));
    }

    @HandleEvent
    public void onFrameEnd(GuiContainerRenderBeforeTooltipEvent event) {
        if (notInGardenConfigurePlots()) return;
        GardenPlotsConfig config = config();
        if (config == null || !config.enabled) return;
        GardenPlotData.getInstance().updateFromChest(frameUnlockedPlots);
        frameUnlockedPlots.clear();
    }

    private static GardenPlotsConfig config() {
        return ATHRConfig.feature == null ? null : ATHRConfig.feature.farming.gardenPlots;
    }

    private static boolean notInGardenConfigurePlots() {
        if (SkyblockData.getCurrentLocation() != SkyblockData.Location.GARDEN) return true;
        String title = ContainerUtils.getContainerName();
        return title == null || !title.contains(TITLE);
    }

    private static PlotInfo info(ItemStack stack) {
        GuiScreen screen = MinecraftCompat.getCurrentScreen();
        if (screen != cacheScreen) {
            cacheScreen = screen;
            PLOT_CACHE.clear();
        }
        PlotInfo cached = PLOT_CACHE.get(stack);
        if (cached != null) return cached;
        PlotInfo computed = compute(stack);
        if (PLOT_CACHE.size() < CACHE_CAP) PLOT_CACHE.put(stack, computed);
        return computed;
    }

    private static PlotInfo compute(ItemStack stack) {
        String name = ColorUtils.stripColor(stack.getDisplayName());
        if (name.isEmpty()) return NON_PLOT;
        Matcher matcher = PLOT_PATTERN.matcher(name);
        if (!matcher.find()) return NON_PLOT;
        int number = Integer.parseInt(matcher.group(1));
        return new PlotInfo(number, classifyLore(ItemUtils.getLoreLinesWithoutColor(stack)));
    }

    private static int classifyLore(List<String> lore) {
        boolean hasModify = false;
        boolean hasPurchase = false;
        boolean hasNeedMore = false;
        boolean hasLocked = false;
        for (String line : lore) {
            if (line.contains("Left-click to modify!") || line.contains("Right-click to teleport to this plot!")) {
                hasModify = true;
            } else if (line.contains("Click to purchase")) {
                hasPurchase = true;
            } else if (line.contains("You need more")) {
                hasNeedMore = true;
            } else if (line.contains("Garden Level") || line.contains("adjacent plot")) {
                hasLocked = true;
            }
        }
        if (hasModify) return UNLOCKED;
        if (hasPurchase) return BUYABLE;
        if (hasNeedMore) return NO_MATERIALS;
        if (hasLocked) return LOCKED;
        return -1;
    }

    private static String tipColor(GardenPlotsConfig config, int type) {
        switch (type) {
            case UNLOCKED:
                return config.tipColorUnlocked;
            case BUYABLE:
                return config.tipColorBuyable;
            case NO_MATERIALS:
                return config.tipColorNoMaterials;
            case LOCKED:
                return config.tipColorLocked;
            default:
                return null;
        }
    }

    private static int color(String colorString, float x, float y) {
        if (ChromaColour.getSpeed(colorString) == 0) return ChromaColour.specialToSimpleRGB(colorString);
        GardenPlotsConfig config = config();
        if (config == null) return ChromaStyle.of(colorString).toArgb(x, y);
        return ChromaStyle.of(colorString, config.chromaMode, config.chromaSize).toArgb(x, y);
    }

    private static final class PlotInfo {
        private final int number;
        private final int type;

        PlotInfo(int number, int type) {
            this.number = number;
            this.type = type;
        }
    }
}