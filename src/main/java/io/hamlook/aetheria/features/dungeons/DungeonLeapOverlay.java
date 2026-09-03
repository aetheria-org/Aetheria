package io.hamlook.aetheria.features.dungeons;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.events.ASMGuiBackgroundDrawEvent;
import io.hamlook.aetheria.events.ASMGuiDrawPreEvent;
import io.hamlook.aetheria.events.ASMGuiOpenEvent;
import io.hamlook.aetheria.events.ASMMouseEvent;
import io.hamlook.aetheria.features.dungeons.overlays.DungeonMapOverlay;
import io.hamlook.aetheria.features.dungeons.overlays.map.DungeonPlayerTracker;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.ContainerUtils;
import io.hamlook.aetheria.utils.compat.ColoredBlockCompat;
import io.hamlook.aetheria.utils.compat.InventoryCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.MouseCompat;
import io.hamlook.aetheria.utils.render.NineSliceUtils;
import io.hamlook.aetheria.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@RegisterEvents
public class DungeonLeapOverlay {

    // ==========================================
    // CONSTANTS & CONFIGURATION
    // ==========================================
    private static final float MAP_SCALE = 2.0f;
    private static final float MAP_SIZE = 128.0f;
    private static final float BASE_HEAD_SIZE = 8.0f;

    private static final int GRID_BUTTON_WIDTH = 110;
    private static final int GRID_BUTTON_HEIGHT = 20;
    private static final int GRID_PADDING = 6;
    private static final int GRID_START_Y = 10;

    public static DungeonMapOverlay overlay;
    public static DungeonPlayerTracker tracker;
    public static ContainerChest leapChest;
    public static boolean isLeapGUI = false;

    public static void start(){
        overlay = DungeonMapOverlay.getInstance();
        if(overlay.isEnabled() && overlay.isLiveActive()) {
            tracker = DungeonMapOverlay.playerTracker;
        }
    }

    @HandleEvent
    public void onClose(ASMGuiOpenEvent e){
        if(e.gui == null) isLeapGUI = false;
    }

    @HandleEvent
    public void onGui(ASMGuiBackgroundDrawEvent event) {
        if (isLeapGUI) return;
        if (!(event.gui instanceof GuiContainer)) return;
        GuiContainer gui = (GuiContainer) event.gui;
        if (!(InventoryCompat.getContainer(gui) instanceof ContainerChest)) return;

        ContainerChest chest = (ContainerChest) InventoryCompat.getContainer(gui);
        String title = ContainerUtils.getTitle(chest);

        isLeapGUI = isLeapGUI(title);
        if (overlay == null) start();
        if (isLeapGUI) leapChest = chest;
    }

    @HandleEvent
    public void onDraw(ASMGuiDrawPreEvent e){
        if(isLeapGUI && e.gui != null && tracker != null){
            Aetheria.logger.fine("[DungeonLeap] Drawing leap overlay");
            e.cancel();
            drawLeapGUI(e.mouseX,e.mouseY,e.gui);
        }
    }

