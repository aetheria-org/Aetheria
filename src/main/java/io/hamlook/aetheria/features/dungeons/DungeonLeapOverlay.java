package io.hamlook.aetheria.features.dungeons;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.dungeons.overlays.DungeonMapOverlay;
import io.hamlook.aetheria.features.dungeons.overlays.map.DungeonPlayerTracker;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.ContainerUtils;
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
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.util.List;
import java.util.regex.Pattern;

@RegisterEvents
public class DungeonLeapOverlay {

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
        Aetheria.logger.info("[DungeonLeap] Detected chest GUI title: " + title);
        isLeapGUI = isLeapGUI(title);
        if(overlay == null){
            start();
        }
        if(isLeapGUI){
            leapChest = chest;
            Aetheria.logger.info("[DungeonLeap] Leap GUI detected");
        }
    }

    @SubscribeEvent
    public void onDraw(GuiScreenEvent.DrawScreenEvent.Pre e){
        if(isLeapGUI && e.gui != null && tracker != null){
            Aetheria.logger.fine("[DungeonLeap] Drawing leap overlay");
            e.setCanceled(true);
            drawLeapGUI(e.mouseX,e.mouseY,e.gui);
        }
    }

    private void drawLeapGUI(int mouseX,int mouseY,GuiScreen gui) {
        float scale = 2f;
        float size = 128f;
        float centerX = (gui.width / 2f);
        float centerY = (gui.height / 2f) + (size/2f);
        float left = centerX - (size * scale) / 2f;
        float top = centerY - (size * scale) / 2f;
        float headPixelSize = 8f * scale;

        GuiScreen.drawRect((int) left, (int) top, (int) (left + (size*scale)), (int) (top+(size*scale)),new Color(0,0,0,120).getRGB());
        overlay.renderDungeonMap(centerX, centerY, scale, true, true);

        Minecraft mc = Minecraft.getMinecraft();

        for (String player : tracker.playerNames) {
            float[] pos = tracker.getPosition(player);
            if (pos == null || pos.length < 3) continue;
            float posX = left + pos[0] * scale;
            float posY = top + pos[1] * scale;
            float yaw = pos[2];

            ResourceLocation skin = getPlayerSkin(player, mc);
            if (ATHRConfig.feature.dungeons.leapConfig.useArrowIcons) {
                RenderUtils.renderPlayerArrow(posX - headPixelSize / 2f, posY - headPixelSize / 2f, scale, yaw, getArrowColor(player), isSelf(player));
            } else {
                RenderUtils.renderPlayerHead(posX - headPixelSize / 2f, posY - headPixelSize / 2f, -1, scale, skin, yaw);
            }
        }
        if(ATHRConfig.feature.dungeons.leapConfig.playerBList) renderTopGridButtons(gui, mc, mouseX, mouseY);
    }

    private boolean isSelf(String player) {
        return Minecraft.getMinecraft().thePlayer.getGameProfile().getName().equalsIgnoreCase(player);
    }

    private int getArrowColor(String player){
        if(isSelf(player)) return 0xFFFFFFFF;
        return 0xFF5AA5FF;
    }

    private void renderTopGridButtons(GuiScreen gui, Minecraft mc, int mouseX, int mouseY) {
        List<String> players = tracker.playerNames;
        players.remove(Minecraft.getMinecraft().thePlayer.getGameProfile().getName());
        if (players.isEmpty()) return;

        int buttonWidth = 110;
        int buttonHeight = 20;
        int padding = 6;

        int startY = 10;
        int totalWidth = (buttonWidth * 2) + padding;
        int startX = (gui.width - totalWidth) / 2;

        for (int i = 0; i < players.size(); i++) {
            String player = players.get(i);
            int row = i / 2;
            int col = i % 2;

            int x = startX + col * (buttonWidth + padding);
            int y = startY + row * (buttonHeight + padding);

            boolean isHovered = mouseX >= x && mouseX <= x + buttonWidth && mouseY >= y && mouseY <= y + buttonHeight;

            int bgColor = isHovered ? 0xCC444444 : 0x00000000;
            NineSliceUtils.draw(
                    Resources.betterContainerNineSlice(ATHRConfig.feature.qol.betterContainers.style),
                    x,y,buttonWidth,buttonHeight,3,18
            );
            GuiScreen.drawRect(x,y,x+buttonWidth,y+buttonHeight,bgColor);

            ResourceLocation skin = getPlayerSkin(player, mc);
            RenderUtils.renderPlayerHead(x + 3, y + 3, -1, 2f, skin, 0);

            mc.fontRendererObj.drawStringWithShadow(player, x + 22, y + 6, 0xFFFFFFFF);
        }
    }

    private void leapToPlayer(String player) {
        if(!isLeapGUI || leapChest == null) return;
        int upperChestSlots = leapChest.getLowerChestInventory().getSizeInventory();
        Minecraft mc = Minecraft.getMinecraft();
        for (int slot = 0; slot < upperChestSlots; slot++) {
            Slot slot1 = leapChest.getSlot(slot);
            if (slot1 == null || !slot1.getHasStack()) continue;

            ItemStack stack = slot1.getStack();
            if (stack.getItem().getRegistryName().equals(Item.getItemFromBlock(Blocks.stained_glass_pane).getRegistryName())) continue;

            String displayName = ColorUtils.stripColor(stack.getDisplayName());
            Pattern pattern = Pattern.compile(".*\\b" + Pattern.quote(player) + "\\b.*");

            if (pattern.matcher(displayName).matches()) {
                Aetheria.logger.info("[DungeonLeap] Clicking slot " + slot + " for player " + player);
                mc.playerController.windowClick(leapChest.windowId,slot,0,0,mc.thePlayer);
                break;
            }
        }
    }

    @SubscribeEvent
    public void onMouseInput(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!isLeapGUI || tracker == null) return;
        if (Mouse.getEventButton() == 0 && Mouse.getEventButtonState()) {
            int mouseX = Mouse.getEventX() * event.gui.width / event.gui.mc.displayWidth;
            int mouseY = event.gui.height - Mouse.getEventY() * event.gui.height / event.gui.mc.displayHeight - 1;


            List<String> players = tracker.playerNames;
            players.remove(Minecraft.getMinecraft().thePlayer.getGameProfile().getName());

            if (ATHRConfig.feature.dungeons.leapConfig.playerBList) {
                int buttonWidth = 110;
                int buttonHeight = 20;
                int padding = 6;
                int startY = 10;
                int totalWidth = (buttonWidth * 2) + padding;
                int startX = (event.gui.width - totalWidth) / 2;

                for (int i = 0; i < players.size(); i++) {
                    String player = players.get(i);
                    int row = i / 2;
                    int col = i % 2;

                    int x = startX + col * (buttonWidth + padding);
                    int y = startY + row * (buttonHeight + padding);

                    if (mouseX >= x && mouseX <= x + buttonWidth && mouseY >= y && mouseY <= y + buttonHeight) {
                        leapToPlayer(player);
                        event.setCanceled(true);
                        return;
                    }
                }
            }

            if (ATHRConfig.feature.dungeons.leapConfig.clickablePlayers) {
                float scale = 2f;
                float size = 128f;
                float centerX = event.gui.width / 2f;
                float centerY = (event.gui.height / 2f) + (size / 2f);
                float left = centerX - (size * scale) / 2f;
                float top = centerY - (size * scale) / 2f;
                float headPixelSize = 8f * scale;
                float halfHeadPixelSize = headPixelSize * 0.5f;

                for (String player : tracker.playerNames) {
                    float[] pos = tracker.getPosition(player);
                    if (pos == null || pos.length < 3) continue;
                    float posX = left + pos[0] * scale;
                    float posY = top + pos[1] * scale;
                    float yaw = pos[2];

                    if (checkIfClickedOptimized(posX, posY, yaw, halfHeadPixelSize, mouseX, mouseY)) {
                        leapToPlayer(player);
                        event.setCanceled(true);
                        break;
                    }
                }
            }
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
        return ColorUtils.stripColor(title).equals("Spirit Leap") && ATHRConfig.feature.dungeons.leapConfig.dungeonLeapOverlay;
    }

}
