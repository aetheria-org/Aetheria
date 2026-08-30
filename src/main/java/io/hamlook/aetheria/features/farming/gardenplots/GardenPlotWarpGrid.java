package io.hamlook.aetheria.features.farming.gardenplots;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.events.GuiContainerRenderBeforeTooltipEvent;
import io.hamlook.aetheria.features.farming.FarmingApi;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.KeybindHelper;
import io.hamlook.aetheria.utils.Position;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.data.SkyblockData;
import io.hamlook.aetheria.utils.render.ItemRenderUtils;
import io.hamlook.aetheria.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Map;

@RegisterEvents
public class GardenPlotWarpGrid {

    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final GardenPlotWarpGrid INSTANCE = new GardenPlotWarpGrid();

    private static final int GRID_DIM = 5;
    private static final int CELL_SIZE = 20;
    private static final int CELL_GAP = 2;
    private static final int GRID_SIZE = GRID_DIM * CELL_SIZE + (GRID_DIM - 1) * CELL_GAP;
    private static final int HOME_GAP = 4;
    private static final int HOME_HEIGHT = 20;
    private static final int BORDER_THICKNESS = 2;

    private static final int[][] LAYOUT = {
            {21, 13, 9, 14, 22},
            {15, 5, 1, 6, 16},
            {10, 2, 0, 4, 11},
            {17, 7, 3, 8, 18},
            {23, 19, 12, 20, 24},
    };

    private static int gridX, gridY, homeY;

    public static GardenPlotWarpGrid getInstance() {
        return INSTANCE;
    }

    private static boolean isEnabled() {
        return ATHRConfig.feature != null && ATHRConfig.feature.farming.gardenPlotWarpGrid.enabled
                && SkyblockData.getCurrentLocation() == SkyblockData.Location.GARDEN;
    }

    private static boolean isSupportedGui(Object gui) {
        return gui instanceof GuiInventory;
    }

    private static int[] calculatePosition(ScaledResolution sr) {
        if (ATHRConfig.feature == null) return new int[]{0, 0};
        Position pos = ATHRConfig.feature.farming.gardenPlotWarpGrid.pos;
        int w = GRID_SIZE;
        int h = GRID_SIZE + HOME_GAP + HOME_HEIGHT;
        int x = pos.getAbsX(sr, w);
        int y = pos.getAbsY(sr, h);
        if (pos.isCenterX()) x -= w / 2;
        if (pos.isCenterY()) y -= h / 2;
        return new int[]{x, y};
    }

    public int getOverlayWidth() {
        return GRID_SIZE;
    }

    public int getOverlayHeight() {
        return GRID_SIZE + HOME_GAP + HOME_HEIGHT;
    }

    public void render(boolean preview) {
        ScaledResolution sr = new ScaledResolution(MC);
        int[] pos = calculatePosition(sr);
        draw(pos[0], pos[1]);
    }

    private static boolean showLockedPlots() {
        return ATHRConfig.feature == null || ATHRConfig.feature.farming.gardenPlotWarpGrid.showLockedPlots;
    }

    private static boolean isPlotHidden(int plot) {
        return plot != 0 && !showLockedPlots() && !FarmingApi.isPlotUnlocked(plot);
    }

    private static void draw(int x, int y) {
        gridX = x;
        gridY = y;
        homeY = y + GRID_SIZE + HOME_GAP;

        Map<Integer, Integer> activePests = FarmingApi.getActivePests();
        boolean visitorAtSpawn = !FarmingApi.getActiveVisitors().isEmpty();

        for (int row = 0; row < GRID_DIM; row++) {
            for (int col = 0; col < GRID_DIM; col++) {
                int plot = LAYOUT[row][col];
                int cx = x + col * (CELL_SIZE + CELL_GAP);
                int cy = y + row * (CELL_SIZE + CELL_GAP);
                drawCell(cx, cy, plot, activePests.containsKey(plot), visitorAtSpawn);
            }
        }

        drawHomeButton(x, homeY, GRID_SIZE);
    }