    private void drawLeapGUI(int mouseX, int mouseY, GuiScreen gui) {
        MapLayout mapLayout = getMapLayout(gui);

        GuiScreen.drawRect(
                (int) mapLayout.left,
                (int) mapLayout.top,
                (int) (mapLayout.left + (MAP_SIZE * MAP_SCALE)),
                (int) (mapLayout.top + (MAP_SIZE * MAP_SCALE)),
                0x78000000
        );

        overlay.renderDungeonMap(mapLayout.centerX, mapLayout.centerY, MAP_SCALE, true, true);

        Minecraft mc = MinecraftCompat.getMinecraft();
        List<String> players = ATHRConfig.feature.dungeons.leapConfig.excludeSelf ? getFilteredPartyMembers() : tracker.playerNames;
        for (String player : players) {
            float[] pos = tracker.getPosition(player);
            if (pos == null || pos.length < 3) continue;

            float posX = mapLayout.left + pos[0] * MAP_SCALE;
            float posY = mapLayout.top + pos[1] * MAP_SCALE;
            float yaw = pos[2];

            ResourceLocation skin = getPlayerSkin(player, mc);
            if (ATHRConfig.feature.dungeons.leapConfig.useArrowIcons) {
                RenderUtils.renderPlayerArrow(
                        posX - mapLayout.headPixelSize / 2f,
                        posY - mapLayout.headPixelSize / 2f,
                        MAP_SCALE,
                        yaw,
                        getArrowColor(player),
                        isSelf(player)
                );
            } else {
                RenderUtils.renderPlayerHead(
                        posX - mapLayout.headPixelSize / 2f,
                        posY - mapLayout.headPixelSize / 2f,
                        -1,
                        MAP_SCALE,
                        skin,
                        yaw
                );
            }
        }

        if (ATHRConfig.feature.dungeons.leapConfig.playerBList) {
            renderTopGridButtons(gui, mc, mouseX, mouseY);
        }
    }

    private void renderTopGridButtons(GuiScreen gui, Minecraft mc, int mouseX, int mouseY) {
        List<String> players = getFilteredPartyMembers();
        if (players.isEmpty()) return;

        GridLayout grid = getGridLayout(gui.width);

        for (int i = 0; i < players.size(); i++) {
            String player = players.get(i);
            Rectangle bounds = grid.getButtonBounds(i);

            boolean isHovered = mouseX >= bounds.x && mouseX <= bounds.x + bounds.width &&
                    mouseY >= bounds.y && mouseY <= bounds.y + bounds.height;

            int bgColor = isHovered ? 0xCC444444 : 0x00000000;

            NineSliceUtils.draw(
                    Resources.betterContainerNineSlice(ATHRConfig.feature.qol.betterContainers.style),
                    bounds.x, bounds.y, bounds.width, bounds.height, 3, 18
            );

            GuiScreen.drawRect(bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height, bgColor);

            ResourceLocation skin = getPlayerSkin(player, mc);
            RenderUtils.renderPlayerHead(bounds.x + 3, bounds.y + 3, -1, 2f, skin, 0);
            mc.fontRendererObj.drawStringWithShadow(player, bounds.x + 22, bounds.y + 6, 0xFFFFFFFF);
        }
    }


    @HandleEvent
    public void onMouseInput(ASMMouseEvent event) {
        if (!isLeapGUI || tracker == null) return;
        if (MouseCompat.getEventButton() == 0 && MouseCompat.getEventButtonState()) {

            int mouseX = MouseCompat.getEventX() * event.gui.width / event.gui.mc.displayWidth;
            int mouseY = event.gui.height - MouseCompat.getEventY() * event.gui.height / event.gui.mc.displayHeight - 1;

            List<String> players = getFilteredPartyMembers();

            // 1. Check Button Grid Clicks
            if (ATHRConfig.feature.dungeons.leapConfig.playerBList && !players.isEmpty()) {
                GridLayout grid = getGridLayout(event.gui.width);

                for (int i = 0; i < players.size(); i++) {
                    Rectangle bounds = grid.getButtonBounds(i);
                    if (bounds.contains(mouseX, mouseY)) {
                        leapToPlayer(players.get(i));
                        event.cancel();
                        return;
                    }
                }
            }

            // 2. Check Map Head Clicks
            if (ATHRConfig.feature.dungeons.leapConfig.clickablePlayers) {
                MapLayout mapLayout = getMapLayout(event.gui);

                for (String player : tracker.playerNames) {
                    float[] pos = tracker.getPosition(player);
                    if (pos == null || pos.length < 3) continue;

                    float posX = mapLayout.left + pos[0] * MAP_SCALE;
                    float posY = mapLayout.top + pos[1] * MAP_SCALE;
                    float yaw = pos[2];

                    if (checkIfClickedOptimized(posX, posY, yaw, mapLayout.halfHeadPixelSize, mouseX, mouseY)) {
                        leapToPlayer(player);
                        event.cancel();
                        break;
                    }
                }
            }
        }
    }

