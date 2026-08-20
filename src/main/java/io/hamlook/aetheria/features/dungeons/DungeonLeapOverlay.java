package io.hamlook.aetheria.features.dungeons;

import io.hamlook.aetheria.features.dungeons.overlays.DungeonMapOverlay;
import io.hamlook.aetheria.features.dungeons.overlays.map.DungeonMapGrid;
import io.hamlook.aetheria.features.dungeons.overlays.map.DungeonPlayerTracker;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.ContainerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ContainerChest;
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

    public static final int[] LEAP_SLOTS = {};
    public static void start(){
        overlay = DungeonMapOverlay.getInstance();
        if(overlay.isEnabled() && overlay.isLiveActive()) {
            tracker = DungeonMapOverlay.playerTracker;
        }

    }

    @SubscribeEvent
    public void onClose(GuiOpenEvent e){
        if(e.gui == null) isLeapGUI = false;
    }

    @SubscribeEvent
    public void onGui(GuiScreenEvent.BackgroundDrawnEvent event) {
        if(isLeapGUI) return;
        if(!(event.gui instanceof GuiContainer)) return;
        GuiContainer gui = (GuiContainer) event.gui;
        if(!(gui.inventorySlots instanceof ContainerChest)) return;
        ContainerChest chest = (ContainerChest) gui.inventorySlots;
        String title = ContainerUtils.getTitle(chest);
        isLeapGUI = isLeapGUI(title);
        if(isLeapGUI){
            leapChest = chest;
        }
    }

    @SubscribeEvent
    public void onDraw(GuiScreenEvent.DrawScreenEvent e){
        if(isLeapGUI && e.gui != null && tracker != null){
            e.setCanceled(true);
            drawLeapGUI(e.mouseX,e.mouseY,e.renderPartialTicks,e.gui);
        }
    }

    private void drawLeapGUI(int mouseX, int mouseY, float partialTicks, GuiScreen gui) {
        float scale = 2f;
        float centerX = gui.width / 2f;
        float centerY = gui.height / 2f;
        overlay.renderDungeonMap(centerX, centerY, scale, true, true);

        DungeonMapGrid grid = overlay.getCachedGrid();
        if (grid == null) return;

        // Match the same centering math DungeonMapRenderer uses when it draws the
        // grid, so marker positions line up with what's actually on screen instead
        // of assuming a fixed 128x128 box.
        float left = centerX - grid.getGridPixelWidth() * scale / 2f;
        float top = centerY - grid.getGridPixelHeight() * scale / 2f;
        float headPixelSize = 8f * scale;
        float halfHeadPixelSize = headPixelSize * 0.5f;

        boolean isMouseDown = Mouse.isButtonDown(0);
        Minecraft mc = Minecraft.getMinecraft();

        for (String player : tracker.playerNames) {
            float[] pos = tracker.getPixelPosition(player, grid);
            if (pos == null || pos.length < 3) continue;
            float posX = left + pos[0] * scale;
            float posY = top + pos[1] * scale;
            float yaw = pos[2];
            boolean clicked = isMouseDown && checkIfClickedOptimized(posX, posY, yaw, halfHeadPixelSize, mouseX, mouseY);
            if(clicked){
                leapToPlayer(player);
            }
            ResourceLocation skin = getPlayerSkin(player, mc);

            DungeonMapOverlay.renderPlayerHead(posX - headPixelSize / 2f, posY - headPixelSize / 2f, -1, scale, skin, yaw);
        }
    }

    private void leapToPlayer(String player) {
        if(!isLeapGUI || leapChest == null) return;
        for(int slot : LEAP_SLOTS){
            // TODO: Check for player slot & click if match
        }
    }

    private boolean checkIfClickedOptimized(float posX, float posY, float yaw, float halfSize, int mouseX, int mouseY) {
        if (mouseX < posX - halfSize || mouseX > posX + halfSize ||
                mouseY < posY - halfSize || mouseY > posY + halfSize) {
            return false;
        }

        float dx = mouseX - posX;
        float dy = mouseY - posY;

        float rad = (float) Math.toRadians(-yaw);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);

        float localX = dx * cos - dy * sin;
        float localY = dx * sin + dy * cos;

        return localX >= -halfSize && localX <= halfSize &&
                localY >= -halfSize && localY <= halfSize;
    }

    private ResourceLocation getPlayerSkin(String player, Minecraft mc) {
        EntityPlayer entity = tracker.getEntity(player);
        NetworkPlayerInfo info = (entity != null)
                ? mc.getNetHandler().getPlayerInfo(entity.getUniqueID())
                : null;

        if (info == null) {
            info = tracker.getNetworkPlayerInfo(player, mc);
        }

        return (info != null && info.getLocationSkin() != null)
                ? info.getLocationSkin()
                : DefaultPlayerSkin.getDefaultSkinLegacy();
    }

    public static boolean isLeapGUI(String title){
        return title.equals("Leap Menu");
    }

}