    private static void drawCell(int x, int y, int plot, boolean hasPest, boolean visitorAtSpawn) {
        boolean hidden = isPlotHidden(plot);

        RenderUtils.drawButton(x, y, CELL_SIZE, CELL_SIZE, null, () -> {
            if (plot != 0 && FarmingApi.isPlayerInPlot(plot)) {
                Gui.drawRect(x, y, x + CELL_SIZE, y + CELL_SIZE, 0x5500FF00);
            }

            if (plot == 0) {
                GlStateManager.color(1f, 1f, 1f, 1f);
                ItemRenderUtils.renderItemIcon(MC, new ItemStack(Blocks.hay_block), x + 2, y + 2, CELL_SIZE - 4);
            } else {
                String text = String.valueOf(plot);
                int tw = MC.fontRendererObj.getStringWidth(text);
                int color = hidden ? 0x777777 : 0xFFFFFF;
                MC.fontRendererObj.drawStringWithShadow(text, x + (CELL_SIZE - tw) / 2f, y + (CELL_SIZE - 8) / 2f, color);
            }
        });

        if (hidden) {
            Gui.drawRect(x, y, x + CELL_SIZE, y + CELL_SIZE, 0x99000000);
        }

        if (!hidden && plot != 0 && hasPest) {
            drawBorder(x, y, CELL_SIZE, BORDER_THICKNESS, flashingColor(0xFF0000));
        }

        if (plot == 0 && visitorAtSpawn) {
            drawBorder(x, y, CELL_SIZE, BORDER_THICKNESS, flashingColor(0xFFD700));
        }
    }

    private static void drawHomeButton(int x, int y, int w) {
        RenderUtils.drawButton(x, y, w, HOME_HEIGHT, null, () -> {
            String text = "Home";
            int tw = MC.fontRendererObj.getStringWidth(text);
            MC.fontRendererObj.drawStringWithShadow(text, x + (w - tw) / 2f, y + (HOME_HEIGHT - 8) / 2f, 0xFFFFFF);
        });
    }

    private static void drawBorder(int x, int y, int size, int thickness, int color) {
        Gui.drawRect(x, y, x + size, y + thickness, color);
        Gui.drawRect(x, y + size - thickness, x + size, y + size, color);
        Gui.drawRect(x, y, x + thickness, y + size, color);
        Gui.drawRect(x + size - thickness, y, x + size, y + size, color);
    }

    private static int flashingColor(int rgb) {
        float pulse = 0.4f + 0.6f * (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 200.0));
        int alpha = ((int) (pulse * 255)) & 0xFF;
        return (alpha << 24) | rgb;
    }

    private static boolean isInside(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    @SubscribeEvent
    public void onDrawGui(GuiContainerRenderBeforeTooltipEvent event) {
        if (!isEnabled() || !isSupportedGui(event.gui)) return;
        GlStateManager.pushMatrix();
        GlStateManager.translate(-event.gui.guiLeft, -event.gui.guiTop, 50);
        ScaledResolution sr = new ScaledResolution(MC);
        int[] pos = calculatePosition(sr);
        draw(pos[0], pos[1]);
        GlStateManager.popMatrix();
    }

    @SubscribeEvent
    public void onMouseInput(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!isEnabled() || !isSupportedGui(event.gui) || !KeybindHelper.getEventButtonState() || KeybindHelper.getEventButton() != 0) return;

        int mouseX = KeybindHelper.getScaledEventX(event.gui.width);
        int mouseY = KeybindHelper.getScaledEventY(event.gui.height);

        for (int row = 0; row < GRID_DIM; row++) {
            for (int col = 0; col < GRID_DIM; col++) {
                int cx = gridX + col * (CELL_SIZE + CELL_GAP);
                int cy = gridY + row * (CELL_SIZE + CELL_GAP);
                if (isInside(mouseX, mouseY, cx, cy, CELL_SIZE, CELL_SIZE)) {
                    if (!isPlotHidden(LAYOUT[row][col])) {
                        FarmingApi.warpToPlot(LAYOUT[row][col]);
                    }
                    event.setCanceled(true);
                    return;
                }
            }
        }

        if (isInside(mouseX, mouseY, gridX, homeY, GRID_SIZE, HOME_HEIGHT)) {
            ChatUtils.sendChatCommand("/warp garden");
            event.setCanceled(true);
        }
    }
}