    private void leapToPlayer(String player) {
        if (!isLeapGUI || leapChest == null) return;
        Minecraft mc = MinecraftCompat.getMinecraft();
        if (mc.playerController == null) return;

        int upperChestSlots = leapChest.getLowerChestInventory().getSizeInventory();

        // Compile regex ONCE outside the slot-check loop
        Pattern pattern = Pattern.compile(".*\\b" + Pattern.quote(player) + "\\b.*");

        for (int slot = 0; slot < upperChestSlots; slot++) {
            Slot slot1 = leapChest.getSlot(slot);
            if (slot1 == null || !slot1.getHasStack()) continue;

            ItemStack stack = slot1.getStack();
            if (stack.getItem().getRegistryName().equals(ColoredBlockCompat.WHITE.createGlassPaneStack(1).getItem().getRegistryName())) continue;

            String displayName = ColorUtils.stripColor(stack.getDisplayName());

            if (pattern.matcher(displayName).matches()) {
                InventoryCompat.windowClick(leapChest.windowId, slot, 0, 0, mc.thePlayer);
                break;
            }
        }
    }

    private List<String> getFilteredPartyMembers() {
        List<String> players = new ArrayList<>(tracker.playerNames);
        EntityPlayer player = MinecraftCompat.getMinecraft().thePlayer;
        if (player != null) {
            players.remove(player.getGameProfile().getName());
        }
        return players;
    }

    private MapLayout getMapLayout(GuiScreen gui) {
        float centerX = gui.width / 2f;
        float centerY = (gui.height / 2f) + (MAP_SIZE / 2f);
        float left = centerX - (MAP_SIZE * MAP_SCALE) / 2f;
        float top = centerY - (MAP_SIZE * MAP_SCALE) / 2f;
        float headPixelSize = BASE_HEAD_SIZE * MAP_SCALE;
        return new MapLayout(centerX, centerY, left, top, headPixelSize, headPixelSize * 0.5f);
    }

    private GridLayout getGridLayout(int screenWidth) {
        int totalWidth = (GRID_BUTTON_WIDTH * 2) + GRID_PADDING;
        int startX = (screenWidth - totalWidth) / 2;
        return new GridLayout(startX, GRID_START_Y, GRID_BUTTON_WIDTH, GRID_BUTTON_HEIGHT, GRID_PADDING);
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
        return ColorUtils.stripColor(title).equals("Spirit Leap") && ATHRConfig.feature.dungeons.leapConfig.dungeonLeapOverlay;
    }


    private boolean isSelf(String player) {
        return MinecraftCompat.getMinecraft().thePlayer.getGameProfile().getName().equalsIgnoreCase(player);
    }

    private int getArrowColor(String player){
        if(isSelf(player)) return 0xFFFFFFFF;
        return 0xFF5AA5FF;
    }

    private static class MapLayout {
        final float centerX, centerY, left, top, headPixelSize, halfHeadPixelSize;

        MapLayout(float centerX, float centerY, float left, float top, float headPixelSize, float halfHeadPixelSize) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.left = left;
            this.top = top;
            this.headPixelSize = headPixelSize;
            this.halfHeadPixelSize = halfHeadPixelSize;
        }
    }

    private static class GridLayout {
        final int startX, startY, buttonWidth, buttonHeight, padding;

        GridLayout(int startX, int startY, int buttonWidth, int buttonHeight, int padding) {
            this.startX = startX;
            this.startY = startY;
            this.buttonWidth = buttonWidth;
            this.buttonHeight = buttonHeight;
            this.padding = padding;
        }

        Rectangle getButtonBounds(int index) {
            int row = index / 2;
            int col = index % 2;
            int x = startX + col * (buttonWidth + padding);
            int y = startY + row * (buttonHeight + padding);
            return new Rectangle(x, y, buttonWidth, buttonHeight);
        }
    }
}
