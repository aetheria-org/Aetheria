package io.hamlook.aetheria.features.dungeons;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.dungeons.overlays.DungeonMapOverlay;
import io.hamlook.aetheria.features.dungeons.overlays.map.DungeonMapGrid;
import io.hamlook.aetheria.features.dungeons.overlays.map.DungeonPlayerTracker;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.ContainerUtils;
import io.hamlook.aetheria.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

@RegisterEvents
public class DungeonLeapOverlay {

    public static DungeonMapOverlay overlay;
    public static DungeonPlayerTracker tracker;

    public static ContainerChest leapChest;
    public static boolean isLeapGUI = false;

    public static void start() {
        overlay = DungeonMapOverlay.getInstance();
        if (overlay != null && overlay.isEnabled() && overlay.isLiveActive()) {
            tracker = DungeonMapOverlay.playerTracker;
        }
    }

    public static boolean isLeapGUI(String title) {
        return title.equals("Leap Menu");
    }

    @SubscribeEvent
    public void onClose(GuiOpenEvent e) {
        if (e.gui == null) isLeapGUI = false;
    }

    @SubscribeEvent
    public void onGui(GuiScreenEvent.BackgroundDrawnEvent event) {
        if (isLeapGUI) return;
        if (!(event.gui instanceof GuiContainer)) return;
        GuiContainer gui = (GuiContainer) event.gui;
        if (!(gui.inventorySlots instanceof ContainerChest)) return;
        ContainerChest chest = (ContainerChest) gui.inventorySlots;
        String title = ContainerUtils.getTitle(chest);
        isLeapGUI = isLeapGUI(title);
        if (isLeapGUI) {
            leapChest = chest;
        }
    }

    @SubscribeEvent
    public void onDraw(GuiScreenEvent.DrawScreenEvent e) {
        if (isLeapGUI && e.gui != null && tracker != null) {
            e.setCanceled(true);
            drawLeapGUI(e.mouseX, e.mouseY, e.gui);
        }
    }

    private void drawLeapGUI(int mouseX, int mouseY, GuiScreen gui) {
        float scale = 2f;
        float centerX = gui.width / 2f;
        float centerY = gui.height / 2f;
        overlay.renderDungeonMap(centerX, centerY, scale, true, true);

        DungeonMapGrid grid = overlay.getCachedGrid();
        // The grid may not have parsed yet — fall back to the fixed 128x128 map box
        // so markers (which live in that pixel space) still line up before it does.
        float size = grid != null ? grid.getGridPixelWidth() : 128f;
        float sizeH = grid != null ? grid.getGridPixelHeight() : 128f;
        float left = centerX - size * scale / 2f;
        float top = centerY - sizeH * scale / 2f;
        float headPixelSize = 8f * scale;
        float halfHeadPixelSize = headPixelSize * 0.5f;

        boolean isMouseDown = Mouse.isButtonDown(0);
        Minecraft mc = Minecraft.getMinecraft();
        String selfName = mc.thePlayer != null ? mc.thePlayer.getName() : null;

        for (String player : tracker.playerNames) {
            float[] pos = tracker.getPosition(player);
            if (pos == null || pos.length < 3) continue;
            float posX = pos[0];
            float posY = pos[1];
            float yaw = pos[2];

            boolean isSelf = player.equalsIgnoreCase(selfName);
            if (isSelf && grid != null && ATHRConfig.feature.dungeons.dungeonMapConfig.players.accurateSelfPosition) {
                EntityPlayer entity = tracker.getEntity(player);
                if (entity != null && !entity.isDead) {
                    posX = grid.worldToPixelX(entity.posX);
                    posY = grid.worldToPixelZ(entity.posZ);
                    yaw = entity.rotationYaw;
                }
            }

            posX = left + posX * scale;
            posY = top + posY * scale;

            boolean clicked = isMouseDown && checkIfClickedOptimized(posX, posY, yaw, halfHeadPixelSize, mouseX, mouseY);
            if (clicked) {
                leapToPlayer(player);
            }
            ResourceLocation skin = tracker.resolveSkin(player, mc);

            RenderUtils.renderPlayerHead(posX - headPixelSize / 2f, posY - headPixelSize / 2f, -1, scale, skin, yaw);
        }
    }

    private void leapToPlayer(String player) {
        if (!isLeapGUI || leapChest == null || player == null) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        IInventory inv = ContainerUtils.getLowerInventory(leapChest);
        if (inv == null) return;

        for (int slot = 0; slot < inv.getSizeInventory(); slot++) {
            ItemStack stack = inv.getStackInSlot(slot);
            if (stack == null) continue;
            String name = ColorUtils.stripColor(stack.getDisplayName()).trim();
            if (name.equalsIgnoreCase(player) || name.endsWith(" " + player)) {
                mc.playerController.windowClick(leapChest.windowId, slot, 0, 0, mc.thePlayer);
                return;
            }
        }
    }

    private boolean checkIfClickedOptimized(float posX, float posY, float yaw, float halfSize, int mouseX, int mouseY) {
        if (mouseX < posX - halfSize || mouseX > posX + halfSize || mouseY < posY - halfSize || mouseY > posY + halfSize) {
            return false;
        }

        float dx = mouseX - posX;
        float dy = mouseY - posY;

        float rad = (float) Math.toRadians(-yaw);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);

        float localX = dx * cos - dy * sin;
        float localY = dx * sin + dy * cos;

        return localX >= -halfSize && localX <= halfSize && localY >= -halfSize && localY <= halfSize;
    }

}